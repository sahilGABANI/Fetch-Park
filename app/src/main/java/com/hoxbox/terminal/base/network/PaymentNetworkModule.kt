package com.hoxbox.terminal.base.network

import android.content.Context
import com.google.gson.GsonBuilder
import com.hoxbox.terminal.BuildConfig
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton


@Module
class PaymentNetworkModule {

    @Provides
    @Singleton
    fun providePaymentInterceptorHeaders(loggedInUserCache: LoggedInUserCache): PaymentInterceptorHeaders {
        return PaymentInterceptorHeaders(loggedInUserCache)
    }

    @Provides
    @Singleton
    @Named("PaymentOkHttpClient")
    fun providePaymentOkHttpClient(
        context: Context,
        paymentInterceptorHeaders: PaymentInterceptorHeaders
    ): OkHttpClient {
        val cacheSize = 10 * 1024 * 1024 // 10 MiB
        val cacheDir = File(context.cacheDir, "HttpCache")
        val cache = Cache(cacheDir, cacheSize.toLong())
        val builder = OkHttpClient.Builder()
            .readTimeout(300, TimeUnit.SECONDS)
            .connectTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .cache(cache)
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(loggingInterceptor)
        }
        return builder.build()
    }

    @Provides
    @Singleton
    @Named("PaymentRetrofit")
    fun provideRetrofit(@Named("PaymentOkHttpClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://secure.nmi.com/")
            .client(okHttpClient)
            .addCallAdapterFactory(RxErrorHandlingCallAdapterFactory.createAsync()) // Using create async means all api calls are automatically created asynchronously using OkHttp's thread pool
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder()
                        .setDateFormat("yyyy-MM-dd HH:mm:ss")
                        .enableComplexMapKeySerialization()
                        .create()
                )
            )
            .build()
    }
}