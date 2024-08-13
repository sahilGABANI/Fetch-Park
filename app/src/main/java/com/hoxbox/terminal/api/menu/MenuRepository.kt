package com.hoxbox.terminal.api.menu

import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.menu.model.MenuListInfo
import com.hoxbox.terminal.api.menu.model.ProductStateRequest
import com.hoxbox.terminal.api.menu.model.ProductsItem
import com.hoxbox.terminal.base.network.HotBoxResponseConverter
import io.reactivex.Single

class MenuRepository(
    private val menuRetrofitAPI: MenuRetrofitAPI, private val loggedInUserCache: LoggedInUserCache
) {

    private val hotBoxResponseConverter: HotBoxResponseConverter = HotBoxResponseConverter()

    fun getMenuByLocation(): Single<MenuListInfo> {
        val locationId = loggedInUserCache.getLocationInfo()?.id ?: throw Exception("location not found")
        return menuRetrofitAPI.getMenuByLocation(locationId).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }

    fun updateMenuState(menuId :Int, request: ProductStateRequest): Single<ProductsItem> {
        return menuRetrofitAPI.updateMenuState(menuId,request).flatMap {
            hotBoxResponseConverter.convertToSingle(it)
        }
    }
}