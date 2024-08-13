package com.hoxbox.terminal.api.userstore.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.hoxbox.terminal.api.order.model.MenuItemModifiersItem
import com.hoxbox.terminal.api.order.model.OptionsItem
import com.hoxbox.terminal.api.stripe.model.PaymentStatus
import kotlinx.parcelize.Parcelize
import java.lang.reflect.Type


data class SubOrderItemData(
    var productName: String,

    var subProductList: List<OptionsItem>? = null,

    var optionImage: String? = null,

    var optionsItem: OptionsItem? = null,
    val modifiers: List<ModifiersItem>? = null,
)

data class AddToCartRequest(

    @field:SerializedName("promised_time")
    val promisedTime: String? = null,

    @field:SerializedName("menu_item_instructions")
    val menuItemInstructions: String? = null,

    @field:SerializedName("order_type_id")
    val orderTypeId: Int? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("mode_id")
    val modeId: Int? = null,

    @field:SerializedName("menu_item_quantity")
    val menuItemQuantity: Int? = null,

    @field:SerializedName("menu_item_modifiers")
    val menuItemModifiers: List<MenuItemModifiersItemRequest>? = null,

    @field:SerializedName("location_id")
    val locationId: Int? = null,

    @field:SerializedName("menu_id")
    val menuId: Int? = null,

    @field:SerializedName("cart_group_id")
    val cartGroupId: Int? = null,


    )

data class MenuItemModifiersItemRequest(

    @field:SerializedName("select_max")
    val selectMax: Int? = null,

    @field:SerializedName("is_required")
    val isRequired: Int? = null,

    @field:SerializedName("pmg_active")
    val pmgActive: Int? = null,

    @field:SerializedName("product_id")
    val productId: Int? = null,

    @field:SerializedName("options")
    val options: List<OptionsItemRequest>? = arrayListOf(),

    @field:SerializedName("active")
    val active: Int? = null,

    @field:SerializedName("group_by")
    val groupBy: Int? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("modification_text")
    val modificationText: String? = null,

    @field:SerializedName("select_min")
    val selectMin: Int? = null
)

data class MenuItemModifiersGuestItemRequest(

    @field:SerializedName("select_max")
    val selectMax: Int? = null,

    @field:SerializedName("is_required")
    val isRequired: Int? = null,

    @field:SerializedName("pmg_active")
    val pmgActive: Int? = null,

    @field:SerializedName("product_id")
    val productId: Int? = null,

    @field:SerializedName("options")
    val options: OptionsItemRequest? = null,

    @field:SerializedName("active")
    val active: Int? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("modification_text")
    val modificationText: String? = null,

    @field:SerializedName("select_min")
    val selectMin: Int? = null
)

data class ModifiersItem(

    @field:SerializedName("date_updated")
    val dateUpdated: Any? = null,

    @field:SerializedName("date_created")
    val dateCreated: Any? = null,

    @field:SerializedName("pmg_active")
    val pmgActive: Int? = null,

    @field:SerializedName("mod_group_id")
    val modGroupId: Any? = null,

    @field:SerializedName("active")
    val active: Int? = null,

    @field:SerializedName("group_by")
    val groupBy: Int? = null,

    @field:SerializedName("product_category_id")
    val productCategoryId: Int? = null,

    @field:SerializedName("select_max")
    val selectMax: Int? = null,

    @field:SerializedName("is_required")
    val isRequired: Int? = null,

    @field:SerializedName("product_id")
    val productId: Int? = null,

    @field:SerializedName("options")
    var options: List<OptionsItem>? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("modification_text")
    val modificationText: String? = null,

    @field:SerializedName("select_min")
    val selectMin: Int? = null,

    var selectedOption: Int? = 0,
    var selectedOptionsItem: ArrayList<OptionsItemRequest> = ArrayList<OptionsItemRequest>()
)

data class OptionsItemRequest(

    @field:SerializedName("option_price")
    var optionPrice: Double? = null,

    @field:SerializedName("mod_group_id")
    var modGroupId: Int? = null,

    @field:SerializedName("active")
    val active: Int? = null,

    @field:SerializedName("option_name")
    var optionName: String? = null,

    @field:SerializedName("id")
    var id: Int? = null,

    @field:SerializedName("modifier_qyt")
    var modifierQyt: Int? = 1,
)

data class AddToCartResponse(

    @field:SerializedName("cart")
    val cart: AddToCartDetails? = null
)

data class UpdateMenuItemQuantity(

    @field:SerializedName("menu_item_quantity")
    val menuItemQuantity: Int? = null
)

data class DeleteCartItemRequest(
    @field:SerializedName("id")
    val id: Int? = null
)


data class AddToCartDetails(

    @field:SerializedName("cart_group_id")
    val cartGroupId: Int? = null,

    @field:SerializedName("menu_item_quantity")
    val menuItemQuantity: Int? = null,

    @field:SerializedName("menu_item_price")
    val menuItemPrice: Double? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("menu_item_instructions")
    val menuItemInstructions: Any? = null,

    @field:SerializedName("menu_item_modifiers")
    val menuItemModifiers: String? = null,

    @field:SerializedName("menu_id")
    val menuId: Int? = null
) {
    fun getData(): List<MenuItemModifiersItemRequest> {
        val gson = Gson()
        val jsonOutput = menuItemModifiers
        val listType: Type = object : TypeToken<List<MenuItemModifiersItemRequest>>() {}.type
        val posts: List<MenuItemModifiersItemRequest> = gson.fromJson(jsonOutput, listType)

        return posts
    }
}

data class CartInfoDetails(

    @field:SerializedName("charges")
    val charges: Charges? = null,

    @field:SerializedName("tip")
    val tip: List<String>? = null,

    @field:SerializedName("cart")
    val cart: List<CartItem>? = null
)

data class Charges(

    @field:SerializedName("delivery_fee")
    val deliveryFee: Int? = null,

    @field:SerializedName("tax")
    val tax: Double? = null
)

data class CartItem(

    @field:SerializedName("promised_time")
    val promisedTime: String? = null,

    @field:SerializedName("order_type_id")
    val orderTypeId: Int? = null,

    @field:SerializedName("product_image")
    val productImage: String? = null,

    @field:SerializedName("product_type_id")
    val productTypeId: Int? = null,

    @field:SerializedName("menu_item_instructions")
    val menuItemInstructions: String? = null,

    @field:SerializedName("product_name")
    val productName: String? = null,

    @field:SerializedName("location_id")
    val locationId: Int? = null,

    @field:SerializedName("cart_group_id")
    val cartGroupId: Int? = null,

    @field:SerializedName("menu_item_quantity")
    var menuItemQuantity: Int? = null,

    @field:SerializedName("menu_item_price")
    var menuItemPrice: Double? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("menu_item_modifiers")
    val menuItemModifiers: List<MenuItemModifiersItem>? = null,

    @field:SerializedName("product_description")
    val productDescription: String? = null,

    @field:SerializedName("menu_id")
    val menuId: Int? = null,

    var isChanging: Boolean? = true,

    var isVisibleComp: Boolean? = false,
    var productComp: Int? = 0
)

data class CreateOrderRequest(

    @field:SerializedName("order_user_id")
    val orderUserId: Int? = null,

    @field:SerializedName("order_mode_id")
    val orderModeId: Int? = null,

    @field:SerializedName("order_tip")
    val orderTip: Int? = null,

    @field:SerializedName("order_type_id")
    val orderTypeId: Int? = null,

    @field:SerializedName("order_cart_group_id")
    val orderCartGroupId: Int? = null,

    @field:SerializedName("gift_card_id")
    val giftCardId: Int? = null,

    @field:SerializedName("order_gift_card_amount")
    val orderGiftCardAmount: Int? = null,

    @field:SerializedName("coupon_code_id")
    val couponCodeId: Int? = null,

    @field:SerializedName("order_instructions")
    val orderInstructions: String? = null,

    @field:SerializedName("order_promised_time")
    val orderPromisedTime: String? = null,

    @field:SerializedName("order_delivery_fee")
    val orderDeliveryFee: Int? = null,

    @field:SerializedName("order_tax")
    val orderTax: Double? = null,

    @field:SerializedName("order_location_id")
    val orderLocationId: Int? = null,

    @field:SerializedName("order_total")
    val orderTotal: Double? = null,

    @field:SerializedName("order_subtotal")
    val orderSubtotal: Double? = null,

    @field:SerializedName("customer_id")
    val customerId: String? = null,

    @field:SerializedName("delivery_tier")
    val deliveryTier: String? = null,

    @field:SerializedName("transaction_amount")
    val transactionAmount: String? = null,

    @field:SerializedName("transaction_charge_id")
    val transactionChargeId: String? = null,

    @field:SerializedName("transaction_id_of_processor")
    val transactionIdOfProcessor: String? = null,

    @field:SerializedName("lat")
    val lat: String? = null,

    @field:SerializedName("long")
    val long: String? = null,

    @field:SerializedName("order_adjustments")
    val orderAdjustments: Int? = null,

    @field:SerializedName("delivery_address")
    val deliveryAddress: String? = null,

    @field:SerializedName("guest_name")
    val guestName: String? = null,

    @field:SerializedName("guest_phone")
    val guestPhone: String? = null,

    @field:SerializedName("credit_amount")
    val creditAmount: Int? = null,

    )

data class CreateOrderResponse(

    @field:SerializedName("order_user_id")
    val orderUserId: Any? = null,

    @field:SerializedName("order_mode_id")
    val orderModeId: Int? = null,

    @field:SerializedName("order_transaction_id")
    val orderTransactionId: Int? = null,

    @field:SerializedName("order_tip")
    val orderTip: Any? = null,

    @field:SerializedName("order_type_id")
    val orderTypeId: Int? = null,

    @field:SerializedName("order_cart_group_id")
    val orderCartGroupId: Int? = null,

    @field:SerializedName("credit_amount")
    val creditAmount: Int? = null,

    @field:SerializedName("order_gift_card_amount")
    val orderGiftCardAmount: Int? = null,

    @field:SerializedName("order_instructions")
    val orderInstructions: Any? = null,

    @field:SerializedName("order_promised_time")
    val orderPromisedTime: String? = null,

    @field:SerializedName("coupon_code_id")
    val couponCodeId: Any? = null,

    @field:SerializedName("order_delivery_fee")
    val orderDeliveryFee: Int? = null,

    @field:SerializedName("order_tax")
    val orderTax: Int? = null,

    @field:SerializedName("order_location_id")
    val orderLocationId: Int? = null,

    @field:SerializedName("order_coupon_code_discount")
    val orderCouponCodeDiscount: Int? = null,

    @field:SerializedName("order_creation_date")
    val orderCreationDate: String? = null,

    @field:SerializedName("order_total")
    val orderTotal: Int? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("order_subtotal")
    val orderSubtotal: Int? = null,

    @field:SerializedName("gift_card_id")
    val giftCardId: Any? = null
)

data class GetPromisedTime(

    @field:SerializedName("time")
    val time: String? = null
)

data class OrderPrice(
    var orderTotal: Double? = null,
    var orderSubtotal: Double? = null,
    var orderTax: Double? = null,
    var employeeDiscount: Double? = null,
    var adjustmentDiscount: Double? = null,
    var mhsMemberDiscount: Double? = null
)

data class UserDetails(
    val name: String? = null,
    val surName: String? = null,
    val phone: String? = null,
    val email: String? = null,
)

data class UserLocationInfo(

    @field:SerializedName("features")
    val features: List<FeaturesItem>? = null,

    @field:SerializedName("query")
    val query: Query? = null,

    @field:SerializedName("type")
    val type: String? = null,

    @field:SerializedName("message")
    val message: String? = null,

)

data class Timezone(

    @field:SerializedName("offset_DST")
    val offsetDST: String? = null,

    @field:SerializedName("offset_DST_seconds")
    val offsetDSTSeconds: Int? = null,

    @field:SerializedName("offset_STD_seconds")
    val offsetSTDSeconds: Int? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("abbreviation_DST")
    val abbreviationDST: String? = null,

    @field:SerializedName("offset_STD")
    val offsetSTD: String? = null,

    @field:SerializedName("abbreviation_STD")
    val abbreviationSTD: String? = null
)

data class Properties(

    @field:SerializedName("country")
    val country: String? = null,

    @field:SerializedName("result_type")
    val resultType: String? = null,

    @field:SerializedName("city")
    val city: String? = null,

    @field:SerializedName("formatted")
    val formatted: String? = null,

    @field:SerializedName("timezone")
    val timezone: Timezone? = null,

    @field:SerializedName("county")
    val county: String? = null,

    @field:SerializedName("postcode")
    val postcode: String? = null,

    @field:SerializedName("lon")
    val lon: Double? = null,

    @field:SerializedName("country_code")
    val countryCode: String? = null,

    @field:SerializedName("address_line2")
    val addressLine2: String? = null,

    @field:SerializedName("address_line1")
    val addressLine1: String? = null,

    @field:SerializedName("datasource")
    val datasource: Datasource? = null,

    @field:SerializedName("district")
    val district: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("suburb")
    val suburb: String? = null,

    @field:SerializedName("rank")
    val rank: Rank? = null,

    @field:SerializedName("state")
    val state: String? = null,

    @field:SerializedName("state_code")
    val stateCode: String? = null,

    @field:SerializedName("category")
    val category: String? = null,

    @field:SerializedName("lat")
    val lat: Double? = null,

    @field:SerializedName("place_id")
    val placeId: String? = null,

    @field:SerializedName("state_district")
    val stateDistrict: String? = null,

    @field:SerializedName("village")
    val village: String? = null,

    @field:SerializedName("street")
    val street: String? = null
)

data class Query(

    @field:SerializedName("text")
    val text: String? = null
)

data class FeaturesItem(

    @field:SerializedName("bbox")
    val bbox: List<Any?>? = null,

    @field:SerializedName("geometry")
    val geometry: Geometry? = null,

    @field:SerializedName("type")
    val type: String? = null,

    @field:SerializedName("properties")
    val properties: Properties? = null
)

data class Datasource(

    @field:SerializedName("license")
    val license: String? = null,

    @field:SerializedName("attribution")
    val attribution: String? = null,

    @field:SerializedName("sourcename")
    val sourcename: String? = null,

    @field:SerializedName("url")
    val url: String? = null
)

data class Rank(

    @field:SerializedName("importance")
    val importance: Any? = null,

    @field:SerializedName("confidence")
    val confidence: Any? = null,

    @field:SerializedName("confidence_city_level")
    val confidenceCityLevel: Int? = null,

    @field:SerializedName("match_type")
    val matchType: String? = null
)

data class Geometry(

    @field:SerializedName("coordinates")
    val coordinates: List<Any?>? = null,

    @field:SerializedName("type")
    val type: String? = null
)
@Parcelize
data class NearByLocationResponse(
    @field:SerializedName("locations")
    val locations :ArrayList<LocationsItem>? = null
) : Parcelable
@Parcelize
data class LocationsItem(

    @field:SerializedName("is_holiday")
    val isHoliday: Int? = null,

    @field:SerializedName("distance")
    val distance: String? = null,

    @field:SerializedName("location_country")
    val locationCountry: String? = null,

    @field:SerializedName("wednesday_open_time")
    val wednesdayOpenTime: String? = null,

    @field:SerializedName("friday_close_time")
    val fridayCloseTime: String? = null,

    @field:SerializedName("location_state")
    val locationState: String? = null,

    @field:SerializedName("friday_open_time")
    val fridayOpenTime: String? = null,

    @field:SerializedName("saturday_open_time")
    val saturdayOpenTime: String? = null,

    @field:SerializedName("close_hour")
    val closeHour: String? = null,

    @field:SerializedName("monday_close_time")
    val mondayCloseTime: String? = null,

    @field:SerializedName("holiday_open_hour")
    val holidayOpenHour: String? = null,

    @field:SerializedName("location_city")
    val locationCity: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("location_zip")
    val locationZip: String? = null,

    @field:SerializedName("open_hour")
    val openHour: String? = null,

    @field:SerializedName("thursday_open_time")
    val thursdayOpenTime: String? = null,

    @field:SerializedName("saturday_close_time")
    val saturdayCloseTime: String? = null,

    @field:SerializedName("thursday_close_time")
    val thursdayCloseTime: String? = null,

    @field:SerializedName("is_open")
    val isOpen: Boolean? = null,

    @field:SerializedName("location_timezone")
    val locationTimezone: Int? = null,

    @field:SerializedName("tuesday_open_time")
    val tuesdayOpenTime: String? = null,

    @field:SerializedName("sunday_open_time")
    val sundayOpenTime: String? = null,

    @field:SerializedName("tuesday_close_time")
    val tuesdayCloseTime: String? = null,

    @field:SerializedName("location_name")
    val locationName: String? = null,

    @field:SerializedName("location_address_1")
    val locationAddress1: String? = null,

    @field:SerializedName("location_address_2")
    val locationAddress2: String? = null,

    @field:SerializedName("holiday_close_hour")
    val holidayCloseHour: String? = null,

    @field:SerializedName("wednesday_close_time")
    val wednesdayCloseTime: String? = null,

    @field:SerializedName("monday_open_time")
    val mondayOpenTime: String? = null,

    @field:SerializedName("sunday_close_time")
    val sundayCloseTime: String? = null,

    @field:SerializedName("location_phone")
    val locationPhone: String? = null
): Parcelable {
    fun getSafeAddressName(): String {
        val addressStringBuilder = StringBuilder().apply {
            if (locationAddress1 != null) {
                append(locationAddress1)
            }
            if (locationAddress2 != null) {
                append(locationAddress2)
            }
            if (locationCity != null) {
                append(", $locationCity")
            }
            if (locationState != null) {
                append(", $locationState")
            }
            if (locationZip != null) {
                append(", $locationZip")
            }
            if (locationCountry != null) {
                append(", $locationCountry")
            }
        }
        return addressStringBuilder.toString()
    }
}

@Keep
enum class AdjustmentType {
    ADJUSTMENT_POSITIVE_TYPE,
    ADJUSTMENT_NEGATIVE_TYPE,

}
data class ResponseItem(

    @field:SerializedName("host_response_code")
    val hostResponseCode: String? = null,

    @field:SerializedName("host_transaction_reference")
    val hostTransactionReference: String? = null,

    @field:SerializedName("status")
    val status: String? = null,

    @field:SerializedName("tip_amount")
    val tipAmount: String? = null,

    @field:SerializedName("total_amount")
    val totalAmount: String? = null,

    @field:SerializedName("authorization_no")
    val authorizationNo: String? = null,

    @field:SerializedName("transaction_amount")
    val transactionAmount: String? = null,

    @field:SerializedName("host_response_text")
    val hostResponseText: String? = null,

    @field:SerializedName("merchant_id")
    val merchantId: String? = null,

    @field:SerializedName("type")
    val type: String? = null,

    @field:SerializedName("tac_denial")
    val tacDenial: String? = null,

    @field:SerializedName("customer_card_description")
    val customerCardDescription: String? = null,

    @field:SerializedName("tac_default")
    val tacDefault: String? = null,

    @field:SerializedName("transaction_time")
    val transactionTime: String? = null,

    @field:SerializedName("customer_language")
    val customerLanguage: String? = null,

    @field:SerializedName("avs_result")
    val avsResult: String? = null,

    @field:SerializedName("terminal_id")
    val terminalId: String? = null,

    @field:SerializedName("transaction_date")
    val transactionDate: String? = null,

    @field:SerializedName("batch_no")
    val batchNo: String? = null,

    @field:SerializedName("cvm_result")
    val cvmResult: String? = null,

    @field:SerializedName("host_response_isocode")
    val hostResponseIsocode: String? = null,

    @field:SerializedName("tac_online")
    val tacOnline: String? = null,
    @field:SerializedName("customer_name")
    val customerName: String? = null,

    @field:SerializedName("reference_no")
    val referenceNo: String? = null
) {
    fun getPaymentStatus(): PaymentStatus {
        return when (status) {
            "cancelled_by_user" -> PaymentStatus.CancelledByUser
            "decline_by_host_or_card" -> PaymentStatus.DeclineByHostOrCard
            "approved" -> PaymentStatus.Success
            "timeout_on_user_input" -> PaymentStatus.TimeoutOnUserInput
            else -> PaymentStatus.InProgress
        }
    }
}

data class CaptureNewPaymentRequest(

    @field:SerializedName("iConnRESTRequest")
    val iConnRESTRequest: IConnRESTRequest? = null,


    @field:SerializedName("endpoint")
    val endpoint: String = "/tsi/v1/payment",

    @field:SerializedName("resource")
    val resource: Resource? = null
)

data class IConnRESTRequest(

    @field:SerializedName("posAccessKey")
    val posAccessKey: String? = null,

    @field:SerializedName("terminalAccessKey")
    val terminalAccessKey: String? = null
)

data class Resource(

    @field:SerializedName("amount")
    val amount: Int,

    @field:SerializedName("type")
    val type: String? = "sale"
)


@Keep
enum class SendReceiptType {
    Email,
    Phone,
    EmailAndPhone,
    Nothing

}