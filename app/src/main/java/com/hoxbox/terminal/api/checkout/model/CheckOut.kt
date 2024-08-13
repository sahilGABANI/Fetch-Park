package com.hoxbox.terminal.api.checkout.model

import com.google.gson.annotations.SerializedName

data class QRScanResponse(
    @field:SerializedName("phone")
    val phone: String? = null,

    @field:SerializedName("fullName")
    val fullName: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("points")
    val points: Int? = null
)

data class HistoryItem(

    @field:SerializedName("applied_points")
    val appliedPoints: Int? = null,

    @field:SerializedName("date_created")
    val dateCreated: String? = null,

    @field:SerializedName("order_id")
    val orderId: Int? = null,

    @field:SerializedName("order_total")
    val orderTotal: Int? = null
)

data class UserLoyaltyPointResponse(

    @field:SerializedName("history")
    val history: List<HistoryItem>? = null,

    @field:SerializedName("points")
    val points: Int? = null
)

data class GiftCardResponse(

    @field:SerializedName("gift_card_amout")
    val giftCardAmout: Int? = null,

    @field:SerializedName("gift_card_redemption")
    val giftCardRedemption: Int? = null,

    @field:SerializedName("id")
    val id: Int? = null
)
data class PromoCodeRequest(

    @field:SerializedName("delivery_fee")
    val deliveryFee: Int? = null,

    @field:SerializedName("coupon")
    val coupon: String? = null,

    @field:SerializedName("subtotal")
    val subtotal: Double? = null,

    @field:SerializedName("cart_group_id")
    val cartGroupId: Int? = null
)
data class PromoCodeResponse(

    @field:SerializedName("valid")
    val valid: Boolean? = null,

    @field:SerializedName("coupon_code_id")
    val couponCodeId: Int? = null,

    @field:SerializedName("deduction_mode")
    val deductionMode: String? = null,

    @field:SerializedName("discount")
    val discount: Int? = null,

    @field:SerializedName("deduction_type")
    val deductionType: String? = null
)

data class CreateUserRequest(
    @field:SerializedName("user_mobile_token")
    val userMobileToken: String? = null,

    @field:SerializedName("subscribed")
    val subscribed: Boolean? = null,

    @field:SerializedName("user_birthday")
    val userBirthday: String? = null,

    @field:SerializedName("user_email")
    val userEmail: String? = null,

    @field:SerializedName("user_password")
    val userPassword: String? = null,

    @field:SerializedName("email_receipt")
    val emailReceipt: Boolean? = null,

    @field:SerializedName("last_name")
    val lastName: String? = null,

    @field:SerializedName("user_phone")
    val userPhone: String? = null,

    @field:SerializedName("text_receipt")
    val textReceipt: Boolean? = null,

    @field:SerializedName("first_name")
    val firstName: String? = null,

    @field:SerializedName("user_company")
    val userCompany: String? = null
)


data class CreateUserResponse(

    @field:SerializedName("user_email")
    val userEmail: String? = null,

    @field:SerializedName("user_password")
    val userPassword: String? = null,

    @field:SerializedName("notes")
    val notes: Any? = null,

    @field:SerializedName("default_perspective")
    val defaultPerspective: String? = null,

    @field:SerializedName("last_name")
    val lastName: String? = null,

    @field:SerializedName("is_ambassador")
    val isAmbassador: Int? = null,

    @field:SerializedName("user_creation_date")
    val userCreationDate: String? = null,

    @field:SerializedName("user_last_ordered")
    val userLastOrdered: Any? = null,

    @field:SerializedName("user_last_active")
    val userLastActive: String? = null,

    @field:SerializedName("token")
    val token: String? = null,

    @field:SerializedName("user_mobile_token")
    val userMobileToken: String? = null,

    @field:SerializedName("is_employee")
    val isEmployee: Int? = null,

    @field:SerializedName("user_birthday")
    val userBirthday: String? = null,

    @field:SerializedName("user_active")
    val userActive: Int? = null,

    @field:SerializedName("corporate")
    val corporate: Any? = null,

    @field:SerializedName("user_qr_code")
    val userQrCode: String? = null,

    @field:SerializedName("user_phone")
    val userPhone: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("vip")
    val vip: Int? = null,

    @field:SerializedName("first_name")
    val firstName: String? = null,

    @field:SerializedName("user_company")
    val userCompany: String? = null,

    @field:SerializedName("refreshToken")
    val refreshToken: String? = null
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

data class UserCreditResponse(

    @field:SerializedName("credits")
    val credits: Int? = null,

    @field:SerializedName("history")
    val history: List<CreditHistoryItem>? = null
)

data class CreditHistoryItem(

    @field:SerializedName("date_created")
    val dateCreated: String? = null,

    @field:SerializedName("applied_credits")
    val appliedCredits: Int? = null,

    @field:SerializedName("order_id")
    val orderId: Any? = null
)