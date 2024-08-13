package com.hoxbox.terminal.api.authentication

import com.hoxbox.terminal.api.authentication.model.*
import com.hoxbox.terminal.base.network.HotBoxResponseConverter
import io.reactivex.Single

class AuthenticationRepository(
    private val authenticationRetrofitAPI: AuthenticationRetrofitAPI,
    private val loggedInUserCache: LoggedInUserCache
) {

    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()

    fun loginCrew(request: LoginCrewRequest): Single<HotBoxUser> {
        return authenticationRetrofitAPI.loginCrew(request)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
            .flatMap { storeUserToken(it) }
            .flatMap { loginCrew ->
                getUserDetails(loginCrew.userId).flatMap {
                    Single.just(LoggedInUser(loginCrew, it))
                }
            }
            .flatMap {
                storeUserInformation(it)
            }
    }

    fun getLocation(serialNumber: String): Single<LocationResponse> {
        return authenticationRetrofitAPI.getLocation(serialNumber).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }.doAfterSuccess {
            loggedInUserCache.setLocationInfo(it)
        }
    }

    private fun storeUserToken(loginCrewResponse: LoginCrewResponse): Single<LoginCrewResponse> {
        loggedInUserCache.setLoggedInUserToken(loginCrewResponse.token)
        return Single.just(loginCrewResponse)
    }

    private fun storeUserInformation(loggedInUser: LoggedInUser): Single<HotBoxUser> {
        loggedInUserCache.setLoggedInUser(loggedInUser.crewResponse, loggedInUser.hotBoxUser)
        return Single.just(loggedInUser.hotBoxUser)
    }

    private fun getUserDetails(userId: Int): Single<HotBoxUser> {
        return authenticationRetrofitAPI.getUserDetails(userId)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }
}