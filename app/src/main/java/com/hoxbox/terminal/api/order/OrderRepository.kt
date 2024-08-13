package com.hoxbox.terminal.api.order

import com.google.gson.Gson
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.authentication.model.HotBoxUser
import com.hoxbox.terminal.api.order.model.*
import com.hoxbox.terminal.base.network.HotBoxResponseConverter
import com.hoxbox.terminal.base.network.model.HotBoxCommonResponse
import com.hoxbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single
import timber.log.Timber

class OrderRepository(
    private val orderRetrofitAPI: OrderRetrofitAPI, private val loggedInUserCache: LoggedInUserCache
) {
    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()
    private val listOfOrderID = arrayListOf<Int>()
    private var playMusic = false

    fun getOrderData(currentDate: String, orderType: String, orderStatus: String?): Single<OrderResponse> {
        val locationId = loggedInUserCache.getLocationInfo()?.id ?: throw Exception("location not found")
        return orderRetrofitAPI.getAllOrder(locationId, orderType, orderStatus).flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun getNewOrderData(currentDate: String, orderType: String, orderStatus: String?): Single<OrderResponse> {
        val locationId = loggedInUserCache.getLocationInfo()?.id ?: throw Exception("location not found")
        return orderRetrofitAPI.getAllOrder(locationId, orderType, orderStatus).flatMap { hotBoxResponseConverter.convertToSingle(it) }.flatMap {
            setIdForNewOrder(it.orders)
            Single.just(it)
        }
    }

    private fun setIdForNewOrder(orders: List<OrdersInfo>?): ArrayList<Int> {
        orders?.forEach {
            if (!listOfOrderID.contains(it.id)) {
                playMusic = true
                it.id?.let { it1 -> listOfOrderID.add(it1) }
            }
        }
        return listOfOrderID
    }

    fun playMusic(): Single<Boolean> {
        return Single.just(playMusic)
    }

    fun setPlayMusicFalse() {
        playMusic = false
    }

    fun getOrderDetailsData(OrderId: Int): Single<OrderDetail> {
        return orderRetrofitAPI.getOrderDetails(OrderId).flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun getStatusLogData(OrderId: Int): Single<StatusLogInfo> {
        return orderRetrofitAPI.getOrderStatusDetails(OrderId).flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun getCartGroupDetail(cartGroupId: Int): Single<StatusLogInfo> {
        return orderRetrofitAPI.getCartGroupDetail(cartGroupId).flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun getUserDetails(userId: Int): Single<HotBoxUser> {
        return orderRetrofitAPI.getUserDetails(userId).flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun updateOrderStatus(orderStatus: String, orderId: Int,userId :Int): Single<UpdatedOrderStatusResponse> {
        Timber.tag("OkHttpClient").i("Order Details Response ${Gson().toJson(OrderStatusRequest(orderStatus, userId, orderId))}")
        return orderRetrofitAPI.updateOrderStatus(
                OrderStatusRequest(orderStatus, userId, orderId)
            ).flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun getOrderTransactionDetail(orderId: Int): Single<TransactionResponse> {
        return orderRetrofitAPI.getOrderTransactionDetail(orderId).flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun refundPayment(orderId: Int): Single<HotBoxCommonResponse> {
        return orderRetrofitAPI.refundPayment(OrderRefundRequest(id = orderId)).flatMap { hotBoxResponseConverter.convertCommonResponse(it) }
    }

    fun sendReceipt(orderId :Int,type :String,email :String?,phone :String?): Single<OrderDetail> {
        return orderRetrofitAPI.sendReceipt(orderId,type, email, phone).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }
}