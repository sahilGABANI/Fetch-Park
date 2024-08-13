package com.hoxbox.terminal.ui.userstore.loyaltycard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.checkout.model.CreateUserRequest
import com.hoxbox.terminal.api.checkout.model.QRScanResponse
import com.hoxbox.terminal.api.userstore.model.UserDetails
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseDialogFragment
import com.hoxbox.terminal.base.RxBus
import com.hoxbox.terminal.base.RxEvent
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.base.extension.*
import com.hoxbox.terminal.databinding.JoinLoyaltyProgramDialogBinding
import com.hoxbox.terminal.ui.userstore.checkout.viewmodel.CheckOutState
import com.hoxbox.terminal.ui.userstore.checkout.viewmodel.CheckOutViewModel
import javax.inject.Inject

class JoinLoyaltyProgramDialog : BaseDialogFragment() {

    private var _binding: JoinLoyaltyProgramDialogBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<CheckOutViewModel>
    private lateinit var checkOutViewModel: CheckOutViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        checkOutViewModel = getViewModelFromFactory(viewModelFactory)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = JoinLoyaltyProgramDialogBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewModel() {
        checkOutViewModel.checkOutState.subscribeAndObserveOnMainThread {
            when (it) {
                is CheckOutState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is CheckOutState.CreateUserInformation -> {
                    val loyaltyCardFragment = LoyaltyRegistrationSuccessDialog()
                    loyaltyCardFragment.show(parentFragmentManager, JoinLoyaltyProgramDialog::class.java.name)
                    dismiss()
                    RxBus.publish(RxEvent.EventPaymentButtonEnabled(true))
                    RxBus.publish(RxEvent.EventDismissLoyaltyDialog)
                    loggedInUserCache.setLoyaltyQrResponse(
                        QRScanResponse(
                            it.createUserResponse.userPhone,
                            it.createUserResponse.fullName(),
                            it.createUserResponse.id,
                            binding.emailEditText.text.toString(),
                            null
                        )
                    )
                }
                else -> {

                }
            }

        }
    }

    private fun listenToViewEvent() {
        binding.registerMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (isValidate()) {
//                val loyaltyCardFragment = LoyaltyRegistrationSuccessDialog()
//                loyaltyCardFragment.show(parentFragmentManager, JoinLoyaltyProgramDialog::class.java.name)
//                dismiss()
//                RxBus.publish(RxEvent.EventPaymentButtonEnabled(true))
//                RxBus.publish(RxEvent.EventDismissLoyaltyDialog)
//                UserDetails(
//                    name = binding.nameEditText.text.toString(),
//                    surName = binding.surNameEditText.text.toString(),
//                    phone = binding.phoneEditText.text.toString(),
//                    email = binding.emailEditText.text.toString()
//                )
//                loggedInUserCache.setLoyaltyQrResponse(
//                    QRScanResponse(
//                        binding.phoneEditText.text.toString(), binding.nameEditText.text.toString(), null, binding.emailEditText.text.toString(), null
//                    )
//                )
                checkOutViewModel.createUser(
                    CreateUserRequest(
                        "TOKENTOKEN",
                        false,
                        "1966-11-17",
                        binding.emailEditText.text.toString(),
                        "pass@1234",
                        true,
                        binding.surNameEditText.text.toString(),
                        binding.phoneEditText.text.toString(),
                        true,
                        binding.nameEditText.text.toString(),
                        "Airtel"
                    )
                )
            }
        }.autoDispose()
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()

    }

    private fun isValidate(): Boolean {
        return when {
            binding.nameEditText.isFieldBlank() -> {
                showToast(getString(R.string.invalid_name))
                false
            }
            binding.surNameEditText.isFieldBlank() -> {
                showToast(getString(R.string.invalid_surname))
                false
            }
//            binding.phoneEditText.isFieldBlank() ->{
//                showToast(getString(R.string.invalid_surname))
//                false
//            }
//            binding.phoneEditText.isNotValidPhoneLength() -> {
//                showToast(getString(R.string.invalid_surname))
//                false
//            }
//            binding.emailEditText.isNotValidEmail() -> {
//                showToast(getString(R.string.invalid_surname))
//                false
//            }
            else -> true
        }
    }
}