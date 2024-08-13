package com.hoxbox.terminal.base.extension

import java.text.SimpleDateFormat
import java.util.*

fun String.toDate(dateFormat: String = "yyy-MM-dd'T'HH:mm:ss.SSS'Z'", timeZone: TimeZone = TimeZone.getTimeZone("UTC")): Date {
    val parser = SimpleDateFormat(dateFormat, Locale.getDefault())
    parser.timeZone = timeZone
    return parser.parse(this)
}

fun Date.formatTo(dateFormat: String, timeZone: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")): String {
    val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
    formatter.timeZone = timeZone
    return formatter.format(this)
}

fun getISTTimeZoneDate(dateString: String?): String {
    var convertTime = ""
    try {
        if (!dateString.isNullOrEmpty()) {
            val inputDateFormat = SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
            inputDateFormat.timeZone = TimeZone.getTimeZone("UTC")

            val outputDateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.ENGLISH)
            outputDateFormat.timeZone = TimeZone.getDefault()

            val parsedDate = inputDateFormat.parse(dateString)
            if (parsedDate != null) {
                val formattedDate = outputDateFormat.format(parsedDate)
                if (!formattedDate.isNullOrEmpty()) {
                    convertTime = formattedDate
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return convertTime
}