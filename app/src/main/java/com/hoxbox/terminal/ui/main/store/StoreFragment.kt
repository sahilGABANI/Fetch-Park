package com.hoxbox.terminal.ui.main.store

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.store.model.AssignedEmployeeInfo
import com.hoxbox.terminal.api.store.model.StoreResponse
import com.hoxbox.terminal.api.store.model.StoreShiftTime
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseFragment
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.base.extension.getViewModelFromFactory
import com.hoxbox.terminal.base.extension.showToast
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.throttleClicks
import com.hoxbox.terminal.databinding.FragmentStoreBinding
import com.hoxbox.terminal.helper.formatTo
import com.hoxbox.terminal.helper.toDate
import com.hoxbox.terminal.ui.main.store.view.AssignedEmployeesAdapter
import com.hoxbox.terminal.ui.main.store.view.OrderingHoursAdapter
import com.hoxbox.terminal.ui.main.store.viewmodel.StoreState
import com.hoxbox.terminal.ui.main.store.viewmodel.StoreViewModel
import com.hoxbox.terminal.utils.UserInteractionInterceptor
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class StoreFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = StoreFragment()
    }

    private var _binding: FragmentStoreBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<StoreViewModel>
    private lateinit var storeViewModel: StoreViewModel
    private lateinit var assignedEmployeesAdapter: AssignedEmployeesAdapter
    private lateinit var orderingHoursAdapter: OrderingHoursAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        storeViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        initAdapter()
        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = true
            binding.storeConstraintLayout.isVisible = false
            Observable.timer(2000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                onResume()
                binding.swipeRefreshLayout.isRefreshing = false
                binding.storeConstraintLayout.isVisible = true
            }.autoDispose()
        }
        binding.storeNameAppCompatTextView.text =
            loggedInUserCache.getLocationInfo()?.locationName ?: ""
        binding.subtractionPickupTimeMaterialCardView.throttleClicks()
            .subscribeAndObserveOnMainThread {
                if (loggedInUserCache.isAdmin()) {
                    storeViewModel.updateBufferTimeForPickUpOrDelivery(
                        isBufferTimePlush = false,
                        isPickUpBufferTime = true
                    )
                } else {
                    showToast("This feature requires elevated permissions")
                }
            }.autoDispose()
        binding.additionPickUpTimeMaterialCardView.throttleClicks()
            .subscribeAndObserveOnMainThread {
                if (loggedInUserCache.isAdmin()) {
                    storeViewModel.updateBufferTimeForPickUpOrDelivery(
                        isBufferTimePlush = true,
                        isPickUpBufferTime = true
                    )
                } else {
                    showToast("This feature requires elevated permissions")
                }
            }.autoDispose()
        binding.subtractionDeliveryTimeMaterialCardView.throttleClicks()
            .subscribeAndObserveOnMainThread {
                if (loggedInUserCache.isAdmin()) {
                    storeViewModel.updateBufferTimeForPickUpOrDelivery(
                        isBufferTimePlush = false,
                        isPickUpBufferTime = false
                    )
                } else {
                    showToast("This feature requires elevated permissions")
                }
            }.autoDispose()
        binding.additionDeliveryTimeMaterialCardView.throttleClicks()
            .subscribeAndObserveOnMainThread {
                if (loggedInUserCache.isAdmin()) {
                    storeViewModel.updateBufferTimeForPickUpOrDelivery(
                        isBufferTimePlush = true,
                        isPickUpBufferTime = false
                    )
                } else {
                    showToast("This feature requires elevated permissions")
                }
            }.autoDispose()
    }

    private fun initAdapter() {
        assignedEmployeesAdapter = AssignedEmployeesAdapter(requireContext())
        orderingHoursAdapter = OrderingHoursAdapter(requireContext())
        binding.assignedEmployeesRecyclerView.apply {
            adapter = assignedEmployeesAdapter
        }
        binding.orderingHoursRecyclerView.apply {
            adapter = orderingHoursAdapter
        }
    }

    private fun listenToViewModel() {
        storeViewModel.storeState.subscribeAndObserveOnMainThread {
            when (it) {
                is StoreState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is StoreState.LoadingState -> {
                }
                is StoreState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is StoreState.StoreResponses -> {
                    binding.storeLocationAppCompatTextView.text =
                        it.storeResponse.getSafeAddressName()
                    binding.storeContactNumberAppCompatTextView.text =
                        it.storeResponse.getSafePhoneNumber()
                    binding.storeEmailIdAppCompatTextView.text =
                        it.storeResponse.assignedEmployee?.firstOrNull()?.userEmail ?: ""
                    binding.storeHeadPersonNameAppCompatTextView.text =
                        it.storeResponse.assignedEmployee?.firstOrNull()?.getSafeFullNameWithRoleName()
                    setAssignedEmployeesData(it.storeResponse.assignedEmployee)
                    setStoreOpenAndClose(it.storeResponse)
                }
                is StoreState.BufferResponses -> {
                    binding.pickupMinNumberAppCompatTextView.text =
                        it.bufferResponse.getSafeTakeOutBufferTime().toString()
                    binding.deliveryMinNumberAppCompatTextView.text =
                        it.bufferResponse.getSafeDeliveryBufferTime().toString()
                }
                is StoreState.LoadStoreShiftTime -> {
                    setTimeHours(it.listOfShiftTime)
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun setStoreOpenAndClose(storeResponse: StoreResponse) {
        val c = Calendar.getInstance()
        val dayOfWeek = c[Calendar.DAY_OF_WEEK]
        val currentTime = c.time.formatTo("HH:mm:ss")
        println("currentTime : $currentTime")
        println("dayOfWeek : $dayOfWeek")
        when (dayOfWeek - 1) {
            0 -> {
                loggedInUserCache
                val openTime = storeResponse.sundayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                val closeTime = storeResponse.sundayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                if (openTime != null && closeTime != null) {
                    if (openTime < currentTime) {
                        if (closeTime > currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = true
                            binding.tvOpenAndClose.isSelected = true
                            binding.tvOpenAndClose.text = resources.getText(R.string.open)
                        } else if (closeTime < currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        } else {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        }
                    } else if (openTime > currentTime) {
                        binding.openStoreTimeLinearLayout.isSelected = false
                        binding.tvOpenAndClose.isSelected = false
                        binding.tvOpenAndClose.text = resources.getText(R.string.close)
                    } else {
                        binding.openStoreTimeLinearLayout.isSelected = true
                        binding.tvOpenAndClose.isSelected = true
                        binding.tvOpenAndClose.text = resources.getText(R.string.open)
                    }
                }
            }
            1 -> {
                val openTime = storeResponse.mondayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                val closeTime = storeResponse.mondayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                if (openTime != null && closeTime != null) {
                    if (openTime < currentTime) {
                        if (closeTime > currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = true
                            binding.tvOpenAndClose.isSelected = true
                            binding.tvOpenAndClose.text = resources.getText(R.string.open)
                        } else if (closeTime < currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        } else {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        }
                    } else if (openTime > currentTime) {
                        binding.openStoreTimeLinearLayout.isSelected = false
                        binding.tvOpenAndClose.isSelected = false
                        binding.tvOpenAndClose.text = resources.getText(R.string.close)
                    } else {
                        binding.openStoreTimeLinearLayout.isSelected = true
                        binding.tvOpenAndClose.isSelected = true
                        binding.tvOpenAndClose.text = resources.getText(R.string.open)
                    }
                }
            }
            2 -> {
                val openTime = storeResponse.tuesdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                val closeTime = storeResponse.tuesdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                if (openTime != null && closeTime != null) {
                    if (openTime < currentTime) {
                        if (closeTime > currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = true
                            binding.tvOpenAndClose.isSelected = true
                            binding.tvOpenAndClose.text = resources.getText(R.string.open)
                        } else if (closeTime < currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        } else {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        }
                    } else if (openTime > currentTime) {
                        binding.openStoreTimeLinearLayout.isSelected = false
                        binding.tvOpenAndClose.isSelected = false
                        binding.tvOpenAndClose.text = resources.getText(R.string.close)
                    } else {
                        binding.openStoreTimeLinearLayout.isSelected = true
                        binding.tvOpenAndClose.isSelected = true
                        binding.tvOpenAndClose.text = resources.getText(R.string.open)
                    }
                }
            }
            3 -> {
                val openTime = storeResponse.wednesdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                val closeTime = storeResponse.wednesdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                if (openTime != null && closeTime != null) {
                    if (openTime < currentTime) {
                        if (closeTime > currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = true
                            binding.tvOpenAndClose.isSelected = true
                            binding.tvOpenAndClose.text = resources.getText(R.string.open)
                        } else if (closeTime < currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        } else {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        }
                    } else if (openTime > currentTime) {
                        binding.openStoreTimeLinearLayout.isSelected = false
                        binding.tvOpenAndClose.isSelected = false
                        binding.tvOpenAndClose.text = resources.getText(R.string.close)
                    } else {
                        binding.openStoreTimeLinearLayout.isSelected = true
                        binding.tvOpenAndClose.isSelected = true
                        binding.tvOpenAndClose.text = resources.getText(R.string.open)
                    }
                }
            }
            4 -> {
                val openTime = storeResponse.thursdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                val closeTime = storeResponse.thursdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                if (openTime != null && closeTime != null) {
                    if (openTime < currentTime) {
                        if (closeTime > currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = true
                            binding.tvOpenAndClose.isSelected = true
                            binding.tvOpenAndClose.text = resources.getText(R.string.open)
                        } else if (closeTime < currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        } else {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        }
                    } else if (openTime > currentTime) {
                        binding.openStoreTimeLinearLayout.isSelected = false
                        binding.tvOpenAndClose.isSelected = false
                        binding.tvOpenAndClose.text = resources.getText(R.string.close)
                    } else {
                        binding.openStoreTimeLinearLayout.isSelected = true
                        binding.tvOpenAndClose.isSelected = true
                        binding.tvOpenAndClose.text = resources.getText(R.string.open)
                    }
                }
            }
            5 -> {
                val openTime = storeResponse.fridayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                val closeTime = storeResponse.fridayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                if (openTime != null && closeTime != null) {
                    if (openTime < currentTime) {
                        if (closeTime > currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = true
                            binding.tvOpenAndClose.isSelected = true
                            binding.tvOpenAndClose.text = resources.getText(R.string.open)
                        } else if (closeTime < currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        } else {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        }
                    } else if (openTime > currentTime) {
                        binding.openStoreTimeLinearLayout.isSelected = false
                        binding.tvOpenAndClose.isSelected = false
                        binding.tvOpenAndClose.text = resources.getText(R.string.close)
                    } else {
                        binding.openStoreTimeLinearLayout.isSelected = true
                        binding.tvOpenAndClose.isSelected = true
                        binding.tvOpenAndClose.text = resources.getText(R.string.open)
                    }
                }
            }
            6 -> {
                val openTime = storeResponse.saturdayOpenTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                val closeTime = storeResponse.saturdayCloseTime?.toDate("HH:mm:ss")?.formatTo("HH:mm:ss")
                if (openTime != null && closeTime != null) {
                    if (openTime < currentTime) {
                        if (closeTime > currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = true
                            binding.tvOpenAndClose.isSelected = true
                            binding.tvOpenAndClose.text = resources.getText(R.string.open)
                        } else if (closeTime < currentTime) {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        } else {
                            binding.openStoreTimeLinearLayout.isSelected = false
                            binding.tvOpenAndClose.isSelected = false
                            binding.tvOpenAndClose.text = resources.getText(R.string.close)
                        }
                    } else if (openTime > currentTime) {
                        binding.openStoreTimeLinearLayout.isSelected = false
                        binding.tvOpenAndClose.isSelected = false
                        binding.tvOpenAndClose.text = resources.getText(R.string.close)
                    } else {
                        binding.openStoreTimeLinearLayout.isSelected = true
                        binding.tvOpenAndClose.isSelected = true
                        binding.tvOpenAndClose.text = resources.getText(R.string.open)
                    }
                }
            }
        }
    }

    private fun setTimeHours(storeShiftTime: List<StoreShiftTime>) {
        orderingHoursAdapter.listOfOrderingHours = storeShiftTime
    }

    private fun setAssignedEmployeesData(assignedEmployeesDetails: List<AssignedEmployeeInfo>?) {
        assignedEmployeesAdapter.listOfAssignedEmployees = assignedEmployeesDetails
    }

    override fun onResume() {
        super.onResume()
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window, activity)
        storeViewModel.loadCurrentStoreResponse()
        storeViewModel.loadBufferTIme()
    }
}