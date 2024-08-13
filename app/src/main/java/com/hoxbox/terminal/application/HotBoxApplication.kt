package com.hoxbox.terminal.application

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.hoxbox.terminal.BuildConfig
import com.hoxbox.terminal.base.ActivityManager
import com.hoxbox.terminal.di.BaseAppComponent
import com.hoxbox.terminal.di.BaseUiApp
import timber.log.Timber

open class HotBoxApplication :  BaseUiApp() {

    companion object {
        lateinit var component: BaseAppComponent

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        context = this
        ActivityManager.getInstance().init(this)
        setupLog()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }

    private fun setupLog() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

    }


    override fun getAppComponent(): BaseAppComponent {
        return component
    }
    override fun setAppComponent(baseAppComponent: BaseAppComponent) {
        component = baseAppComponent
    }
}