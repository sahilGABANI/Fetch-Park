package com.hoxbox.terminal.api.order.model

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.hoxbox.terminal.base.extension.toDollar

@Keep
data class OrderResponse(

    @field:SerializedName("orders")
    val orders: List<OrdersInfo>? = null
)

@Keep
data class OrdersInfo(

    @field:SerializedName("order_user_id")
    val orderUserId: Int? = null,

    @field:SerializedName("user_email")
    val userEmail: String? = null,

    @field:SerializedName("order_mode_id")
    val orderModeId: Int? = null,

    @field:SerializedName("order_transaction_id")
    val orderTransactionId: Int? = null,

    @field:SerializedName("order_tip")
    val orderTip: Double? = null,

    @field:SerializedName("order_type_id")
    val orderTypeId: Int? = null,

    @field:SerializedName("order_cart_group_id")
    val orderCartGroupId: Int? = null,

    @field:SerializedName("last_name")
    val lastName: String? = null,

    @field:SerializedName("order_instructions")
    val orderInstructions: Any? = null,

    @field:SerializedName("order_promised_time")
    val orderPromisedTime: String? = null,

    @field:SerializedName("coupon_code_id")
    val couponCodeId: Any? = null,

    @field:SerializedName("order_delivery_fee")
    val orderDeliveryFee: Int? = null,

    @field:SerializedName("order_tax")
    val orderTax: Double? = null,

    @field:SerializedName("order_location_id")
    val orderLocationId: Int? = null,

    @field:SerializedName("user_phone")
    val userPhone: String? = null,

    @field:SerializedName("order_creation_date")
    val orderCreationDate: String? = null,

    @field:SerializedName("order_type")
    val orderType: String? = null,

    @field:SerializedName("order_status")
    val orderStatus: String? = null,

    @field:SerializedName("order_total")
    val orderTotal: Double? = null,

    @field:SerializedName("order_adjustments")
    val orderAdjustmentAmount: Int? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("order_subtotal")
    val orderSubtotal: Double? = null,

    @field:SerializedName("gift_card_id")
    val giftCardId: Any? = null,

    @field:SerializedName("first_name")
    val firstName: String? = null,

    @field:SerializedName("guest_name")
    val guestName: String? = null,

    @field:SerializedName("guest_phone")
    val guestPhone: String? = null,

    @Expose(serialize = false, deserialize = false)
    var isSelected: Boolean = false

) {

    fun fullName(): String {
        val customerFullNameStringBuilder = StringBuilder().apply {
            if (firstName != null) {
                append("$firstName")
            }
            if (lastName != null) {
                append(" $lastName")
            }
        }
        return customerFullNameStringBuilder.toString()
    }
}

data class SectionInfo(
    val orderId: String,
    val guest: String,
    val total: String,
    val orderType: String,
    val status: String,
    val promiseTime: String,
    val orderPlace: String
)

data class OrderDetailsInfo(
    val productName: String,
    val productDetails: String,
    val productPrize: String,
    val productQuantity: String,
    val cardBow: String,
    val specialInstructions: String
)

@Keep
data class OrderDetailItem(

    @field:SerializedName("product_image")
    val productImage: String? = null,

    @field:SerializedName("cart_group_id")
    val cartGroupId: Int? = null,

    @field:SerializedName("menu_item_quantity")
    val menuItemQuantity: Int? = null,

    @field:SerializedName("menu_item_price")
    val menuItemPrice: Double? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("menu_item_modifiers")
    val menuItemModifiers: List<MenuItemModifiersItem>? = null,

    @field:SerializedName("menu_item_instructions")
    val menuItemInstructions: String? = null,

    @field:SerializedName("product_description")
    val productDescription: String? = null,

    @field:SerializedName("product_name")
    val productName: String? = null,

    @field:SerializedName("menu_id")
    val menuId: Int? = null
)

@Keep
data class OrderDetail(
    @field:SerializedName("order_tip")
    val orderTip: Double? = null,

    @field:SerializedName("order_type_id")
    val orderTypeId: Int? = null,

    @field:SerializedName("is_open")
    val isOpen: Boolean? = null,

    @field:SerializedName("order_cart_group_id")
    val orderCartGroupId: Int? = null,

    @field:SerializedName("discount")
    val discount: Int? = null,

    @field:SerializedName("location_state")
    val locationState: String? = null,


    @field:SerializedName("code_name")
    val codeName: Any? = null,

    @field:SerializedName("order_instructions")
    val orderInstructions: String? = null,

    @field:SerializedName("order_promised_time")
    val orderPromisedTime: String? = null,

    @field:SerializedName("order_status")
    val orderStatus: String? = null,

    @field:SerializedName("coupon_code_id")
    val couponCodeId: Any? = null,

    @field:SerializedName("location_name")
    val locationName: String? = null,

    @field:SerializedName("order_delivery_fee")
    val orderDeliveryFee: Double? = null,

    @field:SerializedName("order_tax")
    val orderTax: Double? = null,

    @field:SerializedName("location_address_1")
    val locationAddress1: String? = null,

    @field:SerializedName("location_address_2")
    val locationAddress2: String? = null,

    @field:SerializedName("location_city")
    val locationCity: String? = null,

    @field:SerializedName("order_creation_date")
    val orderCreationDate: String? = null,

    @field:SerializedName("order_total")
    var orderTotal: Double? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("order_subtotal")
    val orderSubtotal: Double? = null,

    @field:SerializedName("location_zip")
    val locationZip: String? = null,

    @field:SerializedName("items")
    val items: List<OrderDetailItem>? = null,

    @field:SerializedName("order_type")
    val orderType: String? = null,

    @field:SerializedName("order_delivery_address")
    val orderDeliveryAddress: String? = null,

    @field:SerializedName("order_gift_card_amount")
    val orderGiftCardAmount: Double? = null,

    @field:SerializedName("credit_amount")
    val creditAmount: Double? = null,

    @field:SerializedName("order_adjustments")
    val orderAdjustmentAmount: Double? = 0.0,

    @field:SerializedName("order_coupon_code_discount")
    val orderCouponCodeDiscount: Double? = null,

    @field:SerializedName("order_status_history")
    val orderStatusHistory: List<StatusItem>? = null,

    @field:SerializedName("guest_name")
    val guestName: String? = null,

    @field:SerializedName("customer_first_name")
    val customerFirstName: String? = null,

    @field:SerializedName("customer_last_name")
    val customerLastName: String? = null,

    @field:SerializedName("customer_email")
    val customerEmail: String? = null,

    @field:SerializedName("customer_phone")
    val customerPhone: String? = null,

    @field:SerializedName("guest_phone")
    val guestPhone: String? = null,

    ) {
    fun getSafeOrderId(): String {
        val orderIdStringBuilder = StringBuilder().apply {
            if (locationAddress1 != null) {
                append("ORDER #$id")
            }
        }
        return orderIdStringBuilder.toString()
    }

    fun fullName(): String {
        val customerFullNameStringBuilder = StringBuilder().apply {
            if (customerFirstName != null) {
                append("$customerFirstName")
            }
            if (customerLastName != null) {
                append(" $customerLastName")
            }
        }
        return customerFullNameStringBuilder.toString()
    }
}

data class MenuItemModifiersItem(

    @field:SerializedName("select_max")
    val selectMax: Int? = null,

    @field:SerializedName("is_required")
    val isRequired: Int? = null,

    @field:SerializedName("pmg_active")
    val pmgActive: Int? = null,

    @field:SerializedName("product_id")
    val productId: Int? = null,

    @field:SerializedName("options")
    val options: List<OptionsItem>? = null,

    @field:SerializedName("mod_group_id")
    val modGroupId: Int? = null,

    @field:SerializedName("active")
    val active: Int? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("modification_text")
    val modificationText: String? = null,

    @field:SerializedName("select_min")
    val selectMin: Int? = null,

    @field:SerializedName("product_category_id")
    val productCategoryId: Any? = null
) {

    fun getSafeSelectedItemName(): String {
        val selectedItemStringBuilder = StringBuilder().apply {
            if (options != null) {
                for (i in options.indices) {
                    if (options[i].modifierQyt != null) {
                        if (i == 0) {
                            append("${options[i].optionName} (${options[i].modifierQyt})")
                        } else {
                            append(", ${options[i].optionName} (${options[i].modifierQyt})")
                        }
                    } else {
                        if (i == 0) {
                            append("${options[i].optionName}")
                        } else {
                            append(", ${options[i].optionName}")
                        }
                    }

                }
            }
        }
        return selectedItemStringBuilder.toString()
    }

    fun getSafeSelectedItemPrice(): String {
        val selectedItemStringBuilder = StringBuilder().apply {
            if (options != null) {
                for (i in options.indices) {
                    if (options[i].optionPrice?.equals(0.0) == false) {
                        append("(${options[i].optionPrice?.div(100).toDollar()})")
                    } else {
                        append("")
                    }
                }
            }
        }
        return selectedItemStringBuilder.toString()
    }

    fun getSafeModifierQuantity(): String {
        val selectedItemStringBuilder = StringBuilder().apply {
            if (options != null) {
                for (i in options.indices) {
                    if (options[i].modifierQyt != null) {
                        append("(${options[i].modifierQyt})")
                    } else {
                        append("")
                    }
                }
            }
        }
        return selectedItemStringBuilder.toString()
    }
}

data class OptionsItem(

    @field:SerializedName("option_price")
    val optionPrice: Double? = null,

    @field:SerializedName("mod_group_id")
    val modGroupId: Int? = null,

    @field:SerializedName("active")
    val active: Int? = null,

    @field:SerializedName("option_name")
    val optionName: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("option_image")
    val optionImage: String? = null,

    @field:SerializedName("modifier_qyt")
    var modifierQyt: Int? = null,

    var isCheck: Boolean = false,
    var optionQuantity: Int? = 0,
    var productQuantity: Int? = 0,
    var maximumSelectOption: Int? = 0
)

data class StatusLogInfo(

    @field:SerializedName("status")
    val status: List<StatusItem>? = null
)

data class StatusItem(

    @field:SerializedName("order_status")
    val orderStatus: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("order_id")
    val orderId: Int? = null,

    @field:SerializedName("timestamp")
    val timestamp: String? = null,

    @field:SerializedName("first_name")
    var firstName: String? = null,

    @field:SerializedName("last_name")
    var lastName: String? = null,
    @field:SerializedName("role_name")
    var roleName: String? = null,
    @field:SerializedName("role_id")
    var roleId: String? = null
) {
    fun fullName(): String {
        val customerFullNameStringBuilder = StringBuilder().apply {
            if (firstName != null) {
                append("$firstName")
            }
            if (lastName != null) {
                append(" $lastName")
            }
        }
        return customerFullNameStringBuilder.toString()
    }
}

data class UpdatedOrderStatusResponse(

    @field:SerializedName("order_status")
    val orderStatus: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("order_id")
    val orderId: Int? = null,

    @field:SerializedName("timestamp")
    val timestamp: String? = null
)

data class OrderStatusRequest(

    @field:SerializedName("order_status")
    val orderStatus: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("order_id")
    val orderId: Int? = null
)

data class OrderRefundRequest(

    @field:SerializedName("id")
    val id: Int? = null
)

data class TransactionResponse(

    @field:SerializedName("transaction_amount")
    val transactionAmount: Int? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("transaction_id_of_processor")
    val transactionIdOfProcessor: String? = null
)


sealed class SendReceiptStates {
    data class SendReceiptOnEmail(val email: String) : SendReceiptStates()
    data class SendReceiptOnPhone(val phone: String) : SendReceiptStates()
    data class SendReceiptOnPhoneAndEmail(val email: String, val phone: String) : SendReceiptStates()
}

sealed class RefundDialogStates {
    data class DismissedRefundDialog(val data: OrderDetail) : RefundDialogStates()
    data class GetRefund(val data: OrderDetail) : RefundDialogStates()
}