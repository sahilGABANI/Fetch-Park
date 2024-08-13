package com.hoxbox.terminal.base.network

import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

class HotboxInterceptorHeaders(
    private val loggedInUserCache: LoggedInUserCache
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val requestBuilder = original.newBuilder()
        requestBuilder.header("Content-Type", "application/json")
        requestBuilder.header("Accept", "application/json")
        val token = loggedInUserCache.getLoginUserToken() ?: ""
        if (token.isNotEmpty()) {
            // authenticated user
            Timber.tag("OkHttpClient").i("token :$token")
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val response: Response
        try {
            response = chain.proceed(requestBuilder.build())
        } catch (t: Throwable) {
            Timber.e("error in InterceptorHeaders:\n${t.message}")
            throw IOException(t.message)
        }
        return response
    }
}