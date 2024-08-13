package com.hoxbox.terminal.api.stripe

import com.hoxbox.terminal.api.stripe.model.PaymentResponse
import com.hoxbox.terminal.api.userstore.model.CaptureNewPaymentRequest
import com.hoxbox.terminal.api.userstore.model.ResponseItem
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PaymentRetrofitAPI {


    @GET("api/transact.php?type=sale")
    fun performSaleTransaction(
        @Query("poi_device_id") poiDeviceId: String?,
        @Query("security_key") securityKey: String?,
        @Query("amount") amount: Double,
        @Query("poi_prompt_tip") poiPromptTip :Boolean = true,
        @Query("poi_enable_keyed") poiEnableKeyed :Boolean = true,
        @Query("poi_prompt_signature") poiPromptSignature :Boolean = false,
        @Query("poi_prompt_quicktip_percentages") poi_prompt_quicktip_percentages :String = "15.00,18.00,20.00",
        ): Single<String?>

    // production :- https://veritas.rest.iconncloud.net/tsi/v1/payment
    // dev :- https://veritas.rest.uat.iconncloud.net/tsi/v1/payment
    @POST("https://veritas.rest.iconncloud.net/tsi/v1/payment")
    fun captureNewPayment(@Body newPaymentRequest : CaptureNewPaymentRequest) : Single<PaymentResponse>

    @POST("https://veritas.rest.iconncloud.net/tsi/v1/payment")
    fun refundPOSOrder(@Body newPaymentRequest : CaptureNewPaymentRequest) : Single<PaymentResponse>

}
