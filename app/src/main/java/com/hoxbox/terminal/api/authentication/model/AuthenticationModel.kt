package com.hoxbox.terminal.api.authentication.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class LoginCrewRequest(
    @field:SerializedName("user_email")
    val userEmail: String,

    @field:SerializedName("user_password")
    val userPassword: String,

    @field:SerializedName("location_id")
    val locationId: Int
)

@Keep
data class LoginCrewResponse(
    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("role_id")
    val roleId: Int,

    @field:SerializedName("token")
    val token: String,

    @field:SerializedName("refreshToken")
    val refreshToken: String? = null,

    @field:SerializedName("role_name")
    val roleName: String? = null
)

@Keep
data class LocationResponse(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("location_name")
    val locationName: String,

    @field:SerializedName("reader_id")
    val readerId: String,

    @field:SerializedName("poskey")
    val poskey: String? = null,

    @field:SerializedName("terminalkey")
    val terminalkey: String? = null,

    @field:SerializedName("gcposkey")
    val gcPosKey: String? = null,

    @field:SerializedName("gcterminalkey")
    val gcTerminalKey: String? = null,

    @field:SerializedName("print_address")
    val printAddress: String? = null,

    @field:SerializedName("boh_print_address")
    val bohPrintAddress: String? = null,


) {
    fun bothPrinterNotSame() :Boolean {
        return  bohPrintAddress != printAddress
    }
}

@Keep
data class HotBoxUser(
    @field:SerializedName("user_email")
    val userEmail: String? = null,

    @field:SerializedName("default_perspective")
    val defaultPerspective: String? = null,

    @field:SerializedName("last_name")
    val lastName: String? = null,

    @field:SerializedName("user_creation_date")
    val userCreationDate: String? = null,

    @field:SerializedName("user_last_ordered")
    val userLastOrdered: String? = null,

    @field:SerializedName("user_last_active")
    val userLastActive: String? = null,

    @field:SerializedName("user_mobile_token")
    val userMobileToken: String? = null,

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
    val id: Int,

    @field:SerializedName("first_name")
    val firstName: String? = null,

    @field:SerializedName("user_company")
    val userCompany: String? = null
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

@Keep
data class LoggedInUser(
    val crewResponse: LoginCrewResponse,
    val hotBoxUser: HotBoxUser,
)