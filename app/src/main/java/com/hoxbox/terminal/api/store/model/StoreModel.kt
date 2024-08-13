package com.hoxbox.terminal.api.store.model

import com.google.gson.annotations.SerializedName
import com.hoxbox.terminal.helper.formatTo
import com.hoxbox.terminal.helper.toDate
import timber.log.Timber

data class StoreResponse(

    @field:SerializedName("employees")
    val assignedEmployee: List<AssignedEmployeeInfo>? = null,

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

    @field:SerializedName("monday_close_time")
    val mondayCloseTime: String? = null,

    @field:SerializedName("location_city")
    val locationCity: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("location_zip")
    val locationZip: String? = null,

    @field:SerializedName("thursday_open_time")
    val thursdayOpenTime: String? = null,

    @field:SerializedName("saturday_close_time")
    val saturdayCloseTime: String? = null,

    @field:SerializedName("thursday_close_time")
    val thursdayCloseTime: String? = null,

    @field:SerializedName("location_timezone")
    val locationTimezone: Int? = null,

    @field:SerializedName("location_tax_rate")
    val locationTaxRate: Double? = null,

    @field:SerializedName("tuesday_open_time")
    val tuesdayOpenTime: String? = null,

    @field:SerializedName("location_description")
    val locationDescription: String? = null,

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

    @field:SerializedName("wednesday_close_time")
    val wednesdayCloseTime: String? = null,

    @field:SerializedName("monday_open_time")
    val mondayOpenTime: String? = null,

    @field:SerializedName("sunday_close_time")
    val sundayCloseTime: String? = null,

    @field:SerializedName("location_phone")
    val locationPhone: String? = null
) {
    fun getStoreShiftTime(): List<StoreShiftTime> {
        val listOfShift = mutableListOf<StoreShiftTime>()
        listOfShift.add(StoreShiftTime("Sunday", getShiftTime(sundayOpenTime, sundayCloseTime)))
        listOfShift.add(StoreShiftTime("Monday", getShiftTime(mondayOpenTime, mondayCloseTime)))
        listOfShift.add(StoreShiftTime("Tuesday", getShiftTime(tuesdayOpenTime, tuesdayCloseTime)))
        listOfShift.add(StoreShiftTime("Wednesday", getShiftTime(wednesdayOpenTime, wednesdayCloseTime)))
        listOfShift.add(StoreShiftTime("Thursday", getShiftTime(thursdayOpenTime, thursdayCloseTime)))
        listOfShift.add(StoreShiftTime("Friday", getShiftTime(fridayOpenTime, fridayCloseTime)))
        listOfShift.add(StoreShiftTime("Saturday", getShiftTime(saturdayOpenTime, saturdayCloseTime)))
        return listOfShift
    }

    private fun getShiftTime(daysOpen: String?, daysClose: String?): String {
        return try {
            daysOpen?.toDate("hh:mm:ss")?.formatTo("hh:mm a") + " - " + daysClose?.toDate("hh:mm:ss")?.formatTo("hh a")
        } catch (e: Exception) {
            Timber.e(e)
            ""
        }
    }

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
            if (locationCountry != null) {
                append(", $locationCountry")
            }
        }
        return addressStringBuilder.toString()
    }

    fun getSafePhoneNumber(): String {
        return locationPhone ?: "-"
    }
}

data class BufferResponse(

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("location_takeout_buffer")
    val locationTakeoutBuffer: Int? = null,

    @field:SerializedName("location_delivery_buffer")
    val locationDeliveryBuffer: Int? = null
) {
    fun getSafeTakeOutBufferTime(): Int {
        return locationTakeoutBuffer ?: 0
    }

    fun getSafeDeliveryBufferTime(): Int {
        return locationDeliveryBuffer ?: 0
    }
}

data class BufferTimeRequest(
    @field:SerializedName("location_id")
    val locationId: Int,

    @field:SerializedName("type")
    val type: String,

    @field:SerializedName("operator")
    val operator: String
)

data class AssignedEmployeeInfo(
    @field:SerializedName("is_ambassador")
    val isAmbassador: Int? = null,

    @field:SerializedName("location_id")
    val locationId: Int? = null,

    @field:SerializedName("user_active")
    val userActive: Int? = null,

    @field:SerializedName("role_id")
    val roleId: Int? = null,

    @field:SerializedName("user_qr_code")
    val userQrCode: String? = null,

    @field:SerializedName("user_phone")
    val userPhone: String? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("vip")
    val vip: Int? = null,

    @field:SerializedName("first_name")
    val firstName: String? = null,

    @field:SerializedName("user_company")
    val userCompany: String? = null,

    @field:SerializedName("user_email")
    val userEmail: String? = null,

    @field:SerializedName("tax_exempt")
    val taxExempt: Int? = null,

    @field:SerializedName("default_perspective")
    val defaultPerspective: String? = null,

    @field:SerializedName("active")
    val active: Int? = null,

    @field:SerializedName("last_name")
    val lastName: String? = null,

    @field:SerializedName("user_creation_date")
    val userCreationDate: String? = null,

    @field:SerializedName("user_last_ordered")
    val userLastOrdered: Any? = null,

    @field:SerializedName("tax_id")
    val taxId: Any? = null,

    @field:SerializedName("user_last_active")
    val userLastActive: String? = null,

    @field:SerializedName("role_name")
    val roleName: String? = null,

    @field:SerializedName("is_employee")
    val isEmployee: Int? = null,

    @field:SerializedName("user_mobile_token")
    val userMobileToken: String? = null,

    @field:SerializedName("user_birthday")
    val userBirthday: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("assigned")
    val assigned: String? = null
) {
    fun getSafeFullNameWithRoleName(): String {
        val employeeFullNameWithRoleNameStringBuilder = StringBuilder().apply {
            if (roleName != null) {
                append(roleName)
            }
            if (firstName != null) {
                append(" - $firstName")
            }
            if (lastName != null) {
                append(" $lastName")
            }
        }
        return employeeFullNameWithRoleNameStringBuilder.toString()
    }

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

data class StoreShiftTime(
    val dayOfWeek: String,
    val time: String
)

const val PICKUP_BUFFER_TYPE = "takeout"
const val DELIVERY_BUFFER_TYPE = "delivery"
const val BUFFER_TIME_PLUSH = "+"
const val BUFFER_TIME_MINUS = "-"