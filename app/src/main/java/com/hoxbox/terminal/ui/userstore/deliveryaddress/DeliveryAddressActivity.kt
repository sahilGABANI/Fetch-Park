package com.hoxbox.terminal.ui.userstore.deliveryaddress

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.AdapterView
import android.widget.Toast
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.userstore.model.FeaturesItem
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseActivity
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.base.extension.*
import com.hoxbox.terminal.databinding.ActivityDeliveryAddressBinding
import com.hoxbox.terminal.ui.splash.SplashActivity
import com.hoxbox.terminal.ui.userstore.NearByLocationBottomSheetFragment
import com.hoxbox.terminal.ui.userstore.UserStoreActivity
import com.hoxbox.terminal.ui.userstore.deliveryaddress.view.DeliveryAddressAdapter
import com.hoxbox.terminal.ui.userstore.viewmodel.UserStoreWelcomeState
import com.hoxbox.terminal.ui.userstore.viewmodel.UserStoreWelcomeViewModel
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.ArrayList

class DeliveryAddressActivity : BaseActivity() {
    private var searchLocationList: List<FeaturesItem>? = arrayListOf()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserStoreWelcomeViewModel>
    private lateinit var userStoreWelcomeViewModel: UserStoreWelcomeViewModel
    private lateinit var deliveryAddressAdapter: DeliveryAddressAdapter

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var isSelected = false
    private var isMatch = false
    private var searchText = ""
    private val DISCONNECT_TIMEOUT: Long = 3600000
//    private val DISCONNECT_TIMEOUT: Long = 300000

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, DeliveryAddressActivity::class.java)
        }
    }

    private lateinit var binding: ActivityDeliveryAddressBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeliveryAddressBinding.inflate(layoutInflater)
        HotBoxApplication.component.inject(this)
        userStoreWelcomeViewModel = getViewModelFromFactory(viewModelFactory)
        setContentView(binding.root)
        listenToViewModel()
        initUI()
    }

    private fun initUI() {
        initAdapter()
        binding.nameEditText.textChanges().skipInitialValue().doOnNext {

        }.debounce(500, TimeUnit.MILLISECONDS, Schedulers.io()).subscribeOnIoAndObserveOnMainThread({
            if (it.isNotEmpty()) {
                if (searchText != it.toString()) userStoreWelcomeViewModel.loadCurrentStoreResponse(it.toString())
            } else {
                deliveryAddressAdapter.listOfLocation = listOf()
                isSelected = false
                binding.confirmMaterialButton.isEnabled = false
            }
        }, {
            Timber.e(it)
        }).autoDispose()
        binding.confirmMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            loggedInUserCache.setLoggedInUserCartGroupId(0)
            startActivityWithDefaultAnimation(UserStoreActivity.getIntent(this@DeliveryAddressActivity))
            finish()
        }.autoDispose()
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()

        binding.nameEditText.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            searchText = searchLocationList?.get(position)?.properties?.formatted.toString()
            hideKeyboard()
            isSelected = true
            val address: String = searchLocationList?.get(position)?.properties?.formatted.toString()
            val long: Double = searchLocationList?.get(position)?.properties?.lon ?: 0.00
            val lat: Double = searchLocationList?.get(position)?.properties?.lat ?: 0.00

            println("long : $long \n lat $lat")
            userStoreWelcomeViewModel.userLocationStoreResponse(address, long, lat)
        }
    }

    private fun initAdapter() {
        deliveryAddressAdapter = DeliveryAddressAdapter(this@DeliveryAddressActivity).apply {
            addressActionState.subscribeAndObserveOnMainThread {
                binding.nameEditText.setText(it.properties?.formatted.toString())
                deliveryAddressAdapter.listOfLocation = arrayListOf()
                hideKeyboard()
                isSelected = true
                val address: String = it.properties?.formatted.toString()
                searchText = it.properties?.formatted.toString()
                val long: Double = it.properties?.lon ?: 0.00
                val lat: Double = it.properties?.lat ?: 0.00
                loggedInUserCache.setdeliveryLat(lat.toString())
                loggedInUserCache.setdeliveryLong(long.toString())
                loggedInUserCache.setdeliveryAddress(address)
                userStoreWelcomeViewModel.userLocationStoreResponse(address, long, lat)
            }.autoDispose()
        }
        binding.rvLocationAddress.adapter = deliveryAddressAdapter

    }


    private fun listenToViewModel() {
        userStoreWelcomeViewModel.userStoreWelcomeState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserStoreWelcomeState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is UserStoreWelcomeState.LoadingState -> {

                }
                is UserStoreWelcomeState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is UserStoreWelcomeState.UserLocationInformation -> {
                    searchLocationList = it.userLocationInfo.features as List<FeaturesItem>?
                    var employeeNameList = listOf<String>()
                    searchLocationList?.forEach {
                        employeeNameList = employeeNameList + it.properties?.formatted.toString()
                    }
                    deliveryAddressAdapter.listOfLocation =  it.userLocationInfo.features


                }
                is UserStoreWelcomeState.NearLocationInformation -> {
                    val list = it.locationsInfo.locations as ArrayList
                    val locationId = loggedInUserCache.getLocationInfo()?.id ?: throw Exception("location not found")
                    list.forEach {
                        if (it.id == locationId) {
                            isMatch = true
                            binding.confirmMaterialButton.isEnabled = true
                        }
                    }
                    if (!isMatch) {
                        val nearByLocationBottomSheetFragment = NearByLocationBottomSheetFragment.newInstance(list)
                        nearByLocationBottomSheetFragment.show(supportFragmentManager, NearByLocationBottomSheetFragment::class.java.name)
                    }
                }
                is UserStoreWelcomeState.NearLocationErrorMessage -> {
                    showToast(it.errorMessage)
                }
            }

        }.autoDispose()
    }

    override fun onResume() {
        super.onResume()
        resetDisconnectTimer()
    }

    override fun onUserInteraction() {
        Timber.tag("DeliveryAddressActivity").d("onUserInteraction")
        resetDisconnectTimer()
    }

    private val disconnectHandler = Handler {
        Timber.tag("DeliveryAddressActivity").d("disconnectHandler")
        false
    }

    private val disconnectCallback = Runnable { // Perform any required operation on disconnect
        Timber.tag("DeliveryAddressActivity").d("disconnectCallback")
        loggedInUserCache.clearLoggedInUserLocalPrefs()
        Toast.makeText(applicationContext, "Crew time out", Toast.LENGTH_LONG).show()
        startNewActivityWithDefaultAnimation(
            SplashActivity.getIntent(
                this@DeliveryAddressActivity
            )
        )
        finish()
    }

    private fun resetDisconnectTimer() {
        Timber.tag("DeliveryAddressActivity").d("resetDisconnectTimer")
        disconnectHandler.removeCallbacks(disconnectCallback)
        disconnectHandler.postDelayed(disconnectCallback, DISCONNECT_TIMEOUT)
    }

    private fun stopDisconnectTimer() {
        disconnectHandler.removeCallbacks(disconnectCallback)
    }

    override fun onPause() {
        super.onPause()
        Timber.tag("DeliveryAddressActivity").d("stopDisconnectTimer")
        stopDisconnectTimer()
    }
}