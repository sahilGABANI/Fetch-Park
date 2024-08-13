package com.hoxbox.terminal.ui.userstore.payment

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.gson.Gson
import com.hoxbox.terminal.BuildConfig
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.order.model.OrderDetail
import com.hoxbox.terminal.api.stripe.model.EditType
import com.hoxbox.terminal.api.userstore.model.*
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseFragment
import com.hoxbox.terminal.base.RxBus
import com.hoxbox.terminal.base.RxEvent
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.base.extension.*
import com.hoxbox.terminal.databinding.FragmentPaymentBinding
import com.hoxbox.terminal.helper.*
import com.hoxbox.terminal.helper.formatTo
import com.hoxbox.terminal.helper.toDate
import com.hoxbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hoxbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import com.hoxbox.terminal.utils.Constants
import com.hoxbox.terminal.utils.Constants.EMAIL
import com.hoxbox.terminal.utils.Constants.ORDER_MODE_ID
import com.hoxbox.terminal.utils.Constants.ORDER_STATUS_RECEIVE
import com.hoxbox.terminal.utils.Constants.PHONE
import com.hoxbox.terminal.utils.UserInteractionInterceptor
import timber.log.Timber
import java.util.*
import javax.inject.Inject


class PaymentFragment : BaseFragment() {

    private var promisedTime: String? = null
    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!
    private var total: Double = 0.00
    private var orderInstructions: String? = null
    private var giftCardId: Int? = null
    private var enterGiftCardPrize: Int? = null
    private var couponCodeId: Int? = null
    private var creditAmount: Int? = null
    private var count: Int? = 0
    private var isFVisible = true
    private var isSendReceipt: SendReceiptType = SendReceiptType.Nothing
    private var orderDetails: OrderDetail? = null
    private lateinit var bohPrinterHelper: BohPrinterHelper
    private lateinit var fohPrinterHelper: FohPrinterHelper
    private var createPosOrder: CreateOrderResponse? = null
    private var adjustment: Int = 0
    private var employeeDiscount: Int = 0
    private var mhsMemberDiscount: Int = 0

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserStoreViewModel>
    private lateinit var userStoreViewModel: UserStoreViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    companion object {
        @JvmStatic
        fun newInstance() = PaymentFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        userStoreViewModel = getViewModelFromFactory(viewModelFactory)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }


    @SuppressLint("SimpleDateFormat")
    private fun listenToViewModel() {
        userStoreViewModel.userStoreState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserStoreState.ErrorMessage -> {
                    binding.pendingPayment.progressBar.isVisible = false
                    binding.pendingPayment.tryAgainMaterialButton.isVisible = true
                    showToast(it.errorMessage)
                }
                is UserStoreState.OrderDetailItemResponse -> {
                    orderDetails = it.orderDetail
                }
                is UserStoreState.GetOrderPromisedTime -> {
                    promisedTime = it.getPromisedTime.time
//                    val date = it.getPromisedTime.time?.toDate("yyyy-MM-dd HH:mm")
//                    val updatedTimeInMillis = date?.time?.plus((1 * 60 * 60 * 1000))
//                    val formatter = SimpleDateFormat("MM/dd/yyyy, HH:mm", Locale.getDefault())
//                    val promisedFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
//                    val formattedTime = formatter.format(updatedTimeInMillis)
//                    promisedTime = promisedFormatter.format(updatedTimeInMillis)
//                    binding.paymentSuccessPart.tvOrderPromisedTime.text = formattedTime
                    binding.paymentSuccessPart.tvOrderPromisedTime.text =
                        it.getPromisedTime.time?.toDate("yyyy-MM-dd HH:mm")?.formatTo("MM/dd/yyyy, HH:mm") ?: ""
                    val createOrderRequest = CreateOrderRequest(
                        orderUserId = if (loggedInUserCache.getLoyaltyQrResponse()?.id != null) loggedInUserCache.getLoyaltyQrResponse()?.id else null,
                        ORDER_MODE_ID,
                        null,
                        orderTypeId = loggedInUserCache.getorderTypeId(),
                        loggedInUserCache.getLoggedInUserCartGroupId(),
                        orderLocationId = loggedInUserCache.getLocationInfo()?.id,
                        transactionChargeId = "ch_3MD14rHBh9c4S8JH080fPpzq",
                        transactionIdOfProcessor = "ch_3MD14rHBh9c4S8JH080fPpzq",
                        transactionAmount = total.times(100).toString(),
                        deliveryAddress = if (loggedInUserCache.getorderTypeId() == Constants.DELIVERY_ORDER_TYPE_ID) loggedInUserCache.getdeliveryAddress() else null,
                        guestName = (if (loggedInUserCache.getLoyaltyQrResponse()?.fullName?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.fullName.toString() else null),
                        guestPhone = (if (loggedInUserCache.getLoyaltyQrResponse()?.phone?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.phone.toString() else "N/A"),
                        giftCardId = giftCardId,
                        orderGiftCardAmount = enterGiftCardPrize,
                        couponCodeId = couponCodeId,
                        creditAmount = creditAmount,
                        orderPromisedTime = promisedTime,
                        orderAdjustments = adjustment,
                        lat = if (loggedInUserCache.getorderTypeId() == Constants.DELIVERY_ORDER_TYPE_ID) loggedInUserCache.getdeliveryLat() else null,
                        long = if (loggedInUserCache.getorderTypeId() == Constants.DELIVERY_ORDER_TYPE_ID) loggedInUserCache.getdeliveryLong() else null,
                        orderInstructions = orderInstructions
                    )
//                    userStoreViewModel.createOrder(createOrderRequest)
                    println("createOrderRequest : ${Gson().toJson(createOrderRequest)}")
                }
                is UserStoreState.PaymentInfo -> {
                    responseChecker(it.responseData)
                }
                is UserStoreState.CreatePosOrder -> {
                    loggedInUserCache.setdeliveryAddress("")
                    loggedInUserCache.setLoggedInUserCartGroupId(0)
                    binding.pendingPayment.root.visibility = View.GONE
                    binding.paymentSuccessPart.root.visibility = View.VISIBLE
                    successUi()
                    it.cartInfo?.id?.let { it1 -> userStoreViewModel.loadOrderDetailsItem(it1) }
                }
                is UserStoreState.PaymentErrorMessage -> {
                    binding.pendingPayment.progressBar.isVisible = false
                    binding.pendingPayment.tryAgainMaterialButton.isVisible = true
                    Timber.tag("OkHttpClient").i("Response is not Approved")
                }

                is UserStoreState.NewPaymentErrorMessage -> {
                    binding.pendingPayment.progressBar.isVisible = false
                    binding.pendingPayment.tryAgainMaterialButton.isVisible = true
                    Timber.tag("OkHttpClient").i("Response is not Approved")
                }
                is UserStoreState.CaptureNewPaymentIntent -> {
                    if (it.createPaymentIntentResponse.response?.firstOrNull()?.status == "approved") {
                        userStoreViewModel.createOrder(
                            CreateOrderRequest(
                                orderUserId = if (loggedInUserCache.getLoyaltyQrResponse()?.id != null) loggedInUserCache.getLoyaltyQrResponse()?.id else null,
                                ORDER_MODE_ID,
                                it.createPaymentIntentResponse.response[0]?.tipAmount?.toInt(),
                                orderTypeId = loggedInUserCache.getorderTypeId(),
                                loggedInUserCache.getLoggedInUserCartGroupId(),
                                orderLocationId = loggedInUserCache.getLocationInfo()?.id,
                                transactionChargeId = it.createPaymentIntentResponse.response[0]?.retrievalReferenceNo,
                                transactionIdOfProcessor = it.createPaymentIntentResponse.response[0]?.hostTransactionReference,
                                transactionAmount = total.times(100).toString(),
                                deliveryAddress = if (loggedInUserCache.getorderTypeId() == Constants.DELIVERY_ORDER_TYPE_ID) loggedInUserCache.getdeliveryAddress() else null,
                                guestName = (if (loggedInUserCache.getLoyaltyQrResponse()?.fullName?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.fullName.toString() else null),
                                guestPhone = (if (loggedInUserCache.getLoyaltyQrResponse()?.phone?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.phone.toString() else "N/A"),
                                giftCardId = giftCardId,
                                orderGiftCardAmount = enterGiftCardPrize,
                                couponCodeId = couponCodeId,
                                creditAmount = creditAmount,
                                orderPromisedTime = promisedTime,
                                orderAdjustments = adjustment,
                                lat = if (loggedInUserCache.getorderTypeId() == Constants.DELIVERY_ORDER_TYPE_ID) loggedInUserCache.getdeliveryLat() else null,
                                long = if (loggedInUserCache.getorderTypeId() == Constants.DELIVERY_ORDER_TYPE_ID) loggedInUserCache.getdeliveryLong() else null,
                                orderInstructions = orderInstructions
                            )
                        )
                        loggedInUserCache.setLoggedInUserCartGroupId(0)
                        binding.pendingPayment.root.visibility = View.GONE
                        binding.paymentSuccessPart.root.visibility = View.VISIBLE
                        successUi()
                    } else {
                        binding.pendingPayment.dialogHeading.text = resources.getString(R.string.payment_error)
                        binding.pendingPayment.tvDescription.text = it.createPaymentIntentResponse.response?.firstOrNull()?.status.toString()
                        binding.pendingPayment.tryAgainMaterialButton.isVisible = true
                    }

                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun listenToViewEvent() {
        RxBus.listen(RxEvent.EventTotalPayment::class.java).subscribeAndObserveOnMainThread {
            total = it.orderPrice.orderTotal!!
            println("adjustment : ${it.orderPrice.adjustmentDiscount?.toInt()}")
            adjustment = it.orderPrice.adjustmentDiscount?.toInt() ?: 0
            employeeDiscount = it.orderPrice.employeeDiscount?.toInt() ?: 0
            mhsMemberDiscount = it.orderPrice.mhsMemberDiscount?.toInt() ?: 0
            println("adjustment : $adjustment")
            binding.pendingPayment.tvTotalPrizeNumber.text = total.toDollar()
            if (count == 0) {
                val newPaymentRequest = CaptureNewPaymentRequest(resource = Resource(total.times(100).toInt()))
                userStoreViewModel.captureNewPayment(newPaymentRequest)
                count = 1
            }
        }.autoDispose()
        RxBus.listen(RxEvent.PaymentIsNotVisible::class.java).subscribeAndObserveOnMainThread {
            isFVisible = it.boolean
        }.autoDispose()
        if (!isVisible) {
            count = 0
        }
        RxBus.listen(RxEvent.PassPromocodeAndGiftCard::class.java).subscribeAndObserveOnMainThread {
            if (it.giftCardId != 0) {
                giftCardId = it.giftCardId
            }
            if (it.giftCardAmount != 0) {
                enterGiftCardPrize = it.giftCardAmount
            }
            if (it.couponCodeId != 0) {
                couponCodeId = it.couponCodeId
            }
            orderInstructions = it.orderInstructions
        }.autoDispose()
        RxBus.listen(RxEvent.PassCreditAmount::class.java).subscribeAndObserveOnMainThread {
            if (it.creditAmount != 0) {
                creditAmount = it.creditAmount
            }
        }.autoDispose()
        RxBus.listen(RxEvent.OpenOrderSuccessDialog::class.java).subscribeAndObserveOnMainThread {
            println("Open Order Success Dialog")
            binding.paymentSuccessPart.tvOrderPromisedTime.text =
                it.orderId?.orderPromisedTime?.toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("MM/dd/yyyy, HH:mm") ?: ""
            binding.pendingPayment.root.visibility = View.GONE
            binding.paymentSuccessPart.root.visibility = View.VISIBLE
            successUi()
            it.orderId?.id?.let { it1 -> userStoreViewModel.loadOrderDetailsItem(it1) }
//            binding.paymentSuccessPart.orderIdTextView.text = "Order #${it.orderId.id}"

        }.autoDispose()
        binding.pendingPayment.tryAgainMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            binding.pendingPayment.ivPaymentError.setImageResource(R.drawable.ic_scan_loyalty_card)
            binding.pendingPayment.dialogHeading.text = resources.getString(R.string.pending_payment)
            binding.pendingPayment.tvDescription.text = resources.getString(R.string.complete_your_payment_on_the_payment_terminal)
            binding.pendingPayment.tryAgainMaterialButton.isVisible = false
            val newPaymentRequest = CaptureNewPaymentRequest(
                resource = Resource(total.times(100).toInt())
            )
            userStoreViewModel.captureNewPayment(newPaymentRequest)
        }.autoDispose()
        binding.paymentSuccessPart.downArrowImageView.isSelected = true
        binding.paymentSuccessPart.downArrowBackgroundMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.paymentSuccessPart.downArrowImageView.isSelected) {
                binding.paymentSuccessPart.downArrowImageView.isSelected = false
                when (isSendReceipt) {
                    SendReceiptType.Email -> {
                        binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = false
                        binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = false
                        binding.paymentSuccessPart.llButtons.isVisible = false
                        binding.paymentSuccessPart.printBOHReceiptButton.isVisible = false
                        binding.paymentSuccessPart.horizontalView.isVisible = false
                    }
                    SendReceiptType.Phone -> {
                        binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = false
                        binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = false
                        binding.paymentSuccessPart.llButtons.isVisible = false
                        binding.paymentSuccessPart.printBOHReceiptButton.isVisible = false
                        binding.paymentSuccessPart.horizontalView.isVisible = false
                    }
                    SendReceiptType.EmailAndPhone -> {
                        binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = false
                        binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = false
                        binding.paymentSuccessPart.llButtons.isVisible = false
                        binding.paymentSuccessPart.printBOHReceiptButton.isVisible = false
                        binding.paymentSuccessPart.horizontalView.isVisible = false
                    }
                    SendReceiptType.Nothing -> {
                        binding.paymentSuccessPart.userDetailsRelative.isVisible = false
                        binding.paymentSuccessPart.llButtons.isVisible = false
                        binding.paymentSuccessPart.printBOHReceiptButton.isVisible = false
                        binding.paymentSuccessPart.horizontalView.isVisible = false
                    }
                }
            } else {
                when (isSendReceipt) {
                    SendReceiptType.Email -> {
                        binding.paymentSuccessPart.llButtons.isVisible = true
                        binding.paymentSuccessPart.printBOHReceiptButton.isVisible = true
                        binding.paymentSuccessPart.horizontalView.isVisible = true
                        binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = true
                        binding.paymentSuccessPart.userDetailsRelative.isVisible = true
                        binding.paymentSuccessPart.emailCheckBox.isVisible = false
                        binding.paymentSuccessPart.tvEmail.isVisible = false
                        if (!binding.paymentSuccessPart.emailCheckBox.isVisible && !binding.paymentSuccessPart.phoneCheckBox.isVisible) {
                            binding.paymentSuccessPart.sendReceiptButton.isVisible = false
                            binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = true
                            binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = true
                        }
                    }
                    SendReceiptType.Phone -> {
                        binding.paymentSuccessPart.llButtons.isVisible = true
                        binding.paymentSuccessPart.printBOHReceiptButton.isVisible = true
                        binding.paymentSuccessPart.horizontalView.isVisible = true
                        binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = true
                        binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = false
                        binding.paymentSuccessPart.userDetailsRelative.isVisible = true
                        binding.paymentSuccessPart.phoneCheckBox.isVisible = false
                        binding.paymentSuccessPart.tvPhone.isVisible = false
                        if (!binding.paymentSuccessPart.emailCheckBox.isVisible && !binding.paymentSuccessPart.phoneCheckBox.isVisible) {
                            binding.paymentSuccessPart.sendReceiptButton.isVisible = false
                            binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = true
                            binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = true
                        }
                    }
                    SendReceiptType.EmailAndPhone -> {
                        binding.paymentSuccessPart.llButtons.isVisible = true
                        binding.paymentSuccessPart.printBOHReceiptButton.isVisible = true
                        binding.paymentSuccessPart.horizontalView.isVisible = true
                        binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = true
                        binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = true
                    }
                    SendReceiptType.Nothing -> {
                        binding.paymentSuccessPart.userDetailsRelative.isVisible = true
                        binding.paymentSuccessPart.horizontalView.isVisible = true
                        binding.paymentSuccessPart.llButtons.isVisible = true
                        binding.paymentSuccessPart.printBOHReceiptButton.isVisible = true
                    }
                }
                binding.paymentSuccessPart.downArrowImageView.isSelected = true
            }
        }.autoDispose()
        binding.paymentSuccessPart.printBOHReceiptButton.throttleClicks().subscribeAndObserveOnMainThread {
            val t: Thread = object : Thread() {
                override fun run() {
                    val bohPrinterAddress = loggedInUserCache.getLocationInfo()?.bohPrintAddress
                    if (bohPrinterAddress != null) {
                        orderDetails?.let {
                            bohPrint(it, bohPrinterAddress)
                        }
                    }
                }
            }
            t.start()
        }.autoDispose()
        binding.paymentSuccessPart.printReceiptButton.throttleClicks().subscribeAndObserveOnMainThread {
            val t: Thread = object : Thread() {
                override fun run() {
                    val currentTime = getCurrentsStoreTime().formatToStoreTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    val bohPrinterAddress = loggedInUserCache.getLocationInfo()?.printAddress
                    if (bohPrinterAddress != null && loggedInUserCache.getLocationInfo()?.bothPrinterNotSame() == true) {
                        orderDetails?.let {
                            fohPrint(it, bohPrinterAddress, currentTime)
                        }
                    } else {
                        Timber.tag("OkHttpClient").i("Both Printer Address Same")
                        showToast("Both Printer Address Same")
                    }
                }
            }
            t.start()
        }.autoDispose()
        binding.paymentSuccessPart.sendReceiptButton.throttleClicks().subscribeAndObserveOnMainThread {
            when {
                binding.paymentSuccessPart.emailCheckBox.isChecked && !binding.paymentSuccessPart.phoneCheckBox.isChecked -> {
                    binding.paymentSuccessPart.emailCheckBox.isChecked = false
                    isSendReceipt = SendReceiptType.Email
                    orderDetails?.id?.let { it1 ->
                        userStoreViewModel.sendReceipt(
                            orderId = it1, type = EMAIL, email = binding.paymentSuccessPart.tvEmail.text.toString(), phone = null
                        )
                    }
                    binding.paymentSuccessPart.llButtons.isVisible = true
                    binding.paymentSuccessPart.sendReceiptButton.isVisible = false
                    binding.paymentSuccessPart.printBOHReceiptButton.isVisible = true
                    binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = true
                    binding.paymentSuccessPart.userDetailsRelative.isVisible = true
                    binding.paymentSuccessPart.emailCheckBox.isVisible = false
                    binding.paymentSuccessPart.tvEmail.isVisible = false
                    binding.paymentSuccessPart.editEmail.isVisible = false
//                    binding.paymentSuccessPart.printReceiptButton.visibility = View.VISIBLE
                    binding.paymentSuccessPart.printBOHReceiptButton.visibility = View.VISIBLE
                    if (!binding.paymentSuccessPart.emailCheckBox.isVisible && !binding.paymentSuccessPart.phoneCheckBox.isVisible) {
                        binding.paymentSuccessPart.sendReceiptButton.isVisible = false
                        binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = true
                        binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = true
                    }
                }
                binding.paymentSuccessPart.phoneCheckBox.isChecked && !binding.paymentSuccessPart.emailCheckBox.isChecked -> {
                    binding.paymentSuccessPart.phoneCheckBox.isChecked = false
                    binding.paymentSuccessPart.sendReceiptButton.isVisible = false
                    isSendReceipt = SendReceiptType.Phone
                    binding.paymentSuccessPart.llButtons.isVisible = true
                    binding.paymentSuccessPart.printBOHReceiptButton.isVisible = true
                    binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = true
                    binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = false
                    binding.paymentSuccessPart.userDetailsRelative.isVisible = true
                    binding.paymentSuccessPart.phoneCheckBox.isVisible = false
                    binding.paymentSuccessPart.editPhone.isVisible = false
//                    binding.paymentSuccessPart.printReceiptButton.visibility = View.VISIBLE
//                    createPosOrder?.id?.let { it1 ->
//                        userStoreViewModel.sendReceipt(
//                            orderId = it1, type = PHONE, email = null, phone = binding.paymentSuccessPart.tvPhone.text.toString()
//                        )
//                    }
                    binding.paymentSuccessPart.printBOHReceiptButton.visibility = View.VISIBLE
                    binding.paymentSuccessPart.tvPhone.isVisible = false
                    if (!binding.paymentSuccessPart.emailCheckBox.isVisible && !binding.paymentSuccessPart.phoneCheckBox.isVisible) {
                        binding.paymentSuccessPart.sendReceiptButton.isVisible = false
                        binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = true
                        binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = true
                    }
                }
                binding.paymentSuccessPart.emailCheckBox.isChecked && binding.paymentSuccessPart.phoneCheckBox.isChecked -> {
                    isSendReceipt = SendReceiptType.EmailAndPhone
                    binding.paymentSuccessPart.sendReceiptButton.isVisible = false
                    binding.paymentSuccessPart.userDetailsRelative.isVisible = false
                    binding.paymentSuccessPart.editPhone.isVisible = false
                    binding.paymentSuccessPart.editEmail.isVisible = false
                    binding.paymentSuccessPart.tvSendReceiptEmail.visibility = View.VISIBLE
                    binding.paymentSuccessPart.tvSendReceiptPhone.visibility = View.VISIBLE
//                    binding.paymentSuccessPart.printReceiptButton.visibility = View.VISIBLE
                    binding.paymentSuccessPart.printBOHReceiptButton.visibility = View.VISIBLE
                    createPosOrder?.id?.let { it1 ->
                        userStoreViewModel.sendReceipt(
                            orderId = it1, type = EMAIL, email = binding.paymentSuccessPart.tvEmail.text.toString(), phone = null
                        )
                    }
//                    createPosOrder?.id?.let { it1 ->
//                        userStoreViewModel.sendReceipt(
//                            orderId = it1, type = PHONE, email = null, phone = binding.paymentSuccessPart.tvPhone.text.toString()
//                        )
//                    }
                }

                else -> {
                    showToast("Please Select Send Receipt type")
                }
            }
        }.autoDispose()
        binding.paymentSuccessPart.editEmail.throttleClicks().subscribeAndObserveOnMainThread {
            val editEmailOrPhoneFragment = EditEmailOrPhoneFragment.newInstance(EditType.Email, binding.paymentSuccessPart.tvEmail.text.toString())
            editEmailOrPhoneFragment.editSuccess.subscribeAndObserveOnMainThread {
                binding.paymentSuccessPart.tvEmail.text = it
                binding.paymentSuccessPart.tvSendReceiptEmail.text = it
            }.autoDispose()
            editEmailOrPhoneFragment.show(parentFragmentManager, PaymentFragment::class.java.name)
        }.autoDispose()

        binding.paymentSuccessPart.editPhone.throttleClicks().subscribeAndObserveOnMainThread {
            val editEmailOrPhoneFragment = EditEmailOrPhoneFragment.newInstance(EditType.Phone, binding.paymentSuccessPart.tvPhone.text.toString())
            editEmailOrPhoneFragment.editSuccess.subscribeAndObserveOnMainThread {
                binding.paymentSuccessPart.tvPhone.text = it
                binding.paymentSuccessPart.tvSendReceiptPhone.text = it
            }.autoDispose()
            editEmailOrPhoneFragment.show(parentFragmentManager, PaymentFragment::class.java.name)
        }.autoDispose()

        binding.paymentSuccessPart.goToStartButton.throttleClicks().subscribeAndObserveOnMainThread {
            val userId = loggedInUserCache.getLoggedInUserId()
            orderDetails?.id?.let { it1 ->
                if (userId != null) {
                    userStoreViewModel.updateOrderStatusDetails(ORDER_STATUS_RECEIVE, it1, userId)
                }
            }
            val t: Thread = object : Thread() {
                override fun run() {
                    val bohPrinterAddress = loggedInUserCache.getLocationInfo()?.bohPrintAddress
                    if (bohPrinterAddress != null) {
                        orderDetails?.let {
                            bohPrint(it, bohPrinterAddress)
                        }
                    }
                }
            }
            t.start()
            count = 0
            RxBus.publish(RxEvent.EventGotoStartButton)
        }.autoDispose()
    }

    fun bohPrint(orderDetail: OrderDetail, bohPrinterAddress: String) {
        bohPrinterHelper = BohPrinterHelper.getInstance(requireActivity())
        if (!bohPrinterHelper.isPrinterConnected() && bohPrinterAddress != null) {
            try {
                val isConnected = bohPrinterHelper.printerConnect(bohPrinterAddress)
                Timber.tag("AutoReceive").e("Printer connection response ========= $isConnected")
            } catch (e: java.lang.Exception) {
                Timber.tag("AutoReceive").e(e)
            }
        }
        if (BuildConfig.DEBUG) {
            try {
                bohPrinterHelper.runPrintBOHReceiptSequence(orderDetail, bohPrinterAddress)
            } catch (e: java.lang.Exception) {
                Timber.tag("runPrintBOHReceiptSequence").e(e)
            }
        }
        if (bohPrinterAddress != null && bohPrinterHelper.isPrinterConnected()) {
            try {
                bohPrinterHelper.runPrintBOHReceiptSequence(orderDetail, bohPrinterAddress)
            } catch (e: java.lang.Exception) {
                Timber.tag("runPrintBOHReceiptSequence").e(e)
            }
        } else {
            Timber.tag("AutoReceive").e("----------------- Printer not connected -----------------")
        }
    }

    fun fohPrint(orderDetail: OrderDetail, bohPrinterAddress: String, currentTime: String) {
        fohPrinterHelper = FohPrinterHelper.getInstance(requireActivity())
        if (!fohPrinterHelper.isPrinterConnected() && bohPrinterAddress != null) {
            try {
                val isConnected = fohPrinterHelper.printerConnect(bohPrinterAddress)
                Timber.tag("AutoReceive").e("Printer connection response ========= $isConnected")
            } catch (e: java.lang.Exception) {
                Timber.tag("AutoReceive").e(e)
            }
        }
        if (BuildConfig.DEBUG) {
            try {
                fohPrinterHelper.runPrintReceiptSequence(orderDetail, bohPrinterAddress, currentTime)
            } catch (e: java.lang.Exception) {
                Timber.tag("runPrintBOHReceiptSequence").e(e)
            }
        }
        if (bohPrinterAddress != null && fohPrinterHelper.isPrinterConnected()) {
            try {
                fohPrinterHelper.runPrintReceiptSequence(orderDetail, bohPrinterAddress, currentTime)
            } catch (e: java.lang.Exception) {
                Timber.tag("runPrintBOHReceiptSequence").e(e)
            }
        } else {
            Timber.tag("AutoReceive").e("----------------- Printer not connected -----------------")
        }
    }

    fun responseChecker(response: String) {
        val keyValuePairs: List<String> = response.split("&")

        var responseText: String? = null
        var tip: String? = null
        var transactionId: String? = null
        for (keyValuePair in keyValuePairs) {
            val parts = keyValuePair.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.size == 2) {
                val key = parts[0]
                val value = parts[1]
                if (key == "responsetext") {
                    responseText = value
                }

                if (key == "tip") {
                    tip = value
                }

                if (key == "transactionid") {
                    transactionId = value
                }
            }
        }
        val totalAmount = tip?.toDouble()?.times(100)?.toInt()?.let { total.times(100).toInt().plus(it) }
        if (responseText != null && responseText == "Approved") {
            userStoreViewModel.createOrder(
                CreateOrderRequest(
                    orderUserId = if (loggedInUserCache.getLoyaltyQrResponse()?.id != null) loggedInUserCache.getLoyaltyQrResponse()?.id else null,
                    ORDER_MODE_ID,
                    tip?.toDouble()?.times(100)?.toInt(),
                    orderTypeId = loggedInUserCache.getorderTypeId(),
                    loggedInUserCache.getLoggedInUserCartGroupId(),
                    orderLocationId = loggedInUserCache.getLocationInfo()?.id,
                    transactionChargeId = transactionId,
                    deliveryAddress = null,
                    guestName = (if (loggedInUserCache.getLoyaltyQrResponse()?.fullName?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.fullName.toString() else null),
                    guestPhone = (if (loggedInUserCache.getLoyaltyQrResponse()?.phone?.isNotEmpty() == true) loggedInUserCache.getLoyaltyQrResponse()?.phone.toString() else "N/A"),
                    giftCardId = giftCardId,
                    orderGiftCardAmount = enterGiftCardPrize,
                    couponCodeId = couponCodeId,
                    creditAmount = creditAmount,
                    orderPromisedTime = promisedTime,
                    lat = if (loggedInUserCache.getorderTypeId() == Constants.DELIVERY_ORDER_TYPE_ID) loggedInUserCache.getdeliveryLat() else null,
                    long = if (loggedInUserCache.getorderTypeId() == Constants.DELIVERY_ORDER_TYPE_ID) loggedInUserCache.getdeliveryLong() else null
                )
            )
            loggedInUserCache.setLoggedInUserCartGroupId(0)
            binding.pendingPayment.root.visibility = View.GONE
            binding.paymentSuccessPart.root.visibility = View.VISIBLE
            successUi()
        } else {
            binding.pendingPayment.progressBar.isVisible = false
            binding.pendingPayment.tryAgainMaterialButton.isVisible = true
            println("Response is not Approved")
        }
    }

    private fun successUi() {
        if (loggedInUserCache.getLoyaltyQrResponse()?.email?.isNotEmpty() == true || loggedInUserCache.getLoyaltyQrResponse()?.phone?.isNotEmpty() == true) {
            binding.paymentSuccessPart.rlSendReceipt.isVisible = true
            if (loggedInUserCache.getLoyaltyQrResponse()?.email?.isNotEmpty() == true) {
                binding.paymentSuccessPart.tvEmail.isVisible = true
                binding.paymentSuccessPart.emailCheckBox.isVisible = true
                binding.paymentSuccessPart.tvSendReceiptEmail.isVisible = false
                binding.paymentSuccessPart.tvSendReceiptEmail.text = loggedInUserCache.getLoyaltyQrResponse()?.email ?: "-"
                binding.paymentSuccessPart.tvEmail.text = loggedInUserCache.getLoyaltyQrResponse()?.email ?: "-"
            }
            if (loggedInUserCache.getLoyaltyQrResponse()?.phone?.isNotEmpty() == true) {
                binding.paymentSuccessPart.tvPhone.isVisible = true
                binding.paymentSuccessPart.phoneCheckBox.isVisible = true
                binding.paymentSuccessPart.tvSendReceiptPhone.isVisible = false
                binding.paymentSuccessPart.tvSendReceiptPhone.text = loggedInUserCache.getLoyaltyQrResponse()?.phone ?: "-"
                binding.paymentSuccessPart.tvPhone.text = loggedInUserCache.getLoyaltyQrResponse()?.phone ?: "-"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window, activity)
        isFVisible = true
        count = 0
        userStoreViewModel.getOrderPromisedTime()
    }

    override fun onPause() {
        super.onPause()
    }

}