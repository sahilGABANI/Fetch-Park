package com.hoxbox.terminal.api.stripe

import com.google.gson.Gson
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.stripe.model.PaymentResponse
import com.hoxbox.terminal.api.userstore.model.CaptureNewPaymentRequest
import com.hoxbox.terminal.api.userstore.model.IConnRESTRequest
import com.hoxbox.terminal.api.userstore.model.ResponseItem
import io.reactivex.Single

class PaymentRepository(private val paymentRetrofitAPI: PaymentRetrofitAPI, private val loggedInUserCache: LoggedInUserCache) {

    fun performSaleTransaction(amount: Double): Single<String?> {
        val poiDeviceId = loggedInUserCache.getLocationInfo()?.poskey
        val securityKey = loggedInUserCache.getLocationInfo()?.terminalkey
        return paymentRetrofitAPI.performSaleTransaction(poiDeviceId,securityKey,amount)
    }

    fun captureNewPayment(newPaymentRequest: CaptureNewPaymentRequest): Single<PaymentResponse> {
        val newPayment = newPaymentRequest.copy(
            iConnRESTRequest = IConnRESTRequest(
                posAccessKey = loggedInUserCache.getLocationInfo()?.poskey ,
                terminalAccessKey = loggedInUserCache.getLocationInfo()?.terminalkey,
            )
        )
        println("CaptureNewPaymentRequest : ${Gson().toJson(newPayment)}")
        return paymentRetrofitAPI.captureNewPayment(newPayment).flatMap {
            Single.just(it)
        }
    }

    fun capturePaymentForGiftCard(newPaymentRequest: CaptureNewPaymentRequest): Single<PaymentResponse> {
        val newPayment = newPaymentRequest.copy(
            iConnRESTRequest = IConnRESTRequest(
                posAccessKey = loggedInUserCache.getLocationInfo()?.gcPosKey ?: loggedInUserCache.getLocationInfo()?.poskey ,
                terminalAccessKey = loggedInUserCache.getLocationInfo()?.gcTerminalKey?: loggedInUserCache.getLocationInfo()?.terminalkey,
            )
        )
        println("CaptureNewPaymentRequest : ${Gson().toJson(newPayment)}")
        return paymentRetrofitAPI.captureNewPayment(newPayment).flatMap {
            Single.just(it)
        }
    }

    fun refundPOSOrder(newPaymentRequest: CaptureNewPaymentRequest): Single<PaymentResponse> {
        val newPayment = newPaymentRequest.copy(
            iConnRESTRequest = IConnRESTRequest(
                posAccessKey = loggedInUserCache.getLocationInfo()?.poskey ,
                terminalAccessKey = loggedInUserCache.getLocationInfo()?.terminalkey,
            )
        )
        println("CaptureNewPaymentRequest : ${Gson().toJson(newPayment)}")
        return paymentRetrofitAPI.refundPOSOrder(newPayment).flatMap {
            Single.just(it)
        }
    }
}