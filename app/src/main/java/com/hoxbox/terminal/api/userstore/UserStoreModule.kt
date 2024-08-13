package com.hoxbox.terminal.api.userstore

import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class UserStoreModule {

    @Provides
    @Singleton
    fun provideUserStoreRetrofitAPI(retrofit: Retrofit): UserStoreRetrofitAPI {
        return retrofit.create(UserStoreRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideOrderRepository(
        userStoreRetrofitAPI: UserStoreRetrofitAPI,
        loggedInUserCache: LoggedInUserCache
    ): UserStoreRepository {
        return UserStoreRepository(userStoreRetrofitAPI, loggedInUserCache)
    }
}