package com.hoxbox.terminal.ui.main.orderdetail.viewmodel

import com.google.gson.Gson
import com.hoxbox.terminal.api.authentication.model.HotBoxUser
import com.hoxbox.terminal.api.order.OrderRepository
import com.hoxbox.terminal.api.order.model.*
import com.hoxbox.terminal.api.stripe.PaymentRepository
import com.hoxbox.terminal.api.stripe.model.PaymentResponse
import com.hoxbox.terminal.api.userstore.model.CaptureNewPaymentRequest
import com.hoxbox.terminal.base.BaseViewModel
import com.hoxbox.terminal.base.extension.subscribeWithErrorParsing
import com.hoxbox.terminal.base.network.model.ErrorResult
import com.hoxbox.terminal.base.network.model.HotBoxCommonResponse
import com.hoxbox.terminal.base.network.model.HotBoxError
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class OrderDetailsViewModel(private val orderRepository: OrderRepository,private val paymentRepository: PaymentRepository) : BaseViewModel() {
    private val orderDetailsStateSubject: PublishSubject<OrderDetailsViewState> = PublishSubject.create()
    val orderDetailsState: Observable<OrderDetailsViewState> = orderDetailsStateSubject.hide()

    fun loadStatusDetails(orderId: Int) {
        orderRepository.getStatusLogData(orderId)
            .doOnSubscribe {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(true))
            }.doAfterTerminate {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
            }.subscribeWithErrorParsing<StatusLogInfo, HotBoxError>({
                orderDetailsStateSubject.onNext(OrderDetailsViewState.StatusResponse(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

    fun loadCartGroupDetail(cartGroupId: Int) {
        orderRepository.getCartGroupDetail(cartGroupId)
            .doOnSubscribe {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(true))
            }.doAfterTerminate {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
            }.subscribeWithErrorParsing<StatusLogInfo, HotBoxError>({
                orderDetailsStateSubject.onNext(OrderDetailsViewState.StatusResponse(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                        orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

    fun loadUserDetails(userId: Int) {
        orderRepository.getUserDetails(userId)
            .doOnSubscribe {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(true))
            }.doAfterTerminate {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
            }.subscribeWithErrorParsing<HotBoxUser, HotBoxError>({
                orderDetailsStateSubject.onNext(OrderDetailsViewState.CustomerDetails(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

    fun loadOrderDetailsItem(OrderId: Int) {
        orderRepository.getOrderDetailsData(OrderId)
            .doOnSubscribe {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(true))
            }.doAfterTerminate {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
            }.subscribeWithErrorParsing<OrderDetail, HotBoxError>({
                Timber.tag("OkHttpClient").i("Order Details Response ${Gson().toJson(it)}")
                orderDetailsStateSubject.onNext(OrderDetailsViewState.OrderDetailItemResponse(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

    fun updateOrderStatusDetails(orderStatus: String, orderId: Int,userId :Int) {
        orderRepository.updateOrderStatus(orderStatus,orderId,userId).doOnSubscribe {
            orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(true))
        }.doAfterTerminate {
            orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
        }.subscribeWithErrorParsing<UpdatedOrderStatusResponse, HotBoxError>({
            orderDetailsStateSubject.onNext(OrderDetailsViewState.UpdateStatusResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }
    fun getOrderTransactionDetail(orderId: Int) {
        orderRepository.getOrderTransactionDetail(orderId).doOnSubscribe {
            orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(true))
        }.doAfterTerminate {
            orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
        }.subscribeWithErrorParsing<TransactionResponse, HotBoxError>({
            orderDetailsStateSubject.onNext(OrderDetailsViewState.TransactionDetails(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun refundPayment(orderId: Int) {
        orderRepository.refundPayment(orderId).doOnSubscribe {
            orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(true))
        }.doAfterTerminate {
            orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
        }.subscribeWithErrorParsing<HotBoxCommonResponse, HotBoxError>({
            orderDetailsStateSubject.onNext(OrderDetailsViewState.SuccessMessage(it.message))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun refundPOSOrder(newPaymentRequest: CaptureNewPaymentRequest) {
        paymentRepository.refundPOSOrder(newPaymentRequest).doOnSubscribe {
            orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(true))
        }.doAfterTerminate {
            orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
        }.subscribeWithErrorParsing<PaymentResponse, HotBoxError>({
            orderDetailsStateSubject.onNext(OrderDetailsViewState.RefundPaymentIntent(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
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
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(true))
            }.doAfterTerminate {
                orderDetailsStateSubject.onNext(OrderDetailsViewState.LoadingState(false))
            }.subscribeWithErrorParsing<OrderDetail, HotBoxError>({
                orderDetailsStateSubject.onNext(OrderDetailsViewState.SendReceiptSuccessMessage(""))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        orderDetailsStateSubject.onNext(OrderDetailsViewState.ErrorMessage(it.errorResponse.message.toString()))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

}

sealed class OrderDetailsViewState {
    data class ErrorMessage(val errorMessage: String) : OrderDetailsViewState()
    data class SuccessMessage(val successMessage: String?) : OrderDetailsViewState()
    data class LoadingState(val isLoading: Boolean) : OrderDetailsViewState()
    data class StatusResponse(val statusLogInfo: StatusLogInfo) : OrderDetailsViewState()
    data class UpdateStatusResponse(val updatedOrderStatusResponse: UpdatedOrderStatusResponse) : OrderDetailsViewState()
    data class OrderDetailItemResponse(val orderDetails: OrderDetail) : OrderDetailsViewState()
    data class CustomerDetails(val customerDetails: HotBoxUser) : OrderDetailsViewState()
    data class SendReceiptSuccessMessage(val successMessage: String) : OrderDetailsViewState()
    data class TransactionDetails(val transactionResponse: TransactionResponse) : OrderDetailsViewState()
    data class RefundPaymentIntent(val createPaymentIntentResponse: PaymentResponse) : OrderDetailsViewState()
}