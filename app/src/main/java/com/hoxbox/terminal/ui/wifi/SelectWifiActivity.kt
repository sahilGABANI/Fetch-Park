package com.hoxbox.terminal.ui.wifi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.view.isVisible
import com.hoxbox.terminal.R
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseActivity
import com.hoxbox.terminal.base.extension.isFieldBlank
import com.hoxbox.terminal.base.extension.onClick
import com.hoxbox.terminal.base.extension.showToast
import com.hoxbox.terminal.base.extension.startNewActivityWithDefaultAnimation
import com.hoxbox.terminal.databinding.ActivitySelectWifiBinding
import com.hoxbox.terminal.ui.splash.SplashActivity
import com.thanosfisherman.wifiutils.WifiUtils
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionErrorCode
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener

class SelectWifiActivity : BaseActivity() {

    private lateinit var binding: ActivitySelectWifiBinding

    private var wifiName: String? = null

    companion object {
        var WIFI_NAME = "WIFI_NAME"
        fun getIntent(context: Context, wifiName: String): Intent {
            val intent = Intent(context, SelectWifiActivity::class.java)
            intent.putExtra(WIFI_NAME, wifiName)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        binding = ActivitySelectWifiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        initUI()
    }

    private fun initUI() {

        intent?.let {
            wifiName = it.getStringExtra(WIFI_NAME)

            binding.wifiNameAppCompatTextView.text = wifiName
        }


        binding.backLinearLayout.onClick {
            onBackPressed()
        }

        binding.connectMaterialButton.onClick {
            if (isValidate()) {
                WifiUtils.withContext(applicationContext)
                    .connectWith(wifiName ?: "", binding.passwordEditText.text.toString())
                    .setTimeout(60000)
                    .onConnectionResult(object : ConnectionSuccessListener {
                        override fun success() {
                            Toast.makeText(applicationContext, "SUCCESS!", Toast.LENGTH_SHORT).show()
                            startNewActivityWithDefaultAnimation(SplashActivity.getIntent(this@SelectWifiActivity))
                        }

                        override fun failed(@NonNull errorCode: ConnectionErrorCode) {
                            runOnUiThread {
                                binding.errorTextView.isVisible = true
                                binding.connectMaterialButton.text = resources.getString(R.string.try_again)
                            }
                        }
                    }).start()

            } else {
                binding.errorTextView.isVisible = true
            }
        }
    }

    private fun isValidate(): Boolean {
        return when {
            binding.passwordEditText.isFieldBlank() -> {
                showToast(getString(R.string.blank_password))
                false
            }
            else -> true
        }
    }
}