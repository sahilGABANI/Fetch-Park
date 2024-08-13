package com.hoxbox.terminal.ui.splash.viewmodel

import com.hoxbox.terminal.api.authentication.AuthenticationRepository
import com.hoxbox.terminal.api.authentication.model.LocationResponse
import com.hoxbox.terminal.api.order.OrderRepository
import com.hoxbox.terminal.api.order.model.OrderResponse
import com.hoxbox.terminal.base.BaseViewModel
import com.hoxbox.terminal.base.extension.retryWithDelay
import com.hoxbox.terminal.base.extension.subscribeWithErrorParsing
import com.hoxbox.terminal.base.network.model.ErrorResult
import com.hoxbox.terminal.base.network.model.HotBoxError
import com.hoxbox.terminal.ui.login.viewmodel.LoginViewState
import com.hoxbox.terminal.ui.main.store.viewmodel.StoreState
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class LocationViewModel(
    private val authenticationRepository: AuthenticationRepository,
    private val orderRepository : OrderRepository
) : BaseViewModel() {
    private val locationStateSubject: PublishSubject<LocationViewState> = PublishSubject.create()
    val locationState: Observable<LocationViewState> = locationStateSubject.hide()

    private var locationResponse: LocationResponse? = null
    private var orderDataDisposable: Disposable? = null

    fun loadLocation(serialNumber: String) {
        //14b0fbb66d3cac64
        authenticationRepository.getLocation(serialNumber)
            .doOnSubscribe {
                locationStateSubject.onNext(LocationViewState.LoadingState(true))
                locationStateSubject.onNext(LocationViewState.StartButtonState(false))
            }
            .doAfterTerminate {
                locationStateSubject.onNext(LocationViewState.LoadingState(false))
            }
            .doOnError {
                locationStateSubject.onNext(LocationViewState.ErrorMessage("No Location Set"))
            }
            .retryWithDelay(15, 15000)
            .subscribeWithErrorParsing<LocationResponse, HotBoxError>({
                locationResponse = it
                locationStateSubject.onNext(LocationViewState.LocationData(it))
                locationStateSubject.onNext(LocationViewState.StartButtonState(true))
            }, {
                locationStateSubject.onNext(LocationViewState.StartButtonState(false))
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        locationStateSubject.onNext(LocationViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }
                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
            }).autoDispose()

    }

    fun clickOnStartButton() {
        locationResponse?.let {
            locationStateSubject.onNext(LocationViewState.OpenLoginScreen(it))
        } ?: run {
            locationStateSubject.onNext(LocationViewState.ErrorMessage("No location found"))
        }
    }

    fun loadOrderData(random: Int) {
        Observable.interval(random.toLong(), TimeUnit.SECONDS).startWith(0L)
            .flatMap { orderRepository.getNewOrderData("", "All", "new").toObservable() }
            .doOnError {
                OrderResponse(orders = arrayListOf())
            }
            .subscribeWithErrorParsing<OrderResponse, HotBoxError>({
                println("OkHttpClient :LocationViewModel")
                locationStateSubject.onNext(LocationViewState.OrderInfoSate(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        locationStateSubject.onNext(LocationViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
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
                locationStateSubject.onNext(LocationViewState.PlayMusic)
            }
        }, {

        })
    }

    fun stopMusic() {
        orderRepository.setPlayMusicFalse()

    }

    fun clear() {
        clearCompositeDisposable()
    }
}

sealed class LocationViewState {
    data class ErrorMessage(val errorMessage: String) : LocationViewState()
    data class SuccessMessage(val successMessage: String) : LocationViewState()
    data class LoadingState(val isLoading: Boolean) : LocationViewState()
    data class StartButtonState(val isVisible: Boolean) : LocationViewState()
    data class LocationData(val locationResponse: LocationResponse) : LocationViewState()
    data class OpenLoginScreen(val locationResponse: LocationResponse) : LocationViewState()
    data class OrderInfoSate(val bufferResponse: OrderResponse) : LocationViewState()
    object PlayMusic : LocationViewState()
}