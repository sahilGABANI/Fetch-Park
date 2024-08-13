package com.hoxbox.terminal.api.store

import com.hoxbox.terminal.api.store.model.BufferResponse
import com.hoxbox.terminal.api.store.model.BufferTimeRequest
import com.hoxbox.terminal.api.store.model.StoreResponse
import com.hoxbox.terminal.base.network.ErrorType
import com.hoxbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface StoreRetrofitAPI {
    @GET("v1/get-location-by-id/{location_id}")
    @ErrorType
    fun getLocationById(@Path("location_id") locationId: Int): Single<HotBoxResponse<StoreResponse>>

    @GET("v1/get-buffer-times/{location_id}")
    @ErrorType
    fun getBufferTimes(@Path("location_id") locationId: Int): Single<HotBoxResponse<BufferResponse>>

    @POST("v1/update-buffer-times")
    @ErrorType
    fun getUpdatedBufferTimes(@Body bufferTimeRequest: BufferTimeRequest): Single<HotBoxResponse<BufferResponse>>

}