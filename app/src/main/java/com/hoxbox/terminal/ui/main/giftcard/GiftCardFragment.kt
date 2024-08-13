package com.hoxbox.terminal.ui.main.giftcard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.hoxbox.terminal.BuildConfig
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.authentication.model.LocationResponse
import com.hoxbox.terminal.api.giftcard.model.*
import com.hoxbox.terminal.api.menu.model.MenuSectionInfo
import com.hoxbox.terminal.api.menu.model.MenusItem
import com.hoxbox.terminal.api.menu.model.ProductStateRequest
import com.hoxbox.terminal.api.menu.model.ProductsItem
import com.hoxbox.terminal.api.store.model.StoreResponse
import com.hoxbox.terminal.api.userstore.model.CaptureNewPaymentRequest
import com.hoxbox.terminal.api.userstore.model.Resource
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseFragment
import com.hoxbox.terminal.base.RxBus
import com.hoxbox.terminal.base.RxEvent
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.base.extension.*
import com.hoxbox.terminal.databinding.FragmentGiftCardBinding
import com.hoxbox.terminal.helper.BohPrinterHelper
import com.hoxbox.terminal.ui.main.giftcard.viewmodel.GiftCardState
import com.hoxbox.terminal.ui.main.giftcard.viewmodel.GiftCardViewModel
import com.hoxbox.terminal.ui.main.menu.view.MenuAdapter
import com.hoxbox.terminal.ui.userstore.checkout.QrScannerFragment
import com.hoxbox.terminal.utils.Constants
import com.hoxbox.terminal.utils.UserInteractionInterceptor
import timber.log.Timber
import javax.inject.Inject

class GiftCardFragment : BaseFragment() {
    companion object {
        @JvmStatic
        fun newInstance() = GiftCardFragment()
    }

    private lateinit var selectedCategory: String
    private lateinit var category: String
    private var buyPhysicalCardRequest: BuyPhysicalCardRequest? = null
    private var buyVirtualCardRequest: BuyVirtualCardRequest? = null
    private var qrCodeType: String? = null
    private var _binding: FragmentGiftCardBinding? = null
    private val binding get() = _binding!!
    private val CAMERA_PERMISSION_REQUEST_CODE = 2323
    private lateinit var bohPrinterHelper: BohPrinterHelper

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<GiftCardViewModel>
    private lateinit var giftCardViewModel: GiftCardViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var listOfPhysicalGiftCard: ArrayList<VirtualGiftCardInfo> = arrayListOf()
    private var storeResponse: StoreResponse?= null
    private lateinit var giftCardManagementAdapter: MenuAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        giftCardViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGiftCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        initAdapter()
        category = getColoredSpanned(resources.getString(R.string.category), ContextCompat.getColor(requireContext(), R.color.grey))
        selectedCategory = getColoredSpanned(resources.getString(R.string.all), ContextCompat.getColor(requireContext(), R.color.black))
        binding.autoCompleteStatus.setText(Html.fromHtml("$category $selectedCategory"))
        physicalOrVirtualSelection(false)
        val params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        val virtualParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        virtualParams.setMargins(0, 290, 0, 0)
        params.setMargins(0, 0, 0, 0)
//        binding.cardDetailsPart.llCardDetails.isVisible = false
//        binding.cardDetailsPart.llAmount.isVisible = true
//        binding.cardDetailsPart.userDetailsLayout.isVisible = true
//        val params1 = binding.cardDetailsPart.registerCardButton.layoutParams as ConstraintLayout.LayoutParams
//        params1.topToTop = ConstraintLayout.LayoutParams.UNSET
//        params1.endToEnd = ConstraintLayout.LayoutParams.UNSET
//        params1.startToEnd = ConstraintLayout.LayoutParams.UNSET
//        params1.endToStart = ConstraintLayout.LayoutParams.UNSET
//        params1.topToBottom = R.id.userDetailsLayout
//        binding.cardDetailsPart.registerCardButton.requestLayout()
//        binding.cardDetailsPart.amountEditText.imeOptions = EditorInfo.IME_ACTION_NEXT
//        binding.cardDetailsPart.cardTypeRadioGroup.layoutParams = virtualParams
        binding.cardDetailsPart.physicalRadioButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.cardDetailsPart.physicalRadioButton.isChecked) {
                val params1 = binding.cardDetailsPart.registerCardButton.layoutParams as ConstraintLayout.LayoutParams
                params1.topToTop = ConstraintLayout.LayoutParams.UNSET
                params1.endToEnd = ConstraintLayout.LayoutParams.UNSET
                params1.startToEnd = ConstraintLayout.LayoutParams.UNSET
                params1.endToStart = ConstraintLayout.LayoutParams.UNSET
                params1.topToBottom = R.id.llCardDetails
                binding.cardDetailsPart.registerCardButton.requestLayout()
                binding.cardDetailsPart.llCardDetails.isVisible = true
                binding.cardDetailsPart.llAmount.isVisible = false
                binding.cardDetailsPart.userDetailsLayout.isVisible = false
                binding.cardDetailsPart.amountEditText.imeOptions = EditorInfo.IME_ACTION_DONE
                binding.cardDetailsPart.cardTypeRadioGroup.layoutParams = params

//                binding.cardDetailsPart.llCardDetails.isVisible = false
//                binding.cardDetailsPart.llAmount.isVisible = true
//                binding.cardDetailsPart.userDetailsLayout.isVisible = true
//                val params1 = binding.cardDetailsPart.registerCardButton.layoutParams as ConstraintLayout.LayoutParams
//                params1.topToTop = ConstraintLayout.LayoutParams.UNSET
//                params1.endToEnd = ConstraintLayout.LayoutParams.UNSET
//                params1.startToEnd = ConstraintLayout.LayoutParams.UNSET
//                params1.endToStart = ConstraintLayout.LayoutParams.UNSET
//                params1.topToBottom = R.id.userDetailsLayout
//                binding.cardDetailsPart.registerCardButton.requestLayout()
//                binding.cardDetailsPart.amountEditText.imeOptions = EditorInfo.IME_ACTION_NEXT
//                binding.cardDetailsPart.cardTypeRadioGroup.layoutParams = virtualParams
            }
            requireActivity().hideKeyboard(binding.cardDetailsPart.physicalRadioButton.rootView)
        }.autoDispose()
        binding.cardDetailsPart.cardTypeRadioGroup.layoutParams = params
        binding.cardDetailsPart.virtualRadioButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.cardDetailsPart.virtualRadioButton.isChecked) {
                binding.cardDetailsPart.llCardDetails.isVisible = false
                binding.cardDetailsPart.llAmount.isVisible = true
                binding.cardDetailsPart.userDetailsLayout.isVisible = true
                val params1 = binding.cardDetailsPart.registerCardButton.layoutParams as ConstraintLayout.LayoutParams
                params1.topToTop = ConstraintLayout.LayoutParams.UNSET
                params1.endToEnd = ConstraintLayout.LayoutParams.UNSET
                params1.startToEnd = ConstraintLayout.LayoutParams.UNSET
                params1.endToStart = ConstraintLayout.LayoutParams.UNSET
                params1.topToBottom = R.id.userDetailsLayout
                binding.cardDetailsPart.registerCardButton.requestLayout()
                binding.cardDetailsPart.amountEditText.imeOptions = EditorInfo.IME_ACTION_NEXT
                binding.cardDetailsPart.cardTypeRadioGroup.layoutParams = virtualParams
            }
            requireActivity().hideKeyboard(binding.cardDetailsPart.virtualRadioButton.rootView)
        }.autoDispose()

        binding.newCardSelectLinear.throttleClicks().subscribeAndObserveOnMainThread {
            physicalOrVirtualSelection(false)
        }.autoDispose()

        binding.checkBalanceSelectLinear.throttleClicks().subscribeAndObserveOnMainThread {
            physicalOrVirtualSelection(true)
        }.autoDispose()
        binding.checkCardBalance.scanGiftCard.throttleClicks().subscribeAndObserveOnMainThread {
            qrCodeType = Constants.QR_CODE_TYPE_GIFT_CARD
            checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_REQUEST_CODE)
        }.autoDispose()
        binding.cardBalance.okThanksButton.throttleClicks().subscribeAndObserveOnMainThread {
            binding.cardBalance.root.isVisible = false
            binding.checkCardBalance.root.isVisible = true
        }.autoDispose()

        binding.checkCardBalance.checkBalanceMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            giftCardViewModel.applyGiftCard(binding.checkCardBalance.cardIdEditText.text.toString())
        }.autoDispose()
        RxBus.listen(RxEvent.QRCodeText::class.java).subscribeAndObserveOnMainThread {
            if (qrCodeType == Constants.QR_CODE_TYPE_GIFT_CARD) {
                giftCardViewModel.giftCardQRCode(it.data)
            }
            requireActivity().hideKeyboard()
        }.autoDispose()

        binding.cardDetailsPart.registerCardButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            if (binding.cardDetailsPart.physicalRadioButton.isChecked) {
                if (isPhysicalGiftCardValidate()) {
                    val newPaymentRequest = CaptureNewPaymentRequest(
                        resource = Resource(binding.cardDetailsPart.amountEditText.text.toString().toDouble().times(100).toInt())
                    )
                    giftCardViewModel.captureNewPayment(newPaymentRequest)

//                    buyPhysicalCardRequest = BuyPhysicalCardRequest(
//                        giftCardAmout = binding.cardDetailsPart.amountEditText.text.toString().toDouble().times(100).toInt(),
//                        transactionAmount = binding.cardDetailsPart.amountEditText.text.toString().toDouble().times(100).toInt().toString(),
//                        giftCardCode = binding.cardDetailsPart.giftPackagingEditText.text.toString(),
//                        transactionIdOfProcessor = "ch_3MD14rHBh9c4S8JH080fPpzq",
//                        transactionChargeId = "ch_3MD14rHBh9c4S8JH080fPpzq",
//                    )
//                    buyPhysicalCardRequest?.let { item -> giftCardViewModel.buyPhysicalGiftCard(item) }
                    binding.constraintGiftCard.isVisible = false
                    binding.pendingPayment.root.isVisible = true
                    binding.pendingPayment.ivPaymentError.setImageResource(R.drawable.ic_scan_loyalty_card)
                    binding.pendingPayment.dialogHeading.text = resources.getString(R.string.pending_payment)
                    binding.pendingPayment.tvDescription.text = resources.getString(R.string.complete_your_payment_on_the_payment_terminal)
                    binding.pendingPayment.tryAgainMaterialButton.isVisible = false
                    binding.pendingPayment.tvTotalPrizeNumber.text = binding.cardDetailsPart.amountEditText.text.toString().toDouble().toDollar()
                }
            } else {
                if (isVirtualGiftCardValidate()) {
                    val newPaymentRequest = CaptureNewPaymentRequest(
                        resource = Resource(binding.cardDetailsPart.edtAmount.text.toString().toDouble().times(100).toInt())
                    )
                    giftCardViewModel.captureNewPayment(newPaymentRequest)

//                    val giftCardPurchaserFirstName = binding.cardDetailsPart.edtPurchaserNameEditText.text.toString()
//                    val giftCardPurchaserLastName = binding.cardDetailsPart.edtPurchaserSurNameEditText.text.toString()
//                    val giftCardRecipientFirstName = binding.cardDetailsPart.edtRecipientNameEditText.text.toString()
//                    val giftCardRecipientLastName = binding.cardDetailsPart.edtRecipientSurNameEditText.text.toString()
//                    buyVirtualCardRequest = BuyVirtualCardRequest(
//                        giftCardPurchaserFirstName = giftCardPurchaserFirstName,
//                        giftCardPurchaserLastName = giftCardPurchaserLastName,
//                        giftCardPurchaserEmail = binding.cardDetailsPart.edtPurchaserEmailEditText.text.toString(),
//                        giftCardRecipientFirstName = giftCardRecipientFirstName,
//                        giftCardRecipientLastName = giftCardRecipientLastName,
//                        giftCardRecipientEmail = binding.cardDetailsPart.edtRecipientEmailEditText.text.toString(),
//                        giftCardAmout = binding.cardDetailsPart.edtAmount.text.toString().toDouble().times(100).toInt(),
//                        giftCardPersonalMessage = binding.cardDetailsPart.edtPersonalMessage.text.toString().ifEmpty { null },
//                        orderModeId = 11
//                    )
//
//                    buyVirtualCardRequest?.let { item ->
//                        val giftCardRequest = GiftCardRequest(
//                            giftCards = listOf(item),
//                            transactionIdOfProcessor = "ch_3MD14rHBh9c4S8JH080fPpzq",
//                            transactionChargeId = "ch_3MD14rHBh9c4S8JH080fPpzq",
//                            transactionAmount = (binding.cardDetailsPart.edtAmount.text.toString().toDouble().times(100).toInt()).toString()
//                        )
//
//                        println("OkHttpClient : buyVirtualCardRequest :${Gson().toJson(giftCardRequest)}")
//                        giftCardViewModel.buyVirtualGiftCard(giftCardRequest)
//                    }

                    binding.constraintGiftCard.isVisible = false
                    binding.pendingPayment.root.isVisible = true
                    binding.pendingPayment.ivPaymentError.setImageResource(R.drawable.ic_scan_loyalty_card)
                    binding.pendingPayment.dialogHeading.text = resources.getString(R.string.pending_payment)
                    binding.pendingPayment.tvDescription.text = resources.getString(R.string.complete_your_payment_on_the_payment_terminal)
                    binding.pendingPayment.tryAgainMaterialButton.isVisible = false
                    binding.pendingPayment.tvTotalPrizeNumber.text = binding.cardDetailsPart.edtAmount.text.toString().toDouble().toDollar()
                }
            }
        }.autoDispose()
        binding.cardSuccessPart.backToCheckoutButton.throttleClicks().subscribeAndObserveOnMainThread {
            binding.constraintGiftCard.isVisible = true
            binding.checkCardBalance.root.isVisible = false
            binding.cardDetailsPart.root.isVisible = true
            binding.cardSuccessPart.root.isVisible = false
        }.autoDispose()
        binding.cardCreationFailed.backToCheckoutButton.throttleClicks().subscribeAndObserveOnMainThread {
            clearEditText()
            binding.constraintGiftCard.isVisible = true
            binding.checkCardBalance.root.isVisible = false
            binding.cardDetailsPart.root.isVisible = true
            binding.cardSuccessPart.root.isVisible = false
        }.autoDispose()
        binding.cardSuccessPart.printGiftCardButton.throttleClicks().subscribeAndObserveOnMainThread {
            val t: Thread = object : Thread() {
                override fun run() {
                    val bohPrinterAddress = loggedInUserCache.getLocationInfo()?.bohPrintAddress
                    if (bohPrinterAddress != null && listOfPhysicalGiftCard.isNotEmpty()) {
                        val listOfPhysicalGiftCard = listOfPhysicalGiftCard.firstOrNull()
                        val storeResponse = storeResponse
                        if (listOfPhysicalGiftCard != null && storeResponse != null) {
                            giftCardPrint(listOfPhysicalGiftCard, bohPrinterAddress,storeResponse)
                            val t: Thread = object : Thread() {
                                override fun run() {
                                    giftCardPrint(listOfPhysicalGiftCard, bohPrinterAddress,storeResponse)
                                }
                            }
                            t.start()
                        }
                    }
                }
            }
            t.start()
        }.autoDispose()
    }

    private fun listenToViewModel() {
        giftCardViewModel.giftCardState.subscribeAndObserveOnMainThread {
            when (it) {
                is GiftCardState.ErrorMessage -> {
                    showToast(it.errorMessage)
                    binding.checkCardBalance.root.isVisible = false
                    binding.cardDetailsPart.root.isVisible = false
                    binding.cardSuccessPart.root.isVisible = false
                    binding.cardBalance.root.isVisible = true
                    binding.cardBalance.tvCardBalance.text = getString(R.string.card_not_found)
                    binding.cardBalance.tvCardId.text = getString(R.string.card_error_message)
                    binding.cardBalance.cardBalanceImage.setImageResource(R.drawable.ic_error)
//                    binding.cardBalance.tvCardId.text = getString(R.string.card_id_,"${it.data.id}")
                }

                is GiftCardState.VirtualErrorMessage -> {
                    showToast(it.errorMessage)
                    binding.checkCardBalance.root.isVisible = false
                    binding.cardDetailsPart.root.isVisible = false
                    binding.cardSuccessPart.root.isVisible = false
                    binding.cardCreationFailed.root.isVisible = true
                    binding.pendingPayment.root.isVisible = false
//                    binding.cardBalance.tvCardId.text = getString(R.string.card_id_,"${it.data.id}")
                }

                is GiftCardState.PhysicalErrorMessage -> {
                    showToast(it.errorMessage)
                    binding.checkCardBalance.root.isVisible = false
                    binding.cardDetailsPart.root.isVisible = false
                    binding.cardSuccessPart.root.isVisible = false
                    binding.cardCreationFailed.root.isVisible = true
                    binding.pendingPayment.root.isVisible = false
//                    binding.cardBalance.tvCardId.text = getString(R.string.card_id_,"${it.data.id}")
                }
                is GiftCardState.LoadingState -> {

                }
                is GiftCardState.QrCodeScanError -> {
                    showToast(it.errorType)
                }
                is GiftCardState.GiftCardQrResponse -> {
                    binding.checkCardBalance.root.isVisible = false
                    binding.cardDetailsPart.root.isVisible = false
                    binding.cardSuccessPart.root.isVisible = false
                    binding.cardBalance.root.isVisible = true
                    binding.cardBalance.cardBalanceImage.setImageResource(R.drawable.ic_scan_gift_card)
                    val amount = getColoredSpanned(
                        "$${it.data.giftCardAmout?.toDouble()?.div(100)?.toDollar()}", ContextCompat.getColor(requireContext(), R.color.red)
                    )
                    binding.cardBalance.tvCardBalance.text = Html.fromHtml("${getString(R.string.card_balance_100)} $amount")
                    binding.cardBalance.tvCardId.text = getString(R.string.card_id_, "${it.data.id}")
                }

                is GiftCardState.GiftCard -> {
                    binding.checkCardBalance.root.isVisible = false
                    binding.cardDetailsPart.root.isVisible = false
                    binding.cardSuccessPart.root.isVisible = false
                    binding.cardBalance.root.isVisible = true
                    binding.cardBalance.cardBalanceImage.setImageResource(R.drawable.ic_scan_gift_card)
                    val amount = getColoredSpanned(it.data.giftCardAmout?.div(100).toDollar(), ContextCompat.getColor(requireContext(), R.color.red))
                    binding.cardBalance.tvCardBalance.text = Html.fromHtml("${getString(R.string.card_balance_100)} $amount")
                    binding.cardBalance.tvCardId.text = getString(R.string.card_id_, "${it.data.id}")
                }
                is GiftCardState.BuyVirtualGiftCard -> {
                    listOfPhysicalGiftCard.clear()
                    showToast("GiftCard Create SuccessFully")
                    clearEditText()
                    it.data.giftCards?.let { it1 -> listOfPhysicalGiftCard.addAll(it1) }
                    binding.checkCardBalance.root.isVisible = false
                    binding.cardSuccessPart.printGiftCardButton.isVisible = true
                    binding.cardDetailsPart.root.isVisible = false
                    binding.cardSuccessPart.root.isVisible = true
                    binding.pendingPayment.root.isVisible = false
                    physicalOrVirtualSelection(false)
                }

                is GiftCardState.BuyPhysicalGiftCard -> {
                    showToast("GiftCard updated SuccessFully")
                    clearEditText()
                    binding.checkCardBalance.root.isVisible = false
                    binding.cardSuccessPart.printGiftCardButton.isVisible = false
                    binding.cardDetailsPart.root.isVisible = false
                    binding.cardSuccessPart.root.isVisible = true
                    binding.pendingPayment.root.isVisible = false
                    physicalOrVirtualSelection(false)
                }

                is GiftCardState.StoreResponses -> {
                    storeResponse = it.storeResponse
                }
                is GiftCardState.CaptureNewPaymentIntent -> {
                    if (it.createPaymentIntentResponse.response?.firstOrNull()?.status == "approved") {
                        binding.pendingPayment.root.isVisible = false
                        binding.constraintGiftCard.isVisible = false
                        binding.cardSuccessPart.root.isVisible = true
                        binding.cardBalance.tvCardBalance.text = getString(R.string.gift_card_purchased_successfully)
                        if (binding.cardDetailsPart.physicalRadioButton.isChecked) {
                            buyPhysicalCardRequest = BuyPhysicalCardRequest(
                                giftCardAmout = binding.cardDetailsPart.amountEditText.text.toString().toDouble().times(100).toInt(),
                                transactionAmount = binding.cardDetailsPart.amountEditText.text.toString().toDouble().times(100).toInt().toString(),
                                giftCardCode = binding.cardDetailsPart.giftPackagingEditText.text.toString(),
                                transactionChargeId = it.createPaymentIntentResponse.response.firstOrNull()?.retrievalReferenceNo,
                                transactionIdOfProcessor = it.createPaymentIntentResponse.response.firstOrNull()?.hostTransactionReference,
                            )
                            buyPhysicalCardRequest?.let { item -> giftCardViewModel.buyPhysicalGiftCard(item) }

//                            val giftCardPurchaserFirstName = binding.cardDetailsPart.edtPurchaserNameEditText.text.toString()
//                            val giftCardPurchaserLastName = binding.cardDetailsPart.edtPurchaserSurNameEditText.text.toString()
//                            val giftCardRecipientFirstName = binding.cardDetailsPart.edtRecipientNameEditText.text.toString()
//                            val giftCardRecipientLastName = binding.cardDetailsPart.edtRecipientSurNameEditText.text.toString()
//                            buyVirtualCardRequest = BuyVirtualCardRequest(
//                                giftCardPurchaserFirstName = giftCardPurchaserFirstName,
//                                giftCardPurchaserLastName = giftCardPurchaserLastName,
//                                giftCardPurchaserEmail = binding.cardDetailsPart.edtPurchaserEmailEditText.text.toString(),
//                                giftCardRecipientFirstName = giftCardRecipientFirstName,
//                                giftCardRecipientLastName = giftCardRecipientLastName,
//                                giftCardRecipientEmail = binding.cardDetailsPart.edtRecipientEmailEditText.text.toString(),
//                                giftCardAmout = binding.cardDetailsPart.edtAmount.text.toString().toDouble().times(100).toInt(),
//                                giftCardPersonalMessage = binding.cardDetailsPart.edtPersonalMessage.text.toString().ifEmpty { null },
//                                orderModeId = 11
//                            )
//
//                            buyVirtualCardRequest?.let { item ->
//                                val giftCardRequest = GiftCardRequest(
//                                    giftCards = listOf(item),
//                                    transactionIdOfProcessor = it.createPaymentIntentResponse.response.firstOrNull()?.hostTransactionReference,
//                                    transactionChargeId = it.createPaymentIntentResponse.response.firstOrNull()?.referenceNo,
//                                    transactionAmount = (it.createPaymentIntentResponse.response.firstOrNull()?.transactionAmount ?: 0).toString()
//                                )
//
//                                println("OkHttpClient : buyVirtualCardRequest :${Gson().toJson(giftCardRequest)}")
//                                giftCardViewModel.buyVirtualGiftCard(giftCardRequest)
//                            }
                        } else {
                            val giftCardPurchaserFirstName = binding.cardDetailsPart.edtPurchaserNameEditText.text.toString()
                            val giftCardPurchaserLastName = binding.cardDetailsPart.edtPurchaserSurNameEditText.text.toString()
                            val giftCardRecipientFirstName = binding.cardDetailsPart.edtRecipientNameEditText.text.toString()
                            val giftCardRecipientLastName = binding.cardDetailsPart.edtRecipientSurNameEditText.text.toString()
                            buyVirtualCardRequest = BuyVirtualCardRequest(
                                giftCardPurchaserFirstName = giftCardPurchaserFirstName,
                                giftCardPurchaserLastName = giftCardPurchaserLastName,
                                giftCardPurchaserEmail = binding.cardDetailsPart.edtPurchaserEmailEditText.text.toString(),
                                giftCardRecipientFirstName = giftCardRecipientFirstName,
                                giftCardRecipientLastName = giftCardRecipientLastName,
                                giftCardRecipientEmail = binding.cardDetailsPart.edtRecipientEmailEditText.text.toString(),
                                giftCardAmout = binding.cardDetailsPart.edtAmount.text.toString().toDouble().times(100).toInt(),
                                giftCardPersonalMessage = binding.cardDetailsPart.edtPersonalMessage.text.toString().ifEmpty { null },
                                orderModeId = 11
                            )

                            buyVirtualCardRequest?.let { item ->
                                val giftCardRequest = GiftCardRequest(
                                    giftCards = listOf(item),
                                    transactionIdOfProcessor = it.createPaymentIntentResponse.response.firstOrNull()?.hostTransactionReference,
                                    transactionChargeId = it.createPaymentIntentResponse.response.firstOrNull()?.referenceNo,
                                    transactionAmount = (it.createPaymentIntentResponse.response.firstOrNull()?.transactionAmount ?: 0).toString()
                                )

                                println("OkHttpClient : buyVirtualCardRequest :${Gson().toJson(giftCardRequest)}")
                                giftCardViewModel.buyVirtualGiftCard(giftCardRequest)
                            }
                        }
                        clearEditText()
                    } else {
                        binding.pendingPayment.ivPaymentError.setImageResource(R.drawable.ic_error)
                        binding.pendingPayment.dialogHeading.text = resources.getString(R.string.payment_error)
                        binding.pendingPayment.tvDescription.text = it.createPaymentIntentResponse.response?.firstOrNull()?.status.toString()
                        binding.pendingPayment.tryAgainMaterialButton.isVisible = true
                    }
                }
                else -> {}
            }
        }
    }


    private fun initAdapter() {
        giftCardManagementAdapter = MenuAdapter(requireContext()).apply {
            menuActionState.subscribeAndObserveOnMainThread {

            }.autoDispose()
        }
        binding.rvGiftCardManagement.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.rvGiftCardManagement.apply {
            adapter = giftCardManagementAdapter
        }
        giftCardManagementAdapter.headerInfo = MenuSectionInfo(
            getString(R.string.card_amp_bow), getString(R.string.description), getString(R.string.expiration), getString(R.string.state)
        )
        giftCardManagementAdapter.isGiftCard  = true
        setData()
    }

    private fun setData(productsInfo: List<ProductsItem>? = null) {
        val listOfProduct =  ArrayList<ProductsItem>()
        listOfProduct.add(ProductsItem(productName = "NEWATFETCHPARK", productDescription = "Shredded chicken breast, dry chow mein noodles, pickled ginger, and carrots...", productId = 1, price = 800.0, active = 1,dateCreated = "12/31/2024"))
        listOfProduct.add(ProductsItem(productName = "HOLLOWEEN_FPARK", productId = 1,productDescription = "Shredded chicken breast, dry chow mein noodles, pickled ginger, and carrots...", price = 700.0, active = 1,dateCreated = "11/03/2023"))
        listOfProduct.add(ProductsItem(productName = "APPDOWNLOAD", productId = 1,productDescription = "Shredded chicken breast, dry chow mein noodles, pickled ginger, and carrots...", price = 700.0, active = 1,dateCreated = "12/31/2024"))
        listOfProduct.add(ProductsItem(productName = "BLACK FRIDAY", productId = 1,productDescription = "Shredded chicken breast, dry chow mein noodles, pickled ginger, and carrots...", price = 700.0, active = 1, dateCreated = "09/09/2023"))
        giftCardManagementAdapter.listOfMenu = listOfProduct
//        menuAdapter.listOfMenu = productsInfo
    }

    private fun physicalOrVirtualSelection(isPhysical: Boolean) {
        binding.newCardImageview.isSelected = !isPhysical
        binding.newCardSelectLinear.isSelected = !isPhysical
        binding.checkBalanceImageView.isSelected = isPhysical
        binding.newCardTextview.isSelected = !isPhysical
        binding.cardDetailsPart.root.isVisible = !isPhysical
        binding.checkBalanceTextview.isSelected = isPhysical
        binding.newCardSelectLinear.isSelected = !isPhysical
        binding.checkCardBalance.root.isVisible = isPhysical
        binding.checkBalanceSelectLinear.isSelected = isPhysical
        binding.cardBalance.root.isVisible = false
    }

    private fun clearEditText() {
        binding.cardDetailsPart.edtPurchaserNameEditText.text?.clear()
        binding.cardDetailsPart.edtRecipientSurNameEditText.text?.clear()
        binding.cardDetailsPart.edtPurchaserSurNameEditText.text?.clear()
        binding.cardDetailsPart.edtRecipientNameEditText.text?.clear()
        binding.cardDetailsPart.emailEditText.text?.clear()
        binding.cardDetailsPart.amountEditText.text?.clear()
        binding.cardDetailsPart.edtRecipientEmailEditText.text?.clear()
        binding.cardDetailsPart.edtAmount.text?.clear()
        binding.cardDetailsPart.edtPurchaserEmailEditText.text?.clear()
        binding.cardDetailsPart.edtPersonalMessage.text?.clear()
        binding.cardDetailsPart.giftPackagingEditText.text?.clear()
        binding.cardDetailsPart.amountEditText.text?.clear()
    }

    fun giftCardPrint(
        listOfPhysicalGiftCard: VirtualGiftCardInfo,
        bohPrinterAddress: String,
        storeResponse: StoreResponse
    ) {
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
                bohPrinterHelper.runPrintBOHReceiptForGiftCard(listOfPhysicalGiftCard, bohPrinterAddress,storeResponse)
            } catch (e: java.lang.Exception) {
                Timber.tag("runPrintBOHReceiptSequence").e(e)
            }
        }
        if (bohPrinterAddress != null && bohPrinterHelper.isPrinterConnected()) {
            try {
                bohPrinterHelper.runPrintBOHReceiptForGiftCard(listOfPhysicalGiftCard, bohPrinterAddress,storeResponse)
            } catch (e: java.lang.Exception) {
                Timber.tag("runPrintBOHReceiptSequence").e(e)
            }
        } else {
            Timber.tag("AutoReceive").e("----------------- Printer not connected -----------------")
        }
    }

    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), requestCode)
        } else {
            val qrScannerFragment = QrScannerFragment()
            qrScannerFragment.show(requireFragmentManager(), "")

        }
    }

    private fun isPhysicalGiftCardValidate(): Boolean {
        return when {
            binding.cardDetailsPart.giftPackagingEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_gift_card_id), Toast.LENGTH_SHORT).show()
                false
            }

            binding.cardDetailsPart.amountEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_lastName), Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun isVirtualGiftCardValidate(): Boolean {
        return when {
            binding.cardDetailsPart.edtAmount.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show()
                false
            }
            binding.cardDetailsPart.edtPurchaserNameEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_name), Toast.LENGTH_SHORT).show()
                false
            }
            binding.cardDetailsPart.edtPurchaserSurNameEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_surname), Toast.LENGTH_SHORT).show()
                false
            }

            binding.cardDetailsPart.edtPurchaserEmailEditText.isNotValidEmail() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_email), Toast.LENGTH_SHORT).show()
                false
            }
            binding.cardDetailsPart.edtRecipientNameEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_name), Toast.LENGTH_SHORT).show()
                false
            }
            binding.cardDetailsPart.edtRecipientSurNameEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_surname), Toast.LENGTH_SHORT).show()
                false
            }
            binding.cardDetailsPart.edtRecipientEmailEditText.isNotValidEmail() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_email), Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            showToast("CAMERA_PERMISSION_REQUEST_CODE")
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        println("request Code :${grantResults}")
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            showToast("Camera Permission Granted")
        } else {
            Toast.makeText(requireContext(), "Camera Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun hideKeyBoard(view: IBinder) {
        val inputManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            view, SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )
    }

    private fun getColoredSpanned(text: String, color: Int): String {
        return "<font color=$color>$text</font>"
    }

    override fun onResume() {
        super.onResume()
        giftCardViewModel.loadCurrentStoreResponse()
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window, activity)
    }
}