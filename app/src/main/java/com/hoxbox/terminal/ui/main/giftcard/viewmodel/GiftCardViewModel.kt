package com.hoxbox.terminal.ui.main.giftcard.viewmodel

import com.google.gson.Gson
import com.hoxbox.terminal.api.checkout.model.GiftCardResponse
import com.hoxbox.terminal.api.giftcard.GiftCardRepository
import com.hoxbox.terminal.api.giftcard.model.*
import com.hoxbox.terminal.api.store.StoreRepository
import com.hoxbox.terminal.api.store.model.StoreResponse
import com.hoxbox.terminal.api.stripe.PaymentRepository
import com.hoxbox.terminal.api.stripe.model.PaymentResponse
import com.hoxbox.terminal.api.userstore.model.CaptureNewPaymentRequest
import com.hoxbox.terminal.base.BaseViewModel
import com.hoxbox.terminal.base.extension.subscribeWithErrorParsing
import com.hoxbox.terminal.base.network.model.ErrorResult
import com.hoxbox.terminal.base.network.model.HotBoxError
import com.hoxbox.terminal.base.network.parseRetrofitException
import com.hoxbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hoxbox.terminal.utils.Constants
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class GiftCardViewModel(private val giftCardRepository: GiftCardRepository, private val paymentRepository: PaymentRepository, private val storeRepository: StoreRepository) : BaseViewModel() {

    private val giftCardStateSubject: PublishSubject<GiftCardState> = PublishSubject.create()
    val giftCardState: Observable<GiftCardState> = giftCardStateSubject.hide()

    fun giftCardQRCode(data: String) {
        giftCardRepository.giftCardQRCode(data).doOnSubscribe {
            giftCardStateSubject.onNext(GiftCardState.LoadingState(true))
        }.doAfterTerminate {
            giftCardStateSubject.onNext(GiftCardState.LoadingState(false))
        }.subscribeWithErrorParsing<GiftCardResponse, HotBoxError>({
            giftCardStateSubject.onNext(GiftCardState.GiftCardQrResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    giftCardStateSubject.onNext(GiftCardState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    giftCardStateSubject.onNext(
                        GiftCardState.ErrorMessage("Unable to find this gift card")
                    )
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun applyGiftCard(cardNumber: String) {
        giftCardRepository.applyGiftCard(cardNumber).subscribeWithErrorParsing<GiftCardData, HotBoxError>({
            giftCardStateSubject.onNext(GiftCardState.GiftCard(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    giftCardStateSubject.onNext(GiftCardState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    giftCardStateSubject.onNext(GiftCardState.ErrorMessage("Unable to find this gift card"))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun buyVirtualGiftCard(request: GiftCardRequest) {
        Timber.tag("OkHttpClient").i("request :${Gson().toJson(request)}")
        giftCardRepository.buyVirtualGiftCard(request).subscribeWithErrorParsing<VirtualGiftCardResponse, HotBoxError>({
            giftCardStateSubject.onNext(GiftCardState.BuyVirtualGiftCard(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    giftCardStateSubject.onNext(GiftCardState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    giftCardStateSubject.onNext(GiftCardState.ErrorMessage(it.throwable.localizedMessage))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun buyPhysicalGiftCard(request: BuyPhysicalCardRequest) {
        giftCardRepository.buyPhysicalGiftCard(request).subscribeWithErrorParsing<PhysicalGiftCardInfo, HotBoxError>({
            Timber.tag("OkHttpClient").i("BuyPhysicalCardRequest :${Gson().toJson(request)}")
            Timber.tag("OkHttpClient").i("BuyPhysicalGiftCard :${Gson().toJson(it)}")
            giftCardStateSubject.onNext(GiftCardState.BuyPhysicalGiftCard(it.giftCards))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    giftCardStateSubject.onNext(GiftCardState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    giftCardStateSubject.onNext(GiftCardState.ErrorMessage(it.throwable.parseRetrofitException()?.safeErrorMessage ?: ""))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun captureNewPayment(newPaymentRequest: CaptureNewPaymentRequest) {
        paymentRepository.capturePaymentForGiftCard(newPaymentRequest).subscribeWithErrorParsing<PaymentResponse, HotBoxError>({
            giftCardStateSubject.onNext(GiftCardState.CaptureNewPaymentIntent(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    giftCardStateSubject.onNext(GiftCardState.NewPaymentErrorMessage(it.errorResponse.message.toString()))
                }
                is ErrorResult.ErrorThrowable -> {
                    giftCardStateSubject.onNext(GiftCardState.NewPaymentErrorMessage(Constants.CAPTURE_ERROR))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }


    fun loadCurrentStoreResponse() {
        storeRepository.getCurrentStoreInformation().doOnSubscribe {
            giftCardStateSubject.onNext(GiftCardState.LoadingState(true))
        }.doAfterTerminate {
            giftCardStateSubject.onNext(GiftCardState.LoadingState(false))
        }.subscribeWithErrorParsing<StoreResponse, HotBoxError>({
            it.locationTaxRate?.let { it1 -> Constants.setTaxRate(it1) }
            giftCardStateSubject.onNext(GiftCardState.StoreResponses(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    giftCardStateSubject.onNext(GiftCardState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }
}

sealed class GiftCardState {
    data class ErrorMessage(val errorMessage: String) : GiftCardState()
    data class PhysicalErrorMessage(val errorMessage: String) : GiftCardState()
    data class VirtualErrorMessage(val errorMessage: String) : GiftCardState()
    data class NewPaymentErrorMessage(val errorMessage: String) : GiftCardState()
    data class LoadingState(val isLoading: Boolean) : GiftCardState()
    data class GiftCardQrResponse(val data: GiftCardResponse) : GiftCardState()
    data class GiftCard(val data: GiftCardData) : GiftCardState()
    data class CaptureNewPaymentIntent(val createPaymentIntentResponse: PaymentResponse) : GiftCardState()
    data class BuyVirtualGiftCard(val data: VirtualGiftCardResponse) : GiftCardState()
    data class BuyPhysicalGiftCard(val data: List<PhysicalGiftCardResponse>?) : GiftCardState()
    data class QrCodeScanError(val errorType: String) : GiftCardState()
    data class StoreResponses(val storeResponse: StoreResponse) : GiftCardState()
}
