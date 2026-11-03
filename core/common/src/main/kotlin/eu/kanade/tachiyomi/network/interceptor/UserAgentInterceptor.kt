package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor(
    private val defaultUserAgentProvider: () -> String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val host = originalRequest.url.host

        return if (host.contains("mangadex.org", ignoreCase = true)) {
            val newRequest = originalRequest
                .newBuilder()
                .header("User-Agent", "YomuDoom/1.0 (https://github.com/YomuDoom/YomuDoom)")
                .build()
            chain.proceed(newRequest)
        } else if (originalRequest.header("User-Agent").isNullOrEmpty()) {
            val newRequest = originalRequest
                .newBuilder()
                .header("User-Agent", defaultUserAgentProvider())
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
