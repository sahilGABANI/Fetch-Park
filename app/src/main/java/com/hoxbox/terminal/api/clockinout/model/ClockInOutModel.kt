package com.hoxbox.terminal.api.clockinout.model

import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import timber.log.Timber
import java.util.*

@Keep
data class SubmitTimeRequest(
    @field:SerializedName("user_id")
    var user_id: Int,

    @field:SerializedName("action")
    var action: String,

    @field:SerializedName("location_id")
    val locationId: Int,
)

@Keep
data class TimeResponse(
    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("user_id")
    var userId: Int?,

    @field:SerializedName("auto_clock_out")
    var action: String?,

    @field:SerializedName("clock_in_time")
    var actionTime: String?,

    @field:SerializedName("clock_out_time")
    var clockOutTime: String? = null,
) {
    fun getClockType(): ClockType {
        return if (clockOutTime == null) ClockType.ClockIn else ClockType.ClockOut
    }

    fun getActionFormattedTime(dateFormat: String = "MM/dd/yy hh:mm a"): String {
        return try {
            convertUtcToFormattedDate(actionTime,dateFormat)
        } catch (e: Exception) {
            Timber.e(e)
            ""
        }
    }
    fun getLastActionFormattedTime(dateFormat: String = "MM/dd/yy hh:mm a"): String {
        return if (clockOutTime ==  null) {
            try {
                convertUtcToFormattedDate(actionTime,dateFormat)
            } catch (e: Exception) {
                Timber.e(e)
                ""
            }
        } else {
            try {
                convertUtcToFormattedDate(clockOutTime,dateFormat)
            } catch (e: Exception) {
                Timber.e(e)
                ""
            }
        }

    }

    fun getClockOutFormattedTime(dateFormat: String = "MM/dd/yy hh:mm a"): String {
        return try {
            convertUtcToFormattedDate(clockOutTime,dateFormat)
        } catch (e: Exception) {
            Timber.e(e)
            "-"
        }
    }



    fun isInitClockTime(): Boolean {
        return id == 0
    }
}

fun String.toDate(
    dateFormat: String = "yyyy-MM-dd'T'HH:mm:ss.000Z",
    timeZone: TimeZone = TimeZone.getTimeZone("UTC")
): Date? {
    try {
        val parser = SimpleDateFormat(dateFormat, Locale.getDefault())
//        parser.timeZone = timeZone
        return parser.parse(this)
    } catch(e: Exception) {
        Timber.e(e, "Invalid Format Time :'$this' ")
    }
    return null

}

fun convertUtcToFormattedDate(utcTime: String?,format :String): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
//    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val date = dateFormat.parse(utcTime)
    val formattedDate = SimpleDateFormat(format, Locale.US)
    return formattedDate.format(date)
}


fun Date.formatTo(dateFormat: String, timeZone: TimeZone = TimeZone.getDefault()): String {
    val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
//    formatter.timeZone = timeZone
    return formatter.format(this)
}


@Keep
data class ClockInOutHistoryResponse(
    @field:SerializedName("times")
    val listOfTimeResponse: List<TimeResponse>? = null
)

@Keep
enum class ClockType(val type: String, val displayType: String) {
    ClockIn("clock-in", "Clock in"),
    ClockOut("clock-out", "Clock out")
}