package com.hoxbox.terminal.ui.main.order.viewmodel

import com.hoxbox.terminal.api.order.OrderRepository
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
import java.util.concurrent.TimeUnit

class OrderViewModel(private val orderRepository: OrderRepository) : BaseViewModel() {
    private lateinit var currentDate: String
    private lateinit var orderType: String
    private var orderStatus: String? = null
    private lateinit var date: String
    private val calendar = Calendar.getInstance()
    private val year = calendar.get(Calendar.YEAR)
    private var month: Int = calendar.get(Calendar.MONTH) + 1
    private val day = calendar.get(Calendar.DAY_OF_MONTH)
    private val orderStateSubject: PublishSubject<OrderViewState> = PublishSubject.create()
    val orderState: Observable<OrderViewState> = orderStateSubject.hide()

    fun loadOrderData(calenderDate: String, ordersType: String, ordersStatus: String?) {
        orderType = ordersType.ifEmpty { "All" }
        currentDate = "$year-$month-$day".toDate("yyyy-MM-dd")?.formatTo("yyyy-MM-dd").toString()
        date = calenderDate.ifEmpty { currentDate }
        orderStatus = ordersStatus
        Observable.interval(1, TimeUnit.MINUTES)
            .startWith(0L)
            .flatMap { orderRepository.getOrderData(date, orderType, orderStatus).toObservable() }
            .subscribeWithErrorParsing<OrderResponse, HotBoxError>({
                orderStateSubject.onNext(OrderViewState.OrderInfoSate(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        orderStateSubject.onNext(OrderViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()
    }
}

sealed class OrderViewState {
    data class ErrorMessage(val errorMessage: String) : OrderViewState()
    data class SuccessMessage(val successMessage: String) : OrderViewState()
    data class LoadingState(val isLoading: Boolean) : OrderViewState()
    data class OrderInfoSate(val orderInfo: OrderResponse) : OrderViewState()
}