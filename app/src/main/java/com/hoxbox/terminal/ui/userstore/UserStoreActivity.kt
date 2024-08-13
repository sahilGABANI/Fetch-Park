package com.hoxbox.terminal.ui.userstore

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.hoxbox.terminal.BuildConfig
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.checkout.model.QRScanResponse
import com.hoxbox.terminal.api.menu.model.MenuListInfo
import com.hoxbox.terminal.api.menu.model.ProductsItem
import com.hoxbox.terminal.api.store.model.StoreResponse
import com.hoxbox.terminal.api.userstore.model.*
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseActivity
import com.hoxbox.terminal.base.RxBus
import com.hoxbox.terminal.base.RxEvent
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.base.extension.*
import com.hoxbox.terminal.databinding.ActivityUserStoreBinding
import com.hoxbox.terminal.helper.formatTo
import com.hoxbox.terminal.helper.toDate
import com.hoxbox.terminal.ui.splash.SplashActivity
import com.hoxbox.terminal.ui.userstore.cookies.CookiesFragment
import com.hoxbox.terminal.ui.userstore.view.CartAdapter
import com.hoxbox.terminal.ui.userstore.view.CategoryAdapter
import com.hoxbox.terminal.ui.userstore.view.SideNavigationAdapterUserStore
import com.hoxbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hoxbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import com.hoxbox.terminal.utils.Constants
import com.hoxbox.terminal.utils.Constants.DELIVERY_ORDER_TYPE_ID
import com.hoxbox.terminal.utils.Constants.MHS_MEMBER_DISCOUNT
import com.hoxbox.terminal.utils.Constants.ORDER_EMPLOYEE_DISCOUNT
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.properties.Delegates


class UserStoreActivity : BaseActivity() {

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserStoreViewModel>
    private lateinit var userStoreViewModel: UserStoreViewModel

    private var versionName: String = BuildConfig.VERSION_NAME
    private lateinit var cartAdapter: CartAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private var deletedCartItemId by Delegates.notNull<Int>()
    var listOfProductDetails: ArrayList<CartItem>? = null
    var listOfProduct: ArrayList<ProductsItem>? = null
    private var giftCardAmount: Int = 0
    private var giftCardId: Int? = 0
    private var couponCodeId: Int? = 0
    private var promisedTime: String? = null
    private var orderTax: Double = 0.0
    private var orderSubTotalCount: Double = 0.00
    private var orderTotal: Double? = 0.0
    private var orderSubTotal: Double = 0.0
    private var promoCodeAmount: Double = 0.00
    private var userCredit: Double = 0.00
    private var employeeDiscount: Double = 0.00
    private var mhsMemberDiscount: Double = 0.00
    private var adjustmentDiscount: Double = 0.00
    private val DISCONNECT_TIMEOUT: Long = 3600000

    //    private val DISCONNECT_TIMEOUT: Long = 300000
    private var taxRate: Double = 0.00
    private var cash: Double? = 0.0
    private var createOrder: Boolean = false

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, UserStoreActivity::class.java)
        }
    }

    private lateinit var binding: ActivityUserStoreBinding
    private lateinit var sideNavigationAdapterUserStore: SideNavigationAdapterUserStore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserStoreBinding.inflate(layoutInflater)
        HotBoxApplication.component.inject(this)
        userStoreViewModel = getViewModelFromFactory(viewModelFactory)
        onApiCalling()
        setContentView(binding.root)
        listenToViewModel()
        initUI()
        window?.decorView?.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    @SuppressLint("HardwareIds")
    private fun initUI() {
        taxRate = Constants.getTaxRate()
        initAdapter()
        if (loggedInUserCache.getLoggedInUserId() != null) {
            binding.rlEmployeeDetails.visibility = View.VISIBLE
            binding.employeeNameAppCompatTextView.text = loggedInUserCache.getLoggedInUserFullName()
            binding.loggedInUserRoleTextView.text = loggedInUserCache.getLoggedInUserRole()
        } else {
            binding.rlGuest.visibility = View.VISIBLE
        }
        binding.cartView.cartListAndPrizeLinearLayout.isVisible = false
        binding.cartView.emptyMessageAppCompatTextView.isVisible = true
        binding.cartView.adjustmentsMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityWithDefaultAnimation(
                CompAndAdjustmentActivity.getIntent(
                    this@UserStoreActivity, employeeDiscount, adjustmentDiscount, giftCardAmount.toDouble(), promoCodeAmount, mhsMemberDiscount
                )
            )
        }.autoDispose()
        sideNavigationAdapterUserStore = SideNavigationAdapterUserStore(this)
        binding.userStoreViewPager.apply {
            offscreenPageLimit = 1
            adapter = sideNavigationAdapterUserStore
            isUserInputEnabled = false
        }
        binding.userStoreViewPager.currentItem = 0
        binding.backLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            when (binding.userStoreViewPager.currentItem) {
                1 -> {
                    binding.userStoreViewPager.currentItem = 0
                    binding.cartView.rlCredit.isVisible = false
                    binding.cartView.rlGiftCard.isVisible = false
                    binding.cartView.rlPromocode.isVisible = false
//                    binding.cartView.adjustmentsMaterialButton.isVisible = true
                    binding.cartView.adjustmentsMaterialButton.isVisible = false
                    binding.cartView.proceedToPaymentMaterialButton.isVisible = false
                    binding.cartView.proceedToCheckoutMaterialButton.isVisible = true
                    loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(null, null, null, null, null))
                    userCredit = 0.0
                    giftCardAmount = 0
                    promoCodeAmount = 0.0
                    val list = cartAdapter.listOfProductDetails
                    list?.filter { it.isChanging == false }?.forEach {
                        it.isChanging = true
                    }
                    cartAdapter.listOfProductDetails = list
                    orderTotalCount()
                }
                2 -> {
                    checkOutFragment()
                }
                else -> {
                    onBackPressed()
                }
            }
        }.autoDispose()
        RxBus.listen(RxEvent.EventCartGroupIdListen::class.java).subscribeAndObserveOnMainThread {
            userStoreViewModel.getCartDetails(it.cartGroupId)
        }.autoDispose()
        binding.locationAppCompatTextView.text = loggedInUserCache.getLocationInfo()?.locationName ?: throw Exception("location not found")
        binding.versionNameAppCompatTextView.text = resources.getString(R.string.title_version, versionName)
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        binding.androidIdAppCompatTextView.text = resources.getString(R.string.title_device_id, androidId)
        binding.liveTimeTextClock.format12Hour = "hh:mm a"
        binding.cartView.proceedToCheckoutMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            removeCategorySelection()
            cartItemNotChangeNow()
            binding.tvCartName.text = resources.getString(R.string.your_order)
            binding.userStoreViewPager.currentItem = 1
            orderTotalCount()
            UserDetails(null, null, null, null)
            loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(null, null, null, null, null))
            RxBus.publish(
                RxEvent.EventTotalCheckOut(
                    OrderPrice(
                        orderTotal.toConvertDecimalFormat(),
                        orderSubTotalCount,
                        employeeDiscount = employeeDiscount,
                        adjustmentDiscount = adjustmentDiscount,
                        mhsMemberDiscount = mhsMemberDiscount
                    )
                )
            )
        }.autoDispose()
        binding.cartView.createOrderMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            hideKeyboard()
            if (loggedInUserCache.getLoyaltyQrResponse()?.id == null || loggedInUserCache.getLoyaltyQrResponse()?.id == 0) {
                RxBus.publish(RxEvent.EventValidation)
                createOrder = true
            } else {
                val total = binding.cartView.tvTotalPrizeNumber.text.toString().removePrefix("$").toDouble()
                val createOrderRequest = CreateOrderRequest(
                    orderUserId = if (loggedInUserCache.getLoyaltyQrResponse()?.id != null) loggedInUserCache.getLoyaltyQrResponse()?.id else null,
                    Constants.ORDER_MODE_ID,
                    0,
                    orderTypeId = loggedInUserCache.getorderTypeId(),
                    loggedInUserCache.getLoggedInUserCartGroupId(),
                    orderLocationId = loggedInUserCache.getLocationInfo()?.id,
                    transactionChargeId = "COMP'D ORDER",
                    transactionIdOfProcessor = "Transaction Comp--Cart ID: [".plus("${loggedInUserCache.getLoggedInUserCartGroupId()}").plus("]"),
                    transactionAmount = total.times(100).toString(),
                    deliveryAddress = if (loggedInUserCache.getorderTypeId() == Constants.DELIVERY_ORDER_TYPE_ID) loggedInUserCache.getdeliveryAddress() else null,
                    guestName = (if (loggedInUserCache.getLoyaltyQrResponse()?.fullName?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.fullName.toString() else null),
                    guestPhone = (if (loggedInUserCache.getLoyaltyQrResponse()?.phone?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.phone.toString() else "N/A"),
                    giftCardId = if (giftCardId != 0) giftCardId else null,
                    orderGiftCardAmount = if (giftCardAmount != 0) giftCardAmount else null,
                    couponCodeId = if (couponCodeId != 0) couponCodeId else null,
                    creditAmount = if (userCredit.toInt() != 0) userCredit.toInt() else null,
                    orderPromisedTime = promisedTime,
                    orderAdjustments = adjustmentDiscount.toInt(),
                    lat = if (loggedInUserCache.getorderTypeId() == DELIVERY_ORDER_TYPE_ID) loggedInUserCache.getdeliveryLat() else null,
                    long = if (loggedInUserCache.getorderTypeId() == DELIVERY_ORDER_TYPE_ID) loggedInUserCache.getdeliveryLong() else null
                )
                println("createOrderRequest : ${Gson().toJson(createOrderRequest)}")
                userStoreViewModel.createOrder(createOrderRequest)
                createOrder = false
            }
        }.autoDispose()
        binding.cartView.proceedToPaymentMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            hideKeyboard()
            RxBus.publish(RxEvent.EventCheckValidation)
        }.autoDispose()
        RxBus.listen(RxEvent.EventGoToBack::class.java).subscribeAndObserveOnMainThread {
            if (createOrder) {
                val total = binding.cartView.tvTotalPrizeNumber.text.toString().removePrefix("$").toDouble()
                val createOrderRequest = CreateOrderRequest(
                    orderUserId = if (loggedInUserCache.getLoyaltyQrResponse()?.id != null) loggedInUserCache.getLoyaltyQrResponse()?.id else null,
                    Constants.ORDER_MODE_ID,
                    0,
                    orderTypeId = loggedInUserCache.getorderTypeId(),
                    loggedInUserCache.getLoggedInUserCartGroupId(),
                    orderLocationId = loggedInUserCache.getLocationInfo()?.id,
                    transactionChargeId = "COMP'D ORDER",
                    transactionIdOfProcessor = "Transaction Comp--Cart ID: [".plus("${loggedInUserCache.getLoggedInUserCartGroupId()}").plus("]"),
                    transactionAmount = total.times(100).toString(),
                    guestName = if (loggedInUserCache.getLoyaltyQrResponse()?.fullName?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.fullName.toString() else null,
                    guestPhone = (if (loggedInUserCache.getLoyaltyQrResponse()?.phone?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.phone.toString() else "N/A"),
                    giftCardId = if (giftCardId != 0) giftCardId else null,
                    orderGiftCardAmount = if (giftCardAmount != 0) giftCardAmount else null,
                    couponCodeId = if (couponCodeId != 0) couponCodeId else null,
                    creditAmount = if (userCredit.toInt() != 0) userCredit.toInt() else null,
                    orderPromisedTime = promisedTime,
                    orderAdjustments = adjustmentDiscount.toInt(),
                    lat = if (loggedInUserCache.getorderTypeId() == Constants.DELIVERY_ORDER_TYPE_ID) loggedInUserCache.getdeliveryLat() else null,
                    long = if (loggedInUserCache.getorderTypeId() == Constants.DELIVERY_ORDER_TYPE_ID) loggedInUserCache.getdeliveryLong() else null,
                    deliveryAddress = if (loggedInUserCache.getorderTypeId() == Constants.DELIVERY_ORDER_TYPE_ID) loggedInUserCache.getdeliveryAddress() else null,
                )
                println("createOrderRequest : ${Gson().toJson(createOrderRequest)}")
                userStoreViewModel.createOrder(createOrderRequest)
                createOrder = false
            }
        }.autoDispose()
        RxBus.listen(RxEvent.EventGoToPaymentScreen::class.java).subscribeAndObserveOnMainThread {
            if (it.data) {
                removeCategorySelection()
                binding.cartView.proceedToPaymentMaterialButton.isVisible = false
                binding.tvCartName.text = resources.getText(R.string.order_details)
                binding.cartView.customerDetails.isVisible = true
                binding.cartView.tvOrderHeading.isVisible = true
                binding.cartView.rlTax.isVisible = false
                binding.cartView.rlOrderPrice.isVisible = false
                binding.cartView.rlTotal.isVisible = false
                binding.cartView.rlGiftCard.isVisible = false
                binding.cartView.rlPromocode.isVisible = false
                binding.cartView.rlChanging.isVisible = false
                binding.cartView.rlCredit.isVisible = false
                binding.cartView.viewPointBelow.isVisible = false
                binding.cartView.rlAdjustment.isVisible = false
                binding.cartView.adjustmentsMaterialButton.isVisible = false
                binding.cartView.viewOrder.isVisible = true
                binding.userStoreViewPager.currentItem = 2
                binding.cartView.customerNameAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.fullName
                if (loggedInUserCache.getLoyaltyQrResponse()?.email.isNullOrEmpty()) {
                    binding.cartView.customerEmailAppCompatTextView.text = "-"
                } else {
                    binding.cartView.customerEmailAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.email
                }
                if (loggedInUserCache.getLoyaltyQrResponse()?.phone.isNullOrEmpty()) {
                    binding.cartView.customerPhoneNumberAppCompatTextView.text = "-"
                } else {
                    binding.cartView.customerPhoneNumberAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.phone
                }
                if (loggedInUserCache.getLoyaltyQrResponse()?.email.isNullOrEmpty()) {
                    binding.cartView.customerEmailAppCompatTextView.text = "-"
                } else {
                    binding.cartView.customerEmailAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.email
                }
                RxBus.publish(
                    RxEvent.EventTotalPayment(
                        OrderPrice(
                            orderTotal.toConvertDecimalFormat(),
                            orderSubTotalCount,
                            employeeDiscount = employeeDiscount,
                            adjustmentDiscount = adjustmentDiscount,
                            mhsMemberDiscount = mhsMemberDiscount
                        )
                    )
                )
            }
        }.autoDispose()
        RxBus.listen(RxEvent.EventPaymentButtonEnabled::class.java).subscribeAndObserveOnMainThread {
            binding.cartView.proceedToPaymentMaterialButton.isEnabled = it.enable
            binding.cartView.createOrderMaterialButton.isEnabled = it.enable
        }.autoDispose()
        RxBus.listen(RxEvent.AddGiftCart::class.java).subscribeAndObserveOnMainThread {
            binding.cartView.rlGiftCard.isVisible = true
            giftCardAmount = it.giftCardAmount
            giftCardId = it.giftCardId
            binding.cartView.tvCardAndBowCharge.text = "-${it.giftCardAmount.toDouble().div(100).toDollar()}"
            binding.cartView.tvTotalPrizeNumber.text = orderTotal?.minus(it.giftCardAmount.toDouble().div(100)).toDollar()
            orderTotal = orderTotal?.minus(it.giftCardAmount.toDouble().div(100))
            if (orderTotal?.toConvertDecimalFormat() == 0.00) {
                binding.cartView.proceedToPaymentMaterialButton.isVisible = false
                binding.cartView.adjustmentsMaterialButton.isVisible = false
                binding.cartView.createOrderMaterialButton.isVisible = true
                binding.cartView.createOrderMaterialButton.isEnabled = true
            } else {
                binding.cartView.proceedToPaymentMaterialButton.isVisible = true
                binding.cartView.createOrderMaterialButton.isVisible = false
            }
        }.autoDispose()
        RxBus.listen(RxEvent.AddPromoCode::class.java).subscribeAndObserveOnMainThread {
            binding.cartView.rlPromocode.isVisible = true
            promoCodeAmount = it.promocodeAmount
            couponCodeId = it.couponCodeId
            binding.cartView.tvPromoCodeDiscount.text = "-${it.promocodeAmount.div(100).toDollar()}"
            binding.cartView.tvTotalPrizeNumber.text = orderTotal?.minus(it.promocodeAmount.div(100)).toDollar()
            orderTotal = orderTotal?.minus(it.promocodeAmount.div(100))
            if (orderTotal?.toConvertDecimalFormat() == 0.00) {
                binding.cartView.proceedToPaymentMaterialButton.isVisible = false
                binding.cartView.adjustmentsMaterialButton.isVisible = false
                binding.cartView.createOrderMaterialButton.isVisible = true
                binding.cartView.createOrderMaterialButton.isEnabled = true
            } else {
                binding.cartView.proceedToPaymentMaterialButton.isVisible = true
                binding.cartView.createOrderMaterialButton.isVisible = false
            }
        }.autoDispose()
        RxBus.listen(RxEvent.RemoveGiftCart::class.java).subscribeAndObserveOnMainThread {
            binding.cartView.rlGiftCard.isVisible = false
            orderTotal = orderTotal?.plus(giftCardAmount.toDouble().div(100))
            giftCardAmount = 0
            binding.cartView.tvTotalPrizeNumber.text = orderTotal.toConvertDecimalFormat().toDollar()
//            orderTotalCount()
        }.autoDispose()
        RxBus.listen(RxEvent.AddEmployeeDiscount::class.java).subscribeAndObserveOnMainThread {
            binding.cartView.rlEmployeeDiscount.isVisible = it.discount != 0.00
            employeeDiscount = it.discount
            binding.cartView.tvEmployeeDiscountPrize.text = "-${employeeDiscount.div(100).toDollar()}"
            binding.cartView.tvTotalPrizeNumber.text = orderTotal?.minus(employeeDiscount.div(100)).toDollar()
            orderTotal = orderTotal?.minus(employeeDiscount.div(100))
            if (orderTotal?.toConvertDecimalFormat() == 0.00) {
                binding.cartView.proceedToPaymentMaterialButton.isVisible = false
                binding.cartView.adjustmentsMaterialButton.isVisible = false
                binding.cartView.createOrderMaterialButton.isVisible = true
                binding.cartView.createOrderMaterialButton.isEnabled = true
            } else {
                binding.cartView.proceedToPaymentMaterialButton.isVisible = true
                binding.cartView.createOrderMaterialButton.isVisible = false
            }
        }.autoDispose()
        RxBus.listen(RxEvent.AddMHSMemberDiscount::class.java).subscribeAndObserveOnMainThread {
            binding.cartView.rlOrderDiscount.isVisible = it.discount != 0.00
            mhsMemberDiscount = it.discount
            binding.cartView.tvEmployeeDiscountPrize.text = "-${mhsMemberDiscount.div(100).toDollar()}"
            binding.cartView.tvTotalPrizeNumber.text = orderTotal?.minus(mhsMemberDiscount.div(100)).toDollar()
            orderTotal = orderTotal?.minus(mhsMemberDiscount.div(100))
            if (orderTotal?.toConvertDecimalFormat() == 0.00) {
                binding.cartView.proceedToPaymentMaterialButton.isVisible = false
                binding.cartView.adjustmentsMaterialButton.isVisible = false
                binding.cartView.createOrderMaterialButton.isVisible = true
                binding.cartView.createOrderMaterialButton.isEnabled = true
            } else {
                binding.cartView.proceedToPaymentMaterialButton.isVisible = true
                binding.cartView.createOrderMaterialButton.isVisible = false
            }
        }.autoDispose()
        RxBus.listen(RxEvent.AddAdjustmentDiscount::class.java).subscribeAndObserveOnMainThread {
            if (it.discount == 0.00) {
                binding.cartView.rlAdjustment.isVisible = false
                orderTotal = orderTotal?.plus(adjustmentDiscount.div(100))
                adjustmentDiscount = 0.00
                binding.cartView.tvTotalPrizeNumber.text = orderTotal?.plus(adjustmentDiscount.div(100)).toDollar()
                orderTotal = orderTotal?.minus(employeeDiscount.div(100))
                orderTotal = orderTotal?.minus(mhsMemberDiscount.div(100))
            } else {
                binding.cartView.rlAdjustment.isVisible = true
                orderTotal = orderTotal?.plus(adjustmentDiscount.div(100))
                adjustmentDiscount = it.discount
                if (it.discount > 0) {
                    binding.cartView.tvAdjustmentDiscountPrize.text = adjustmentDiscount.div(100).toDollar()
                } else {
                    binding.cartView.tvAdjustmentDiscountPrize.text = "-${abs(adjustmentDiscount.toDouble()).div(100).toDollar()}"
                }
                binding.cartView.tvTotalPrizeNumber.text = orderTotal?.plus(adjustmentDiscount.div(100)).toDollar()
                orderTotal = orderTotal?.minus(employeeDiscount.div(100))
                orderTotal = orderTotal?.minus(mhsMemberDiscount.div(100))
            }
            orderTotalCount()
        }.autoDispose()
        RxBus.listen(RxEvent.RemovePromoCode::class.java).subscribeAndObserveOnMainThread {
            binding.cartView.rlPromocode.isVisible = false
            orderTotal = orderTotal?.plus(promoCodeAmount.div(100))
            promoCodeAmount = 0.00
            binding.cartView.tvTotalPrizeNumber.text = orderTotal.toConvertDecimalFormat().toDollar()
//            orderTotalCount()
        }.autoDispose()
        RxBus.listen(RxEvent.RemoveCredit::class.java).subscribeAndObserveOnMainThread {
            binding.cartView.rlCredit.isVisible = false
            orderTotal = orderTotal?.plus(userCredit.div(100))
            userCredit = 0.00
            binding.cartView.tvTotalPrizeNumber.text = orderTotal.toConvertDecimalFormat().toDollar()
//            orderTotalCount()
        }.autoDispose()
        RxBus.listen(RxEvent.EventGotoStartButton::class.java).subscribeAndObserveOnMainThread {
            binding.userStoreViewPager.currentItem = 0
            categorySelection()
            binding.tvCartName.text = resources.getString(R.string.cart)
            binding.cartView.customerDetails.isVisible = false
            binding.cartView.tvOrderHeading.isVisible = false
            binding.cartView.viewOrder.isVisible = false
            binding.cartView.rlGiftCard.isVisible = false
            binding.cartView.rlPromocode.isVisible = false
            binding.cartView.rlCredit.isVisible = false
            binding.cartView.cartListAndPrizeLinearLayout.isVisible = false
            binding.cartView.emptyMessageAppCompatTextView.isVisible = true
            promoCodeAmount = 0.0
            giftCardAmount = 0
            userCredit = 0.0
            adjustmentDiscount = 0.00
            loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(null, null, null, null, null))
        }.autoDispose()
        RxBus.listen(RxEvent.AddCredit::class.java).subscribeAndObserveOnMainThread {
            println("credit :${it.credit}")
            binding.cartView.rlCredit.isVisible = true
            userCredit = it.credit
            binding.cartView.tvCreditDiscount.text = "-${it.credit.div(100).toDollar()}"
            binding.cartView.tvTotalPrizeNumber.text = orderTotal?.minus(it.credit.div(100)).toDollar()
            orderTotal = orderTotal?.minus(it.credit.div(100))
        }.autoDispose()
        binding.openStoreTimeLinearLayout.isSelected = true
        binding.tvOpenAndClose.isSelected = true

        if (loggedInUserCache.getorderTypeId() == DELIVERY_ORDER_TYPE_ID) {
            binding.storeAppCompatImageView.setImageResource(R.drawable.ic_deliveries_selected)
        }
    }

    private fun cartItemNotChangeNow() {
        val list = cartAdapter.listOfProductDetails
        list?.filter { it.isChanging == true }?.forEach {
            it.isChanging = false
        }
        cartAdapter.listOfProductDetails = list
    }

    private fun removeCategorySelection() {
        binding.CategoryRecycle.alpha = 0.5F
        val listOfMenu = categoryAdapter.listOfMenu
        listOfMenu?.filter { it.isSelected }?.forEach {
            it.isSelected = false
        }
        categoryAdapter.listOfMenu = listOfMenu
    }

    private fun categorySelection() {
        binding.CategoryRecycle.alpha = 1F
        val listOfMenu = categoryAdapter.listOfMenu
        listOfMenu?.get(0)?.isSelected = true
        categoryAdapter.listOfMenu = listOfMenu
        CookiesFragment.listOfProduct = listOfProduct
    }


    private fun orderTotalCount() {
        orderSubTotal = 0.00
        var productTotal = 0.00
        listOfProductDetails?.forEach { it ->
            if (it.productComp != 1) {
                it.menuItemPrice?.let {
                    productTotal = it.toDouble()
                }
            } else {
                productTotal = 0.00
            }
            it.menuItemModifiers?.forEach {
                it?.options?.forEach { item ->
                    productTotal = productTotal.plus(item.optionPrice!!)
                }
            }
            productTotal = it.menuItemQuantity?.let { it1 -> productTotal.times(it1) } ?: 0.00
            orderSubTotal = orderSubTotal.plus(productTotal)
        }
        orderSubTotalCount = orderSubTotal.div(100)
        employeeDiscount.let {
            if (it != 0.00) {
                employeeDiscount = orderSubTotalCount.times(ORDER_EMPLOYEE_DISCOUNT)
            }
        }

        mhsMemberDiscount.let {
            if (it != 0.00) {
                mhsMemberDiscount = orderSubTotalCount.times(MHS_MEMBER_DISCOUNT)
            }
        }

        orderTax = orderSubTotalCount.times(taxRate).div(100).toConvertDecimalFormat()
        orderTotal = orderSubTotalCount + orderTax
        orderTotal = orderTotal?.minus(giftCardAmount.toDouble().div(100))
        orderTotal = orderTotal?.minus(employeeDiscount.div(100))
        orderTotal = orderTotal?.minus(mhsMemberDiscount.div(100))

        orderTotal = orderTotal?.minus(promoCodeAmount.div(100))
        if (orderTotal.toConvertDecimalFormat() == -0.00) {
            orderTotal = orderTotal?.let { it1 -> kotlin.math.abs(it1) }
        }


        adjustmentDiscount.let {
            if (it != 0.00) {
                binding.cartView.rlAdjustment.isVisible = true
                orderTotal = orderTotal.toConvertDecimalFormat().plus(adjustmentDiscount.div(100))
            }
        }
        binding.cartView.tvOrderPrizeNumber.isVisible = true
        binding.cartView.tvTaxNumber.isVisible = true
        binding.cartView.tvTotalPrizeNumber.isVisible = true
        if (employeeDiscount != -0.00) {
            binding.cartView.rlEmployeeDiscount.isVisible = true
        }

        if (cash != null && cash != 0.00) {

            binding.cartView.rlFinalTotal.isVisible = binding.userStoreViewPager.currentItem != 2
            if (orderTotal.toConvertDecimalFormat() < 0.00) {
                orderTotal = orderTotal?.let { it1 -> abs(it1) }
                binding.cartView.tvTotalPrizeNumber.text = orderTotal.toConvertDecimalFormat().toDollar()
            }
            if ((orderTotal ?: 0.00) > (cash ?: 0.00).div(100)) {
                orderTotal = orderTotal?.minus((cash ?: 0.00).div(100))
            } else {
                val changing = orderTotal?.minus((cash ?: 0.00).div(100))
                orderTotal = orderTotal?.minus((cash ?: 0.00).div(100))
                if (changing != 0.00) {
                    binding.cartView.rlChanging.isVisible = binding.userStoreViewPager.currentItem != 2
                    binding.cartView.tvChangingNumber.text = changing?.let { abs(it).toConvertDecimalFormat().toDollar() }
                }
                changing?.let {
                    orderTotal = orderTotal?.plus(abs(it))
                }

            }

        } else {
            binding.cartView.rlFinalTotal.isVisible = false
            orderTotal = orderTotal?.let { it1 -> abs(it1) }
            binding.cartView.tvTotalPrizeNumber.text = orderTotal.toConvertDecimalFormat().toDollar()
        }
        binding.cartView.tvOrderPrizeNumber.text = orderSubTotalCount.toConvertDecimalFormat().toDollar()
        binding.cartView.tvTaxNumber.text = orderTax.toConvertDecimalFormat().toDollar()
        binding.cartView.tvEmployeeDiscountPrize.text = "-" + employeeDiscount.div(100).toConvertDecimalFormat().toDollar()
        binding.cartView.tvOrderDiscountPrize.text = "-" + mhsMemberDiscount.div(100).toConvertDecimalFormat().toDollar()
        binding.cartView.tvFinalTotalPrizeNumber.text = orderTotal.toConvertDecimalFormat().toDollar()


        if (binding.userStoreViewPager.currentItem == 0) {
            if (orderTotal.toConvertDecimalFormat() == 0.00) {
                binding.cartView.proceedToCheckoutMaterialButton.isVisible = true
                binding.cartView.proceedToPaymentMaterialButton.isVisible = false
                binding.cartView.createOrderMaterialButton.isVisible = false
                binding.cartView.adjustmentsMaterialButton.isVisible = false
            } else {
                binding.cartView.proceedToCheckoutMaterialButton.isVisible = true
                binding.cartView.proceedToPaymentMaterialButton.isVisible = false
                binding.cartView.createOrderMaterialButton.isVisible = false
//                binding.cartView.adjustmentsMaterialButton.isVisible = loggedInUserCache.isUserLoggedIn()
                binding.cartView.adjustmentsMaterialButton.isVisible = false
            }
        } else if (binding.userStoreViewPager.currentItem == 1) {
            if (orderTotal.toConvertDecimalFormat() == 0.00) {
                binding.cartView.proceedToCheckoutMaterialButton.isVisible = false
                binding.cartView.proceedToPaymentMaterialButton.isVisible = false
                binding.cartView.createOrderMaterialButton.isVisible = true
                binding.cartView.adjustmentsMaterialButton.isVisible = false
            } else {
                binding.cartView.proceedToCheckoutMaterialButton.isVisible = false
                binding.cartView.proceedToPaymentMaterialButton.isVisible = true
                binding.cartView.createOrderMaterialButton.isVisible = false
//                binding.cartView.adjustmentsMaterialButton.isVisible = loggedInUserCache.isUserLoggedIn()
                binding.cartView.adjustmentsMaterialButton.isVisible = false
            }
            RxBus.publish(RxEvent.EventTotalCheckOut(OrderPrice(orderTotal.toConvertDecimalFormat(), orderSubTotalCount)))
        } else {
            binding.cartView.rlTotal.isVisible = false
            binding.cartView.rlCredit.isVisible = false
            binding.cartView.rlPromocode.isVisible = false
            binding.cartView.rlGiftCard.isVisible = false
            binding.cartView.rlEmployeeDiscount.isVisible = false
            binding.cartView.rlOrderPrice.isVisible = false
            binding.cartView.rlAdjustment.isVisible = false
            binding.cartView.adjustmentsMaterialButton.isVisible = false
            binding.cartView.rlTax.isVisible = false
        }
//        if (orderTotal == 0.00) {
//            binding.cartView.createOrderMaterialButton.isVisible = false
//            binding.cartView.proceedToCheckoutMaterialButton.isVisible = true
//            binding.cartView.adjustmentsMaterialButton.isVisible = false
//            binding.cartView.rlPoint.isVisible = false
//        } else {
//            binding.cartView.createOrderMaterialButton.isVisible = false
//            if (binding.userStoreViewPager.currentItem == 0) binding.cartView.proceedToCheckoutMaterialButton.isVisible = true
//            if (loggedInUserCache.isUserLoggedIn()) binding.cartView.adjustmentsMaterialButton.isVisible = true
//        }

//        if (loggedInUserCache.getIsEmployeeMeal() == true) {
//            binding.cartView.rlPoint.isVisible = false
//            binding.cartView.viewPointBelow.isVisible = false
//        }
//        binding.cartView.tvPoint.text = orderSubTotalCount.times(10).toInt().toString()
    }

    private fun initAdapter() {
        cartAdapter = CartAdapter(this).apply {
            userStoreCartActionState.subscribeAndObserveOnMainThread {
                val productQuantity = it.menuItemQuantity?.plus(1)
                it.id?.let { it1 -> userStoreViewModel.updateMenuItemQuantity(it1, UpdateMenuItemQuantity(productQuantity)) }
            }.autoDispose()

            userStoreCartQuantitySubscriptionActionState.subscribeAndObserveOnMainThread {
                if (it.menuItemQuantity != 1) {
                    val productQuantity = it.menuItemQuantity?.minus(1)
                    it.id?.let { it1 -> userStoreViewModel.updateMenuItemQuantity(it1, UpdateMenuItemQuantity(productQuantity)) }
                } else {
                    showToast(getString(R.string.minimum_Quantity))
                }
            }.autoDispose()

            deleteCartItemActionState.subscribeAndObserveOnMainThread {
                it.id?.let { item -> deletedCartItemId = item }
                it.id?.let { item -> userStoreViewModel.deleteCartItem(DeleteCartItemRequest(item)) }
            }.autoDispose()
        }
        binding.cartView.CartRecycleView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.cartView.CartRecycleView.apply {
            adapter = cartAdapter
        }
        categoryAdapter = CategoryAdapter(this).apply {
            userStoreCategoryActionState.subscribeAndObserveOnMainThread { item ->
                val listOfMenu = categoryAdapter.listOfMenu
                listOfMenu?.filter { it.isSelected }?.forEach {
                    it.isSelected = false
                }
                listOfMenu?.find { it.id == item.id }?.apply {
                    isSelected = true
                }
                categoryAdapter.listOfMenu = listOfMenu
                val listOfFilterItems = listOfMenu?.filter { it.categoryName == item.categoryName }
                if ((listOfFilterItems?.size ?: 0) > 0) CookiesFragment.listOfProduct = listOfFilterItems?.firstOrNull()?.products
            }.autoDispose()
        }
        binding.CategoryRecycle.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.CategoryRecycle.apply {
            adapter = categoryAdapter
        }
    }

    private fun listenToViewModel() {
        userStoreViewModel.userStoreState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserStoreState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is UserStoreState.LoadingState -> {

                }
                is UserStoreState.GetOrderPromisedTime -> {
                    promisedTime = it.getPromisedTime.time
                }
                is UserStoreState.CreatePosOrder -> {
                    binding.userStoreViewPager.currentItem = 2
                    RxBus.publish(RxEvent.OpenOrderSuccessDialog(it.cartInfo))
                    binding.cartView.createOrderMaterialButton.isVisible = false
                    binding.cartView.rlOrderPrice.isVisible = false
                    binding.cartView.rlGiftCard.isVisible = false
                    binding.cartView.rlOrderPrice.isVisible = false
                    binding.cartView.rlTax.isVisible = false
                    binding.cartView.rlTotal.isVisible = false
                    binding.cartView.rlGiftCard.isVisible = false
                    binding.cartView.rlPromocode.isVisible = false
                    binding.cartView.rlAdjustment.isVisible = false
                    binding.cartView.adjustmentsMaterialButton.isVisible = false
                    binding.cartView.customerDetails.isVisible = true
                    binding.cartView.tvOrderHeading.isVisible = true
                    binding.cartView.viewOrder.isVisible = true
                    binding.cartView.viewOrder.isVisible = true
                    val list = cartAdapter.listOfProductDetails
                    list?.filter { item -> item.isChanging == true }?.forEach { item ->
                        item.isChanging = false
                    }
                    cartAdapter.listOfProductDetails = list
                    binding.cartView.customerNameAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.fullName
                    if (loggedInUserCache.getLoyaltyQrResponse()?.email != "") {
                        binding.cartView.customerEmailAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.email
                    }
                    if (loggedInUserCache.getLoyaltyQrResponse()?.phone != "") {
                        binding.cartView.customerPhoneNumberAppCompatTextView.text = loggedInUserCache.getLoyaltyQrResponse()?.phone
                    }
                    binding.cartView.createOrderMaterialButton.isVisible = false
                }
                is UserStoreState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is UserStoreState.UpdatedCartInfo -> {
                    if (this::cartAdapter.isInitialized) {
                        val updatedCartItemId = it.cartInfo?.id
                        val productQuantity = it.cartInfo?.menuItemQuantity
                        val cartList = cartAdapter.listOfProductDetails
                        cartList?.find { item -> updatedCartItemId == item.id }?.apply {
                            menuItemQuantity = productQuantity
                            menuItemPrice = it.cartInfo?.menuItemPrice
                        }
                        cartAdapter.listOfProductDetails = cartList
                        listOfProductDetails = cartList as ArrayList<CartItem>?
                        orderTotalCount()
                    }
                }
                is UserStoreState.DeletedCartItem -> {
                    val cartList: ArrayList<CartItem> = cartAdapter.listOfProductDetails as ArrayList<CartItem>
                    val deletedCartItem: List<Int> =
                        cartList.withIndex().filter { item -> item.value.id == deletedCartItemId }.map { item -> item.index }
                    val index = deletedCartItem[0]
                    cartList.removeAt(index)
                    cartAdapter.listOfProductDetails = cartList
                    listOfProductDetails = cartList
                    orderTotalVisibility(cartList)
                    orderTotalCount()
                    binding.tvCartName.text = "${resources.getString(R.string.cart)} (${cartList.size})"
                }
                is UserStoreState.CartDetailsInfo -> {
                    listOfProductDetails = it.cartInfo.cart as ArrayList<CartItem>?
                    setCartData(it.cartInfo.cart)
                    binding.tvCartName.text = resources.getString(R.string.cart).plus(" (${it.cartInfo.cart?.size})")
                    orderTotalCount()
                }
                is UserStoreState.MenuInfo -> {
                    CookiesFragment.listOfProduct = it.menuListInfo.menus?.firstOrNull()?.products
                    listOfProduct = it.menuListInfo.menus?.firstOrNull()?.products as ArrayList<ProductsItem>?
                    setCategoryData(it.menuListInfo)
                }
                is UserStoreState.StoreResponses -> {
                    setStoreOpenAndClose(it.storeResponse)
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun orderTotalVisibility(cartList: ArrayList<CartItem>) {
        if (cartList.size == 0) {
            binding.cartView.cartListAndPrizeLinearLayout.isVisible = false
            binding.cartView.emptyMessageAppCompatTextView.isVisible = true
        }
    }

    private fun checkOutFragment() {
        RxBus.publish(RxEvent.PaymentIsNotVisible(false))
        binding.userStoreViewPager.currentItem = 1
        binding.cartView.proceedToPaymentMaterialButton.isVisible = true
        binding.cartView.customerDetails.isVisible = false
        binding.cartView.viewOrder.isVisible = false
        binding.cartView.tvOrderHeading.isVisible = false
    }

    private fun setCategoryData(menuListInfo: MenuListInfo) {
        menuListInfo.menus?.firstOrNull()?.apply {
            isSelected = true
        }
        categoryAdapter.listOfMenu = menuListInfo.menus
    }

    private fun setCartData(cartItem: List<CartItem>?) {
        if (cartItem?.size != 0) {
            binding.cartView.cartListAndPrizeLinearLayout.isVisible = true
            binding.cartView.emptyMessageAppCompatTextView.isVisible = false
            if (binding.userStoreViewPager.currentItem != 0) {
                cartItem?.filter { it.isChanging == true }?.forEach {
                    it.isChanging = false
                }
            } else {
                cartItem?.filter { it.isChanging == false }?.forEach {
                    it.isChanging = true
                }
            }
            cartAdapter.listOfProductDetails = cartItem
            listOfProductDetails = cartItem as ArrayList<CartItem>?
            binding.cartView.rlTax.isVisible = true
            binding.cartView.rlOrderPrice.isVisible = true
            binding.cartView.rlTotal.isVisible = true
            if (binding.userStoreViewPager.currentItem == 0) binding.cartView.proceedToCheckoutMaterialButton.isVisible = true
            orderTotalCount()
            if (loggedInUserCache.isUserLoggedIn()) {
                if (binding.userStoreViewPager.currentItem != 2) {
//                    binding.cartView.adjustmentsMaterialButton.isVisible = true
                    binding.cartView.adjustmentsMaterialButton.isVisible = false
                    binding.cartView.adjustmentsMaterialButton.isEnabled = true
                }

            }
        } else {
            binding.cartView.cartListAndPrizeLinearLayout.isVisible = false
            binding.cartView.emptyMessageAppCompatTextView.isVisible = true
            cartAdapter.listOfProductDetails = null
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

    private fun onApiCalling() {
        userStoreViewModel.getMenuProductByLocation()
        if (loggedInUserCache.getLoggedInUserCartGroupId() != 0) {
            loggedInUserCache.getLoggedInUserCartGroupId()?.let { userStoreViewModel.getCartDetails(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        userStoreViewModel.loadCurrentStoreResponse()
        userStoreViewModel.getOrderPromisedTime()
        resetDisconnectTimer()
    }

    override fun onUserInteraction() {
        Timber.tag("UserStoreActivity").d("onUserInteraction")
        resetDisconnectTimer()
    }

    private val disconnectHandler = Handler {
        Timber.tag("UserStoreActivity").d("disconnectHandler")
        false
    }

    private val disconnectCallback = Runnable { // Perform any required operation on disconnect
        Timber.tag("UserStoreActivity").d("disconnectCallback")
        loggedInUserCache.clearLoggedInUserLocalPrefs()
        Toast.makeText(applicationContext, "Crew time out", Toast.LENGTH_LONG).show()
        startNewActivityWithDefaultAnimation(
            SplashActivity.getIntent(
                this@UserStoreActivity
            )
        )
        finish()
    }

    private fun resetDisconnectTimer() {
        Timber.tag("UserStoreActivity").d("resetDisconnectTimer")
        disconnectHandler.removeCallbacks(disconnectCallback)
        disconnectHandler.postDelayed(disconnectCallback, DISCONNECT_TIMEOUT)
    }

    private fun stopDisconnectTimer() {
        disconnectHandler.removeCallbacks(disconnectCallback)
    }

    override fun onPause() {
        super.onPause()
        Timber.tag("UserStoreActivity").d("stopDisconnectTimer")
        stopDisconnectTimer()
    }
}