package com.hoxbox.terminal.ui.main.orderdetail

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.hoxbox.terminal.BuildConfig
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.order.model.*
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseFragment
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.base.extension.*
import com.hoxbox.terminal.databinding.FragmentOrderDetailsBinding
import com.hoxbox.terminal.helper.BohPrinterHelper
import com.hoxbox.terminal.helper.formatTo
import com.hoxbox.terminal.helper.toDate
import com.hoxbox.terminal.ui.main.orderdetail.view.OrderDetailsAdapter
import com.hoxbox.terminal.ui.main.orderdetail.view.StatusLogAdapter
import com.hoxbox.terminal.ui.main.orderdetail.viewmodel.OrderDetailsViewModel
import com.hoxbox.terminal.ui.main.orderdetail.viewmodel.OrderDetailsViewState
import com.hoxbox.terminal.utils.UserInteractionInterceptor
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.properties.Delegates

class OrderDetailsFragment : BaseFragment() {

    companion object {
        private const val ORDER_ID = "orderId"
        private const val ORDER_USER_ID = "orderUserId"
        private const val ORDER_CART_GROUP_ID = "orderCartGroupId"

        @JvmStatic
        fun newInstance(orderId: Int?, orderUserId: Int?, orderCartGroupId: Int?): OrderDetailsFragment {
            val args = Bundle()
            orderId?.let { args.putInt(ORDER_ID, it) }
            orderUserId?.let { args.putInt(ORDER_USER_ID, it) }
            orderCartGroupId?.let { args.putInt(ORDER_CART_GROUP_ID, it) }
            val fragment = OrderDetailsFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<OrderDetailsViewModel>

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var orderDetailsViewModel: OrderDetailsViewModel
    private var orderId by Delegates.notNull<Int>()
    private var orderUserId by Delegates.notNull<Int>()
    private var orderCartGroupId by Delegates.notNull<Int>()
    private lateinit var statusLogAdapter: StatusLogAdapter
    private lateinit var orderDetailsAdapter: OrderDetailsAdapter
    private lateinit var bohPrinterHelper: BohPrinterHelper
    private var _binding: FragmentOrderDetailsBinding? = null
    private val binding get() = _binding!!
    private var orderStatus: String = ""
    private var orderDetails: OrderDetail? = null
    private var bohPrintAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderId = arguments?.getInt(ORDER_ID, 0) ?: throw IllegalStateException("No args provided")
        orderUserId = arguments?.getInt(ORDER_USER_ID, 0) ?: throw IllegalStateException("No args provided")
        orderCartGroupId = arguments?.getInt(ORDER_CART_GROUP_ID, 0) ?: throw IllegalStateException("No args provided")
        HotBoxApplication.component.inject(this)
        orderDetailsViewModel = getViewModelFromFactory(viewModelFactory)
        allApiCalling()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewEvent() {

        bohPrintAddress = loggedInUserCache.getLocationInfo()?.bohPrintAddress
        initAdapter()
        binding.orderDetailsHeaderLayout.backImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()
        binding.orderDetailsHeaderLayout.completedOrderButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (orderStatus.isNotEmpty()) {
                println("orderStatus :${orderStatus.lowercase()}")
                if (orderStatus.lowercase() == "received") {
                    val userId = loggedInUserCache.getLoggedInUserId()
                    if (userId != null) {
                        orderDetailsViewModel.updateOrderStatusDetails(orderStatus.lowercase(), orderId,userId)
                    }
                    binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = false
                    val t: Thread = object : Thread() {
                        override fun run() {
                            orderDetails?.let { bohPrint(it, bohPrintAddress) }
                        }
                    }
                    t.start()
                } else {
                    val userId = loggedInUserCache.getLoggedInUserId()
                    if (userId != null) {
                        orderDetailsViewModel.updateOrderStatusDetails(orderStatus.lowercase(), orderId,userId)
                    }
                    binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = false
                }
            }
        }.autoDispose()

        binding.customerDetails.btnPrintReceipt.throttleClicks().subscribeAndObserveOnMainThread {
            val t: Thread = object : Thread() {
                override fun run() {
                    orderDetails?.let { bohPrint(it, bohPrintAddress) }
                }
            }
            t.start()
        }.autoDispose()


        binding.orderDetailsHeaderLayout.refundButton.throttleClicks().subscribeAndObserveOnMainThread {
//            if (orderDetails?.transactionIdOfProcessor != "0") {
            val printReceiptDialog = RefundFragmentDialog.newInstance(orderDetails).apply {
                refundDialogState.subscribeAndObserveOnMainThread {
                    when (it) {
                        is RefundDialogStates.DismissedRefundDialog -> {
                            this.dismiss()
                            orderDetailsViewModel.loadOrderDetailsItem(orderId)
                        }
                        is RefundDialogStates.GetRefund -> {
                            this.dismiss()
                            orderDetailsViewModel.loadOrderDetailsItem(orderId)
                            val userId = loggedInUserCache.getLoggedInUserId()
                            if (userId != null) {
                                orderDetailsViewModel.updateOrderStatusDetails(resources.getString(R.string.cancelled_refunded).toLowerCase(), orderId,userId)
                            }
                            binding.orderDetailsHeaderLayout.refundButton.isVisible = false
                        }
                    }
                }.autoDispose()
            }
            printReceiptDialog.show(parentFragmentManager, PrintReceiptDialog::class.java.name)
//            } else {
//                val t: Thread = object : Thread() {
//                    override fun run() {
//                        orderDetailsViewModel.updateOrderStatusDetails(resources.getString(R.string.refunded).toLowerCase(), orderId)
////                        openCashDrawer()
//                    }
//                }
//                t.start()
//            }

        }.autoDispose()
    }

    fun bohPrint(orderDetail: OrderDetail, bohPrinterAddress: String?) {
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

    private fun listenToViewModel() {
        orderDetailsViewModel.orderDetailsState.subscribeAndObserveOnMainThread {
            when (it) {
                is OrderDetailsViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is OrderDetailsViewState.LoadingState -> {
                    binding.progressBar.isVisible = it.isLoading
                }
                is OrderDetailsViewState.SuccessMessage -> {
                    it.successMessage?.let { it1 -> showToast(it1) }
                }
                is OrderDetailsViewState.OrderDetailItemResponse -> {
                    orderDetails = it.orderDetails
                    setOrderDetailsData(it.orderDetails.items)
                    initUI(it.orderDetails)
                }
                is OrderDetailsViewState.CustomerDetails -> {
                    binding.customerDetails.customerNameAppCompatTextView.text = it.customerDetails.fullName()
                    binding.customerDetails.customerPhoneNumberAppCompatTextView.text = it.customerDetails.userPhone
                    binding.customerDetails.customerEmailAppCompatTextView.text = it.customerDetails.userEmail
                }
                is OrderDetailsViewState.UpdateStatusResponse -> {
                    orderDetailsViewModel.loadOrderDetailsItem(orderId)
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    @SuppressLint("SetTextI18n")
    private fun initUI(orderDetails: OrderDetail) {
        orderDetails.orderInstructions?.let {
            binding.orderListLayout.specialTextLinear.isVisible = true
            binding.orderListLayout.specialInstructionsTextView.text = it.toString()
        }
        if (orderDetails.orderStatusHistory?.lastOrNull()?.firstName != null && orderDetails.orderStatusHistory.lastOrNull()?.lastName != null) {
            setStatusLogInfo(orderDetails.orderStatusHistory)
        } else {
            val list = orderDetails.orderStatusHistory
            if (orderDetails.guestName != null) {
                list?.lastOrNull()?.firstName = orderDetails.guestName
            } else {
                list?.lastOrNull()?.firstName = orderDetails.fullName()
            }
            setStatusLogInfo(orderDetails.orderStatusHistory)
        }
        if (orderDetails.orderTypeId == 20) {
            binding.orderDetailsHeaderLayout.orderStatusTextview.text = "Delivery " + orderDetails.orderType
        } else {
            binding.orderDetailsHeaderLayout.orderStatusTextview.text = orderDetails.orderType
        }
        if (orderDetails.guestName != null) {
            binding.customerDetails.customerNameAppCompatTextView.text = orderDetails.guestName.ifEmpty { "-" }
        } else {
            binding.customerDetails.customerNameAppCompatTextView.text = orderDetails.fullName().ifEmpty { "-" }
        }
        if (orderDetails.customerPhone != null && orderDetails.customerPhone != "N/A") {
            binding.customerDetails.customerPhoneNumberAppCompatTextView.text = orderDetails.customerPhone
        }
        binding.customerDetails.customerEmailAppCompatTextView.text = orderDetails.customerEmail?.ifEmpty { "-" }
//        orderDetails.orderTotal = orderDetails.orderTotal?.minus((orderDetails.orderEmpDiscount ?: 0.00))
//        orderDetails.orderTotal = orderDetails.orderTotal?.minus((orderDetails.orderRefundAmount ?: 0.00))
//        orderDetails.orderTotal = orderDetails.orderTotal?.minus((orderDetails.orderDiscount ?: 0.00))
        orderDetails.orderTotal = orderDetails.orderTotal?.plus((orderDetails.orderAdjustmentAmount?.toDouble() ?: 0.00))
        val total = orderDetails.orderTotal
        if (total != null) {
            orderDetails.orderTotal = if (total < 0) 0.00 else total
        }
//        binding.orderDetailsHeaderLayout.refundButton.isVisible = orderDetails?.orderStatusHistory?.first()?.orderStatus != resources.getString(R.string.new_text)
//            .lowercase(Locale.getDefault()) && loggedInUserCache.isAdmin() && orderDetails?.orderTotal != 0.00 && orderDetails?.orderStatusHistory?.first()?.orderStatus != resources.getString(
//            R.string.refunded
//        ).lowercase(Locale.getDefault())

        orderDetails.orderTotal?.let {
            binding.orderDetailsHeaderLayout.orderPrizeTextView.text = ((it).div(100)).toDollar()
            binding.orderListLayout.orderPrizePart.tvTotalPrizeNumber.text = ((it).div(100)).toDollar()
        }
        orderDetails.orderSubtotal?.let {
            binding.orderListLayout.orderPrizePart.tvOrderPrizeNumber.text = ((it).div(100)).toDollar()

        }
        orderDetails.orderTax?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.orderPrizePart.orderTaxRelativeLayout.isVisible = true
                binding.orderListLayout.orderPrizePart.tvTaxPrize.text = ((it).div(100)).toDollar()
            }
        }
        orderDetails.orderTip?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.orderPrizePart.orderTipRelativeLayout.isVisible = true
                binding.orderListLayout.orderPrizePart.tvTipsPrize.text = ((it).div(100)).toDollar()
            }
        }
        orderDetails.orderDeliveryFee?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.orderPrizePart.orderDeliveryRelativeLayout.isVisible = true
                binding.orderListLayout.orderPrizePart.tvDeliveryCharge.text = ((it).div(100)).toDollar()
            }
        }
        orderDetails.orderCouponCodeDiscount?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.orderPrizePart.orderPromocodeRelativeLayout.isVisible = true
                binding.orderListLayout.orderPrizePart.tvPromocodeDiscountPrize.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        orderDetails.orderGiftCardAmount?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.orderPrizePart.orderCardAndBowRelativeLayout.isVisible = true
                binding.orderListLayout.orderPrizePart.tvCardAndBowCharge.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        orderDetails.creditAmount?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.orderPrizePart.rlCredit.isVisible = true
                binding.orderListLayout.orderPrizePart.tvCreditAmount.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        if (orderDetails.orderAdjustmentAmount != null && orderDetails.orderAdjustmentAmount != 0.0) {
            if (orderDetails.orderAdjustmentAmount > 0) {
                binding.orderListLayout.orderPrizePart.rlAdjustment.isVisible = true
                binding.orderListLayout.orderPrizePart.tvAdjustmentCharge.text = "${((orderDetails.orderAdjustmentAmount).div(100)).toDollar()}"
            } else {
                binding.orderListLayout.orderPrizePart.rlAdjustment.isVisible = true
                binding.orderListLayout.orderPrizePart.tvAdjustmentCharge.text = "-${(abs(orderDetails.orderAdjustmentAmount).div(100)).toDollar()}"
            }
        }
        binding.orderDetailsHeaderLayout.orderIdTextView.text = orderDetails.getSafeOrderId()
//        val date =  orderDetails.orderCreationDate?.toDate("yyyy-MM-dd hh:mm:ss a")
//        val updatedTimeInMillis = date?.time?.plus((1 * 60 * 60 * 1000))
//        val formatter = SimpleDateFormat("MM/dd/yyyy, hh:mm a", Locale.getDefault())
//        val formattedTime  = formatter.format(updatedTimeInMillis)
//        binding.orderDetailsHeaderLayout.orderDateAndTimeTextView.text = formattedTime
        binding.orderDetailsHeaderLayout.orderDateAndTimeTextView.text =
            orderDetails.orderCreationDate?.toDate("yyyy-MM-dd hh:mm:ss a")?.formatTo("MM/dd/yyyy, hh:mm a")
        orderDetails.orderPromisedTime?.let {
            binding.orderListLayout.timeTextView.text = it.toDate()?.formatTo("MM/dd/yyyy, hh:mm a")
        }
        binding.customerDetails.btnSendReceipt.throttleClicks().subscribeAndObserveOnMainThread {
            val sendReceiptDialogFragment = SendReceiptDialogFragment.newInstance(
                binding.customerDetails.customerEmailAppCompatTextView.text.toString(),
                binding.customerDetails.customerPhoneNumberAppCompatTextView.text.toString()
            ).apply {
                refundDialogState.subscribeAndObserveOnMainThread {
                    when (it) {
                        is SendReceiptStates.SendReceiptOnEmail -> {
                            dismiss()
                            orderDetails.id?.let { it1 -> orderDetailsViewModel.sendReceipt(it1,"Email", it.email,null) }
                            binding.customerDetails.customerEmailAppCompatTextView.text = it.email
                        }
                        is SendReceiptStates.SendReceiptOnPhone -> {
                            dismiss()
                            orderDetails.id?.let { it1 -> orderDetailsViewModel.sendReceipt(it1,"Phone", null,it.phone) }
                            binding.customerDetails.customerPhoneNumberAppCompatTextView.text = it.phone
                        }
                        is SendReceiptStates.SendReceiptOnPhoneAndEmail -> {
                            dismiss()
                            orderDetails.id?.let { it1 -> orderDetailsViewModel.sendReceipt(it1,"Email", it.email,null) }
                            orderDetails.id?.let { it1 -> orderDetailsViewModel.sendReceipt(it1,"Phone", null,it.phone) }
                            binding.customerDetails.customerPhoneNumberAppCompatTextView.text = it.phone
                            binding.customerDetails.customerEmailAppCompatTextView.text = it.email
                        }
                    }
                }.autoDispose()
            }
            sendReceiptDialogFragment.show(parentFragmentManager, SendReceiptDialogFragment::class.java.name)
        }.autoDispose()
        when (orderDetails.orderStatus) {
            resources.getString(R.string.new_text).lowercase() -> {
                orderStatus = resources.getString(R.string.received)
                binding.orderDetailsHeaderLayout.completedOrderButton.text = resources.getString(R.string.received_order)
                binding.orderDetailsHeaderLayout.newDotSelect.isVisible = true
                binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.completedDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = true
                binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.black))
                binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.completedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
            }
            resources.getString(R.string.received).lowercase() -> {
                orderStatus = resources.getString(R.string.packaging)
                binding.orderDetailsHeaderLayout.completedOrderButton.text = resources.getString(R.string.packing_order)
                binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = true
                binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.completedDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = true
                binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.black))
                binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.completedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
            }
            resources.getString(R.string.packaging).lowercase() -> {
                orderStatus = resources.getString(R.string.completed)
                binding.orderDetailsHeaderLayout.completedOrderButton.text = resources.getString(R.string.complete_order)
                binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = true
                binding.orderDetailsHeaderLayout.completedDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = true
                binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.black))
                binding.orderDetailsHeaderLayout.completedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
            }
            resources.getString(R.string.completed).lowercase() -> {
                binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = false
                binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.completedDotSelect.isVisible = true
                binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.completedTextView.setTextColor(getColor(requireContext(), R.color.black))
            }
            else -> {
                binding.orderDetailsHeaderLayout.completedOrderButton.isVisible = false
                binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.completedDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.newTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.completedTextView.setTextColor(getColor(requireContext(), R.color.grey_999999))
            }
        }
    }


    private fun setOrderDetailsData(orderDetailsInfo: List<OrderDetailItem>?) {
        orderDetailsAdapter.listOfOrderDetailsInfo = orderDetailsInfo
    }

    private fun setStatusLogInfo(statusLogInfo: List<StatusItem>?) {
        statusLogAdapter.listOfStatusLog = statusLogInfo
    }

    private fun initAdapter() {
        statusLogAdapter = StatusLogAdapter(requireContext())
        binding.statusLogDetails.rvStatusLog.apply {
            adapter = statusLogAdapter
        }
        orderDetailsAdapter = OrderDetailsAdapter(requireContext())
        binding.orderListLayout.rvOrderDetailsView.apply {
            adapter = orderDetailsAdapter
        }
    }

    private fun allApiCalling() {
        orderDetailsViewModel.loadOrderDetailsItem(orderId)
        if (orderUserId != 0) {
            orderDetailsViewModel.loadUserDetails(orderUserId)
        }

    }

    override fun onResume() {
        super.onResume()
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window, activity)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val manager: FragmentManager? = fragmentManager
        val transaction: FragmentTransaction? = manager?.beginTransaction()
        manager?.popBackStack()
        transaction?.remove(this)
        transaction?.commit()
    }
}