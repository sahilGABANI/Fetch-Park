package com.hoxbox.terminal.ui.userstore.welcome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseActivity
import com.hoxbox.terminal.base.extension.startActivityWithDefaultAnimation
import com.hoxbox.terminal.base.extension.startNewActivityWithDefaultAnimation
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.throttleClicks
import com.hoxbox.terminal.databinding.ActivityUserStoreWelcomeBinding
import com.hoxbox.terminal.ui.splash.SplashActivity
import com.hoxbox.terminal.ui.userstore.deliveryaddress.DeliveryAddressActivity
import com.hoxbox.terminal.ui.userstore.UserStoreActivity
import com.hoxbox.terminal.utils.Constants.DELIVERY_ORDER_TYPE_ID
import com.hoxbox.terminal.utils.Constants.ORDER_TYPE_ID
import timber.log.Timber
import javax.inject.Inject

class UserStoreWelcomeActivity : BaseActivity() {

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, UserStoreWelcomeActivity::class.java)
        }
    }

    private lateinit var binding: ActivityUserStoreWelcomeBinding

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private val DISCONNECT_TIMEOUT: Long = 3600000
//    private val DISCONNECT_TIMEOUT: Long = 300000



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserStoreWelcomeBinding.inflate(layoutInflater)
        HotBoxApplication.component.inject(this)
        setContentView(binding.root)
        initUI()
        window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    private fun initUI() {
        binding.clDineIn.throttleClicks().subscribeAndObserveOnMainThread {
            manageSelectionVisibility("DINE IN")
            loggedInUserCache.setorderTypeId(ORDER_TYPE_ID)
            loggedInUserCache.setLoggedInUserCartGroupId(0)
            startActivityWithDefaultAnimation(UserStoreActivity.getIntent(this))
            finish()
        }.autoDispose()
        binding.clDelivery.throttleClicks().subscribeAndObserveOnMainThread {
            manageSelectionVisibility("DELIVERY")
            loggedInUserCache.setorderTypeId(DELIVERY_ORDER_TYPE_ID)
            loggedInUserCache.setLoggedInUserCartGroupId(0)
            startActivityWithDefaultAnimation(DeliveryAddressActivity.getIntent(this))
            finish()
        }.autoDispose()

        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()
    }


    private fun manageSelectionVisibility(isSelected: String? = null) {
        when (isSelected) {
            "DINE IN" -> {
                binding.clDineIn.isSelected = true
                binding.dineInCheckImageView.isVisible = true
                binding.clDelivery.isSelected = false
                binding.foodToGoCheckImageView.isVisible = false
            }
            "DELIVERY" -> {
                binding.clDineIn.isSelected = false
                binding.dineInCheckImageView.isVisible = false
                binding.clDelivery.isSelected = true
                binding.foodToGoCheckImageView.isVisible = true
            }
            else -> {
                binding.clDineIn.isSelected = false
                binding.dineInCheckImageView.isVisible = false
                binding.clDelivery.isSelected = false
                binding.foodToGoCheckImageView.isVisible = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        manageSelectionVisibility()
        resetDisconnectTimer()
    }

    override fun onUserInteraction() {
        Timber.tag("UserStoreWelcomeActivity").d("onUserInteraction")
        resetDisconnectTimer()
    }

    private val disconnectHandler = Handler {
        Timber.tag("UserStoreWelcomeActivity").d("disconnectHandler")
        false
    }

    private val disconnectCallback = Runnable { // Perform any required operation on disconnect
        Timber.tag("UserStoreWelcomeActivity").d("disconnectCallback")
        loggedInUserCache.clearLoggedInUserLocalPrefs()
        Toast.makeText(applicationContext, "Crew time out", Toast.LENGTH_LONG).show()
        startNewActivityWithDefaultAnimation(
            SplashActivity.getIntent(
                this@UserStoreWelcomeActivity
            )
        )
        finish()
    }

    private fun resetDisconnectTimer() {
        Timber.tag("UserStoreWelcomeActivity").d("resetDisconnectTimer")
        disconnectHandler.removeCallbacks(disconnectCallback)
        disconnectHandler.postDelayed(disconnectCallback, DISCONNECT_TIMEOUT)
    }

    private fun stopDisconnectTimer() {
        disconnectHandler.removeCallbacks(disconnectCallback)
    }

    override fun onPause() {
        super.onPause()
        Timber.tag("UserStoreWelcomeActivity").d("stopDisconnectTimer")
        stopDisconnectTimer()
    }
}