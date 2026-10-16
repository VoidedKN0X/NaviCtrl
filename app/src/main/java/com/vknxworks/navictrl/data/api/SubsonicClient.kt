package com.vknxworks.navictrl.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Talks to a Navidrome server. Almost everything goes through the Subsonic API with token auth
 * (md5 of password + salt). API calls get a fresh salt every time. Artwork and stream URLs use a
 * salt that lives only as long as this client (one app session), so they stay stable on screen;
 * the image and playback caches key them without the auth parameters (see [authFreeKey]).
 * The all-songs list uses Navidrome's own API, which can sort.
 */
class SubsonicClient(
    baseUrl: String,
    val username: String,
    private val password: String,
    private val http: OkHttpClient,
) {
    val baseUrl: String = normalizeBaseUrl(baseUrl)
    private val mediaSalt = newSalt()
    private val mediaToken = md5(password + mediaSalt)
    private val base: HttpUrl = this.baseUrl.toHttpUrl()

    /**
     * Builds a /rest URL with auth. Used for API calls, artwork and streams.
     * [freshAuth] signs it with a new one-off salt; otherwise the per-session media salt is used.
     */
    fun restUrl(endpoint: String, params: List<Pair<String, Any?>> = emptyList(), freshAuth: Boolean = false): String {
        val salt = if (freshAuth) newSalt() else mediaSalt
        val token = if (freshAuth) md5(password + salt) else mediaToken
        val b = base.newBuilder()
            .addPathSegment("rest")
            .addPathSegment(endpoint)
            .addQueryParameter("u", username)
            .addQueryParameter("t", token)
            .addQueryParameter("s", salt)
            .addQueryParameter("v", API_VERSION)
            .addQueryParameter("c", CLIENT)
            .addQueryParameter("f", "json")
        params.forEach { (k, v) -> if (v != null) b.addQueryParameter(k, v.toString()) }
        return b.build().toString()
    }

    /** Calls an endpoint and returns the inner subsonic-response object. */
    suspend fun call(endpoint: String, params: List<Pair<String, Any?>> = emptyList()): JsonObject = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(restUrl(endpoint, params, freshAuth = true)).get().build()
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful && body.isBlank()) {
                throw ApiException(
                    when (resp.code) {
                        401, 403 -> "The server refused the sign-in"
                        404 -> "No Navidrome server at this address"
                        else -> "Server error ${resp.code}"
                    },
                    resp.code,
                )
            }
            val root = runCatching { ApiJson.parseToJsonElement(body).jsonObject }.getOrNull()
                ?: throw ApiException(if (resp.isSuccessful) "This doesn't look like a Navidrome server" else "Server error ${resp.code}")
            val r = root["subsonic-response"] as? JsonObject ?: throw ApiException("This doesn't look like a Navidrome server")
            if ((r["status"] as? JsonPrimitive)?.contentOrNull != "ok") {
                val err = r["error"] as? JsonObject
                val code = (err?.get("code") as? JsonPrimitive)?.intOrNull ?: 0
                val msg = (err?.get("message") as? JsonPrimitive)?.contentOrNull
                throw ApiException(
                    when (code) {
                        40, 41 -> "Wrong username or password"
                        50 -> "Your account isn't allowed to do that"
                        70 -> msg ?: "Not found"
                        else -> msg ?: "Server error"
                    },
                    code,
                )
            }
            r
        }
    }

    suspend fun <T> get(endpoint: String, key: String, serializer: KSerializer<T>, params: List<Pair<String, Any?>> = emptyList()): T {
        val r = call(endpoint, params)
        val el: JsonElement = r[key] ?: JsonObject(emptyMap())
        return ApiJson.decodeFromJsonElement(serializer, el)
    }

    // ---------- Navidrome native API ----------

    private var jwt: String? = null
    private val loginLock = Mutex()

    suspend fun nativeLogin(): NativeLogin = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("username", username)
            put("password", password)
        }.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(base.newBuilder().addPathSegments("auth/login").build()).post(body).build()
        http.newCall(req).execute().use { resp ->
            if (resp.code == 401) throw ApiException("Wrong username or password", 401)
            if (!resp.isSuccessful) throw ApiException("Sign-in failed (${resp.code})", resp.code)
            val login = ApiJson.decodeFromString(NativeLogin.serializer(), resp.body?.string().orEmpty())
            jwt = login.token ?: throw ApiException("The server returned no session")
            login
        }
    }

    private suspend fun ensureJwt(): String = loginLock.withLock { jwt ?: nativeLogin().token!! }

    /** GET on Navidrome's own API. Returns the body and the X-Total-Count header. */
    private suspend fun nativeGet(path: String, params: List<Pair<String, Any?>>, retry: Boolean = true): Pair<String, Int?> {
        val token = ensureJwt()
        val result = withContext(Dispatchers.IO) {
            val url = base.newBuilder().addPathSegments(path).apply {
                params.forEach { (k, v) -> if (v != null) addQueryParameter(k, v.toString()) }
            }.build()
            val req = Request.Builder().url(url).header("X-ND-Authorization", "Bearer $token").get().build()
            http.newCall(req).execute().use { resp ->
                resp.header("X-ND-Authorization")?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }?.let { jwt = it }
                when {
                    resp.code == 401 -> null
                    !resp.isSuccessful -> throw ApiException("Server error ${resp.code}", resp.code)
                    else -> resp.body?.string().orEmpty() to resp.header("X-Total-Count")?.toIntOrNull()
                }
            }
        }
        if (result != null) return result
        if (!retry) throw ApiException("The server refused the sign-in", 401)
        loginLock.withLock { jwt = null }
        return nativeGet(path, params, retry = false)
    }

    /** One page of every song in the library, sorted on the server. */
    suspend fun songsPage(start: Int, count: Int, sort: SongSort, ascending: Boolean): Pair<List<Song>, Int> {
        val (body, total) = nativeGet(
            "api/song",
            listOf("_start" to start, "_end" to start + count, "_sort" to sort.field, "_order" to if (ascending) "ASC" else "DESC"),
        )
        val songs = ApiJson.decodeFromString(ListSerializer(NativeSong.serializer()), body).map { it.toSong() }
        return songs to (total ?: (start + songs.size))
    }

    companion object {
        const val API_VERSION = "1.16.1"
        const val CLIENT = "NaviCtrl"

        fun normalizeBaseUrl(raw: String): String {
            var s = raw.trim().trimEnd('/')
            if (!s.startsWith("http://", true) && !s.startsWith("https://", true)) s = "https://$s"
            // People sometimes paste a web UI link; the API lives at the root of the server path.
            s = s.removeSuffix("/app").removeSuffix("/app/#").removeSuffix("/rest")
            return s.trimEnd('/')
        }

        fun md5(s: String): String =
            MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

        fun sha256(s: String): String =
            MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

        private val random = SecureRandom()

        fun newSalt(): String {
            val bytes = ByteArray(12).also { random.nextBytes(it) }
            return bytes.joinToString("") { "%02x".format(it) }
        }

        /** Auth parameters that change between sessions; left out of cache keys. */
        private val AUTH_PARAMS = listOf("u", "t", "s", "p")

        /** A stable cache key for an artwork or stream URL: the URL without its auth parameters. */
        fun authFreeKey(url: String): String {
            val parsed = url.toHttpUrlOrNull() ?: return url
            val b = parsed.newBuilder()
            AUTH_PARAMS.forEach { b.removeAllQueryParameters(it) }
            return b.build().toString()
        }
    }
}

/** Reads server type and version from any subsonic-response. */
fun JsonObject.serverInfo(url: String) = ServerInfo(
    url = url,
    type = (this["type"] as? JsonPrimitive)?.contentOrNull,
    serverVersion = (this["serverVersion"] as? JsonPrimitive)?.contentOrNull,
    apiVersion = (this["version"] as? JsonPrimitive)?.contentOrNull,
    openSubsonic = (this["openSubsonic"] as? JsonPrimitive)?.booleanOrNull == true,
)
