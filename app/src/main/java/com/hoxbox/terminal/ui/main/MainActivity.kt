package com.hoxbox.terminal.ui.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.hoxbox.terminal.BuildConfig
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.order.model.OrderDetail
import com.hoxbox.terminal.api.store.model.StoreResponse
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseActivity
import com.hoxbox.terminal.base.RxBus
import com.hoxbox.terminal.base.RxEvent
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.base.extension.*
import com.hoxbox.terminal.databinding.ActivityMainBinding
import com.hoxbox.terminal.helper.BohPrinterHelper
import com.hoxbox.terminal.helper.FohPrinterHelper
import com.hoxbox.terminal.helper.formatTo
import com.hoxbox.terminal.helper.toDate
import com.hoxbox.terminal.ui.main.store.viewmodel.StoreState
import com.hoxbox.terminal.ui.main.store.viewmodel.StoreViewModel
import com.hoxbox.terminal.ui.main.view.SideNavigationAdapter
import com.hoxbox.terminal.ui.splash.SplashActivity
import com.hoxbox.terminal.ui.userstore.welcome.UserStoreWelcomeActivity
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates
import kotlin.random.Random


class MainActivity : BaseActivity() {

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<StoreViewModel>
    private lateinit var storeViewModel: StoreViewModel
    private lateinit var bohPrinterHelper: BohPrinterHelper
    private lateinit var fohPrinterHelper: FohPrinterHelper
    var versionName: String = BuildConfig.VERSION_NAME
    private var lastSelectedMenuId by Delegates.notNull<Int>()
    private var bohPrintAddress: String? = null
    private var fohPrintAddress: String? = null
    private val DISCONNECT_TIMEOUT: Long = 3600000
//    private val DISCONNECT_TIMEOUT: Long = 300000
    private val listOfOrderID = arrayListOf<Int>()

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var sideNavigationAdapter: SideNavigationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        storeViewModel = getViewModelFromFactory(viewModelFactory)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
        window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        listenToViewModel()
//        bohPrinterInitialize()
//        fohPrinterInitialize()
//        storeViewModel.loadOrderData(generateRandomNumber())
    }

    private fun bohPrinterInitialize() {
        Timber.tag("Printer").i("Call Connect BOH Printer")
        bohPrinterHelper = BohPrinterHelper.getInstance(this@MainActivity)
        bohPrinterHelper.printerInitialize(this@MainActivity)
        bohPrintAddress = loggedInUserCache.getLocationInfo()?.bohPrintAddress
        if (bohPrintAddress != null) {
            val t: Thread = object : Thread() {
                override fun run() {
                    bohPrinterHelper.printerConnect(bohPrintAddress)
                }
            }
            t.start()
        }
    }

    private fun fohPrinterInitialize() {
        Timber.tag("Printer").i("Call Connect BOH Printer")
        fohPrinterHelper = FohPrinterHelper.getInstance(this@MainActivity)
        fohPrinterHelper.printerInitialize(this@MainActivity)
        fohPrintAddress = loggedInUserCache.getLocationInfo()?.printAddress
        if (bohPrintAddress != fohPrintAddress) {
            if (fohPrintAddress != null) {
                val t: Thread = object : Thread() {
                    override fun run() {
                        fohPrinterHelper.printerConnect(fohPrintAddress)
                    }
                }
                t.start()

            }
        }


    }


    @SuppressLint("HardwareIds")
    private fun initUI() {
        sideNavigationAdapter = SideNavigationAdapter(this)

        binding.viewpager.apply {
            offscreenPageLimit = 1
            adapter = sideNavigationAdapter
            isUserInputEnabled = false
        }
        setLastSelectedMenu(R.id.orderMenuView)
        binding.orderMenuView.isMenuSelected = true

        binding.orderMenuView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.viewpager.currentItem = 0
            manageSelection(R.id.orderMenuView)
        }.autoDispose()

        binding.deliveriesMenuView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.viewpager.currentItem = 1
            manageSelection(R.id.deliveriesMenuView)
        }.autoDispose()

        binding.orderMenuMenuView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.viewpager.currentItem = 1
            manageSelection(R.id.orderMenuMenuView)
        }.autoDispose()

        binding.timeManagementMenuView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.viewpager.currentItem = 3
            manageSelection(R.id.timeManagementMenuView)
        }.autoDispose()

        binding.storeMenuView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.viewpager.currentItem = 2
            manageSelection(R.id.storeMenuView)
        }.autoDispose()

        binding.giftCardMenuView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.viewpager.currentItem = 5
            manageSelection(R.id.giftCardMenuView)
        }.autoDispose()

        binding.settingMenuView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.viewpager.currentItem = 6
            manageSelection(R.id.settingMenuView)
        }.autoDispose()

        binding.logOutMenuView.throttleClicks().subscribeAndObserveOnMainThread {
            val alertDialog =
                AlertDialog.Builder(this).setTitle(resources.getText(R.string.logout)).setMessage(resources.getText(R.string.are_you_sure_log_out))
                    .setNegativeButton(resources.getText(R.string.label_cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }.setPositiveButton(resources.getText(R.string.label_ok)) { _, _ ->
                        loggedInUserCache.clearLoggedInUserLocalPrefs()
                        finish()
                    }.show()
            alertDialog.window?.setLayout(800, WindowManager.LayoutParams.WRAP_CONTENT)
        }.autoDispose()

        binding.locationAppCompatTextView.text = loggedInUserCache.getLocationInfo()?.locationName ?: ""
        binding.versionNameAppCompatTextView.text = resources.getString(R.string.title_version, versionName)
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        binding.androidIdAppCompatTextView.text = resources.getString(R.string.title_device_id, androidId)
        binding.liveTimeTextClock.format12Hour = "hh:mm a"
        binding.loginCrewNameTextView.text = loggedInUserCache.getLoggedInUserFullName()
        binding.loggedInUserJobPostTextView.text = loggedInUserCache.getLoggedInUserRole()
        binding.openStoreTimeLinearLayout.isSelected = true
        binding.tvOpenAndClose.isSelected = true
        RxBus.listen(RxEvent.EventOrderCountListen::class.java).subscribeAndObserveOnMainThread {
            binding.orderMenuView.notificationCount = it.count
        }.autoDispose()

        RxBus.listen(RxEvent.EventDeliveryCountListen::class.java).subscribeAndObserveOnMainThread {
            binding.deliveriesMenuView.notificationCount = it.count
        }.autoDispose()
        binding.searchEditText.textChanges().skipInitialValue().doOnNext {

        }.debounce(300, TimeUnit.MILLISECONDS, Schedulers.io()).subscribeOnIoAndObserveOnMainThread({
//            if (it.isNotEmpty()) {
//                RxBus.publish(RxEvent.SearchOrderFilter(it.toString()))
//            } else {
//                RxBus.publish(RxEvent.SearchOrderFilter(""))
//            }
        }, {
            Timber.e(it)
        }).autoDispose()
        binding.newOrderMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
//            startActivityWithDefaultAnimation(UserStoreWelcomeActivity.getIntent(this))
        }.autoDispose()
    }

    private fun generateRandomNumber(): Int {
        val random = Random(System.currentTimeMillis())
        return random.nextInt(15, 59)
    }

    private fun listenToViewModel() {
        storeViewModel.storeState.subscribeOnComputationAndObserveOnMainThread({
            when (it) {
                is StoreState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is StoreState.LoadingState -> {
                }
                is StoreState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is StoreState.StoreResponses -> {
                    setStoreOpenAndClose(it.storeResponse)
                }
                is StoreState.OrderDetailItemResponse -> {
                    val t: Thread = object : Thread() {
                        override fun run() {
                            autoReceive(it.bufferResponse)
                        }
                    }
                    t.start()
                }
                is StoreState.OrderInfoSate -> {
                    storeViewModel.playMusic()
                }
                is StoreState.PlayMusic -> {
                    var  mediaPlayer = MediaPlayer.create(applicationContext,R.raw.notification)
                    mediaPlayer.start()
                    storeViewModel.stopMusic()
                }
                else -> {

                }
            }
        }, {
            showToast(it.toString())
        }).autoDispose()
    }

    private fun autoReceive(orderDetails: OrderDetail) {
        if (!bohPrinterHelper.isPrinterConnected() && bohPrintAddress != null) {
            try {
                val isConnected = bohPrinterHelper.printerConnect(bohPrintAddress)
                Timber.tag("AutoReceive").e("Printer connection response ========= $isConnected")
            } catch (e: java.lang.Exception) {
                Timber.tag("AutoReceive").e(e)
            }
        }
        if (BuildConfig.DEBUG) {
            try {
                bohPrinterHelper.runPrintBOHReceiptSequence(orderDetails, bohPrintAddress)
            } catch (e: java.lang.Exception) {
                Timber.tag("runPrintBOHReceiptSequence").e(e)
            }
        }
        if (bohPrintAddress != null && bohPrinterHelper.isPrinterConnected()) {
            try {
                bohPrinterHelper.runPrintBOHReceiptSequence(orderDetails, bohPrintAddress)
            } catch (e: java.lang.Exception) {
                Timber.tag("runPrintBOHReceiptSequence").e(e)
            }
        } else {
            Timber.tag("AutoReceive").e("----------------- Printer not connected -----------------")
        }

    }

    private fun manageSelection(selectedMenuViewId: Int) {
        removeSelectionForLastSelection()
        setLastSelectedMenu(selectedMenuViewId)
        when (selectedMenuViewId) {
            R.id.orderMenuView -> {
                binding.orderMenuView.isMenuSelected = true
            }
            R.id.deliveriesMenuView -> {
                binding.deliveriesMenuView.isMenuSelected = true
            }
            R.id.orderMenuMenuView -> {
                binding.orderMenuMenuView.isMenuSelected = true
            }
            R.id.timeManagementMenuView -> {
                binding.timeManagementMenuView.isMenuSelected = true
            }
            R.id.storeMenuView -> {
                binding.storeMenuView.isMenuSelected = true
            }
            R.id.giftCardMenuView -> {
                binding.giftCardMenuView.isMenuSelected = true
            }
            R.id.settingMenuView -> {
                binding.settingMenuView.isMenuSelected = true
            }
        }
    }

    private fun setStoreOpenAndClose(storeResponse: StoreResponse) {
        val c = Calendar.getInstance()
        val dayOfWeek = c[Calendar.DAY_OF_WEEK]
        val currentTime = c.time.formatTo("HH:mm:ss")
        println("currentTime : $currentTime")
        println("dayOfWeek : $dayOfWeek")
        when (dayOfWeek - 1) {
            0 -> {
                loggedInUserCache
                val openTime = storeResponse.sundayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                val closeTime = storeResponse.sundayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                if (openTime != null && closeTime != null) {
                    if (openTime < currentTime) {
                        if (closeTime > currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = true
                            binding.tvOpenAndClose.isSelected = true
                            binding.tvOpenAndClose.text = resources.getText(R.string.open)
                        } else if (closeTime < currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        } else {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        }
                    } else if (openTime > currentTime) {
                        binding.openStoreTimeLinearLayout.isSelected = false
                        binding.tvOpenAndClose.isSelected = false
                        binding.tvOpenAndClose.text = resources.getText(R.string.close)
                    } else {
                        binding.openStoreTimeLinearLayout.isSelected = true
                        binding.tvOpenAndClose.isSelected = true
                        binding.tvOpenAndClose.text = resources.getText(R.string.open)
                    }
                }
            }
            1 -> {
                val openTime = storeResponse.mondayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                val closeTime = storeResponse.mondayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                if (openTime != null && closeTime != null) {
                    if (openTime < currentTime) {
                        if (closeTime > currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = true
                            binding.tvOpenAndClose.isSelected = true
                            binding.tvOpenAndClose.text = resources.getText(R.string.open)
                        } else if (closeTime < currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        } else {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        }
                    } else if (openTime > currentTime) {
                        binding.openStoreTimeLinearLayout.isSelected = false
                        binding.tvOpenAndClose.isSelected = false
                        binding.tvOpenAndClose.text = resources.getText(R.string.close)
                    } else {
                        binding.openStoreTimeLinearLayout.isSelected = true
                        binding.tvOpenAndClose.isSelected = true
                        binding.tvOpenAndClose.text = resources.getText(R.string.open)
                    }
                }
            }
            2 -> {
                val openTime = storeResponse.tuesdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                val closeTime = storeResponse.tuesdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                if (openTime != null && closeTime != null) {
                    if (openTime < currentTime) {
                        if (closeTime > currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = true
                            binding.tvOpenAndClose.isSelected = true
                            binding.tvOpenAndClose.text = resources.getText(R.string.open)
                        } else if (closeTime < currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        } else {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        }
                    } else if (openTime > currentTime) {
                        binding.openStoreTimeLinearLayout.isSelected = false
                        binding.tvOpenAndClose.isSelected = false
                        binding.tvOpenAndClose.text = resources.getText(R.string.close)
                    } else {
                        binding.openStoreTimeLinearLayout.isSelected = true
                        binding.tvOpenAndClose.isSelected = true
                        binding.tvOpenAndClose.text = resources.getText(R.string.open)
                    }
                }
            }
            3 -> {
                val openTime = storeResponse.wednesdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                val closeTime = storeResponse.wednesdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                if (openTime != null && closeTime != null) {
                    if (openTime < currentTime) {
                        if (closeTime > currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = true
                            binding.tvOpenAndClose.isSelected = true
                            binding.tvOpenAndClose.text = resources.getText(R.string.open)
                        } else if (closeTime < currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        } else {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        }
                    } else if (openTime > currentTime) {
                        binding.openStoreTimeLinearLayout.isSelected = false
                        binding.tvOpenAndClose.isSelected = false
                        binding.tvOpenAndClose.text = resources.getText(R.string.close)
                    } else {
                        binding.openStoreTimeLinearLayout.isSelected = true
                        binding.tvOpenAndClose.isSelected = true
                        binding.tvOpenAndClose.text = resources.getText(R.string.open)
                    }
                }
            }
            4 -> {
                val openTime = storeResponse.thursdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                val closeTime = storeResponse.thursdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                if (openTime != null && closeTime != null) {
                    if (openTime < currentTime) {
                        if (closeTime > currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = true
                            binding.tvOpenAndClose.isSelected = true
                            binding.tvOpenAndClose.text = resources.getText(R.string.open)
                        } else if (closeTime < currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        } else {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        }
                    } else if (openTime > currentTime) {
                        binding.openStoreTimeLinearLayout.isSelected = false
                        binding.tvOpenAndClose.isSelected = false
                        binding.tvOpenAndClose.text = resources.getText(R.string.close)
                    } else {
                        binding.openStoreTimeLinearLayout.isSelected = true
                        binding.tvOpenAndClose.isSelected = true
                        binding.tvOpenAndClose.text = resources.getText(R.string.open)
                    }
                }
            }
            5 -> {
                val openTime = storeResponse.fridayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                val closeTime = storeResponse.fridayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                if (openTime != null && closeTime != null) {
                    if (openTime < currentTime) {
                        if (closeTime > currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = true
                            binding.tvOpenAndClose.isSelected = true
                            binding.tvOpenAndClose.text = resources.getText(R.string.open)
                        } else if (closeTime < currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        } else {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        }
                    } else if (openTime > currentTime) {
                        binding.openStoreTimeLinearLayout.isSelected = false
                        binding.tvOpenAndClose.isSelected = false
                        binding.tvOpenAndClose.text = resources.getText(R.string.close)
                    } else {
                        binding.openStoreTimeLinearLayout.isSelected = true
                        binding.tvOpenAndClose.isSelected = true
                        binding.tvOpenAndClose.text = resources.getText(R.string.open)
                    }
                }
            }
            6 -> {
                val openTime = storeResponse.saturdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                val closeTime = storeResponse.saturdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                if (openTime != null && closeTime != null) {
                    if (openTime < currentTime) {
                        if (closeTime > currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = true
                            binding.tvOpenAndClose.isSelected = true
                            binding.tvOpenAndClose.text = resources.getText(R.string.open)
                        } else if (closeTime < currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        } else {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        }
                    } else if (openTime > currentTime) {
                        binding.openStoreTimeLinearLayout.isSelected = false
                        binding.tvOpenAndClose.isSelected = false
                        binding.tvOpenAndClose.text = resources.getText(R.string.close)
                    } else {
                        binding.openStoreTimeLinearLayout.isSelected = true
                        binding.tvOpenAndClose.isSelected = true
                        binding.tvOpenAndClose.text = resources.getText(R.string.open)
                    }
                }
            }
        }
    }

    private fun setLastSelectedMenu(selectedMenuViewId: Int) {
        lastSelectedMenuId = selectedMenuViewId
    }

    private fun removeSelectionForLastSelection() {
        when (lastSelectedMenuId) {
            R.id.orderMenuView -> {
                binding.orderMenuView.isMenuSelected = false
            }
            R.id.deliveriesMenuView -> {
                binding.deliveriesMenuView.isMenuSelected = false
            }
            R.id.orderMenuMenuView -> {
                binding.orderMenuMenuView.isMenuSelected = false
            }
            R.id.timeManagementMenuView -> {
                binding.timeManagementMenuView.isMenuSelected = false
            }
            R.id.storeMenuView -> {
                binding.storeMenuView.isMenuSelected = false
            }
            R.id.giftCardMenuView -> {
                binding.giftCardMenuView.isMenuSelected = false
            }
            R.id.settingMenuView -> {
                binding.settingMenuView.isMenuSelected = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resetDisconnectTimer()
        storeViewModel.loadCurrentStoreResponse()
    }

    override fun onUserInteraction() {
        binding.toolbarRelativeLayout.alpha = 1F
        binding.toolbarDivider.alpha = 1F
        Timber.tag("MainActivity").d("onUserInteraction")
        resetDisconnectTimer()
    }

    private val disconnectHandler = Handler {
        Timber.tag("MainActivity").d("disconnectHandler")
        false
    }

    private val disconnectCallback = Runnable { // Perform any required operation on disconnect
        Timber.tag("MainActivity").d("disconnectCallback")
        loggedInUserCache.clearLoggedInUserLocalPrefs()
        Toast.makeText(applicationContext, "Crew time out", Toast.LENGTH_LONG).show()
        startNewActivityWithDefaultAnimation(
            SplashActivity.getIntent(
                this@MainActivity
            )
        )
        finish()
    }

    private fun openLogOutDialog() {
        val alertDialog =
            AlertDialog.Builder(this).setTitle(resources.getText(R.string.logout)).setMessage(resources.getText(R.string.are_you_sure_log_out))
                .setNegativeButton(resources.getText(R.string.label_cancel)) { dialog, _ ->
                    dialog.dismiss()
                }.setPositiveButton(resources.getText(R.string.label_ok)) { _, _ ->
                    runOnUiThread {
                        loggedInUserCache.clearLoggedInUserLocalPrefs()
                        finish()
                    }
                }.show()

        alertDialog.window?.setLayout(800, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    private fun resetDisconnectTimer() {
        Timber.tag("MainActivity").d("resetDisconnectTimer")
        disconnectHandler.removeCallbacks(disconnectCallback)
        disconnectHandler.postDelayed(disconnectCallback, DISCONNECT_TIMEOUT)
    }

    private fun stopDisconnectTimer() {
        disconnectHandler.removeCallbacks(disconnectCallback)
    }

    override fun onPause() {
        super.onPause()
        Timber.tag("MainActivity").d("stopDisconnectTimer")
        stopDisconnectTimer()
    }

    override fun onBackPressed() {
        openLogOutDialog()
    }

}