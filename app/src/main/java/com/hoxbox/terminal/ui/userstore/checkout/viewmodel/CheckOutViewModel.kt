package com.hoxbox.terminal.ui.userstore.checkout.viewmodel

import com.hoxbox.terminal.api.checkout.CheckOutRepository
import com.hoxbox.terminal.api.checkout.model.*
import com.hoxbox.terminal.base.BaseViewModel
import com.hoxbox.terminal.base.extension.subscribeWithErrorParsing
import com.hoxbox.terminal.base.network.model.ErrorResult
import com.hoxbox.terminal.base.network.model.HotBoxError
import com.hoxbox.terminal.base.network.parseRetrofitException
import com.hoxbox.terminal.ui.userstore.viewmodel.UserStoreState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.json.JSONObject
import retrofit2.adapter.rxjava2.HttpException
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

class CheckOutViewModel(private val checkOutRepository: CheckOutRepository) : BaseViewModel() {

    private val checkOutStateSubject: PublishSubject<CheckOutState> = PublishSubject.create()
    val checkOutState: Observable<CheckOutState> = checkOutStateSubject.hide()

    fun getQRData(data: String) {
        checkOutRepository.getDataFromQRCode(data).subscribeWithErrorParsing<QRScanResponse, HotBoxError>({
            checkOutStateSubject.onNext(CheckOutState.QrCodeData(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.throwable.parseRetrofitException()?.message ?: "Invalid Loyalty QR"))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun giftCardQRCode(data: String) {
        checkOutRepository.giftCardQRCode(data).doOnSubscribe {
            checkOutStateSubject.onNext(CheckOutState.LoadingState(true))
        }.doAfterTerminate {
            checkOutStateSubject.onNext(CheckOutState.LoadingState(false))
        }.subscribeWithErrorParsing<GiftCardResponse, HotBoxError>({
            checkOutStateSubject.onNext(CheckOutState.GiftCardQrResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage("Invalid Gift Card QR"))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun getLoyaltyPointDetails(userId: Int?) {
        checkOutRepository.getLoyaltyPoints(userId = userId).subscribeWithErrorParsing<UserLoyaltyPointResponse, HotBoxError>({
            checkOutStateSubject.onNext(CheckOutState.UserLoyaltyPoint(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.throwable.localizedMessage))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun getUserCredit(userId: Int?) {
        checkOutRepository.getUserCredit(userId = userId).subscribeWithErrorParsing<UserCreditResponse, HotBoxError>({
            checkOutStateSubject.onNext(CheckOutState.UserCreditPoint(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.throwable.localizedMessage))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun applyGiftCard(cardNumber: String) {
        checkOutRepository.applyGiftCard(cardNumber).subscribeWithErrorParsing<GiftCardResponse, HotBoxError>({
            checkOutStateSubject.onNext(CheckOutState.GiftCard(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.throwable.localizedMessage))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun applyPromocode(request: PromoCodeRequest) {
        checkOutRepository.applyPromocode(request).subscribeWithErrorParsing<PromoCodeResponse, HotBoxError>({
            checkOutStateSubject.onNext(CheckOutState.PromocodeResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage("Invalid PromoCode"))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun createUser(request: CreateUserRequest) {
        checkOutRepository.createUser(request).subscribeWithErrorParsing<CreateUserResponse, HotBoxError>({
            checkOutStateSubject.onNext(CheckOutState.CreateUserInformation(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    val message = (it.throwable as HttpException).response()?.errorBody()?.byteStream()
                    val responseString = BufferedReader(InputStreamReader(message)).use { it.readText() }
                    val responseJson = JSONObject(responseString)
                    val messageInfo = responseJson.getString("message")
                    checkOutStateSubject.onNext(CheckOutState.ErrorMessage(messageInfo))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }


}

sealed class CheckOutState {
    data class ErrorMessage(val errorMessage: String) : CheckOutState()
    data class LoadingState(val isLoading: Boolean) : CheckOutState()
    data class QrCodeData(val data: QRScanResponse) : CheckOutState()
    data class GiftCardQrResponse(val data: GiftCardResponse) : CheckOutState()
    data class UserLoyaltyPoint(val data: UserLoyaltyPointResponse) : CheckOutState()
    data class UserCreditPoint(val data: UserCreditResponse) : CheckOutState()
    data class GiftCard(val data: GiftCardResponse) : CheckOutState()
    data class QrCodeScanError(val errorType: String) : CheckOutState()
    data class PromocodeResponse(val promocode: PromoCodeResponse) : CheckOutState()
    data class CreateUserInformation(val createUserResponse: CreateUserResponse) : CheckOutState()
}