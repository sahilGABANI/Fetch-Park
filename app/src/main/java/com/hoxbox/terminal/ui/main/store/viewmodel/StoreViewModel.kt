package com.hoxbox.terminal.ui.main.store.viewmodel

import com.hoxbox.terminal.api.order.OrderRepository
import com.hoxbox.terminal.api.order.model.OrderDetail
import com.hoxbox.terminal.api.order.model.OrderResponse
import com.hoxbox.terminal.api.order.model.UpdatedOrderStatusResponse
import com.hoxbox.terminal.api.store.StoreRepository
import com.hoxbox.terminal.api.store.model.BufferResponse
import com.hoxbox.terminal.api.store.model.StoreResponse
import com.hoxbox.terminal.api.store.model.StoreShiftTime
import com.hoxbox.terminal.base.BaseViewModel
import com.hoxbox.terminal.base.extension.subscribeWithErrorParsing
import com.hoxbox.terminal.base.network.model.ErrorResult
import com.hoxbox.terminal.base.network.model.HotBoxError
import com.hoxbox.terminal.ui.login.viewmodel.LoginViewState
import com.hoxbox.terminal.ui.main.orderdetail.viewmodel.OrderDetailsViewState
import com.hoxbox.terminal.ui.splash.viewmodel.LocationViewState
import com.hoxbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hoxbox.terminal.utils.Constants
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class StoreViewModel(
    private val storeRepository: StoreRepository,
    private val orderRepository :OrderRepository
) : BaseViewModel() {

    private val storeStateSubject: PublishSubject<StoreState> = PublishSubject.create()
    val storeState: Observable<StoreState> = storeStateSubject.hide()
    private var orderDataDisposable: Disposable? = null

    fun loadCurrentStoreResponse() {
        storeRepository.getCurrentStoreInformation().doOnSubscribe {
            storeStateSubject.onNext(StoreState.LoadingState(true))
        }.doAfterTerminate {
            storeStateSubject.onNext(StoreState.LoadingState(false))
        }.subscribeWithErrorParsing<StoreResponse, HotBoxError>({
            it.locationTaxRate?.let { it1 -> Constants.setTaxRate(it1) }
            storeStateSubject.onNext(StoreState.StoreResponses(it))
            storeStateSubject.onNext(StoreState.LoadStoreShiftTime(it.getStoreShiftTime()))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    storeStateSubject.onNext(StoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun loadBufferTIme() {
        storeRepository.getBufferInformation().doOnSubscribe {
            storeStateSubject.onNext(StoreState.LoadingState(true))
        }.doAfterTerminate {
            storeStateSubject.onNext(StoreState.LoadingState(false))
        }.subscribeWithErrorParsing<BufferResponse, HotBoxError>({
            storeStateSubject.onNext(StoreState.BufferResponses(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    storeStateSubject.onNext(StoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun updateBufferTimeForPickUpOrDelivery(isBufferTimePlush: Boolean, isPickUpBufferTime: Boolean) {
        storeRepository.updateBufferTimeForPickUpOrDelivery(isBufferTimePlush, isPickUpBufferTime).doOnSubscribe {
            storeStateSubject.onNext(StoreState.LoadingState(true))
        }.doAfterTerminate {
            storeStateSubject.onNext(StoreState.LoadingState(true))
        }.subscribeWithErrorParsing<BufferResponse, HotBoxError>({
            storeStateSubject.onNext(StoreState.BufferResponses(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    storeStateSubject.onNext(StoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun loadOrderData(random: Int) {
        orderDataDisposable?.dispose()
        orderDataDisposable = Observable.interval(random.toLong(), TimeUnit.SECONDS).startWith(0L)
            .flatMap { orderRepository.getNewOrderData("", "All", "new").toObservable() }
            .doOnError {
                OrderResponse(orders = arrayListOf())
            }
            .subscribeWithErrorParsing<OrderResponse, HotBoxError>({
                storeStateSubject.onNext(StoreState.OrderInfoSate(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        storeStateSubject.onNext(StoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }

                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            })
    }

    fun updateOrderStatusDetails(orderStatus: String, orderId: Int,userId :Int) {
        orderRepository.updateOrderStatus(orderStatus,orderId,userId).doOnSubscribe {
            storeStateSubject.onNext(StoreState.LoadingState(true))
        }.doAfterTerminate {
            storeStateSubject.onNext(StoreState.LoadingState(false))
        }.subscribeWithErrorParsing<UpdatedOrderStatusResponse, HotBoxError>({
            storeStateSubject.onNext(StoreState.UpdateStatusResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    storeStateSubject.onNext(StoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun loadOrderDetailsItem(OrderId: Int) {
        orderRepository.getOrderDetailsData(OrderId).doOnSubscribe {
            storeStateSubject.onNext(StoreState.LoadingState(true))
        }.doAfterTerminate {
            storeStateSubject.onNext(StoreState.LoadingState(false))
        }.subscribeWithErrorParsing<OrderDetail, HotBoxError>({
            Timber.tag("TAG").e(it.toString())
            storeStateSubject.onNext(StoreState.OrderDetailItemResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    storeStateSubject.onNext(StoreState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }

                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun playMusic() {
        orderRepository.playMusic().doOnSubscribe {

        }.doAfterTerminate {

        }.subscribeWithErrorParsing<Boolean, HotBoxError>({
            if (it) {
                storeStateSubject.onNext(StoreState.PlayMusic)
            }
        }, {

        })

    }

    fun stopMusic() {
        orderRepository.setPlayMusicFalse()

    }
}

sealed class StoreState {
    data class ErrorMessage(val errorMessage: String) : StoreState()
    data class SuccessMessage(val successMessage: String) : StoreState()
    data class LoadingState(val isLoading: Boolean) : StoreState()
    data class StoreResponses(val storeResponse: StoreResponse) : StoreState()
    data class LoadStoreShiftTime(val listOfShiftTime: List<StoreShiftTime>) : StoreState()
    data class UpdateStatusResponse(val updatedOrderStatusResponse: UpdatedOrderStatusResponse) : StoreState()
    data class BufferResponses(val bufferResponse: BufferResponse) : StoreState()
    data class OrderInfoSate(val bufferResponse: OrderResponse) : StoreState()
    data class OrderDetailItemResponse(val bufferResponse: OrderDetail) : StoreState()
    object PlayMusic : StoreState()
}