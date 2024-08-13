package com.hoxbox.terminal.api.checkout

import com.hoxbox.terminal.api.checkout.model.*
import com.hoxbox.terminal.base.network.HotBoxResponseConverter
import com.hoxbox.terminal.utils.Constants
import io.reactivex.Single

class CheckOutRepository(private val checkOutRetrofitAPI: CheckOutRetrofitAPI) {

    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()

    fun getDataFromQRCode(data: String): Single<QRScanResponse> {
        return checkOutRetrofitAPI.getDataFromQRCode(data,Constants.QR_CODE_TYPE_LOYALTY)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun giftCardQRCode(data: String): Single<GiftCardResponse> {
        return checkOutRetrofitAPI.giftCardQRCode(data,Constants.QR_CODE_TYPE_GIFT_CARD)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun getLoyaltyPoints(userId : Int?) :Single<UserLoyaltyPointResponse> {
        return checkOutRetrofitAPI.getLoyaltyPoints(userId)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }

    fun getUserCredit(userId: Int?):Single<UserCreditResponse> {
        return checkOutRetrofitAPI.getUserCredit(userId)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }
    fun applyGiftCard(cardNumber: String):Single<GiftCardResponse> {
        return checkOutRetrofitAPI.applyGiftCard(cardNumber)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }
    fun applyPromocode(request: PromoCodeRequest):Single<PromoCodeResponse> {
        return checkOutRetrofitAPI.applyPromocode(request)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }
    fun createUser(request: CreateUserRequest):Single<CreateUserResponse> {
        return checkOutRetrofitAPI.createUser(request)
            .flatMap { hotBoxResponseConverter.convertToSingle(it) }
    }
}