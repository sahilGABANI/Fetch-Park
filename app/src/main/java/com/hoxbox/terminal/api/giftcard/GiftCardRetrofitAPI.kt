package com.hoxbox.terminal.api.giftcard

import com.hoxbox.terminal.api.checkout.model.GiftCardResponse
import com.hoxbox.terminal.api.giftcard.model.*
import com.hoxbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


interface GiftCardRetrofitAPI {

    @GET("v1/get-qr-info")
    fun giftCardQRCode(@Query("data") data :String,@Query("type") type :String) :Single<HotBoxResponse<GiftCardResponse>>

    @GET("v1/apply-gift-card/{gift_card_code}")
    fun applyGiftCard(@Path("gift_card_code") giftCardCode :String?) : Single<HotBoxResponse<GiftCardData>>

    @POST("v1/buy-pos-gift-card")
    fun buyVirtualGiftCard(@Body request: GiftCardRequest) : Single<HotBoxResponse<VirtualGiftCardResponse>>

    @POST("v1/buy-reload-physical-gift-card")
    fun buyPhysicalGiftCard(@Body request: BuyPhysicalCardRequest) : Single<HotBoxResponse<PhysicalGiftCardInfo>>
}