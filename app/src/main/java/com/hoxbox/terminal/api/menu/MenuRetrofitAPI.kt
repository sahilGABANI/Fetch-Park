package com.hoxbox.terminal.api.menu

import com.hoxbox.terminal.api.menu.model.MenuListInfo
import com.hoxbox.terminal.api.menu.model.ProductStateRequest
import com.hoxbox.terminal.api.menu.model.ProductsItem
import com.hoxbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MenuRetrofitAPI {

    @GET("v1/get-menus-products-by-location/{locationId}")
    fun getMenuByLocation(@Path("locationId") locationId: Int): Single<HotBoxResponse<MenuListInfo>>

    @POST("v1/update-menu/{menuId}")
    fun updateMenuState(@Path("menuId") menuId: Int, @Body request: ProductStateRequest): Single<HotBoxResponse<ProductsItem>>
}