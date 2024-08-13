package com.hoxbox.terminal.ui.userstore.viewmodel

import com.hoxbox.terminal.api.userstore.UserStoreRepository
import com.hoxbox.terminal.api.userstore.model.*
import com.hoxbox.terminal.base.BaseViewModel
import com.hoxbox.terminal.base.extension.subscribeWithErrorParsing
import com.hoxbox.terminal.base.network.model.ErrorResult
import com.hoxbox.terminal.base.network.model.HotBoxError
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class UserStoreWelcomeViewModel(
    private val userStoreRepository: UserStoreRepository
) : BaseViewModel() {
    private val userStoreWelcomeStateSubject: PublishSubject<UserStoreWelcomeState> = PublishSubject.create()
    val userStoreWelcomeState: Observable<UserStoreWelcomeState> = userStoreWelcomeStateSubject.hide()

    fun loadCurrentStoreResponse(searchText :String) {
        userStoreRepository.getDeliveryAddress(searchText).doOnSubscribe {
            userStoreWelcomeStateSubject.onNext(UserStoreWelcomeState.LoadingState(true))
        }.doAfterTerminate {
            userStoreWelcomeStateSubject.onNext(UserStoreWelcomeState.LoadingState(false))
        }.subscribeWithErrorParsing<UserLocationInfo, HotBoxError>({
            userStoreWelcomeStateSubject.onNext(UserStoreWelcomeState.UserLocationInformation(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreWelcomeStateSubject.onNext(UserStoreWelcomeState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun userLocationStoreResponse(searchText :String,long :Double,lat :Double) {
        userStoreRepository.userLocationStoreResponse(searchText,long,lat).doOnSubscribe {
            userStoreWelcomeStateSubject.onNext(UserStoreWelcomeState.LoadingState(true))
        }.doAfterTerminate {
            userStoreWelcomeStateSubject.onNext(UserStoreWelcomeState.LoadingState(false))
        }.subscribeWithErrorParsing<NearByLocationResponse, HotBoxError>({
            userStoreWelcomeStateSubject.onNext(UserStoreWelcomeState.NearLocationInformation(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    userStoreWelcomeStateSubject.onNext(UserStoreWelcomeState.NearLocationErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    userStoreWelcomeStateSubject.onNext(UserStoreWelcomeState.NearLocationErrorMessage("Not found store for this address"))
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

}

sealed class UserStoreWelcomeState {
    data class ErrorMessage(val errorMessage: String) : UserStoreWelcomeState()
    data class NearLocationErrorMessage(val errorMessage: String) : UserStoreWelcomeState()
    data class SuccessMessage(val successMessage: String) : UserStoreWelcomeState()
    data class LoadingState(val isLoading: Boolean) : UserStoreWelcomeState()
    data class UserLocationInformation(val userLocationInfo: UserLocationInfo) : UserStoreWelcomeState()
    data class NearLocationInformation(val locationsInfo: NearByLocationResponse) : UserStoreWelcomeState()
}