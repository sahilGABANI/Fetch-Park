package com.hoxbox.terminal.ui.userstore.checkout

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.checkout.model.GiftCardResponse
import com.hoxbox.terminal.api.checkout.model.HistoryItem
import com.hoxbox.terminal.api.checkout.model.PromoCodeRequest
import com.hoxbox.terminal.api.checkout.model.QRScanResponse
import com.hoxbox.terminal.api.userstore.model.UserDetails
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseFragment
import com.hoxbox.terminal.base.RxBus
import com.hoxbox.terminal.base.RxEvent
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.base.extension.*
import com.hoxbox.terminal.databinding.FragmentCheckOutBinding
import com.hoxbox.terminal.helper.formatTo
import com.hoxbox.terminal.helper.toDate
import com.hoxbox.terminal.ui.userstore.checkout.viewmodel.CheckOutState
import com.hoxbox.terminal.ui.userstore.checkout.viewmodel.CheckOutViewModel
import com.hoxbox.terminal.ui.userstore.loyaltycard.JoinLoyaltyProgramDialog
import com.hoxbox.terminal.utils.Constants
import com.hoxbox.terminal.utils.UserInteractionInterceptor
import javax.inject.Inject
import kotlin.math.log

class CheckOutFragment : BaseFragment() {

    private var subtotal1: Int = 0
    private var giftCardId: Int = 0
    private var enterGiftCardPrize: Int = 0
    private var couponCodeId: Int = 0
    private var subTotal: Double? = null
    private var orderTotal: Double = 0.0
    private var _binding: FragmentCheckOutBinding? = null
    private val binding get() = _binding!!
    private val CAMERA_PERMISSION_REQUEST_CODE = 2323

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<CheckOutViewModel>
    private lateinit var checkOutViewModel: CheckOutViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var qrScanResponse = QRScanResponse()
    private var giftCardResponse = GiftCardResponse()
    private var usersCreditPrice : Int = 0
    private var listOfHistory: List<HistoryItem>? = null
    var qrCodeType: String = Constants.QR_CODE_TYPE_LOYALTY
    var qrUserId :Int =0

    companion object {
        @JvmStatic
        fun newInstance() = CheckOutFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        checkOutViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckOutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.checkoutPartLinearLayout.isVisible = false
        binding.loyaltyCardDialogLayout.isVisible = true
        listenToViewModel()
        listenToViewEvent()
    }

    @SuppressLint("SetTextI18n")
    private fun listenToViewModel() {
        checkOutViewModel.checkOutState.subscribeAndObserveOnMainThread {
            when (it) {
                is CheckOutState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is CheckOutState.LoadingState -> {

                }
                is CheckOutState.QrCodeData -> {
                    loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(it.data.phone, it.data.fullName, it.data.id, it.data.email, it.data.points))
                    binding.loyaltyCardLayout.tvLoyaltyCard.text = "Loyalty Card #${it.data.id}"
                    if (it.data.id != null) {
                        qrUserId =  it.data.id
                        checkOutViewModel.getLoyaltyPointDetails(it.data.id)
                        checkOutViewModel.getUserCredit(it.data.id)
                    }
                    println("loggedInUserCache: ${loggedInUserCache.getLoyaltyQrResponse()?.email}")
                    qrScanResponse = it.data
                    dialogVisibility()
                    userDetailsEditTextVisibility(true)
                    RxBus.publish(RxEvent.EventPaymentButtonEnabled(true))
                    userDetailsSet()
                }
                is CheckOutState.QrCodeScanError -> {
                    showToast(it.errorType)
                }
                is CheckOutState.GiftCard -> {
                    giftCardId = it.data.id!!
                    giftCardResponse = it.data
                    binding.addGiftCardLayout.rlHeader.visibility = View.GONE
                    binding.addGiftCardLayout.rlFooter.visibility = View.VISIBLE
                    binding.addGiftCardLayout.expandable.collapse()
                    binding.addGiftCardLayout.expandable.expand()
                    binding.addGiftCardLayout.giftCardBalanceTextView.text = it.data.giftCardAmout?.toDouble()?.div(100).toDollar()
                }
                is CheckOutState.GiftCardQrResponse -> {
                    giftCardId = it.data.id!!
                    giftCardResponse = it.data
                    binding.addGiftCardLayout.rlHeader.visibility = View.GONE
                    binding.addGiftCardLayout.rlFooter.visibility = View.VISIBLE
                    binding.addGiftCardLayout.expandable.collapse()
                    binding.addGiftCardLayout.expandable.expand()
                    binding.addGiftCardLayout.giftCardBalanceTextView.text = it.data.giftCardAmout?.toDouble()?.div(100).toDollar()
                }
                is CheckOutState.PromocodeResponse -> {
                    couponCodeId = it.promocode.couponCodeId!!
                    binding.addPromocodeLayout.expandable.collapse()
                    requireActivity().hideKeyboard()
                    binding.addPromocodeLayout.promoCodeEditText.text?.clear()
                    binding.addPromocodeLayout.downArrowMaterialCardView.isVisible = false
                    binding.addPromocodeLayout.ivClose.isVisible = true
                    binding.addPromocodeLayout.giftCardDiscountPrizeTextView.isVisible = true
                    val discountPrice = it.promocode.discount.toString().removePrefix("-").toDouble()
                    binding.addPromocodeLayout.giftCardDiscountPrizeTextView.text = "-${discountPrice.div(100).toDollar()}"
                    RxBus.publish(RxEvent.AddPromoCode(discountPrice,couponCodeId))
                }
                is CheckOutState.UserLoyaltyPoint -> {
//                    checkOutViewModel.getUserCredit(loggedInUserCache.getLoyaltyQrResponse()?.id)
                    binding.loyaltyCardLinearLayout.isVisible = true
                    listOfHistory = it.data.history
                    binding.loyaltyCardLayout.llHistory.removeAllViews()
                    binding.loyaltyCardLayout.llHistory.isVisible = true
                    it.data.history?.forEach { item ->
                        item.let { item1 ->
                            val v: View = View.inflate(context, R.layout.loyalty_history, null)
                            v.findViewById<AppCompatTextView>(R.id.productTextview).text = "${item1.appliedPoints} points"
                            v.findViewById<AppCompatTextView>(R.id.productTextDescription).text =
                                item1.dateCreated?.toDate()?.formatTo("MM/dd/yyyy, hh:mm a")
                            binding.loyaltyCardLayout.llHistory.addView(v)
                        }
                    }
                }
                is CheckOutState.UserCreditPoint -> {
                    binding.llCredits.isVisible = true
                    binding.userCreditsLayout.creditPointTextView.isVisible =  true
                    binding.userCreditsLayout.tvPoint.isVisible = true
                    binding.userCreditsLayout.creditPointTextView.text = it.data.credits.toString()
                    if (it.data.credits != 0) {
                        usersCreditPrice = it.data.credits!!
                    }
                    println("usersCreditPrice : $usersCreditPrice")
                }
                else -> {}
            }
        }
    }

    private fun userDetailsSet() {
        binding.personalInformationLayout.userNameAppCompatTextView.text = qrScanResponse.fullName
        binding.personalInformationLayout.userPhoneNumberAppCompatTextView.text = qrScanResponse.phone
        binding.personalInformationLayout.userEmailAppCompatTextView.text = qrScanResponse.email
        binding.loyaltyCardLayout.tvLoyaltyPoint.isVisible = true
        binding.loyaltyCardLayout.tvLoyaltyPoint.text = qrScanResponse.points.toString()
        binding.loyaltyCardLayout.tvPoint.isVisible = true
    }

    private fun dialogVisibility() {
        binding.loyaltyCardDialogLayout.isVisible = false
        binding.checkoutPartLinearLayout.isVisible = true
    }

    private fun listenToViewEvent() {
        binding.checkoutPartLinearLayout.isVisible = false
        binding.loyaltyCardDialogLayout.isVisible = true
        binding.skipMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            userDetailsEditTextVisibility(false)
            binding.loyaltyCardDialogLayout.isVisible = false
            binding.checkoutPartLinearLayout.isVisible = true
            binding.loyaltyCardLinearLayout.isVisible = false
            RxBus.publish(RxEvent.EventPaymentButtonEnabled(true))
        }.autoDispose()
        binding.joinLoyaltyProgramLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            val joinLoyaltyProgramDialog = JoinLoyaltyProgramDialog()
            joinLoyaltyProgramDialog.show(requireFragmentManager(), CheckOutFragment::class.java.name)
        }.autoDispose()
        binding.scanCardButton.throttleClicks().subscribeAndObserveOnMainThread {
            qrCodeType = Constants.QR_CODE_TYPE_LOYALTY
            checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_REQUEST_CODE)
        }.autoDispose()
        binding.addGiftCardLayout.scanGiftCard.throttleClicks().subscribeAndObserveOnMainThread {
            qrCodeType = Constants.QR_CODE_TYPE_GIFT_CARD
            checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_REQUEST_CODE)
        }.autoDispose()
        binding.tipTheCrewLayout.applyButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
        }.autoDispose()
        binding.addGiftCardLayout.applyButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            if (isGiftCardValidate()) {
                checkOutViewModel.applyGiftCard(binding.addGiftCardLayout.giftPackagingEditText.text.toString())
            }
        }.autoDispose()
        RxBus.listen(RxEvent.EventTotalCheckOut::class.java).subscribeAndObserveOnMainThread {
            subTotal = it.orderPrice.orderSubtotal
            it.orderPrice.orderTotal?.let { it ->
                orderTotal = it
            }
            binding.checkoutPartLinearLayout.isVisible = false
            binding.loyaltyCardDialogLayout.isVisible = true
            removeGiftCardAndPromoCode()
            binding.personalInformationLayout.nameEditText.text = null
            binding.personalInformationLayout.surNameEditText.text = null
            binding.personalInformationLayout.phoneEditText.text = null
            binding.personalInformationLayout.emailEditText.text = null
        }.autoDispose()
        RxBus.listen(RxEvent.EventValidation::class.java).subscribeAndObserveOnMainThread {
            if (binding.personalInformationLayout.userDetailsLayout.isVisible) {
                if (isValidate()) {
                    val userName = binding.personalInformationLayout.nameEditText.text.toString()
                    val surName = binding.personalInformationLayout.surNameEditText.text.toString()
                    val phone = binding.personalInformationLayout.phoneEditText.text.toString()
                    val email = binding.personalInformationLayout.emailEditText.text.toString()
                    val orderInstructions = binding.orderSpecialInstructionsEditText.text.toString().ifEmpty { null }
                    if (surName.isNotEmpty()){
                        loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(phone, "$userName $surName", null, email))
                    } else{
                        loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(phone, userName, null, email))
                    }
                    RxBus.publish(RxEvent.PassPromocodeAndGiftCard(couponCodeId, enterGiftCardPrize, giftCardId,orderInstructions))
                    RxBus.publish(RxEvent.PassCreditAmount(subtotal1))
                    RxBus.publish(RxEvent.EventGoToBack(true))
                }
            }
        }.autoDispose()
        RxBus.listen(RxEvent.EventGotoStartButton::class.java).subscribeAndObserveOnMainThread {
            binding.checkoutPartLinearLayout.isVisible = false
            binding.loyaltyCardDialogLayout.isVisible = true
            removeGiftCardAndPromoCode()
            binding.personalInformationLayout.nameEditText.text = null
            binding.personalInformationLayout.surNameEditText.text = null
            binding.personalInformationLayout.phoneEditText.text = null
            binding.personalInformationLayout.emailEditText.text = null
        }.autoDispose()
        binding.addPromocodeLayout.applyButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            if (isPromocodeValidate()) {
                checkOutViewModel.applyPromocode(
                    PromoCodeRequest(
                        0,
                        binding.addPromocodeLayout.promoCodeEditText.text.toString(),
                        subTotal?.times(100),
                        loggedInUserCache.getLoggedInUserCartGroupId()
                    )
                )
            }
        }.autoDispose()
        binding.loyaltyCardLayout.additionMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.loyaltyCardLayout.expandable.isExpanded) {
                binding.loyaltyCardLayout.downArrowImageView.isSelected = false
                binding.loyaltyCardLayout.expandable.collapse()
            } else {
                binding.loyaltyCardLayout.downArrowImageView.isSelected = true
                binding.loyaltyCardLayout.expandable.expand()
            }
        }.autoDispose()
        binding.addGiftCardLayout.downArrowMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.addGiftCardLayout.expandable.isExpanded) {
                binding.addGiftCardLayout.downArrowImageView.isSelected = false
                binding.addGiftCardLayout.expandable.collapse()
            } else {
                binding.addGiftCardLayout.downArrowImageView.isSelected = true
                binding.addGiftCardLayout.expandable.expand()
            }
        }.autoDispose()
        binding.addPromocodeLayout.downArrowMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.addPromocodeLayout.expandable.isExpanded) {
                binding.addPromocodeLayout.downArrowImageView.isSelected = false
                binding.addPromocodeLayout.expandable.collapse()
            } else {
                binding.addPromocodeLayout.downArrowImageView.isSelected = true
                binding.addPromocodeLayout.expandable.expand()
            }
        }.autoDispose()
        binding.userCreditsLayout.downArrowMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.userCreditsLayout.expandable.isExpanded) {
                binding.userCreditsLayout.downArrowImageView.isSelected = false
                binding.userCreditsLayout.expandable.collapse()
            } else {
                binding.userCreditsLayout.downArrowImageView.isSelected = true
                binding.userCreditsLayout.expandable.expand()
            }
        }.autoDispose()
        binding.userCreditsLayout.applyButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            val subtotal = binding.userCreditsLayout.giftPackagingEditText.text.toString().removePrefix("$").toDouble().toConvertDecimalFormat()
             subtotal1 = binding.userCreditsLayout.giftPackagingEditText.text.toString().removePrefix("$").toInt()
            println("subtotal : $subtotal")
            if (subtotal1 <= usersCreditPrice && subtotal != 0.0) {
                if(subtotal > orderTotal) {
                    showToast("can't add credit more than your order price")
                } else {
                    binding.userCreditsLayout.downArrowImageView.isSelected = false
                    binding.userCreditsLayout.expandable.collapse()
                    println("subtotal true $subtotal < $usersCreditPrice ${subtotal < usersCreditPrice}")
                    RxBus.publish(RxEvent.AddCredit(subtotal.times(100)))
                    binding.userCreditsLayout.downArrowMaterialCardView.isVisible = false
                    binding.userCreditsLayout.ivClose.isVisible = true
                    binding.userCreditsLayout.creditPointTextView.text = "-${subtotal.toDollar()}"
                    binding.userCreditsLayout.tvPoint.text = "Credit"
                    binding.userCreditsLayout.giftPackagingEditText.setText("$")
                }
            } else {
                if(usersCreditPrice != 0) {
                    if (subtotal == 0.0) {
                        showToast("can't give credit 0")
                    }else {
                        showToast("can't give credit more then your credit Point")
                    }
                } else {
                    showToast("your credit point is 0")
                }
            }
        }.autoDispose()
        binding.userCreditsLayout.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            binding.userCreditsLayout.downArrowMaterialCardView.isVisible = true
            binding.userCreditsLayout.ivClose.isVisible = false
            binding.userCreditsLayout.creditPointTextView.text = "$usersCreditPrice"
            binding.userCreditsLayout.tvPoint.text = resources.getString(R.string.credits_points)
            RxBus.publish(RxEvent.RemoveCredit(false))
        }.autoDispose()
        binding.addGiftCardLayout.edtAmountToPay.throttleClicks().subscribeAndObserveOnMainThread {
            binding.addGiftCardLayout.edtAmountToPay.requestFocus()
        }.autoDispose()
        binding.addGiftCardLayout.applyAmountButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.addGiftCardLayout.edtAmountToPay.text.toString() != "$" && binding.addGiftCardLayout.edtAmountToPay.text.toString() != "") {
                requireActivity().hideKeyboard()
                val giftCard = binding.addGiftCardLayout.edtAmountToPay.text.toString()
                binding.addGiftCardLayout.edtAmountToPay.setText("$")
                enterGiftCardPrize = (giftCard.removePrefix("$").toDouble() * 100).toInt()
                if (giftCardResponse.giftCardAmout!! >= enterGiftCardPrize) {
                    if ( enterGiftCardPrize.toDouble().div(100) <= orderTotal){
                        binding.addGiftCardLayout.expandable.collapse()
                        binding.addGiftCardLayout.downArrowMaterialCardView.isVisible = false
                        binding.addGiftCardLayout.ivClose.isVisible = true
                        binding.addGiftCardLayout.giftCardDiscountPrizeTextView.isVisible = true
                        binding.addGiftCardLayout.giftCardDiscountPrizeTextView.text = enterGiftCardPrize.toDouble().div(100).toDollarWithOutFormat()
                        RxBus.publish(RxEvent.AddGiftCart(enterGiftCardPrize,giftCardId))
                    } else {
                        enterGiftCardPrize = orderTotal.times(100).toInt()
                        binding.addGiftCardLayout.expandable.collapse()
                        binding.addGiftCardLayout.downArrowMaterialCardView.isVisible = false
                        binding.addGiftCardLayout.ivClose.isVisible = true
                        binding.addGiftCardLayout.giftCardDiscountPrizeTextView.isVisible = true
                        binding.addGiftCardLayout.giftCardDiscountPrizeTextView.text = enterGiftCardPrize.toDouble().div(100).toDollarWithOutFormat()
                        RxBus.publish(RxEvent.AddGiftCart(enterGiftCardPrize,giftCardId))
                    }
                } else {
                    showToast("please enter Valid Value")
                }
            } else {
                showToast("please enter GiftCard Amount")
            }
        }.autoDispose()
        binding.addGiftCardLayout.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            binding.addGiftCardLayout.downArrowMaterialCardView.isVisible = true
            binding.addGiftCardLayout.ivClose.isVisible = false
            binding.addGiftCardLayout.giftCardDiscountPrizeTextView.isVisible = false
            binding.addPromocodeLayout.downArrowImageView.isSelected = false
            binding.addGiftCardLayout.rlHeader.isVisible = true
            binding.addGiftCardLayout.rlFooter.isVisible = false
            RxBus.publish(RxEvent.RemoveGiftCart(false))
        }.autoDispose()
        binding.addPromocodeLayout.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            binding.addPromocodeLayout.promoCodeEditText.setHint(R.string.xxxx_xxxx)
            binding.addPromocodeLayout.downArrowMaterialCardView.isVisible = true
            binding.addPromocodeLayout.ivClose.isVisible = false
            binding.addPromocodeLayout.giftCardDiscountPrizeTextView.isVisible = false
            binding.addPromocodeLayout.downArrowImageView.isSelected = false
            RxBus.publish(RxEvent.RemovePromoCode(false))
        }.autoDispose()
        RxBus.listen(RxEvent.EventCheckValidation::class.java).subscribeAndObserveOnMainThread {
            if (loggedInUserCache.getLoyaltyQrResponse()?.fullName == null || binding.personalInformationLayout.userDetailsLayout.isVisible) {
                if (isValidate()) {
                    val userName = binding.personalInformationLayout.nameEditText.text.toString()
                    val surName = binding.personalInformationLayout.surNameEditText.text.toString()
                    val phone = binding.personalInformationLayout.phoneEditText.text.toString()
                    val email = binding.personalInformationLayout.emailEditText.text.toString()
                    val orderInstructions = binding.orderSpecialInstructionsEditText.text.toString().ifEmpty { null }
                    if (surName.isNotEmpty()){
                        loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(phone, "$userName $surName", null, email))
                    } else{
                        loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(phone, "$userName", null, email))
                    }
                    RxBus.publish(RxEvent.EventGoToPaymentScreen(true))
                    RxBus.publish(RxEvent.PassPromocodeAndGiftCard(couponCodeId, enterGiftCardPrize, giftCardId,orderInstructions))
                    RxBus.publish(RxEvent.PassCreditAmount(subtotal1))
                }
            }
            else {
                if (binding.personalInformationLayout.userDetailsLayout.isVisible){
                    if (isValidate()) {
                        val userName = binding.personalInformationLayout.nameEditText.text.toString()
                        val surName = binding.personalInformationLayout.surNameEditText.text.toString()
                        val phone = binding.personalInformationLayout.phoneEditText.text.toString()
                        val email = binding.personalInformationLayout.emailEditText.text.toString()
                        val orderInstructions = binding.orderSpecialInstructionsEditText.text.toString().ifEmpty { null }
                        if (surName.isNotEmpty()){
                            loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(phone, "$userName $surName", null, email))
                        } else{
                            loggedInUserCache.setLoyaltyQrResponse(QRScanResponse(phone, "$userName", null, email))
                        }
                        RxBus.publish(RxEvent.EventGoToPaymentScreen(true))
                        RxBus.publish(RxEvent.PassPromocodeAndGiftCard(couponCodeId, enterGiftCardPrize, giftCardId,orderInstructions))
                        RxBus.publish(RxEvent.PassCreditAmount(subtotal1))
                    }
                }else{

                    val orderInstructions = binding.orderSpecialInstructionsEditText.text.toString().ifEmpty { null }
                    RxBus.publish(RxEvent.EventGoToPaymentScreen(true))
                    RxBus.publish(RxEvent.PassPromocodeAndGiftCard(couponCodeId, enterGiftCardPrize, giftCardId,orderInstructions))
                    RxBus.publish(RxEvent.PassCreditAmount(subtotal1))
                    RxBus.publish(RxEvent.EventGoToPaymentScreen(true))
                }
            }
        }.autoDispose()
        RxBus.listen(RxEvent.QRCodeText::class.java).subscribeAndObserveOnMainThread {
            if (qrCodeType == Constants.QR_CODE_TYPE_LOYALTY) {
                checkOutViewModel.getQRData(it.data)
            } else {
                checkOutViewModel.giftCardQRCode(it.data)
            }
            requireActivity().hideKeyboard()
        }.autoDispose()
        RxBus.listen(RxEvent.EventDismissLoyaltyRegistrationSuccess::class.java).subscribeAndObserveOnMainThread {
            binding.loyaltyCardDialogLayout.isVisible = false
            userDetailsEditTextVisibility(true)
            binding.personalInformationLayout.userNameAppCompatTextView.text =  loggedInUserCache.getLoyaltyQrResponse()?.fullName
            binding.personalInformationLayout.userPhoneNumberAppCompatTextView.text =  loggedInUserCache.getLoyaltyQrResponse()?.phone
            binding.personalInformationLayout.userEmailAppCompatTextView.text =  loggedInUserCache.getLoyaltyQrResponse()?.email
            checkOutViewModel.getLoyaltyPointDetails(loggedInUserCache.getLoyaltyQrResponse()?.id)
            checkOutViewModel.getUserCredit(loggedInUserCache.getLoyaltyQrResponse()?.id)
        }.autoDispose()
    }


    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), requestCode)
        } else {
            val qrScannerFragment = QrScannerFragment()
            qrScannerFragment.show(requireFragmentManager(), "")

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == CAMERA_PERMISSION_REQUEST_CODE) {
            println("CAMERA_PERMISSION_REQUEST_CODE")
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        println("request Code :${grantResults}")
        if (grantResults[0] == CAMERA_PERMISSION_REQUEST_CODE) {
            println("Camera Permission Granted")
        } else {
            Toast.makeText(requireContext(), "Camera Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isValidate(): Boolean {
        return when {
            binding.personalInformationLayout.nameEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_name), Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun isGiftCardValidate(): Boolean {
        return when {
            binding.addGiftCardLayout.giftPackagingEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_GiftCard), Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun isPromocodeValidate(): Boolean {
        return when {
            binding.addPromocodeLayout.promoCodeEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_Promocode), Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun userDetailsEditTextVisibility(b: Boolean) {
        binding.checkoutPartLinearLayout.isVisible = b
        binding.personalInformationLayout.userNameAppCompatTextView.isVisible = b
        binding.personalInformationLayout.customerBirthDateCompatTextView.isVisible = b
        binding.personalInformationLayout.userPhoneNumberAppCompatTextView.isVisible = b
        binding.personalInformationLayout.userEmailAppCompatTextView.isVisible = b
        binding.personalInformationLayout.userDetailsLayout.isVisible = !b
    }
    private fun removeGiftCardAndPromoCode() {
        binding.addGiftCardLayout.ivClose.isVisible = false
        binding.addGiftCardLayout.giftCardDiscountPrizeTextView.isVisible = false
        binding.addGiftCardLayout.downArrowMaterialCardView.isVisible = true
        binding.addGiftCardLayout.downArrowImageView.isSelected = false
        binding.addGiftCardLayout.rlHeader.isVisible = true
        binding.addGiftCardLayout.rlFooter.isVisible = false
        binding.addPromocodeLayout.ivClose.isVisible = false
        binding.addPromocodeLayout.giftCardDiscountPrizeTextView.isVisible = false
        binding.addPromocodeLayout.downArrowMaterialCardView.isVisible = true
        binding.addPromocodeLayout.downArrowImageView.isSelected = false
        binding.userCreditsLayout.creditPointTextView.isVisible = false
        binding.userCreditsLayout.tvPoint.isVisible = false
        binding.userCreditsLayout.downArrowMaterialCardView.isVisible = true
        binding.userCreditsLayout.downArrowImageView.isSelected = false
    }

    override fun onResume() {
        super.onResume()
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window, activity)
//        if (qrUserId != 0) {
//            checkOutViewModel.getUserCredit(qrUserId)
//            checkOutViewModel.getLoyaltyPointDetails(qrUserId)
//        }
    }
    override fun onPause() {
        super.onPause()
    }
}