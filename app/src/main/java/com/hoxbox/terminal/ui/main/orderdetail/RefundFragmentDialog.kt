package com.hoxbox.terminal.ui.main.orderdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.gson.Gson
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.order.model.RefundDialogStates
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.extension.*
import com.hoxbox.terminal.ui.main.orderdetail.viewmodel.OrderDetailsViewState
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.order.model.OrderDetail
import com.hoxbox.terminal.api.order.model.TransactionResponse
import com.hoxbox.terminal.api.userstore.model.CaptureNewPaymentRequest
import com.hoxbox.terminal.api.userstore.model.Resource
import com.hoxbox.terminal.base.BaseDialogFragment
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.databinding.FragmentRefundDialogBinding
import com.hoxbox.terminal.ui.main.orderdetail.viewmodel.OrderDetailsViewModel
import com.hoxbox.terminal.utils.Constants.DELIVERY_ORDER_TYPE_ID
import com.hoxbox.terminal.utils.Constants.ORDER_TYPE_ID
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class RefundFragmentDialog : BaseDialogFragment() {

    private var _binding: FragmentRefundDialogBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    lateinit var orderDetails: OrderDetail

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<OrderDetailsViewModel>
    private lateinit var orderDetailsViewModel: OrderDetailsViewModel

    private val refundDialogSubject: PublishSubject<RefundDialogStates> = PublishSubject.create()
    val refundDialogState: Observable<RefundDialogStates> = refundDialogSubject.hide()
    private lateinit var transactionResponse: TransactionResponse

    companion object {
        const val INTENT_CART_GROUP = "Intent Cart Group"
        fun newInstance(orderDetailsInfo: OrderDetail?): RefundFragmentDialog {
            val args = Bundle()
            val gson = Gson()
            val json: String = gson.toJson(orderDetailsInfo)
            json.let { args.putString(INTENT_CART_GROUP, it) }
            val fragment = RefundFragmentDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        orderDetailsViewModel = getViewModelFromFactory(viewModelFactory)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRefundDialogBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.decorView?.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewEvent()
        listenToViewModel()
    }

    private fun listenToViewModel() {
        orderDetailsViewModel.orderDetailsState.subscribeAndObserveOnMainThread {
            when (it) {
                is OrderDetailsViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                    buttonVisibility(false)
                }
                is OrderDetailsViewState.SuccessMessage -> {
                    refundDialogSubject.onNext(RefundDialogStates.DismissedRefundDialog(orderDetails))
                }
                is OrderDetailsViewState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is OrderDetailsViewState.TransactionDetails -> {
                    transactionResponse = it.transactionResponse
                    binding.transactionIdTextView.text =
                        getString(R.string.transaction_id, it.transactionResponse.transactionIdOfProcessor.toString())
                    binding.totalAmount.text =
                        getString(R.string.order_total, it.transactionResponse.transactionAmount?.toDouble()?.div(100).toDollar())
                }
//
//                is OrderDetailsViewState.CaptureNewPaymentIntent -> {
//                    when (it.createPaymentIntentResponse?.firstOrNull()?.getPaymentStatus()) {
//                        PaymentStatus.Success -> {
//                            refundDialogSubject.onNext(RefundDialogStates.GetRefund(orderDetails))
//                        }
//                        else -> {
//                            showToast(it.createPaymentIntentResponse?.firstOrNull()?.status.toString())
//                        }
//                    }
//                }
//                is OrderDetailsViewState.RefundResponse -> {
//                }
                is OrderDetailsViewState.RefundPaymentIntent -> {
                    if (it.createPaymentIntentResponse.response?.firstOrNull()?.status == "approved" && it.createPaymentIntentResponse.response?.firstOrNull()?.type == "refund") {
                        refundDialogSubject.onNext(RefundDialogStates.GetRefund(orderDetails))
                    }
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun buttonVisibility(loading: Boolean) {
        binding.progressBar.isVisible = loading
        binding.refundButton.isVisible = !loading
    }

    private fun listenToViewEvent() {
        val productsDetails = arguments?.getString(PrintReceiptDialog.INTENT_CART_GROUP)
        val gson = Gson()
        orderDetails = gson.fromJson(productsDetails, OrderDetail::class.java)
        orderDetails.id?.let { orderDetailsViewModel.getOrderTransactionDetail(it) }
        binding.amountEditText.setText("$")
//        binding.transactionIdTextView.text = getString(R.string.transaction_id, orderDetails.transaction?.transactionIdOfProcessor.toString())
        orderDetails.orderTotal?.let {
            binding.amountEditText.setText((it).div(100).toDollar())
            binding.totalAmount.text = getString(R.string.order_total, (it).div(100).toDollar())
        }
        binding.cancelButton.throttleClicks().subscribeAndObserveOnMainThread {
            refundDialogSubject.onNext(RefundDialogStates.DismissedRefundDialog(orderDetails))
        }.autoDispose()
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            refundDialogSubject.onNext(RefundDialogStates.DismissedRefundDialog(orderDetails))
        }.autoDispose()
        binding.refundButton.throttleClicks().subscribeAndObserveOnMainThread {
//            if (isAmountValidate()) {
//                requireActivity().hideKeyboard(binding.refundButton.rootView)
//                val giftCard = binding.amountEditText.text.toString()
//                binding.amountEditText.setText("$")
//                val enterGiftCardPrize = (giftCard.removePrefix("$").toDouble().times(100)).toInt()
//                orderDetails.id?.let { it1 -> orderDetailsViewModel.refundOrderPayment(it1,enterGiftCardPrize) }
//            }
            if (orderDetails.orderTypeId == ORDER_TYPE_ID || orderDetails.orderTypeId == DELIVERY_ORDER_TYPE_ID){
                val newPaymentRequest = CaptureNewPaymentRequest(
                    resource = Resource(transactionResponse.transactionAmount ?: 0, type = "refund")

                )
                orderDetailsViewModel.refundPOSOrder(newPaymentRequest)
            } else {
                orderDetails.id?.let { it1 -> orderDetailsViewModel.refundPayment(it1) }
            }
        }.autoDispose()
    }

    private fun isAmountValidate(): Boolean {
        return when {
            binding.amountEditText.text.toString().removePrefix("$").trim().isEmpty() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }
}