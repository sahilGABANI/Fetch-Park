package com.hoxbox.terminal.ui.main.menu.viewModel

import com.hoxbox.terminal.api.menu.MenuRepository
import com.hoxbox.terminal.api.menu.model.MenuListInfo
import com.hoxbox.terminal.api.menu.model.MenusItem
import com.hoxbox.terminal.api.menu.model.ProductStateRequest
import com.hoxbox.terminal.api.menu.model.ProductsItem
import com.hoxbox.terminal.base.BaseViewModel
import com.hoxbox.terminal.base.extension.subscribeWithErrorParsing
import com.hoxbox.terminal.base.network.model.ErrorResult
import com.hoxbox.terminal.base.network.model.HotBoxError
import com.hoxbox.terminal.utils.Constants
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class MenuViewModel(private val menuRepository: MenuRepository) : BaseViewModel() {

    private val menuStateSubject: PublishSubject<MenuState> = PublishSubject.create()
    val menuState: Observable<MenuState> = menuStateSubject.hide()
    private var listOfSelectCategoryMenuItem: List<MenusItem>? = null

    fun getMenuByLocation(isCheck: String, categorySelection: String) {
        menuRepository.getMenuByLocation().doOnSubscribe {
            menuStateSubject.onNext(MenuState.LoadingState(true))
        }.doAfterTerminate {
            menuStateSubject.onNext(MenuState.LoadingState(false))
        }.subscribeWithErrorParsing<MenuListInfo, HotBoxError>({ menuListInfo ->
            when (isCheck) {
                Constants.CHECK_ALL -> {
                    if (categorySelection != Constants.CATEGORY_FILTER_ALL) {
                        listOfSelectCategoryMenuItem = menuListInfo.menus?.filter { it.categoryName == categorySelection }
                        if (listOfSelectCategoryMenuItem?.size != 0) {
                            menuStateSubject.onNext(MenuState.ProductItemInfo(listOfSelectCategoryMenuItem?.get(0)?.products))
                        } else {
                            menuStateSubject.onNext(MenuState.ProductItemInfo(null))
                        }
                    } else {
                        val products = mutableListOf<ProductsItem>()
                        menuListInfo.menus?.forEach {
                            products.addAll(it.products ?: listOf())
                        }
                        menuStateSubject.onNext(MenuState.ProductItemInfo(products))
                    }
                }
                Constants.CHECK_AVAILABLE -> {
                    if (categorySelection != Constants.CATEGORY_FILTER_ALL) {
                        listOfSelectCategoryMenuItem = menuListInfo.menus?.filter { it.categoryName == categorySelection }
                        if (listOfSelectCategoryMenuItem?.size != 0) {
                            menuStateSubject.onNext(MenuState.ProductItemInfo(listOfSelectCategoryMenuItem?.get(0)?.products?.filter { item -> item.active == 1 }))
                        } else {
                            menuStateSubject.onNext(MenuState.ProductItemInfo(null))
                        }
                    } else {
                        val products = mutableListOf<ProductsItem>()
                        menuListInfo.menus?.forEach {
                            products.addAll(it.products?.filter { item -> item.active == 1 } ?: listOf())
                        }
                        menuStateSubject.onNext(MenuState.ProductItemInfo(products))
                    }
                }
                Constants.CHECK_UNAVAILABLE -> {
                    if (categorySelection != Constants.CATEGORY_FILTER_ALL) {
                        listOfSelectCategoryMenuItem = menuListInfo.menus?.filter { it.categoryName == categorySelection }
                        if (listOfSelectCategoryMenuItem?.size != 0) {
                            menuStateSubject.onNext(MenuState.ProductItemInfo(listOfSelectCategoryMenuItem?.get(0)?.products?.filter { item -> item.active == 0 }))
                        } else {
                            menuStateSubject.onNext(MenuState.ProductItemInfo(null))
                        }
                    } else {
                        val products = mutableListOf<ProductsItem>()
                        menuListInfo.menus?.forEach {
                            products.addAll(it.products?.filter { item -> item.active == 0 } ?: listOf())
                        }
                        menuStateSubject.onNext(MenuState.ProductItemInfo(products))
                    }
                }
            }
            menuStateSubject.onNext(MenuState.MenuItemInfo(menuListInfo.menus))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    menuStateSubject.onNext(MenuState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun updateMenuState(request: ProductStateRequest, menuId: Int) {
        menuRepository.updateMenuState(menuId, request).doOnSubscribe {
            menuStateSubject.onNext(MenuState.LoadingState(true))
        }.doAfterTerminate {
            menuStateSubject.onNext(MenuState.LoadingState(false))
        }.subscribeWithErrorParsing<ProductsItem, HotBoxError>({
            menuStateSubject.onNext(MenuState.UpdatedProductItemResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    menuStateSubject.onNext(MenuState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }
}

sealed class MenuState {
    data class ErrorMessage(val errorMessage: String) : MenuState()
    data class SuccessMessage(val successMessage: String) : MenuState()
    data class LoadingState(val isLoading: Boolean) : MenuState()
    data class MenuItemInfo(val MenuItemInfo: List<MenusItem>?) : MenuState()
    data class ProductItemInfo(val ProductsItem: List<ProductsItem>?) : MenuState()
    data class UpdatedProductItemResponse(val MenuItemInfo: ProductsItem) : MenuState()

}