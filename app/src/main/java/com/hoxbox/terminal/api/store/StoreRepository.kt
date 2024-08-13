package com.hoxbox.terminal.api.store

import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.store.model.*
import com.hoxbox.terminal.base.network.HotBoxResponseConverter
import io.reactivex.Single

class StoreRepository(
    private val storeRetrofitAPI: StoreRetrofitAPI,
    private val loggedInUserCache: LoggedInUserCache
) {
    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()

    fun getCurrentStoreInformation(): Single<StoreResponse> {
        val locationId =
            loggedInUserCache.getLocationInfo()?.id ?: throw Exception("location not found")
        return storeRetrofitAPI.getLocationById(locationId)
            .flatMap { hotBoxResponseConverter.convert(it) }
    }

    fun getCurrentStoreLocation(): Single<String> {
        return getCurrentStoreInformation().map {
            it.getSafeAddressName()
        }
    }

    fun getBufferInformation(): Single<BufferResponse> {
        val locationId =
            loggedInUserCache.getLocationInfo()?.id ?: throw Exception("location not found")
        return storeRetrofitAPI.getBufferTimes(locationId)
            .flatMap { hotBoxResponseConverter.convert(it) }
    }

    fun getUpdatedBufferTimes(request: BufferTimeRequest): Single<BufferResponse> {
        return storeRetrofitAPI.getUpdatedBufferTimes(request)
            .flatMap { hotBoxResponseConverter.convert(it) }
    }

    fun updateBufferTimeForPickUpOrDelivery(
        isBufferTimePlush: Boolean, isPickUpBufferTime: Boolean
    ): Single<BufferResponse> {
        val locationId =
            loggedInUserCache.getLocationInfo()?.id ?: throw Exception("location not found")
        val operator = if (isBufferTimePlush) {
            BUFFER_TIME_PLUSH
        } else {
            BUFFER_TIME_MINUS
        }
        val type = if (isPickUpBufferTime) {
            PICKUP_BUFFER_TYPE
        } else {
            DELIVERY_BUFFER_TYPE
        }
        return storeRetrofitAPI.getUpdatedBufferTimes(
            BufferTimeRequest(
                locationId,
                type,
                operator
            )
        ).flatMap { hotBoxResponseConverter.convert(it) }
    }
}