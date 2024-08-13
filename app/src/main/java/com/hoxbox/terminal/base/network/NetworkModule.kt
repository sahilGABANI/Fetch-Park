package com.hoxbox.terminal.base.network

import android.content.Context
//import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.google.gson.GsonBuilder
import com.hoxbox.terminal.BuildConfig
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.base.extension.getAPIBaseUrl
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


@Module
class NetworkModule {

    @Provides
    @Singleton
    fun provideWristBandInterceptorHeaders(loggedInUserCache: LoggedInUserCache): HotboxInterceptorHeaders {
        return HotboxInterceptorHeaders(loggedInUserCache)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        context: Context,
        hotboxInterceptorHeaders: HotboxInterceptorHeaders
    ): OkHttpClient {
        val cacheSize = 10 * 1024 * 1024 // 10 MiB
        val cacheDir = File(context.cacheDir, "HttpCache")
        val cache = Cache(cacheDir, cacheSize.toLong())
        val builder = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(240, TimeUnit.SECONDS)
            .cache(cache)
//            .addInterceptor(ChuckerInterceptor(context))
            .addInterceptor(hotboxInterceptorHeaders)
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(loggingInterceptor)
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(getAPIBaseUrl())
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