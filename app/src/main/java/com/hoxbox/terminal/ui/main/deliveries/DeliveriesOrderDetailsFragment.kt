package com.hoxbox.terminal.ui.main.deliveries

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
import com.hoxbox.terminal.databinding.FragmentDeliveriesOrderDetailsBinding
import com.hoxbox.terminal.helper.BohPrinterHelper
import com.hoxbox.terminal.helper.formatTo
import com.hoxbox.terminal.helper.toDate
import com.hoxbox.terminal.ui.main.orderdetail.PrintReceiptDialog
import com.hoxbox.terminal.ui.main.orderdetail.RefundFragmentDialog
import com.hoxbox.terminal.ui.main.orderdetail.SendReceiptDialogFragment
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

class DeliveriesOrderDetailsFragment : BaseFragment() {


    companion object {
        private const val ORDER_ID = "orderId"
        private const val ORDER_USER_ID = "orderUserId"

        @JvmStatic
        fun newInstance(orderId: Int?, orderUserId: Int?): DeliveriesOrderDetailsFragment {
            val args = Bundle()
            orderId?.let { args.putInt(ORDER_ID, it) }
            orderUserId?.let { args.putInt(ORDER_USER_ID, it) }
            val fragment = DeliveriesOrderDetailsFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentDeliveriesOrderDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var statusLogAdapter: StatusLogAdapter
    private lateinit var orderDetailsAdapter: OrderDetailsAdapter
    private lateinit var bohPrinterHelper: BohPrinterHelper
    private var orderId by Delegates.notNull<Int>()
    private var orderUserId by Delegates.notNull<Int>()
    private var orderStatus: String = ""
    private var orderDetails: OrderDetail? = null
    private var bohPrintAddress: String? = null

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<OrderDetailsViewModel>
    private lateinit var orderDetailsViewModel: OrderDetailsViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderId = arguments?.getInt(ORDER_ID, 0) ?: throw IllegalStateException("No args provided")
        orderUserId = arguments?.getInt(ORDER_USER_ID, 0) ?: throw IllegalStateException("No args provided")
        HotBoxApplication.component.inject(this)
        orderDetailsViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeliveriesOrderDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        initAdapter()

        bohPrintAddress = loggedInUserCache.getLocationInfo()?.bohPrintAddress
        binding.orderDetailsHeaderLayout.backImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()
        binding.customerDetails.customerLocationAppCompatTextView.isVisible = false
        binding.orderDetailsHeaderLayout.assignDriverButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (orderStatus.lowercase() != resources.getString(R.string.assigned).lowercase()) {
                val userId = loggedInUserCache.getLoggedInUserId()
                if (userId != null) {
                    orderDetailsViewModel.updateOrderStatusDetails(orderStatus.lowercase(), orderId,userId)
                }
                binding.orderDetailsHeaderLayout.assignDriverButton.isVisible = false
            } else {
                val assignDriverDialogFragment = AssignDriverDialogFragment().apply {
                    assignDriverDialogState.subscribeAndObserveOnMainThread {
                        dismiss()
                        orderDetailsViewModel.updateOrderStatusDetails(orderStatus.lowercase(), orderId,it.id)
                    }.autoDispose()
                }
                assignDriverDialogFragment.show(parentFragmentManager,DeliveriesOrderDetailsFragment::class.java.name)
            }
        }.autoDispose()
        binding.mapDetails.zoomMapAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            val zoomMapDialogFragment = ZoomMapDialogFragment()
            zoomMapDialogFragment.show(parentFragmentManager, DeliveriesFragment::class.java.name)
        }.autoDispose()

        binding.customerDetails.btnPrintReceipt.throttleClicks().subscribeAndObserveOnMainThread {
            val t: Thread = object : Thread() {
                override fun run() {
                    orderDetails?.let { bohPrint(it, bohPrintAddress) }
                }
            }
            t.start()
        }.autoDispose()

        binding.customerDetails.btnSendReceipt.throttleClicks().subscribeAndObserveOnMainThread {
            val sendReceiptDialogFragment = SendReceiptDialogFragment.newInstance(
                binding.customerDetails.customerEmailAppCompatTextView.text.toString(),
                binding.customerDetails.customerPhoneNumberAppCompatTextView.text.toString()
            ).apply {
                refundDialogState.subscribeAndObserveOnMainThread {
                    when (it) {
                        is SendReceiptStates.SendReceiptOnEmail -> {
                            dismiss()
                            orderDetails?.id?.let { it1 -> orderDetailsViewModel.sendReceipt(it1,"Email", it.email,null) }
                            binding.customerDetails.customerEmailAppCompatTextView.text = it.email
                        }
                        is SendReceiptStates.SendReceiptOnPhone -> {
                            dismiss()
                            orderDetails?.id?.let { it1 -> orderDetailsViewModel.sendReceipt(it1,"Phone", null,it.phone) }
                            binding.customerDetails.customerPhoneNumberAppCompatTextView.text = it.phone
                        }
                        is SendReceiptStates.SendReceiptOnPhoneAndEmail -> {
                            dismiss()
                            orderDetails?.id?.let { it1 -> orderDetailsViewModel.sendReceipt(it1,"Email", it.email,null) }
                            orderDetails?.id?.let { it1 -> orderDetailsViewModel.sendReceipt(it1,"Phone", null,it.phone) }
                            binding.customerDetails.customerPhoneNumberAppCompatTextView.text = it.phone
                            binding.customerDetails.customerEmailAppCompatTextView.text = it.email
                        }
                        else -> {

                        }
                    }
                }.autoDispose()
            }
            sendReceiptDialogFragment.show(parentFragmentManager, SendReceiptDialogFragment::class.java.name)
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
                            binding.orderDetailsHeaderLayout.refundButton.isVisible = false
                            val userId = loggedInUserCache.getLoggedInUserId()
                            if (userId != null) {
                                orderDetailsViewModel.updateOrderStatusDetails(resources.getString(R.string.cancelled_refunded).toLowerCase(), orderId,userId)
                            }
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
                is OrderDetailsViewState.StatusResponse -> {
//                    setData(it.statusLogInfo)
                }
                is OrderDetailsViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is OrderDetailsViewState.LoadingState -> {

                }
                is OrderDetailsViewState.SuccessMessage -> {
                    it.successMessage?.let { it1 -> showToast(it1) }
                }
                is OrderDetailsViewState.UpdateStatusResponse -> {
                    orderDetailsViewModel.loadOrderDetailsItem(orderId)
                }
                is OrderDetailsViewState.CustomerDetails -> {
                    binding.customerDetails.customerNameAppCompatTextView.text = it.customerDetails.fullName()
                    binding.customerDetails.customerPhoneNumberAppCompatTextView.text = it.customerDetails.userPhone
                    binding.customerDetails.customerEmailAppCompatTextView.text = it.customerDetails.userEmail
                }
                is OrderDetailsViewState.OrderDetailItemResponse -> {
                    orderDetails = it.orderDetails
                    setOrderDetailsData(it.orderDetails.items)
                    if (it.orderDetails.orderStatusHistory?.lastOrNull()?.firstName != null && it.orderDetails.orderStatusHistory.lastOrNull()?.lastName != null) {
                        setStatusData(it.orderDetails.orderStatusHistory)
                    } else {
                        val list = it.orderDetails.orderStatusHistory
                        if (it.orderDetails.guestName != null) {
                            list?.lastOrNull()?.firstName = it.orderDetails.guestName
                        } else {
                            list?.lastOrNull()?.firstName = it.orderDetails.fullName()
                        }
                        setStatusData(list)
                    }
                    initUI(it.orderDetails)
                }
                else -> {}
            }
        }.autoDispose()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initUI(orderDetails: OrderDetail) {
        orderDetails.orderInstructions?.let {
            binding.orderListLayout.specialTextLinear.isVisible = true
            binding.orderListLayout.tvSpecialInstruction.text = it
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
        orderDetails.orderDeliveryAddress?.let {
            binding.customerDetails.customerLocationAppCompatTextView.isVisible = true
            binding.customerDetails.customerLocationAppCompatTextView.text = it
            binding.mapDetails.destinationLocationAppCompatTextView.text = it
        }

//        orderDetails.orderTotal = orderDetails.orderTotal?.minus((orderDetails.orderEmpDiscount ?: 0.00))
//        orderDetails.orderTotal = orderDetails.orderTotal?.minus((orderDetails.orderRefundAmount ?: 0.00))
//        orderDetails.orderTotal = orderDetails.orderTotal?.minus((orderDetails.orderDiscount ?: 0.00))
        orderDetails.orderTotal = orderDetails.orderTotal?.plus((orderDetails.orderAdjustmentAmount?.toDouble() ?: 0.00))
        val total = orderDetails.orderTotal
        if (total != null) {
            orderDetails.orderTotal = if (total < 0) 0.00 else total
        }
//        binding.orderDetailsHeaderLayout.refundButton.isVisible = orderDetails.orderStatusHistory?.first()?.orderStatus != resources.getString(R.string.new_text)
//            .lowercase(Locale.getDefault()) && loggedInUserCache.isAdmin() && orderDetails.orderTotal != 0.00 && orderDetails.orderStatusHistory?.first()?.orderStatus != resources.getString(
//            R.string.refunded
//        ).lowercase(Locale.getDefault())
        orderDetails.orderSubtotal?.let {
            binding.orderListLayout.deliveryOrderPrizeLayout.tvOrderPrizeNumber.text = ((it).div(100)).toDollar()

        }
        orderDetails.orderTax?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.deliveryOrderPrizeLayout.orderTaxRelativeLayout.isVisible = true
                binding.orderListLayout.deliveryOrderPrizeLayout.tvTaxPrize.text = ((it).div(100)).toDollar()
            }
        }
        orderDetails.orderTotal?.let {
            binding.orderDetailsHeaderLayout.orderPrizeTextView.text = ((it).div(100)).toDollar()
            binding.orderListLayout.deliveryOrderPrizeLayout.tvTotalPrizeNumber.text = ((it).div(100)).toDollar()
        }
        orderDetails.orderTip?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.deliveryOrderPrizeLayout.orderTipRelativeLayout.isVisible = true
                binding.orderListLayout.deliveryOrderPrizeLayout.tvTipsPrize.text = ((it).div(100)).toDollar()
            }
        }
        orderDetails.orderDeliveryFee?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.deliveryOrderPrizeLayout.orderDeliveryRelativeLayout.isVisible = true
                binding.orderListLayout.deliveryOrderPrizeLayout.tvDeliveryCharge.text = ((it).div(100)).toDollar()
            }
        }
        orderDetails.orderCouponCodeDiscount?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.deliveryOrderPrizeLayout.orderPromocodeRelativeLayout.isVisible = true
                binding.orderListLayout.deliveryOrderPrizeLayout.tvPromocodeDiscountPrize.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        orderDetails.orderGiftCardAmount?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.deliveryOrderPrizeLayout.orderCardAndBowRelativeLayout.isVisible = true
                binding.orderListLayout.deliveryOrderPrizeLayout.tvCardAndBowCharge.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        orderDetails.creditAmount?.let {
            if (!it.equals(0.0)) {
                binding.orderListLayout.deliveryOrderPrizeLayout.rlCredit.isVisible = true
                binding.orderListLayout.deliveryOrderPrizeLayout.tvCreditAmount.text = "-${((it).div(100)).toMinusDollar().toDollar()}"
            }
        }
        if (orderDetails.orderAdjustmentAmount != null && orderDetails.orderAdjustmentAmount != 0.0) {
            if (orderDetails.orderAdjustmentAmount > 0) {
                binding.orderListLayout.deliveryOrderPrizeLayout.rlAdjustment.isVisible = true
                binding.orderListLayout.deliveryOrderPrizeLayout.tvAdjustmentCharge.text =
                    "${((orderDetails.orderAdjustmentAmount).div(100)).toDollar()}"
            } else {
                binding.orderListLayout.deliveryOrderPrizeLayout.rlAdjustment.isVisible = true
                binding.orderListLayout.deliveryOrderPrizeLayout.tvAdjustmentCharge.text =
                    "-${(abs(orderDetails.orderAdjustmentAmount).div(100)).toDollar()}"
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
        binding.orderListLayout.timeTextView.text = orderDetails.orderPromisedTime?.toDate()?.formatTo("MM/dd/yyyy, hh:mm a")
        when (orderDetails.orderStatus) {
            resources.getString(R.string.new_text).lowercase() -> {
                orderStatus = resources.getString(R.string.received)
                binding.orderDetailsHeaderLayout.assignDriverButton.setText(R.string.received_order)
                binding.orderDetailsHeaderLayout.assignDriverButton.isVisible = true
                binding.orderDetailsHeaderLayout.assignDriverButton.icon = resources.getDrawable(R.drawable.ic_complete_order_icon)
                binding.orderDetailsHeaderLayout.newDotSelect.isVisible = true
                binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.newTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
            }
            resources.getString(R.string.received).lowercase() -> {
                orderStatus = resources.getString(R.string.packaging)
                binding.orderDetailsHeaderLayout.assignDriverButton.setText(R.string.packing_order)
                binding.orderDetailsHeaderLayout.assignDriverButton.icon = resources.getDrawable(R.drawable.ic_complete_order_icon)
                binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.assignDriverButton.isVisible = true
                binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = true
                binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.newTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
            }
            resources.getString(R.string.packaging).lowercase() -> {
                orderStatus = resources.getString(R.string.assigned)
                binding.orderDetailsHeaderLayout.assignDriverButton.setText(R.string.assign_driver)
                binding.orderDetailsHeaderLayout.assignDriverButton.icon = resources.getDrawable(R.drawable.ic_deliveries)
                binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = true
                binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.assignDriverButton.isVisible = true
                binding.orderDetailsHeaderLayout.newTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
            }

            resources.getString(R.string.assigned).lowercase() -> {
                orderStatus =  resources.getString(R.string.dispatched)
                binding.orderDetailsHeaderLayout.assignDriverButton.setText(R.string.dispatched_order)
                binding.orderDetailsHeaderLayout.assignDriverButton.icon = resources.getDrawable(R.drawable.ic_complete_order_icon)
                binding.orderDetailsHeaderLayout.assignDriverButton.isVisible = true
                binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = true
                binding.orderDetailsHeaderLayout.dispatchedDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.deliveredDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.newTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                binding.orderDetailsHeaderLayout.deliveredAppCompatTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
            }
            resources.getString(R.string.dispatched).lowercase() -> {
                orderStatus =  resources.getString(R.string.delivered)
                binding.orderDetailsHeaderLayout.assignDriverButton.setText(R.string.delivered_order)
                binding.orderDetailsHeaderLayout.assignDriverButton.icon = resources.getDrawable(R.drawable.ic_complete_order_icon)
                binding.orderDetailsHeaderLayout.assignDriverButton.isVisible = true
                binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.dispatchedDotSelect.isVisible = true
                binding.orderDetailsHeaderLayout.deliveredDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.newTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.deliveredAppCompatTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            }
            resources.getString(R.string.delivered).lowercase() -> {
                binding.orderDetailsHeaderLayout.assignDriverButton.isVisible = false
                binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.dispatchedDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.deliveredDotSelect.isVisible = true
                binding.orderDetailsHeaderLayout.newTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.dispatchedAppCompatTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.deliveredAppCompatTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            }
            else -> {
                binding.orderDetailsHeaderLayout.assignDriverButton.isVisible = false
                binding.orderDetailsHeaderLayout.newDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.receivedDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.packagingDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.assignDotSelect.isVisible = false
                binding.orderDetailsHeaderLayout.newTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.receivedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.packagingTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
                binding.orderDetailsHeaderLayout.assignedAppCompatTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_999999))
            }
        }
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

    private fun setOrderDetailsData(orderDetailsInfo: List<OrderDetailItem>?) {
        orderDetailsAdapter.listOfOrderDetailsInfo = orderDetailsInfo
    }

    private fun setStatusData(statusLogInfo: List<StatusItem>?) {
        statusLogAdapter.listOfStatusLog = statusLogInfo
    }

    override fun onResume() {
        super.onResume()
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window, activity)
        orderDetailsViewModel.loadOrderDetailsItem(orderId)
        if (orderUserId != 0) {
            orderDetailsViewModel.loadUserDetails(orderUserId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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