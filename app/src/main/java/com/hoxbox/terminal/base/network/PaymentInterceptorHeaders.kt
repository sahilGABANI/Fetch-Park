package com.hoxbox.terminal.base.network

import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

class PaymentInterceptorHeaders(
    private val loggedInUserCache: LoggedInUserCache
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val requestBuilder = original.newBuilder()
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