package com.vknxworks.navictrl.data.auth

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vknxworks.navictrl.data.api.ApiException
import com.vknxworks.navictrl.data.api.ApiJson
import com.vknxworks.navictrl.data.api.SubsonicClient
import com.vknxworks.navictrl.data.api.ping
import com.vknxworks.navictrl.data.api.serverInfo
import com.vknxworks.navictrl.data.api.user
import com.vknxworks.navictrl.data.appDataStore
import com.vknxworks.navictrl.data.net.CleartextPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.OkHttpClient

@Serializable
data class Account(
    val serverUrl: String,
    val username: String,
    /** Password encrypted with the Keystore. Needed to sign Subsonic requests and Navidrome sessions. */
    val passwordEnc: String,
    /** No longer used: auth salts are now fresh per request / per session. Kept so old saved accounts still load. */
    val salt: String = "",
    val serverVersion: String? = null,
    val isAdmin: Boolean = false,
) {
    val key: String get() = "$serverUrl|$username"
    val serverName: String get() = Uri.parse(serverUrl).host ?: serverUrl
    val isEncrypted: Boolean get() = serverUrl.startsWith("https://", ignoreCase = true)
}

/**
 * Thrown when HTTPS couldn't be reached for an address typed without a scheme. The UI asks the user
 * whether to try plain HTTP (which sends the password unencrypted) and calls signIn again with [httpUrl].
 */
class HttpFallbackNeeded(val httpUrl: String, cause: Throwable) :
    Exception("Couldn't connect securely (HTTPS). The server may only support plain HTTP.", cause)

data class ActiveSession(val account: Account, val client: SubsonicClient)

/** Saved Navidrome accounts and the active one. Passwords never leave the Keystore-encrypted store. */
class SessionManager(
    private val context: Context,
    private val http: OkHttpClient,
    private val scope: CoroutineScope,
) {
    private val accountsKey = stringPreferencesKey("accounts")
    private val activeKey = stringPreferencesKey("active_account")

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _active = MutableStateFlow<ActiveSession?>(null)
    val active: StateFlow<ActiveSession?> = _active.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    init {
        scope.launch { load() }
    }

    private suspend fun load() {
        val prefs = context.appDataStore.data.first()
        val list = prefs[accountsKey]?.let {
            runCatching { ApiJson.decodeFromString(ListSerializer(Account.serializer()), it) }.getOrNull()
        }.orEmpty()
        _accounts.value = list
        val acc = list.firstOrNull { it.key == prefs[activeKey] } ?: list.firstOrNull()
        if (acc != null) activate(acc)
        _ready.value = true
    }

    /**
     * Checks the address and credentials, then saves and switches to the account.
     * An address without a scheme is tried over HTTPS only. If that fails with a connection error
     * (not a TLS/certificate error), [HttpFallbackNeeded] is thrown so the UI can ask before using
     * plain HTTP. An explicit http:// address is used as typed (the sign-in screen warns about it).
     */
    suspend fun signIn(address: String, username: String, password: String): Account {
        val raw = address.trim()
        val explicit = raw.startsWith("http://", true) || raw.startsWith("https://", true)
        val url = if (explicit) raw else "https://$raw"
        val client = SubsonicClient(url, username.trim(), password, http)
        // Only reached for an http:// address the user typed or confirmed in the "unencrypted" dialog.
        CleartextPolicy.allowServer(client.baseUrl)
        try {
            val info = client.ping().serverInfo(client.baseUrl)
            if (info.type != null && !info.type.equals("navidrome", ignoreCase = true)) {
                throw ApiException("This is a ${info.type} server. NaviCtrl works with Navidrome.")
            }
            // Navidrome's own API powers the sortable song list; signing in to it also confirms admin rights.
            val isAdmin = runCatching { client.nativeLogin().isAdmin }
                .recoverCatching { client.user().adminRole }
                .getOrDefault(false)
            val account = Account(
                serverUrl = client.baseUrl,
                username = username.trim(),
                passwordEnc = KeystoreCrypto.encrypt(password),
                serverVersion = info.serverVersion,
                isAdmin = isAdmin,
            )
            val updated = _accounts.value.filterNot { it.key == account.key } + account
            persist(updated, account.key)
            activate(account, client)
            return account
        } catch (e: ApiException) {
            // A real answer from the server (wrong password, not Navidrome).
            throw e
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: java.net.UnknownHostException) {
            throw e
        } catch (e: java.io.IOException) {
            // TLS and certificate failures never lead to HTTP: the server (or someone in between)
            // answered on the HTTPS port. Plain connection failures may offer HTTP, but only behind
            // the explicit "unencrypted" confirmation.
            if (explicit) throw e
            if (e.isTlsProblem()) {
                throw ApiException(
                    "Secure connection failed: ${e.message ?: "TLS error"}. " +
                        "If your server only supports plain HTTP, type http:// in front of the address.",
                )
            }
            throw HttpFallbackNeeded("http://$raw", e)
        }
    }

    private fun Throwable.isTlsProblem(): Boolean {
        var t: Throwable? = this
        while (t != null) {
            if (t is javax.net.ssl.SSLException ||
                t is java.security.cert.CertificateException ||
                t is java.security.cert.CertPathValidatorException
            ) return true
            t = t.cause
        }
        return false
    }

    suspend fun switchTo(key: String) {
        val acc = _accounts.value.firstOrNull { it.key == key } ?: return
        context.appDataStore.edit { it[activeKey] = key }
        activate(acc)
    }

    suspend fun signOut(key: String) {
        val remaining = _accounts.value.filterNot { it.key == key }
        val nextActive = if (_active.value?.account?.key == key) remaining.firstOrNull() else _active.value?.account
        persist(remaining, nextActive?.key)
        if (nextActive != null) activate(nextActive) else _active.value = null
    }

    fun updateAdmin(isAdmin: Boolean) {
        val acc = _active.value?.account ?: return
        if (acc.isAdmin == isAdmin) return
        val updated = acc.copy(isAdmin = isAdmin)
        _active.value = _active.value?.copy(account = updated)
        scope.launch { persist(_accounts.value.map { if (it.key == acc.key) updated else it }, acc.key) }
    }

    private fun clientFor(acc: Account): SubsonicClient? {
        val password = KeystoreCrypto.decrypt(acc.passwordEnc) ?: return null
        // Reconnects always use the saved address and scheme; never switch an https account to http.
        CleartextPolicy.allowServer(acc.serverUrl)
        return SubsonicClient(acc.serverUrl, acc.username, password, http)
    }

    private fun activate(acc: Account, client: SubsonicClient? = null) {
        val c = client ?: clientFor(acc)
        _active.value = c?.let { ActiveSession(acc, it) }
    }

    private suspend fun persist(list: List<Account>, activeKeyValue: String?) {
        _accounts.value = list
        context.appDataStore.edit { prefs ->
            prefs[accountsKey] = ApiJson.encodeToString(ListSerializer(Account.serializer()), list)
            if (activeKeyValue != null) prefs[activeKey] = activeKeyValue else prefs.remove(activeKey)
        }
    }
}
