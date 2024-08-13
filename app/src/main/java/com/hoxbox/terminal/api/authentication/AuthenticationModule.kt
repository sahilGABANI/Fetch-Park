package com.hoxbox.terminal.api.authentication

import com.hoxbox.terminal.base.prefs.LocalPrefs
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class AuthenticationModule {

    @Provides
    @Singleton
    fun provideLoggedInUserCache(prefs: LocalPrefs): LoggedInUserCache {
        return LoggedInUserCache(prefs)
    }


    @Provides
    @Singleton
    fun provideAuthenticationRetrofitAPI(retrofit: Retrofit): AuthenticationRetrofitAPI {
        return retrofit.create(AuthenticationRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideAuthenticationRepository(
        authenticationRetrofitAPI: AuthenticationRetrofitAPI,
        loggedInUserCache: LoggedInUserCache
        ): AuthenticationRepository {
        return AuthenticationRepository(authenticationRetrofitAPI, loggedInUserCache)
    }
}