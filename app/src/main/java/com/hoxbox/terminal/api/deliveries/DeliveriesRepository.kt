package com.hoxbox.terminal.api.deliveries

import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.order.model.OrderResponse
import com.hoxbox.terminal.base.network.HotBoxResponseConverter
import io.reactivex.Single

class DeliveriesRepository(
    private val deliveriesRetrofitAPI: DeliveriesRetrofitAPI,
    private val loggedInUserCache: LoggedInUserCache
) {
    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()

    fun getDeliveriesOrderData(orderType: String, orderStatus: String?): Single<OrderResponse> {
        val locationId =
            loggedInUserCache.getLocationInfo()?.id ?: throw Exception("location not found")
        return deliveriesRetrofitAPI.getDeliveriesOrder(locationId, orderType,orderStatus).flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }
}