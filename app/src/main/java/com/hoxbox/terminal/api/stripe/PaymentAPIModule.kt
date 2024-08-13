package com.hoxbox.terminal.api.stripe

import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
class PaymentAPIModule {
    @Provides
    @Singleton
    fun providePaymentRetrofitAPI(@Named("PaymentRetrofit") retrofit: Retrofit): PaymentRetrofitAPI {
        return retrofit.create(PaymentRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun providesPaymentRepository(stripeRetrofitAPI: PaymentRetrofitAPI, loggedInUserCache: LoggedInUserCache) : PaymentRepository {
        return PaymentRepository(stripeRetrofitAPI,loggedInUserCache)
    }
}