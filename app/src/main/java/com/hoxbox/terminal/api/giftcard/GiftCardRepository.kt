package com.hoxbox.terminal.api.giftcard


import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.checkout.model.GiftCardResponse
import com.hoxbox.terminal.api.giftcard.model.*
import com.hoxbox.terminal.base.network.HotBoxResponseConverter
import com.hoxbox.terminal.utils.Constants
import io.reactivex.Single

class GiftCardRepository(private val giftCardRetrofitAPI: GiftCardRetrofitAPI, private val loggedInUserCache: LoggedInUserCache) {

    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()

    fun giftCardQRCode(data: String): Single<GiftCardResponse> {
        return giftCardRetrofitAPI.giftCardQRCode(data, Constants.QR_CODE_TYPE_GIFT_CARD)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun applyGiftCard(cardNumber: String): Single<GiftCardData> {
        return giftCardRetrofitAPI.applyGiftCard(cardNumber)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun buyVirtualGiftCard(request: GiftCardRequest): Single<VirtualGiftCardResponse> {
        return giftCardRetrofitAPI.buyVirtualGiftCard(request)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun buyPhysicalGiftCard(request: BuyPhysicalCardRequest): Single<PhysicalGiftCardInfo> {
        return giftCardRetrofitAPI.buyPhysicalGiftCard(request)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }
}