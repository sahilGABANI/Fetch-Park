package com.hoxbox.terminal.api.menu.model

import com.google.gson.annotations.SerializedName
import com.hoxbox.terminal.api.userstore.model.ModifiersItem

data class MenuSectionInfo(
    val productName: String,
    val productDescription: String,
    val productPrice: String,
    val productState: String,
)

data class MenuListInfo(

    @field:SerializedName("menus")
    val menus: List<MenusItem>? = null
)

data class MenusItem(

    @field:SerializedName("menu_group_id")
    val menuGroupId: Int? = null,

    @field:SerializedName("category_name")
    val categoryName: String? = null,

    @field:SerializedName("list_order")
    val listOrder: Int? = null,

    @field:SerializedName("active")
    val active: Int? = null,

    @field:SerializedName("id")
    val id: Int? = null,



    @field:SerializedName("products")
    val products: List<ProductsItem>? = null,

    var isSelected: Boolean = false
)

data class ProductsItem(

    @field:SerializedName("date_updated")
    val dateUpdated: Any? = null,

    @field:SerializedName("product_image")
    val productImage: String? = null,

    @field:SerializedName("date_created")
    val dateCreated: String? = null,

    @field:SerializedName("product_type_id")
    val productTypeId: Int? = null,

    @field:SerializedName("product_upccode")
    val productUpccode: String? = null,

    @field:SerializedName("product_name")
    val productName: String? = null,

    @field:SerializedName("product_category_id")
    val productCategoryId: Int? = null,

    @field:SerializedName("menu_group_id")
    val menuGroupId: Int? = null,

    @field:SerializedName("price")
    var price: Double? = null,

    @field:SerializedName("product_id")
    val productId: Int? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("product_active")
    val productActive: Int? = null,

    @field:SerializedName("product_description")
    val productDescription: String? = null,

    @field:SerializedName("menu_id")
    val menuId: Int? = null,

    @field:SerializedName("active")
    var active: Int? = null,

    @field:SerializedName("modifiers")
    val modifiers: List<ModifiersItem>? = null,

    var isActive: Boolean? =  false,

    @field:SerializedName("price_override")
    val priceOverride: Double? = null,
)

data class ProductStateRequest(
    @field:SerializedName("active")
    val active: Boolean? = false,

    @field:SerializedName("menu_group_id")
    val menuGroupId: Boolean? = null,

    @field:SerializedName("activation_time")
    val activationTime: String? = null,

    @field:SerializedName("expiration_time")
    val expirationTime: String? = null,
)

