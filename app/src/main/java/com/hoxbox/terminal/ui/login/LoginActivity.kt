package com.hoxbox.terminal.ui.login

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import com.hoxbox.terminal.BuildConfig
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.authentication.model.LoggedInUser
import com.hoxbox.terminal.api.authentication.model.LoginCrewRequest
import com.hoxbox.terminal.api.checkout.model.QRScanResponse
import com.hoxbox.terminal.api.clockinout.model.ClockType
import com.hoxbox.terminal.api.store.model.AssignedEmployeeInfo
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseActivity
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.base.extension.*
import com.hoxbox.terminal.databinding.ActivityLoginBinding
import com.hoxbox.terminal.ui.login.viewmodel.LoginViewModel
import com.hoxbox.terminal.ui.login.viewmodel.LoginViewState
import com.hoxbox.terminal.ui.main.CheckInDialogFragment
import com.hoxbox.terminal.ui.main.MainActivity
import com.hoxbox.terminal.ui.userstore.UserStoreActivity
import javax.inject.Inject

class LoginActivity : BaseActivity() {

    companion object {
        const val LOCATION_ID = "LOCATION_ID"
        fun getIntent(context: Context, locationId: Int): Intent {
            val intent = Intent(context, LoginActivity::class.java)
            intent.putExtra(LOCATION_ID, locationId)
            return intent
        }
    }

    private var email: String = ""
    private lateinit var binding: ActivityLoginBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LoginViewModel>
    private lateinit var loginViewModel: LoginViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var employeeList : ArrayList<AssignedEmployeeInfo> = arrayListOf()
    private var employeeNameList = listOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        loginViewModel = getViewModelFromFactory(viewModelFactory)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        initUI()
        listenToViewModel()
        loginViewModel.loadOrderData(30)

    }

    private fun listenToViewModel() {
        loginViewModel.loginState.subscribeAndObserveOnMainThread {
            when (it) {
                is LoginViewState.ErrorMessage -> {
                    updateErrorText(it.errorMessage)
                }
                is LoginViewState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is LoginViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is LoginViewState.LoginSuccess -> {
                    showToast("Crew login success")
//                    val checkInDialogFragment = CheckInDialogFragment.newInstance(ClockType.ClockIn)
//                    checkInDialogFragment.clockInOutSuccess.subscribeAndObserveOnMainThread {
//                        startNewActivityWithDefaultAnimation(MainActivity.getIntent(this))
//                    }.autoDispose()
//                    checkInDialogFragment.show(supportFragmentManager, LoginActivity::class.java.name)
                    startNewActivityWithDefaultAnimation(MainActivity.getIntent(this))
                }
                is LoginViewState.OrderInfoSate -> {
                    loginViewModel.playMusic()
                }
                is LoginViewState.PlayMusic -> {
                    val mediaPlayer = MediaPlayer.create(applicationContext,R.raw.notification)
                    mediaPlayer.start()
                    loginViewModel.stopMusic()
                }
                is LoginViewState.StoreResponses -> {
                    employeeList.clear()
                    it.storeResponse.assignedEmployee?.let {
                        employeeList.addAll(it)
                    }
                    employeeNameList = emptyList()
                    employeeList.forEach { employee ->
                        employeeNameList = employeeNameList + employee.fullName().trim().plus(" - ${employee.roleName}")
                    }
                    val arrayAdapter = ArrayAdapter(this@LoginActivity, android.R.layout.simple_spinner_dropdown_item, employeeNameList)
                    binding.viewLogInCrew.emailSpinner.setAdapter(arrayAdapter)
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun initUI() {
        guestOrCrewSelection(false)
        binding.guestSelectLinear.throttleClicks().subscribeAndObserveOnMainThread {
            guestOrCrewSelection(false)
        }.autoDispose()
        binding.crewSelectLinear.throttleClicks().subscribeAndObserveOnMainThread {
            guestOrCrewSelection(true)
        }.autoDispose()
        binding.viewLogInCrew.forgotPasswordTextView.throttleClicks()
            .subscribeAndObserveOnMainThread {
                binding.viewLogInCrew.root.visibility = View.GONE
                binding.viewLogInGuest.root.visibility = View.GONE
                binding.viewForgotPassword.root.visibility = View.VISIBLE
            }.autoDispose()

        binding.viewForgotPassword.loginAgainButton.throttleClicks()
            .subscribeAndObserveOnMainThread {
                binding.crewImageView.isSelected = true
                binding.guestImageview.isSelected = false
                binding.guestTextview.isSelected = false
                binding.crewTextview.isSelected = true
                binding.guestSelectLinear.isSelected = false
                binding.crewSelectLinear.isSelected = true
                binding.viewLogInGuest.root.visibility = View.GONE
                binding.viewForgotPassword.root.visibility = View.GONE
                binding.viewLogInCrew.root.visibility = View.VISIBLE
            }.autoDispose()

        binding.viewLogInCrew.loginButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (isValidate()) {
                val password = binding.viewLogInCrew.passwordEditText.text.toString()
                val locationId = intent.getIntExtra(LOCATION_ID, 0)
                binding.viewLogInCrew.errorTextView.isVisible = false
                loginViewModel.loginCrew(LoginCrewRequest(email, password, locationId))
            } else {
                binding.viewLogInCrew.errorTextView.isVisible = true
            }
        }.autoDispose()
        binding.viewLogInGuest.proceedToMenuButton.throttleClicks().subscribeAndObserveOnMainThread {
//            loggedInUserCache.setLoggedInUserCartGroupId(0)
//            startActivityWithDefaultAnimation(UserStoreActivity.getIntent(this))
        }.autoDispose()

        binding.viewLogInCrew.emailSpinner.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            binding.viewLogInCrew.emailSpinner.isSelected = false
//            email = employeeList[position].userEmail?.trim().toString()
            email = if (BuildConfig.DEBUG) {
                "emp3@gmail.com"
            } else {
                employeeList[position].userEmail?.trim().toString()
            }
            binding.viewLogInCrew.loginButton.isEnabled = true
        }
    }

    private fun isValidate(): Boolean {
        return when {
//            binding.viewLogInCrew.emailEditText.isFieldBlank() -> {
//                updateErrorText(getText(R.string.blank_email).toString())
//                false
//            }
//            binding.viewLogInCrew.emailEditText.isNotValidEmail() -> {
//                updateErrorText(getText(R.string.invalid_email).toString())
//                false
//            }
            binding.viewLogInCrew.passwordEditText.isFieldBlank() -> {
                updateErrorText(getText(R.string.blank_password).toString())
                false
            }
            email.trim().isEmpty() -> {
                updateErrorText(getText(R.string.invalid_email).toString())
                false
            }

            else -> true
        }
    }

    private fun buttonVisibility(isLoading: Boolean) {
        binding.viewLogInCrew.loginButton.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.viewLogInCrew.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun updateErrorText(errorMessage: String) {
        binding.viewLogInCrew.errorTextView.isVisible = true
        binding.viewLogInCrew.errorTextView.text = errorMessage
    }

    private fun guestOrCrewSelection(isCrew: Boolean) {
        binding.guestImageview.isSelected = !isCrew
        binding.crewImageView.isSelected = isCrew
        binding.guestTextview.isSelected = !isCrew
        binding.crewTextview.isSelected = isCrew
        binding.guestSelectLinear.isSelected = !isCrew
        binding.crewSelectLinear.isSelected = isCrew
        binding.viewLogInGuest.root.visibility = if (isCrew) View.GONE else View.VISIBLE
        binding.viewLogInCrew.root.visibility = if (isCrew) View.VISIBLE else View.GONE
        binding.viewForgotPassword.root.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        loginViewModel.loadCurrentStoreResponse()
    }
}