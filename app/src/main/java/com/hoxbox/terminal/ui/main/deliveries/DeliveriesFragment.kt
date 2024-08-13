package com.hoxbox.terminal.ui.main.deliveries

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentTransaction
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.order.model.OrdersInfo
import com.hoxbox.terminal.api.order.model.SectionInfo
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseFragment
import com.hoxbox.terminal.base.RxBus
import com.hoxbox.terminal.base.RxEvent
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.base.extension.*
import com.hoxbox.terminal.databinding.FragmentDeliveriesBinding
import com.hoxbox.terminal.helper.formatTo
import com.hoxbox.terminal.helper.toDate
import com.hoxbox.terminal.ui.main.deliveries.viewmodel.DeliveriesViewModel
import com.hoxbox.terminal.ui.main.deliveries.viewmodel.DeliveriesViewState
import com.hoxbox.terminal.ui.main.order.view.OrderAdapter
import com.hoxbox.terminal.utils.Constants
import com.hoxbox.terminal.utils.UserInteractionInterceptor
import java.util.*
import javax.inject.Inject

class DeliveriesFragment : BaseFragment() {
    companion object {
        @JvmStatic
        fun newInstance() = DeliveriesFragment()
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<DeliveriesViewModel>
    private lateinit var deliveriesViewModel: DeliveriesViewModel

    private var orderList: List<OrdersInfo> = arrayListOf()
    private var orderListFilter: List<OrdersInfo> = arrayListOf()

    private var currentDate: String = ""
    private var calenderDate: String = ""
    private var calenderSelectedDate: String = ""
    private var searchText: String = ""
    private lateinit var orderAdapter: OrderAdapter
    private var _binding: FragmentDeliveriesBinding? = null
    private val binding get() = _binding!!
    private val calendar = Calendar.getInstance()
    private val year: Int = calendar.get(Calendar.YEAR)
    private var month: Int = calendar.get(Calendar.MONTH)
    private val day: Int = calendar.get(Calendar.DAY_OF_MONTH)
    private var isCheck: String? = null
    private var isLastSelected: String? = Constants.ACTIVE


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        deliveriesViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeliveriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewModel() {
        deliveriesViewModel.deliveriesState.subscribeAndObserveOnMainThread {
            when (it) {
                is DeliveriesViewState.OrderInfoSate -> {
                    orderList = it.orderInfo.orders ?: arrayListOf()
                    setOrderData(it.orderInfo.orders)
                }
                is DeliveriesViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is DeliveriesViewState.LoadingState -> {
                    binding.progressBar.isVisible = it.isLoading
                }
                is DeliveriesViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
            }
        }.autoDispose()
    }

    private fun setOrderData(listOfOderInfo: List<OrdersInfo>?) {
        if(!listOfOderInfo.isNullOrEmpty()) {
            binding.emptyMessageAppCompatTextView.isVisible = false

            orderListFilter = when (isLastSelected) {
                Constants.NEW -> {
                    listOfOderInfo.filter { it.orderStatus.toString() == resources.getString(R.string.new_text).toLowerCase() }
                }
                Constants.ACTIVE -> {
                    listOfOderInfo.filter {
                        it.orderStatus.toString() != resources.getString(R.string.completed).toLowerCase() &&
                                it.orderStatus.toString() != resources.getString(R.string.cancelled_refunded).toLowerCase() &&
                                it.orderStatus.toString() != resources.getString(R.string.delivered).toLowerCase() &&
                                it.orderStatus.toString() != resources.getString(R.string.new_text).toLowerCase()
                    }
                }
                Constants.PAST -> {
                    listOfOderInfo.filter {
                        it.orderStatus.toString() == resources.getString(R.string.completed)
                            .toLowerCase() || it.orderStatus.toString() == resources.getString(R.string.cancelled_refunded)
                            .toLowerCase() || it.orderStatus.toString() == resources.getString(R.string.delivered).toLowerCase()
                    }
                }
                else -> {
                    listOfOderInfo.filter {
                        it.orderStatus.toString() != resources.getString(R.string.completed).toLowerCase() &&
                                it.orderStatus.toString() != resources.getString(R.string.cancelled_refunded).toLowerCase() &&
                                it.orderStatus.toString() != resources.getString(R.string.delivered).toLowerCase() &&
                                it.orderStatus.toString() != resources.getString(R.string.new_text).toLowerCase()
                    }
                }
            }

            orderAdapter.listOfOrder = orderListFilter
//            if(isLastSelected == Constants.ACTIVE) {
//                val newOrderCount = orderListFilter.size
//                RxBus.publish(RxEvent.EventDeliveryCountListen(newOrderCount))
//            }
            val newOrderCount = listOfOderInfo.filter { it.orderStatus == "new" }.size
            RxBus.publish(RxEvent.EventDeliveryCountListen(newOrderCount))
        } else {
            setEmptyMessageAndVisibility()
            orderAdapter.listOfOrder = null
            RxBus.publish(RxEvent.EventDeliveryCountListen(0))
        }

        RxBus.listen(RxEvent.SearchOrderFilter::class.java).subscribeAndObserveOnMainThread { item ->
            if (isVisible) {
                searchText = item.searchText
                if (item.searchText.isNotBlank()) {
                    var list = orderListFilter
                    list = list.filter {
                        it.id.toString().contains(item.searchText) || it?.firstName?.contains(item.searchText, ignoreCase = true) == true || it?.lastName?.contains(
                            item.searchText, ignoreCase = true
                        ) == true || it?.guestPhone?.contains(item.searchText, ignoreCase = true) == true || it.guestName?.contains(
                            item.searchText, ignoreCase = true
                        ) == true || it.userEmail?.contains(
                            item.searchText, ignoreCase = true
                        ) == true || it.userPhone?.contains(item.searchText, ignoreCase = true) == true
                    }
                    orderAdapter.listOfOrder = list.reversed()
                } else {
                    orderAdapter.listOfOrder = orderListFilter.reversed()
                }
            }
            //it.customerFullName().contains(item.searchText)
        }.autoDispose()
    }

    private fun setEmptyMessageAndVisibility() {
        binding.emptyMessageAppCompatTextView.isVisible = true
        if (calenderDate.isNotEmpty() && currentDate != currentDate) {
            binding.emptyMessageAppCompatTextView.text = getString(R.string.no_deliveries_for, calenderDate)
        } else {
            binding.emptyMessageAppCompatTextView.text = getText(R.string.no_deliveries_for_today)
        }
    }

    private fun listenToViewEvent() {
        initAdapter()
        month++
        currentDate = "$month/$day/$year".toDate("MM/dd/yyyy")?.formatTo("MM/dd/yyyy").toString()
        val arrayAdapterDate = ArrayAdapter.createFromResource(
            requireContext(), R.array.dateArray, android.R.layout.simple_spinner_dropdown_item
        )
        binding.dateTextView.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            orderListFilter = when (position) {
                0 -> {
                    isLastSelected = parent.getItemAtPosition(position).toString().lowercase()
                    if (orderList.isNotEmpty()) {
                        orderList.filter {
                            it.orderStatus.toString() != resources.getString(R.string.completed).toLowerCase() &&
                                    it.orderStatus.toString() != resources.getString(R.string.cancelled_refunded).toLowerCase() &&
                                    it.orderStatus.toString() != resources.getString(R.string.delivered).toLowerCase() &&
                                    it.orderStatus.toString() != resources.getString(R.string.new_text).toLowerCase()
                        }
                    } else {
                        arrayListOf()
                    }
                }
                1 -> {
                    isLastSelected = parent.getItemAtPosition(position).toString().lowercase()
                    if (orderList.isNotEmpty()) {
                        orderList.filter { it.orderStatus.toString() == resources.getString(R.string.new_text).toLowerCase() }
                    } else {
                        arrayListOf()
                    }
                }
                2 -> {
                    isLastSelected = parent.getItemAtPosition(position).toString().lowercase()
                    if (orderList.isNotEmpty()) {
                        orderList.filter {
                            it.orderStatus.toString() == resources.getString(R.string.completed)
                                .toLowerCase() || it.orderStatus.toString() == resources.getString(R.string.cancelled_refunded)
                                .toLowerCase() || it.orderStatus.toString() == resources.getString(R.string.delivered).toLowerCase()
                        }
                    } else {
                        arrayListOf()
                    }
                }
                else -> {
                    isLastSelected = Constants.ACTIVE
                    orderList.filter {
                        it.orderStatus.toString() != resources.getString(R.string.completed).toLowerCase() &&
                                it.orderStatus.toString() != resources.getString(R.string.cancelled_refunded).toLowerCase() &&
                                it.orderStatus.toString() != resources.getString(R.string.delivered).toLowerCase() &&
                                it.orderStatus.toString() != resources.getString(R.string.new_text).toLowerCase()
                    }
                }
            }
            if (orderListFilter.isNotEmpty()) {
                orderAdapter.listOfOrder = orderListFilter
            } else {
                setEmptyMessageAndVisibility()
                orderAdapter.listOfOrder = null
                RxBus.publish(RxEvent.EventOrderCountListen(0))
            }
//            if (isLastSelected == Constants.ACTIVE) {
//                val newOrderCount = orderListFilter.size
//                RxBus.publish(RxEvent.EventOrderCountListen(newOrderCount))
//            }
        }
        binding.dateTextView.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.dateTextView.setAdapter(arrayAdapterDate)
            binding.dateTextView.showDropDown()
        }.autoDispose()
        binding.allCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            binding.allCheckBox.isChecked = true
            binding.newCheckBox.isChecked = false
            binding.receivedCheckBox.isChecked = false
            binding.assignedCheckBox.isChecked = false
            binding.deliveredCheckBox.isChecked = false
            binding.dispatchedCheckBox.isChecked = false
            onCheckboxClicked()
        }.autoDispose()
        binding.newCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            binding.allCheckBox.isChecked = false
            binding.newCheckBox.isChecked = true
            binding.receivedCheckBox.isChecked = false
            binding.assignedCheckBox.isChecked = false
            binding.deliveredCheckBox.isChecked = false
            binding.dispatchedCheckBox.isChecked = false
            onCheckboxClicked()
        }.autoDispose()
        binding.receivedCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            binding.allCheckBox.isChecked = false
            binding.newCheckBox.isChecked = false
            binding.receivedCheckBox.isChecked = true
            binding.assignedCheckBox.isChecked = false
            binding.deliveredCheckBox.isChecked = false
            binding.dispatchedCheckBox.isChecked = false
            onCheckboxClicked()
        }.autoDispose()
        binding.assignedCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            binding.allCheckBox.isChecked = false
            binding.newCheckBox.isChecked = false
            binding.receivedCheckBox.isChecked = false
            binding.dispatchedCheckBox.isChecked = false
            binding.deliveredCheckBox.isChecked = false
            binding.assignedCheckBox.isChecked = true
            onCheckboxClicked()
        }.autoDispose()
        binding.dispatchedCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            binding.allCheckBox.isChecked = false
            binding.newCheckBox.isChecked = false
            binding.receivedCheckBox.isChecked = false
            binding.assignedCheckBox.isChecked = false
            binding.deliveredCheckBox.isChecked = false
            binding.dispatchedCheckBox.isChecked = true
            onCheckboxClicked()
        }.autoDispose()
        binding.deliveredCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            binding.allCheckBox.isChecked = false
            binding.newCheckBox.isChecked = false
            binding.receivedCheckBox.isChecked = false
            binding.assignedCheckBox.isChecked = false
            binding.dispatchedCheckBox.isChecked = false
            binding.deliveredCheckBox.isChecked = true
            onCheckboxClicked()
        }.autoDispose()
    }

    private fun onCheckboxClicked() {
        isCheck = when {
            binding.allCheckBox.isChecked -> {
                null
            }
            binding.newCheckBox.isChecked -> {
                resources.getString(R.string.new_text)
            }
            binding.receivedCheckBox.isChecked -> {
                resources.getString(R.string.received)
            }
            binding.assignedCheckBox.isChecked -> {
                resources.getString(R.string.assigned)
            }
            binding.dispatchedCheckBox.isChecked -> {
                resources.getString(R.string.dispatched)
            }
            binding.deliveredCheckBox.isChecked ->{
                resources.getString(R.string.delivered)
            }
            else -> {
                resources.getString(R.string.all)
            }
        }
        deliveriesViewModel.loadDeliverOrderData(calenderSelectedDate,getString(R.string.delivery),isCheck?.toLowerCase())
    }

    private fun initAdapter() {
        orderAdapter = OrderAdapter(requireContext()).apply {
            orderActionState.subscribeAndObserveOnMainThread {
                val trans: FragmentTransaction = parentFragmentManager.beginTransaction()
                trans.replace(R.id.deliveriesFrameLayout, DeliveriesOrderDetailsFragment.newInstance(it.id,it.orderUserId))
                trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                trans.commit()
                onResume()
            }.autoDispose()
        }
        binding.rvOrderView.apply {
            adapter = orderAdapter
        }
        orderAdapter.headerInfo = SectionInfo(
            getString(R.string.order_id),
            getString(R.string.guest),
            getString(R.string.total),
            getString(R.string.order_type),
            getString(R.string.status),
            getString(R.string.promised_time_text),
            getString(R.string.order_Placed)
        )
    }

    private fun openCalender() {
//        val datePickerDialog = DatePickerDialog(requireContext(), { _, year, monthOfYear, dayOfMonth ->
//            val calenderMonth = monthOfYear + 1
//            calenderSelectedDate = "$year-$calenderMonth-$dayOfMonth".toDate("yyyy-MM-dd")?.formatTo("yyyy-MM-dd").toString()
//            calenderDate = "$calenderMonth/$dayOfMonth/$year".toDate("MM/dd/yyyy")?.formatTo("MM/dd/yyyy").toString()
//            deliveriesViewModel.loadDeliverOrderData(calenderSelectedDate, getString(R.string.delivery),isCheck?.toLowerCase())
//            binding.dateTextView.text = if (calenderDate == currentDate) {
//                resources.getText(R.string.today)
//            } else {
//                calenderDate
//            }
//        }, year, month, day)
//        datePickerDialog.show()
//        val maximumDate = Calendar.getInstance()
//        maximumDate.set(Calendar.DAY_OF_MONTH, day)
//        maximumDate.set(Calendar.MONTH, month - 1)
//        maximumDate.set(Calendar.YEAR, year)
//        datePickerDialog.datePicker.maxDate = maximumDate.timeInMillis
    }

    override fun onResume() {
        super.onResume()
        deliveriesViewModel.loadDeliverOrderData(calenderSelectedDate, getString(R.string.delivery),null)
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window, activity)
    }

    override fun onPause() {
        super.onPause()
    }
}