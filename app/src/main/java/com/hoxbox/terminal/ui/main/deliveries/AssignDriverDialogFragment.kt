package com.hoxbox.terminal.ui.main.deliveries

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.clockinout.model.ClockType
import com.hoxbox.terminal.api.order.model.SendReceiptStates
import com.hoxbox.terminal.api.store.model.AssignedEmployeeInfo
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseDialogFragment
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.base.extension.*
import com.hoxbox.terminal.databinding.AssignDriverDialogBinding
import com.hoxbox.terminal.ui.login.LoginActivity
import com.hoxbox.terminal.ui.login.viewmodel.LoginViewModel
import com.hoxbox.terminal.ui.login.viewmodel.LoginViewState
import com.hoxbox.terminal.ui.main.CheckInDialogFragment
import com.hoxbox.terminal.ui.main.MainActivity
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class AssignDriverDialogFragment : BaseDialogFragment() {

    private var selectedUser: AssignedEmployeeInfo? = null
    private var _binding: AssignDriverDialogBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LoginViewModel>
    private lateinit var loginViewModel: LoginViewModel

    private var employeeList : ArrayList<AssignedEmployeeInfo> = arrayListOf()
    private var employeeNameList = listOf<String>()

    private val assignDriverDialogSubject: PublishSubject<AssignedEmployeeInfo> = PublishSubject.create()
    val assignDriverDialogState: Observable<AssignedEmployeeInfo> = assignDriverDialogSubject.hide()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AssignDriverDialogBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        HotBoxApplication.component.inject(this)
        loginViewModel = getViewModelFromFactory(viewModelFactory)
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
        listenToViewEvent()
        listenToViewModel()
    }

    private fun listenToViewModel() {
        loginViewModel.loginState.subscribeAndObserveOnMainThread {
            when (it) {
                is LoginViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is LoginViewState.StoreResponses -> {

                    employeeNameList = emptyList()
                    it.storeResponse.assignedEmployee?.forEach { employee ->
                        if (employee.roleName == "Delivery Driver") {
                            employeeList.add(employee)
                            employeeNameList = employeeNameList + employee.fullName().trim()
                        }
                    }
                    val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, employeeNameList)
                    binding.assignDriverSpinner.setAdapter(arrayAdapter)
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun listenToViewEvent() {
        binding.confirmMaterialButton.isEnabled = false
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dialog?.dismiss()
        }.autoDispose()
        binding.assignDriverSpinner.onItemClickListener = AdapterView.OnItemClickListener { adapter, v, position, id ->
            binding.confirmMaterialButton.isEnabled = true
            selectedUser = employeeList[position]
        }

        binding.confirmMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.confirmMaterialButton.isEnabled && selectedUser != null) {
                selectedUser?.let {
                    assignDriverDialogSubject.onNext(it)
                }
            }
        }.autoDispose()
    }

    override fun onResume() {
        super.onResume()
        loginViewModel.loadCurrentStoreResponse()
    }
}