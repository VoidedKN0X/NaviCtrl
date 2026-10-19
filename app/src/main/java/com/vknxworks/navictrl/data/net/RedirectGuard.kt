package com.vknxworks.navictrl.data.net

import okhttp3.HttpUrl
import okhttp3.Interceptor
import java.io.IOException

/**
 * OkHttp follows redirects on its own. It drops the standard Authorization header when a redirect
 * leaves the host, but not Navidrome's X-ND-Authorization session header, and a 307/308 redirect
 * sends the request body again (the sign-in request carries the password). Installed as a network
 * interceptor, this sees every hop: a hop to another server loses the session headers, and a
 * request with a body is never passed on to another server at all, and the Subsonic sign-in
 * parameters are taken out of the address. Moving the same host from HTTP to HTTPS counts as the
 * same server.
 */
object RedirectGuard {
    private val SENSITIVE_HEADERS = listOf("X-ND-Authorization", "Authorization", "Cookie", "Proxy-Authorization")

    private fun sameServer(hop: HttpUrl, original: HttpUrl): Boolean {
        if (!hop.host.equals(original.host, ignoreCase = true)) return false
        if (hop.scheme == original.scheme) return hop.port == original.port
        return !original.isHttps && hop.isHttps
    }

    val interceptor = Interceptor { chain ->
        val request = chain.request()
        val original = chain.call().request()
        if (sameServer(request.url, original.url)) return@Interceptor chain.proceed(request)
        if (request.body != null) {
            throw IOException("Refused to send this request on to another server (${request.url.host})")
        }
        // Subsonic signs every request in its address (u = user, t = md5(password + salt), s = salt), and
        // redirects often copy the query along ("https://new.host$request_uri"). Another server must not
        // get them: with a token and its salt it could sign in as you, or try to guess the password.
        // (Only for calls the app signed itself; other addresses are left as they are.)
        val signed = original.url.queryParameter("u") != null || original.url.queryParameter("apiKey") != null ||
            original.url.queryParameter("jwt") != null
        val url = if (!signed) request.url else request.url.newBuilder().apply { AUTH_QUERY.forEach { removeAllQueryParameters(it) } }.build()
        val stripped = request.newBuilder()
            .url(url)
            .apply { SENSITIVE_HEADERS.forEach { removeHeader(it) } }
            .build()
        chain.proceed(stripped)
    }

    /** Query parameters that sign a Subsonic/OpenSubsonic request. */
    private val AUTH_QUERY = listOf("u", "t", "s", "p", "jwt", "apiKey")
}
