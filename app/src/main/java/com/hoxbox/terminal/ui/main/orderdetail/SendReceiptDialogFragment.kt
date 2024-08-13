package com.hoxbox.terminal.ui.main.orderdetail

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import com.google.gson.Gson
import com.hoxbox.terminal.api.order.model.SendReceiptStates
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.order.model.OrderDetail
import com.hoxbox.terminal.base.BaseDialogFragment
import com.hoxbox.terminal.base.extension.*
import com.hoxbox.terminal.databinding.FragmentSendReceiptDialogBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SendReceiptDialogFragment : BaseDialogFragment() {

    private var isCheck: String? =null
    private var _binding: FragmentSendReceiptDialogBinding? = null
    private val binding get() = _binding!!


    private val refundDialogSubject: PublishSubject<SendReceiptStates> = PublishSubject.create()
    val refundDialogState: Observable<SendReceiptStates> = refundDialogSubject.hide()

    companion object {
        const val EMAIL = "email"
        const val PHONE = "phone"
        fun newInstance(email:String,phone :String): SendReceiptDialogFragment {
            val args = Bundle()
            val gson = Gson()
            args.putString(EMAIL, email)
            args.putString(PHONE, phone)
            val fragment = SendReceiptDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSendReceiptDialogBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.decorView?.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
    }

    private fun initUI() {
        val email = arguments?.getString(EMAIL)
        val phone = arguments?.getString(PHONE)
        if (email != "-" && email != null) {
            binding.emailEditText.setText(email)
        }
        if (phone != "N/A" && phone != "-" ) {
            binding.phoneEditText.setText(phone)
        }
        binding.btnSendReceipt.throttleClicks().subscribeAndObserveOnMainThread {
            when {
                binding.emailCheckBox.isChecked && !binding.phoneCheckBox.isChecked -> {
                    if(isEmailValidate()) {
                        refundDialogSubject.onNext(SendReceiptStates.SendReceiptOnEmail(binding.emailEditText.text.toString()))
                    }
                }
                binding.phoneCheckBox.isChecked && !binding.emailCheckBox.isChecked -> {
                    if (isPhoneValidate()) {
                        refundDialogSubject.onNext(SendReceiptStates.SendReceiptOnPhone(binding.phoneEditText.text.toString()))
                    }
                }
                binding.emailCheckBox.isChecked && binding.phoneCheckBox.isChecked -> {
                    if (isPhoneAndEmailValidate()) {
                        refundDialogSubject.onNext(SendReceiptStates.SendReceiptOnPhoneAndEmail(binding.emailEditText.text.toString(),binding.phoneEditText.text.toString()))
                    }
                }

                else -> {
                    showToast("Please Select Send Receipt type")
                }
            }

        }.autoDispose()

        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
    }


    private fun isEmailValidate(): Boolean {
        return when {
            binding.emailEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_enter_email), Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun isPhoneValidate(): Boolean {
        return when {
            binding.phoneEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_phone), Toast.LENGTH_SHORT).show()
                false
            }
            binding.phoneEditText.isPhoneNumber() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_phone), Toast.LENGTH_SHORT).show()
                false
            }
            binding.phoneEditText.isNotValidPhoneLength() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_phone), Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }


    private fun isPhoneAndEmailValidate(): Boolean {
        return when {
            binding.emailEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_enter_email), Toast.LENGTH_SHORT).show()
                false
            }
            binding.phoneEditText.isFieldBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_phone), Toast.LENGTH_SHORT).show()
                false
            }
            binding.phoneEditText.isPhoneNumber() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_phone), Toast.LENGTH_SHORT).show()
                false
            }
            binding.phoneEditText.isNotValidPhoneLength() -> {
                Toast.makeText(requireContext(), getString(R.string.invalid_phone), Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }


}