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
import com.vknxworks.navictrl.data.lock.LockVault
import com.vknxworks.navictrl.data.net.CleartextPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

/**
 * Saved Navidrome accounts and the active one. Passwords never leave the Keystore-encrypted store and,
 * with the app lock on, are also sealed with the app lock's key, so they can only be read while it's unlocked.
 */
class SessionManager(
    private val context: Context,
    private val http: OkHttpClient,
    private val vault: LockVault,
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

    /** The saved accounts have been read (their passwords may still be sealed while the app is locked). */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val mutex = Mutex()

    /** The app is locked (only ever true with the app lock on). */
    @Volatile var isLocked = false
        private set

    /**
     * Set by the app: whether music is playing (or paused with a queue) or downloads are running.
     * Those keep the signed-in session in memory while the app is locked, so playback, media
     * controls, scrobbling and downloads carry on. Everything else drops it until the next unlock.
     */
    @Volatile var mediaBusy: () -> Boolean = { false }

    init {
        scope.launch { load() }
    }

    private suspend fun load() = mutex.withLock {
        val prefs = context.appDataStore.data.first()
        val list = prefs[accountsKey]?.let {
            runCatching { ApiJson.decodeFromString(ListSerializer(Account.serializer()), it) }.getOrNull()
        }.orEmpty()
        _accounts.value = list
        _loaded.value = true
        // With the app lock on, the password can only be decrypted after unlocking; onUnlocked() continues.
        if (vault.enabled && !vault.isOpen) {
            isLocked = true
            return@withLock
        }
        activateSaved()
        _ready.value = true
    }

    private suspend fun activateSaved() {
        val activeKeyValue = context.appDataStore.data.first()[activeKey]
        val list = _accounts.value
        val acc = list.firstOrNull { it.key == activeKeyValue } ?: list.firstOrNull()
        if (acc != null) activate(acc) else _active.value = null
    }

    /** The password of [acc], or null while it's sealed by the app lock and the app is locked. */
    private fun passwordOf(acc: Account): String? {
        val plain = KeystoreCrypto.decrypt(acc.passwordEnc) ?: return null
        if (!LockVault.isSealed(plain)) return plain
        return vault.open(plain)
    }

    /** Encrypts a password for saving: with the Keystore, and also with the app lock's key when it's on. */
    private fun storePassword(password: String, seal: Boolean = vault.enabled): String {
        val inner = if (seal) vault.seal(password) ?: throw ApiException("Unlock NaviCtrl first.") else password
        return KeystoreCrypto.encrypt(inner)
    }

    /** The app locked: forget the session and its password until the next unlock, unless music or downloads need it. */
    fun onLocked() {
        if (!vault.enabled) return
        isLocked = true
        if (!mediaBusy()) dropSession()
    }

    /** Music or the downloads stopped: if the app is locked, nothing needs the session any more. */
    fun mediaStopped() {
        if (isLocked && vault.enabled && !mediaBusy()) dropSession()
    }

    private fun dropSession() {
        scope.launch {
            mutex.withLock {
                if (!isLocked || mediaBusy()) return@withLock
                _ready.value = false
                _active.value = null
            }
        }
    }

    /** The app unlocked: decrypt the password and open the session again. */
    fun onUnlocked() {
        isLocked = false
        scope.launch {
            mutex.withLock {
                if (!_loaded.value) return@withLock // load() is still running and will activate it
                if (_active.value == null) activateSaved()
                _ready.value = true
            }
        }
    }

    /** The app lock was turned on: seal every saved password with its key too. */
    suspend fun sealPasswords() = reencrypt(seal = true)

    /** The app lock is being turned off (while unlocked): back to Keystore-only encryption. */
    suspend fun unsealPasswords() = reencrypt(seal = false)

    private suspend fun reencrypt(seal: Boolean) = mutex.withLock {
        val list = _accounts.value.map { acc ->
            val password = passwordOf(acc) ?: return@map acc
            acc.copy(passwordEnc = storePassword(password, seal))
        }
        persist(list, context.appDataStore.data.first()[activeKey])
        _active.value?.let { s -> list.firstOrNull { it.key == s.account.key }?.let { _active.value = s.copy(account = it) } }
    }

    /** "Forgot PIN?": the passwords can't be decrypted any more, so forget every account. */
    suspend fun forgetAllAccounts() = mutex.withLock {
        _active.value = null
        persist(emptyList(), null)
        isLocked = false
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
                passwordEnc = storePassword(password),
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
        val password = passwordOf(acc) ?: return null
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
