package com.hoxbox.terminal.api.order

import com.hoxbox.terminal.api.authentication.model.HotBoxUser
import com.hoxbox.terminal.api.order.model.*
import com.hoxbox.terminal.api.store.model.BufferResponse
import com.hoxbox.terminal.base.network.model.HotBoxCommonResponse
import com.hoxbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single
import retrofit2.http.*

interface OrderRetrofitAPI {

    @GET("v1/get-all-orders")
    fun getAllOrder(@Query("location_id") locationId: Int, @Query("order_type") orderType :String,@Query("order_status") orderStatus :String? = null): Single<HotBoxResponse<OrderResponse>>

    @GET("v1/get-pos-order-details/{order_id}")
    fun getOrderDetails(@Path("order_id") OrderId :Int): Single<HotBoxResponse<OrderDetail>>

    @GET("v1/get-orders-status/{order_id}")
    fun getOrderStatusDetails(@Path("order_id") OrderId :Int): Single<HotBoxResponse<StatusLogInfo>>

    @GET("v1/get-cart/{cart_group_id}")
    fun getCartGroupDetail(@Path("cart_group_id") cartGroupId :Int): Single<HotBoxResponse<StatusLogInfo>>

    @GET("v1/get-user/{user_id}")
    fun getUserDetails(@Path("user_id") userId :Int): Single<HotBoxResponse<HotBoxUser>>

    @POST("v1/update-order-status")
    fun updateOrderStatus(@Body request: OrderStatusRequest) :Single<HotBoxResponse<UpdatedOrderStatusResponse>>

    @GET("v1/get-order-transaction")
    fun getOrderTransactionDetail(@Query("id") id :Int) :Single<HotBoxResponse<TransactionResponse>>

    @POST("v1/refund-order")
    fun refundPayment(@Body request: OrderRefundRequest) :Single<HotBoxCommonResponse>

    @GET("v1/send-receipt")
    fun sendReceipt(@Query("order_id") orderId :Int?,@Query("type")type :String,@Query("email") email :String? =null,@Query("phone") phone :String? =null) :Single<HotBoxResponse<OrderDetail>>

}