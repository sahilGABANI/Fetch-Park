package com.hoxbox.terminal.api.authentication

import com.hoxbox.terminal.api.authentication.model.HotBoxUser
import com.hoxbox.terminal.api.authentication.model.LocationResponse
import com.hoxbox.terminal.api.authentication.model.LoginCrewRequest
import com.hoxbox.terminal.api.authentication.model.LoginCrewResponse
import com.hoxbox.terminal.base.network.ErrorType
import com.hoxbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthenticationRetrofitAPI {

    @GET("v1/get-location-by-pos/{serial-number}")
    @ErrorType
    fun getLocation(@Path("serial-number") serialNumber: String): Single<HotBoxResponse<LocationResponse>>

    @POST("v1/employee-access")
    @ErrorType
    fun loginCrew(@Body request: LoginCrewRequest): Single<HotBoxResponse<LoginCrewResponse>>

    @GET("v1/get-user/{user_id}")
    @ErrorType
    fun getUserDetails(@Path("user_id") userId: Int): Single<HotBoxResponse<HotBoxUser>>

    @POST("v1/get-location-by-id")
    @ErrorType
    fun getLocationById()
}