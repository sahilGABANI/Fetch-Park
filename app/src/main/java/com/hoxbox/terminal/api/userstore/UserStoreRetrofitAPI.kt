package com.hoxbox.terminal.api.userstore

import com.hoxbox.terminal.api.menu.model.MenuListInfo
import com.hoxbox.terminal.api.menu.model.ProductsItem
import com.hoxbox.terminal.api.userstore.model.*
import com.hoxbox.terminal.base.network.model.HotBoxCommonResponse
import com.hoxbox.terminal.base.network.model.HotBoxResponse
import io.reactivex.Single
import retrofit2.http.*
import retrofit2.http.Query

interface UserStoreRetrofitAPI {

    @GET("v1/get-menus-products-by-location/{locationId}")
    fun getMenuProductByLocation(@Path("locationId") locationId: Int): Single<HotBoxResponse<MenuListInfo>>

    @GET("v1/get-product-details/{productId}/{menuGroupId}")
    fun getProductDetails(
        @Path("productId") productId: Int? = null, @Path("menuGroupId") menuGroupId: Int? = null
    ): Single<HotBoxResponse<ProductsItem>>

    @POST("v1/add-to-cart")
    fun addToCartProduct(@Body request: AddToCartRequest): Single<HotBoxResponse<AddToCartResponse>>

    @GET("v1/get-cart/{cartGroupId}")
    fun getCartDetails(@Path("cartGroupId") cartGroupId: Int): Single<HotBoxResponse<CartInfoDetails>>

    @POST("v1/update-cart/{cartId}")
    fun updateMenuItemQuantity(@Path("cartId") cartId: Int, @Body request: UpdateMenuItemQuantity): Single<HotBoxResponse<AddToCartDetails>>

    @POST("v1/delete-cart-item")
    fun deleteCartItem(@Body request: DeleteCartItemRequest): Single<HotBoxCommonResponse>

    @POST("v1/create-pos-order")
    fun createOrder(@Body request: CreateOrderRequest) :Single<HotBoxResponse<CreateOrderResponse>>

    @POST("v1/clear-cart")
    fun clearCart(@Body request: DeleteCartItemRequest) :Single<HotBoxCommonResponse>

    @GET("v1/get-next-available-time/{location_id}/{order_type_id}")
    fun getOrderPromisedTime(@Path("location_id") locationId :Int,@Path("order_type_id") orderTypeId :Int) :Single<HotBoxResponse<GetPromisedTime>>

    @GET("https://api.geoapify.com/v1/geocode/autocomplete")
    fun getOrderDeliveryAddress(@Query("text") text :String, @Query("apiKey") apiKey :String,@Query("filter")countryCode :String) :Single<UserLocationInfo>

    @GET("v1/get-nearby-shops")
    fun userLocationStoreResponse(@Query("address") text :String, @Query("long") long :Double, @Query("lat") lat :Double) :Single<HotBoxResponse<NearByLocationResponse>>

}