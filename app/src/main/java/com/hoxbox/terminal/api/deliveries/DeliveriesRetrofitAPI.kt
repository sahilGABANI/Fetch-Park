package com.hoxbox.terminal.api.deliveries

import com.hoxbox.terminal.api.order.model.OrderResponse
import com.hoxbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface DeliveriesRetrofitAPI {

    @GET("v1/get-all-orders")
    fun getDeliveriesOrder(@Query("location_id") locationId: Int, @Query("order_type") orderType :String,@Query("order_status") orderStatus :String? = null): Single<HotBoxResponse<OrderResponse>>
}