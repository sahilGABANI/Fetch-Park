package com.hoxbox.terminal.ui.userstore.viewmodel

import com.google.gson.Gson
import com.hoxbox.terminal.api.menu.model.MenuListInfo
import com.hoxbox.terminal.api.menu.model.ProductsItem
import com.hoxbox.terminal.api.order.OrderRepository
import com.hoxbox.terminal.api.order.model.OrderDetail
import com.hoxbox.terminal.api.order.model.UpdatedOrderStatusResponse
import com.hoxbox.terminal.api.store.StoreRepository
import com.hoxbox.terminal.api.store.model.StoreResponse
import com.hoxbox.terminal.api.stripe.PaymentRepository
import com.hoxbox.terminal.api.stripe.model.PaymentResponse
import com.hoxbox.terminal.api.userstore.UserStoreRepository
import com.hoxbox.terminal.api.userstore.model.*
import com.hoxbox.terminal.base.BaseViewModel
import com.hoxbox.terminal.base.extension.subscribeOnIoAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.subscribeWithErrorParsing
import com.hoxbox.terminal.base.network.model.ErrorResult
import com.hoxbox.terminal.base.network.model.HotBoxCommonResponse
import com.hoxbox.terminal.base.network.model.HotBoxError
import com.hoxbox.terminal.ui.main.orderdetail.viewmodel.OrderDetailsViewState
import com.hoxbox.terminal.ui.main.store.viewmodel.StoreState
import com.hoxbox.terminal.utils.Constants
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.json.JSONObject
import retrofit2.adapter.rxjava2.HttpException
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

class UserStoreViewModel(
    private val userStoreRepository: UserStoreRepository, private val paymentRepository: PaymentRepository,
    private val storeRepository :StoreRepository, private val orderRepository: OrderRepository
) : BaseViewModel() {

    private val userStoreStateSubject: PublishSubject<UserStoreState> = PublishSubject.create()
    val userStoreState: Observable<UserStoreState> = userStoreStateSubject.hide()

    fun getMenuProductByLocation() {
        userStoreRepository.getMenuProductByLocation().doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<MenuListInfo, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.MenuInfo(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun getProductDetails(productId: Int?, menuGroupId: Int?) {
        userStoreRepository.getMenuProductByLocation(productId, menuGroupId).doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<ProductsItem, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.SubProductState(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun addToCartProduct(request: AddToCartRequest) {
        Timber.tag("OkHttpClient").i("Add To Cart Request : ${Gson().toJson(request)}")
        userStoreRepository.addToCartProduct(request).doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<AddToCartResponse, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.AddToCartProductResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun getCartDetails(cartGroupId: Int) {
        userStoreRepository.getCartDetails(cartGroupId).doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<CartInfoDetails, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.CartDetailsInfo(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun updateMenuItemQuantity(cartId: Int, request: UpdateMenuItemQuantity) {
        userStoreRepository.updateMenuItemQuantity(cartId, request).doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<AddToCartDetails, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.UpdatedCartInfo(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun deleteCartItem(request: DeleteCartItemRequest) {
        userStoreRepository.deleteCartItem(request).doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<HotBoxCommonResponse, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.DeletedCartItem(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.message.toString()))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun createOrder(request: CreateOrderRequest) {
        userStoreRepository.createOrder(request).doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<CreateOrderResponse, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.CreatePosOrder(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.message.toString()))
                }
                is ErrorResult.ErrorThrowable -> {
                    val message = (it.throwable as HttpException).response()?.errorBody()?.byteStream()
                    val responseString = BufferedReader(InputStreamReader(message)).use { it.readText() }
                    val responseJson = JSONObject(responseString)
                    val messageInfo = responseJson.getString("message")
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(messageInfo))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun performSaleTransaction(amount: Double) {
        paymentRepository.performSaleTransaction(amount).doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            if (it != null){
                userStoreStateSubject.onNext(UserStoreState.PaymentInfo(it))
            } else {
                userStoreStateSubject.onNext(UserStoreState.PaymentErrorMessage(""))
            }
        }, { throwable ->
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
            throwable.localizedMessage?.let {

                userStoreStateSubject.onNext(UserStoreState.PaymentErrorMessage(it))
            }
        }).autoDispose()
    }
    fun getOrderPromisedTime() {
        userStoreRepository.getOrderPromisedTime().doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<GetPromisedTime, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.GetOrderPromisedTime(it))
        }, {

            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.message.toString()))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }
    fun loadCurrentStoreResponse() {
        storeRepository.getCurrentStoreInformation().doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<StoreResponse, HotBoxError>({
            it.locationTaxRate?.let { it1 -> Constants.setTaxRate(it1) }
            userStoreStateSubject.onNext(UserStoreState.StoreResponses(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }
    fun loadOrderDetailsItem(OrderId: Int) {
        orderRepository.getOrderDetailsData(OrderId).doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<OrderDetail, HotBoxError>({
            Timber.tag("TAG").e(it.toString())
            userStoreStateSubject.onNext(UserStoreState.OrderDetailItemResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }

                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun captureNewPayment(newPaymentRequest: CaptureNewPaymentRequest) {
        paymentRepository.captureNewPayment(newPaymentRequest).subscribeWithErrorParsing<PaymentResponse, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.CaptureNewPaymentIntent(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.NewPaymentErrorMessage(it.errorResponse.message.toString()))
                }
                is ErrorResult.ErrorThrowable -> {
                    userStoreStateSubject.onNext(UserStoreState.NewPaymentErrorMessage(Constants.CAPTURE_ERROR))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }


    fun updateOrderStatusDetails(orderStatus: String, orderId: Int,userId :Int) {
        orderRepository.updateOrderStatus(orderStatus,orderId,userId).doOnSubscribe {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
        }.doAfterTerminate {
            userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
        }.subscribeWithErrorParsing<UpdatedOrderStatusResponse, HotBoxError>({
            userStoreStateSubject.onNext(UserStoreState.UpdateStatusResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun sendReceipt(orderId: Int, type: String, email: String?, phone: String?) {
        orderRepository.sendReceipt(orderId, type, email, phone)
            .doOnSubscribe {
                userStoreStateSubject.onNext(UserStoreState.LoadingState(true))
            }.doAfterTerminate {
                userStoreStateSubject.onNext(UserStoreState.LoadingState(false))
            }.subscribeWithErrorParsing<OrderDetail, HotBoxError>({
                userStoreStateSubject.onNext(UserStoreState.SendReceiptSuccessMessage(""))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        userStoreStateSubject.onNext(UserStoreState.ErrorMessage(it.errorResponse.message.toString()))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

}

sealed class UserStoreState {
    data class ErrorMessage(val errorMessage: String) : UserStoreState()
    data class SuccessMessage(val successMessage: String) : UserStoreState()
    data class LoadingState(val isLoading: Boolean) : UserStoreState()
    data class SendReceiptSuccessMessage(val successMessage: String) : UserStoreState()
    data class SubProductState(val productsItem: ProductsItem) : UserStoreState()
    data class CartDetailsInfo(val cartInfo: CartInfoDetails) : UserStoreState()
    data class UpdatedCartInfo(val cartInfo: AddToCartDetails?) : UserStoreState()
    data class DeletedCartItem(val cartInfo: HotBoxCommonResponse?) : UserStoreState()
    data class CreatePosOrder(val cartInfo: CreateOrderResponse?) : UserStoreState()
    data class UpdateStatusResponse(val updatedOrderStatusResponse: UpdatedOrderStatusResponse) : UserStoreState()
    data class AddToCartProductResponse(val addToCartResponse: AddToCartResponse) : UserStoreState()
    data class MenuInfo(val menuListInfo: MenuListInfo) : UserStoreState()
    data class GetOrderPromisedTime(val getPromisedTime: GetPromisedTime) : UserStoreState()
    data class PaymentInfo(val responseData: String) : UserStoreState()
    data class PaymentErrorMessage(val message: String) : UserStoreState()
    data class StoreResponses(val storeResponse: StoreResponse) : UserStoreState()
    data class OrderDetailItemResponse(val orderDetail: OrderDetail): UserStoreState()
    data class NewPaymentErrorMessage(val errorMessage: String) : UserStoreState()
    data class CaptureNewPaymentIntent(val createPaymentIntentResponse: PaymentResponse) : UserStoreState()

}