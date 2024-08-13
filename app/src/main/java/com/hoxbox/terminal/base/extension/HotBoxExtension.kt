package com.hoxbox.terminal.base.extension

import android.os.Bundle
import com.hoxbox.terminal.BuildConfig

fun getAPIBaseUrl(): String {
    return if (BuildConfig.DEBUG) {
//        "https://hot-box-prod-api.azurewebsites.net/" // Production
        "https://hot-box-dev-api.azurewebsites.net/"    // Development
    } else {
        "https://hot-box-prod-api.azurewebsites.net/" // Production
//       "https://hot-box-dev-api.azurewebsites.net/"    // Development
    }
}

fun Bundle.putEnum(key:String, enum: Enum<*>){
    putString(key, enum.name)
}

inline fun <reified T: Enum<T>> Bundle.getEnum(key: String, default: T): T {
    val found = getString(key)
    return if (found == null) { default } else enumValueOf(found)
}