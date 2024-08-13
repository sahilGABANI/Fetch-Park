package com.hoxbox.terminal.ui.login.viewmodel

import com.hoxbox.terminal.api.authentication.AuthenticationRepository
import com.hoxbox.terminal.api.authentication.model.HotBoxUser
import com.hoxbox.terminal.api.authentication.model.LoginCrewRequest
import com.hoxbox.terminal.api.order.OrderRepository
import com.hoxbox.terminal.api.order.model.OrderResponse
import com.hoxbox.terminal.api.store.StoreRepository
import com.hoxbox.terminal.api.store.model.StoreResponse
import com.hoxbox.terminal.base.BaseViewModel
import com.hoxbox.terminal.base.extension.subscribeWithErrorParsing
import com.hoxbox.terminal.base.network.model.ErrorResult
import com.hoxbox.terminal.base.network.model.HotBoxError
import com.hoxbox.terminal.ui.main.order.viewmodel.OrderViewState
import com.hoxbox.terminal.ui.main.store.viewmodel.StoreState
import com.hoxbox.terminal.ui.splash.viewmodel.LocationViewState
import com.hoxbox.terminal.ui.userstore.UserStoreActivity
import com.hoxbox.terminal.utils.Constants
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class LoginViewModel(
    private val authenticationRepository: AuthenticationRepository,
    private val storeRepository: StoreRepository,
    private val orderRepository: OrderRepository
) : BaseViewModel() {

    private val loginStateSubject: PublishSubject<LoginViewState> = PublishSubject.create()
    val loginState: Observable<LoginViewState> = loginStateSubject.hide()
    private var orderDataDisposable: Disposable? = null
    private var playDisposable: Disposable? = null

    fun loginCrew(request: LoginCrewRequest) {
        authenticationRepository.loginCrew(request).doOnSubscribe {
            loginStateSubject.onNext(LoginViewState.LoadingState(true))
        }.doAfterTerminate {
            loginStateSubject.onNext(LoginViewState.LoadingState(false))
        }.subscribeWithErrorParsing<HotBoxUser, HotBoxError>({
            loginStateSubject.onNext(LoginViewState.LoginSuccess)
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    loginStateSubject.onNext(LoginViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }


    fun loadCurrentStoreResponse() {
        storeRepository.getCurrentStoreInformation().doOnSubscribe {
            loginStateSubject.onNext(LoginViewState.LoadingState(true))
        }.doAfterTerminate {
            loginStateSubject.onNext(LoginViewState.LoadingState(false))
        }.subscribeWithErrorParsing<StoreResponse, HotBoxError>({
            it.locationTaxRate?.let { it1 -> Constants.setTaxRate(it1) }
            loginStateSubject.onNext(LoginViewState.StoreResponses(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    loginStateSubject.onNext(LoginViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun loadOrderData(random: Int) {
        Observable.interval(random.toLong(), TimeUnit.SECONDS).startWith(0L)
            .flatMap { orderRepository.getNewOrderData("", "All", "new").toObservable() }.doOnError {
                OrderResponse(orders = arrayListOf())
            }.subscribeWithErrorParsing<OrderResponse, HotBoxError>({
                println("OkHttpClient :LoginViewModel")
                loginStateSubject.onNext(LoginViewState.OrderInfoSate(it))
            }, {
                when (it) {
                    is ErrorResult.ErrorMessage -> {
                        loginStateSubject.onNext(LoginViewState.ErrorMessage(it.errorResponse.safeErrorMessage))
                    }

                    is ErrorResult.ErrorThrowable -> {
                        Timber.e(it.throwable)
                    }
                }
           }).autoDispose()
    }

    fun playMusic() {
        playDisposable?.dispose()
        playDisposable = Observable.interval(20, TimeUnit.SECONDS).startWith(0L).flatMap {
            orderRepository.playMusic().toObservable()
        }.subscribeWithErrorParsing<Boolean, HotBoxError>({
            if (it) {
                loginStateSubject.onNext(LoginViewState.PlayMusic)
            }
        }, {

        })

    }

    fun stopMusic() {
        orderRepository.setPlayMusicFalse()

    }
}

sealed class LoginViewState {
    data class ErrorMessage(val errorMessage: String) : LoginViewState()
    data class SuccessMessage(val successMessage: String) : LoginViewState()
    data class LoadingState(val isLoading: Boolean) : LoginViewState()
    data class StoreResponses(val storeResponse: StoreResponse) : LoginViewState()
    object LoginSuccess : LoginViewState()
    object PlayMusic : LoginViewState()
    data class OrderInfoSate(val bufferResponse: OrderResponse) : LoginViewState()
}