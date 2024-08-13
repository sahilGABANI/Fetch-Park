package com.hoxbox.terminal.ui.main.deliveries.viewmodel

import com.hoxbox.terminal.api.deliveries.DeliveriesRepository
import com.hoxbox.terminal.api.order.model.OrderResponse
import com.hoxbox.terminal.base.BaseViewModel
import com.hoxbox.terminal.base.extension.subscribeWithErrorParsing
import com.hoxbox.terminal.base.network.model.ErrorResult
import com.hoxbox.terminal.base.network.model.HotBoxError
import com.hoxbox.terminal.helper.formatTo
import com.hoxbox.terminal.helper.toDate
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.*

class DeliveriesViewModel(private val deliveriesRepository: DeliveriesRepository) : BaseViewModel() {

    private lateinit var currentDate: String
    private lateinit var date: String
    private val calendar = Calendar.getInstance()
    private val year = calendar.get(Calendar.YEAR)
    private var month: Int = calendar.get(Calendar.MONTH) + 1
    private val day = calendar.get(Calendar.DAY_OF_MONTH)
    private val deliveriesStateSubject: PublishSubject<DeliveriesViewState> = PublishSubject.create()
    val deliveriesState: Observable<DeliveriesViewState> = deliveriesStateSubject.hide()

    fun loadDeliverOrderData(calenderDate: String, orderType: String,orderStatus :String?) {
        currentDate = "$year-$month-$day".toDate("yyyy-MM-dd")?.formatTo("yyyy-MM-dd").toString()
        date = calenderDate.ifEmpty { currentDate }
        deliveriesRepository.getDeliveriesOrderData(orderType,orderStatus)
            .doOnSubscribe {
                deliveriesStateSubject.onNext(DeliveriesViewState.LoadingState(true))
            }.doAfterTerminate {
                deliveriesStateSubject.onNext(DeliveriesViewState.LoadingState(false))
            }.subscribeWithErrorParsing<OrderResponse, HotBoxError>({
                deliveriesStateSubject.onNext(DeliveriesViewState.OrderInfoSate(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        deliveriesStateSubject.onNext(DeliveriesViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }

//    fun loadStatusDetails() {
//        deliveriesRepository.getStatusLogData()
//            .doOnSubscribe {
//                deliveriesStateSubject.onNext(DeliveriesViewState.LoadingState(true))
//            }.doAfterTerminate {
//                deliveriesStateSubject.onNext(DeliveriesViewState.LoadingState(false))
//            }.subscribeWithErrorParsing<List<StatusLogInfo>, HotBoxError>({
//                deliveriesStateSubject.onNext(DeliveriesViewState.StatusResponse(it))
//            }, {
//                when (it) {
//                    is ErrorResult.ErrorMessage -> {
//                        deliveriesStateSubject.onNext(DeliveriesViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
//                    }
//                    is ErrorResult.ErrorThrowable -> {
//                        Timber.e(it.throwable)
//                    }
//                }
//            }).autoDispose()
//    }
//
//    fun loadOrderDetailsItem() {
//        deliveriesRepository.getOrderDetailsData()
//            .doOnSubscribe {
//                deliveriesStateSubject.onNext(DeliveriesViewState.LoadingState(true))
//            }.doAfterTerminate {
//                deliveriesStateSubject.onNext(DeliveriesViewState.LoadingState(false))
//            }.subscribeWithErrorParsing<List<OrderDetailsInfo>, HotBoxError>({
//                deliveriesStateSubject.onNext(DeliveriesViewState.OrderDetailItemResponse(it))
//            }, {
//                when (it) {
//                    is ErrorResult.ErrorMessage -> {
//                        deliveriesStateSubject.onNext(DeliveriesViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
//                    }
//                    is ErrorResult.ErrorThrowable -> {
//                        Timber.e(it.throwable)
//                    }
//                }
//            }).autoDispose()
//    }

}

sealed class DeliveriesViewState {
    data class ErrorMessage(val errorMessage: String) : DeliveriesViewState()
    data class SuccessMessage(val successMessage: String) : DeliveriesViewState()
    data class LoadingState(val isLoading: Boolean) : DeliveriesViewState()
    data class OrderInfoSate(val orderInfo: OrderResponse) : DeliveriesViewState()
//    data class StatusResponse(val statusLogInfo: List<StatusLogInfo>) : DeliveriesViewState()
//    data class OrderDetailItemResponse(val orderDetailsInfo: List<OrderDetailsInfo>) : DeliveriesViewState()
}