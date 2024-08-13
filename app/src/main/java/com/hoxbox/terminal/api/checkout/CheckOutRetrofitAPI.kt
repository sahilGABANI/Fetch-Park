package com.hoxbox.terminal.api.checkout

import com.hoxbox.terminal.api.checkout.model.*
import com.hoxbox.terminal.api.userstore.model.CreateOrderRequest
import com.hoxbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CheckOutRetrofitAPI {

    @GET("v1/get-qr-info")
    fun getDataFromQRCode(@Query("data") data :String,@Query("type") type :String) :Single<HotBoxResponse<QRScanResponse>>

    @GET("v1/get-qr-info")
    fun giftCardQRCode(@Query("data") data :String,@Query("type") type :String) :Single<HotBoxResponse<GiftCardResponse>>

    @GET("v1/get-user-loyalty-points/{userId}")
    fun getLoyaltyPoints(@Path("userId") userId :Int?):Single<HotBoxResponse<UserLoyaltyPointResponse>>

    @GET("v1/get-user-credits/{userId}")
    fun getUserCredit(@Path("userId") userId :Int?) :Single<HotBoxResponse<UserCreditResponse>>

    @GET("v1/apply-gift-card/{cardNumber}")
    fun applyGiftCard(@Path("cardNumber") cardNumber :String?) :Single<HotBoxResponse<GiftCardResponse>>

    @POST("v1/apply-coupon")
    fun applyPromocode(@Body request: PromoCodeRequest) :Single<HotBoxResponse<PromoCodeResponse>>

    @POST("/v1/create-user")
    fun createUser(@Body request: CreateUserRequest) :Single<HotBoxResponse<CreateUserResponse>>
}