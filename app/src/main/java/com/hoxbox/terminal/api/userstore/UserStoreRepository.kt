package com.hoxbox.terminal.api.userstore

import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.menu.model.MenuListInfo
import com.hoxbox.terminal.api.menu.model.ProductsItem
import com.hoxbox.terminal.api.userstore.model.*
import com.hoxbox.terminal.base.network.HotBoxResponseConverter
import com.hoxbox.terminal.base.network.model.HotBoxCommonResponse
import com.hoxbox.terminal.utils.Constants.COUNTRYCODE
import com.hoxbox.terminal.utils.Constants.DELIVERY_ADDRESS_API_KEY
import com.hoxbox.terminal.utils.Constants.ORDER_TYPE_ID
import io.reactivex.Single

class UserStoreRepository(private val userStoreRetrofitAPI: UserStoreRetrofitAPI, private val loggedInUserCache: LoggedInUserCache) {

    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()


    fun getMenuProductByLocation(): Single<MenuListInfo> {
        val locationId = loggedInUserCache.getLocationInfo()?.id ?: throw Exception("location not found")
        return userStoreRetrofitAPI.getMenuProductByLocation(locationId).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }

    fun getMenuProductByLocation(productId: Int?, menuGroupId: Int?): Single<ProductsItem> {
        return userStoreRetrofitAPI.getProductDetails(productId, menuGroupId).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }

    fun addToCartProduct(request: AddToCartRequest): Single<AddToCartResponse> {
        return userStoreRetrofitAPI.addToCartProduct(request).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }

    fun getCartDetails(cartGroupId: Int): Single<CartInfoDetails> {
        return userStoreRetrofitAPI.getCartDetails(cartGroupId).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }

    fun updateMenuItemQuantity(cartId: Int,request : UpdateMenuItemQuantity) : Single<AddToCartDetails> {
        return userStoreRetrofitAPI.updateMenuItemQuantity(cartId,request).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }

    fun deleteCartItem(request : DeleteCartItemRequest) : Single<HotBoxCommonResponse> {
        return userStoreRetrofitAPI.deleteCartItem(request).flatMap {
            hotBoxResponseConverter.convertCommonResponse(it)
        }
    }

    fun createOrder(request: CreateOrderRequest) :Single<CreateOrderResponse> {
        return userStoreRetrofitAPI.createOrder(request).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }
    fun clearCart(request : DeleteCartItemRequest): Single<HotBoxCommonResponse> {
        return userStoreRetrofitAPI.clearCart(request).flatMap {
            hotBoxResponseConverter.convertCommonResponse(it)
        }
    }

    fun getOrderPromisedTime(): Single<GetPromisedTime> {
        val locationId = loggedInUserCache.getLocationInfo()?.id ?: throw Exception("location not found")
        return userStoreRetrofitAPI.getOrderPromisedTime(locationId,ORDER_TYPE_ID).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }

    fun getDeliveryAddress(searchText :String): Single<UserLocationInfo> {
        return userStoreRetrofitAPI.getOrderDeliveryAddress(searchText,DELIVERY_ADDRESS_API_KEY,COUNTRYCODE)
    }

    fun userLocationStoreResponse(searchText :String,long :Double,lat :Double): Single<NearByLocationResponse> {
        return userStoreRetrofitAPI.userLocationStoreResponse(searchText,long,lat).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }
}