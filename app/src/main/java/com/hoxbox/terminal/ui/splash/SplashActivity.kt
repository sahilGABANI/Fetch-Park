package com.hoxbox.terminal.ui.splash

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.Settings.Secure
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.hoxbox.terminal.BuildConfig
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.authentication.model.LocationResponse
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseActivity
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.base.extension.*
import com.hoxbox.terminal.databinding.ActivitySplashBinding
import com.hoxbox.terminal.ui.login.LoginActivity
import com.hoxbox.terminal.ui.main.MainActivity
import com.hoxbox.terminal.ui.splash.viewmodel.LocationViewModel
import com.hoxbox.terminal.ui.splash.viewmodel.LocationViewState
import com.hoxbox.terminal.ui.wifi.NoWifiActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LocationViewModel>
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var androidId: String
    private val listOfOrderID = arrayListOf<Int>()

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, SplashActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        locationViewModel = getViewModelFromFactory(viewModelFactory)
        setContentView(binding.root)
        window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        listenToViewModel()
        initUI()
    }

    @SuppressLint("HardwareIds")
    private fun initUI() {
//        val animSlide = AnimationUtils.loadAnimation(applicationContext, R.anim.image)
//        binding.ivLogo.startAnimation(animSlide);
        if (!loggedInUserCache.isUserLoggedIn()) {
            androidId = Secure.getString(contentResolver, Secure.ANDROID_ID).toString()
            binding.androidIdTextView.text = androidId
            binding.startButton.throttleClicks().subscribeAndObserveOnMainThread {
                locationViewModel.clickOnStartButton()
            }.autoDispose()
            ReactiveNetwork.observeInternetConnectivity().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe { isConnectedToInternet: Boolean ->
                    if (!isConnectedToInternet) {
                        startActivityWithDefaultAnimation(NoWifiActivity.getIntent(this))
                    }
                }.autoDispose()
            binding.androidIdTextView.throttleClicks().subscribeAndObserveOnMainThread {
                val clip = ClipData.newPlainText("Copied Text", androidId)
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                showToast("Android id is copied")
            }
        } else {
            finish()
            startActivity(MainActivity.getIntent(this@SplashActivity))
        }

    }

    override fun onResume() {
        if (BuildConfig.DEBUG) {
            androidId = "14b0fbb66d3cac64"
        }
        locationViewModel.loadLocation(androidId)
        locationViewModel.loadOrderData(30)
        super.onResume()
    }

    override fun onPause() {
        locationViewModel.clear()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationViewModel.clear()
    }

    private fun listenToViewModel() {
        locationViewModel.locationState.subscribeAndObserveOnMainThread {
            when (it) {
                is LocationViewState.ErrorMessage -> {
                    binding.locationTextView.text = getString(R.string.no_location_set)
                }
                is LocationViewState.LoadingState -> {
                    progressVisibility(it.isLoading)
                }
                is LocationViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is LocationViewState.LocationData -> {
                    setLocationInformation(it.locationResponse)
                }
                is LocationViewState.OpenLoginScreen -> {
                    startActivityWithDefaultAnimation(
                        LoginActivity.getIntent(
                            this, it.locationResponse.id
                        )
                    )
                    finish()
                }
                is LocationViewState.OrderInfoSate -> {
                    println("OkHttpClient: CheckForNewOrder")
                    locationViewModel.playMusic()

                }
                is LocationViewState.StartButtonState -> {
                    startButtonVisibility(it.isVisible)
                }
                is LocationViewState.PlayMusic -> {
                    val mediaPlayer = MediaPlayer.create(applicationContext,R.raw.notification)
                    mediaPlayer.start()
                    locationViewModel.stopMusic()
                }
                else ->  {

                }
            }
        }.autoDispose()
    }

    private fun setLocationInformation(locationResponse: LocationResponse) {
        binding.locationTextView.text = locationResponse.locationName
    }

    private fun progressVisibility(isVisible: Boolean) {
        binding.progressBar.isVisible = isVisible
    }

    private fun startButtonVisibility(isVisible: Boolean) {
        binding.startButton.isVisible = isVisible
    }
}