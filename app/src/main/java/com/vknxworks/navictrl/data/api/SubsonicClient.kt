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
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.security.SecureRandom
import com.vknxworks.navictrl.R
import com.vknxworks.navictrl.str

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
    fun restUrl(endpoint: String, params: List<Pair<String, Any?>> = emptyList(), freshAuth: Boolean = false, client: String = CLIENT): String {
        val salt = if (freshAuth) newSalt() else mediaSalt
        val token = if (freshAuth) md5(password + salt) else mediaToken
        val b = base.newBuilder()
            .addPathSegment("rest")
            .addPathSegment(endpoint)
            .addQueryParameter("u", username)
            .addQueryParameter("t", token)
            .addQueryParameter("s", salt)
            .addQueryParameter("v", API_VERSION)
            .addQueryParameter("c", client)
            .addQueryParameter("f", "json")
        params.forEach { (k, v) -> if (v != null) b.addQueryParameter(k, v.toString()) }
        return b.build().toString()
    }

    /** Calls an endpoint and returns the inner subsonic-response object. */
    suspend fun call(endpoint: String, params: List<Pair<String, Any?>> = emptyList(), client: String = CLIENT): JsonObject = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(restUrl(endpoint, params, freshAuth = true, client = client)).get().build()
        http.newCall(req).execute().use { resp -> parse(resp) }
    }

    /** Whether the server takes parameters in a POST body (OpenSubsonic formPost); null until asked. */
    @Volatile var formPost: Boolean? = null

    /** Like [call], but every parameter (auth included) goes in a form-encoded POST body (OpenSubsonic formPost). */
    suspend fun callForm(endpoint: String, params: List<Pair<String, Any?>>, client: String = CLIENT): JsonObject = withContext(Dispatchers.IO) {
        val salt = newSalt()
        val form = FormBody.Builder()
            .add("u", username)
            .add("t", md5(password + salt))
            .add("s", salt)
            .add("v", API_VERSION)
            .add("c", client)
            .add("f", "json")
        params.forEach { (k, v) -> if (v != null) form.add(k, v.toString()) }
        val url = base.newBuilder().addPathSegment("rest").addPathSegment(endpoint).build()
        val req = Request.Builder().url(url).post(form.build()).build()
        http.newCall(req).execute().use { resp -> parse(resp) }
    }

    private fun parse(resp: Response): JsonObject {
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful && body.isBlank()) {
            throw ApiException(
                when (resp.code) {
                    401, 403 -> str(R.string.the_server_refused_the_sign_in)
                    404 -> str(R.string.no_navidrome_server_at_this_address)
                    else -> str(R.string.server_error_x, resp.code)
                },
                resp.code,
            )
        }
        val root = runCatching { ApiJson.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: throw ApiException(if (resp.isSuccessful) str(R.string.this_doesn_t_look_like_a) else str(R.string.server_error_x, resp.code))
        val r = root["subsonic-response"] as? JsonObject ?: throw ApiException(str(R.string.this_doesn_t_look_like_a))
        if ((r["status"] as? JsonPrimitive)?.contentOrNull != "ok") {
            val err = r["error"] as? JsonObject
            val code = (err?.get("code") as? JsonPrimitive)?.intOrNull ?: 0
            val msg = (err?.get("message") as? JsonPrimitive)?.contentOrNull
            throw ApiException(
                when (code) {
                    40, 41 -> str(R.string.wrong_username_or_password)
                    50 -> str(R.string.your_account_isn_t_allowed_to)
                    70 -> msg ?: str(R.string.not_found)
                    else -> msg ?: str(R.string.server_error)
                },
                code,
            )
        }
        return r
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
            if (resp.code == 401) throw ApiException(str(R.string.wrong_username_or_password), 401)
            if (!resp.isSuccessful) throw ApiException(str(R.string.sign_in_failed_x, resp.code), resp.code)
            val login = ApiJson.decodeFromString(NativeLogin.serializer(), resp.body?.string().orEmpty())
            jwt = login.token ?: throw ApiException(str(R.string.the_server_returned_no_session))
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
                    !resp.isSuccessful -> throw ApiException(str(R.string.server_error_x, resp.code), resp.code)
                    else -> resp.body?.string().orEmpty() to resp.header("X-Total-Count")?.toIntOrNull()
                }
            }
        }
        if (result != null) return result
        if (!retry) throw ApiException(str(R.string.the_server_refused_the_sign_in), 401)
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
