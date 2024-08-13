package com.hoxbox.terminal.ui.main.order

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.order.model.OrdersInfo
import com.hoxbox.terminal.api.order.model.SectionInfo
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseFragment
import com.hoxbox.terminal.base.RxBus
import com.hoxbox.terminal.base.RxEvent
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.base.extension.*
import com.hoxbox.terminal.databinding.FragmentOrdersBinding
import com.hoxbox.terminal.ui.main.deliveries.DeliveriesOrderDetailsFragment
import com.hoxbox.terminal.ui.main.order.view.OrderAdapter
import com.hoxbox.terminal.ui.main.order.viewmodel.OrderViewModel
import com.hoxbox.terminal.ui.main.order.viewmodel.OrderViewState
import com.hoxbox.terminal.ui.main.orderdetail.OrderDetailsFragment
import com.hoxbox.terminal.utils.Constants.ACTIVE
import com.hoxbox.terminal.utils.Constants.NEW
import com.hoxbox.terminal.utils.Constants.PAST
import com.hoxbox.terminal.utils.UserInteractionInterceptor
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class OrdersFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = OrdersFragment()
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<OrderViewModel>
    private lateinit var orderViewModel: OrderViewModel

    private var orderList: List<OrdersInfo> = arrayListOf()
    private var orderListFilter: List<OrdersInfo> = arrayListOf()

    private lateinit var orderAdapter: OrderAdapter
    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!
    private var currentDate: String? = null
    private val calendar = Calendar.getInstance()
    private val year = calendar.get(Calendar.YEAR)
    private var month: Int = calendar.get(Calendar.MONTH)
    private val day = calendar.get(Calendar.DAY_OF_MONTH)
    private var calenderSelectedDate: String = ""
    private var calenderDate: String = ""
    private var statusSelection: String? = null
    private var isCheck: String = ""
    private var searchText: String = ""
    private var isLastSelected: String? = ACTIVE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        orderViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewModel() {
        orderViewModel.orderState.subscribeAndObserveOnMainThread {
            when (it) {
                is OrderViewState.OrderInfoSate -> {
                    orderList = it.orderInfo.orders ?: arrayListOf()
                    setOrderData(it.orderInfo.orders)
                }
                is OrderViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is OrderViewState.LoadingState -> {
                    binding.progressBar.isVisible = it.isLoading
                }
                is OrderViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
            }
        }.autoDispose()
    }

    private fun setOrderData(listOfOderInfo: List<OrdersInfo>?) {
        if (!listOfOderInfo.isNullOrEmpty()) {
            binding.emptyMessageAppCompatTextView.isVisible = false
            isLastSelected =  binding.dateTextView.text.toString().lowercase()
            orderListFilter = when (isLastSelected) {
                NEW -> {
                    listOfOderInfo.filter { it.orderStatus.toString() == resources.getString(R.string.new_text).toLowerCase() }
                }
                ACTIVE -> {
                    listOfOderInfo.filter {
                        it.orderStatus.toString() != resources.getString(R.string.completed).toLowerCase() &&
                                it.orderStatus.toString() != resources.getString(R.string.cancelled_refunded).toLowerCase() &&
                                it.orderStatus.toString() != resources.getString(R.string.delivered).toLowerCase() &&
                                it.orderStatus.toString() != resources.getString(R.string.new_text).toLowerCase()
                    }
                }
                PAST -> {
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
            if (orderListFilter.isNotEmpty()) {
                binding.emptyMessageAppCompatTextView.isVisible = false
                orderAdapter.listOfOrder = orderListFilter
            } else {
                emptyMessageVisibility()
                orderAdapter.listOfOrder = arrayListOf()
            }
//            if(isLastSelected == ACTIVE) {
//                val newOrderCount = orderListFilter.size
//                RxBus.publish(RxEvent.EventOrderCountListen(newOrderCount))
//            }
            val newOrderCount = listOfOderInfo.filter { it.orderStatus == "new" }.size
            RxBus.publish(RxEvent.EventOrderCountListen(newOrderCount))
        } else {
            emptyMessageVisibility()
            orderAdapter.listOfOrder = null
            RxBus.publish(RxEvent.EventOrderCountListen(0))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun emptyMessageVisibility() {
        binding.emptyMessageAppCompatTextView.isVisible = true
        if (calenderDate.isNotEmpty() && calenderDate != currentDate) {
            binding.emptyMessageAppCompatTextView.text = getString(R.string.no_order_for, calenderDate)
        } else {
            binding.emptyMessageAppCompatTextView.text = getText(R.string.no_order_for_today)
        }
    }

    private fun listenToViewEvent() {
        month++
        initAdapter()
        val arrayAdapterDate = ArrayAdapter.createFromResource(
            requireContext(), R.array.dateArray, android.R.layout.simple_spinner_dropdown_item
        )
        isLastSelected =  binding.dateTextView.text.toString().lowercase()
        binding.dateTextView.onItemClickListener = OnItemClickListener { parent, _, position, _ ->
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
                    orderList.filter {
                        it.orderStatus.toString() != resources.getString(R.string.completed).toLowerCase() &&
                                it.orderStatus.toString() != resources.getString(R.string.cancelled_refunded).toLowerCase() &&
                                it.orderStatus.toString() != resources.getString(R.string.delivered).toLowerCase() &&
                                it.orderStatus.toString() != resources.getString(R.string.new_text).toLowerCase()
                    }
                }
            }
            if (orderListFilter.isNotEmpty()) {
                binding.emptyMessageAppCompatTextView.isVisible = false
                orderAdapter.listOfOrder = orderListFilter
            } else {
                emptyMessageVisibility()
                orderAdapter.listOfOrder = null
                RxBus.publish(RxEvent.EventOrderCountListen(0))
            }
            val newOrderCount = orderList.filter { it.orderStatus == "new" }.size
            RxBus.publish(RxEvent.EventOrderCountListen(newOrderCount))
//            if(isLastSelected == ACTIVE) {
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
            binding.pickupCheckBox.isChecked = false
            binding.deliveryCheckBox.isChecked = false
            binding.inStoreCheckBox.isChecked = false
            onCheckboxClicked()
            orderViewModel.loadOrderData(calenderSelectedDate, isCheck, statusSelection)
        }.autoDispose()
        binding.pickupCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            binding.allCheckBox.isChecked = false
            binding.pickupCheckBox.isChecked = true
            binding.deliveryCheckBox.isChecked = false
            binding.inStoreCheckBox.isChecked = false
            onCheckboxClicked()
            orderViewModel.loadOrderData(calenderSelectedDate, isCheck, statusSelection)
        }.autoDispose()
        binding.deliveryCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            binding.allCheckBox.isChecked = false
            binding.pickupCheckBox.isChecked = false
            binding.deliveryCheckBox.isChecked = true
            binding.inStoreCheckBox.isChecked = false
            onCheckboxClicked()
            orderViewModel.loadOrderData(calenderSelectedDate, isCheck, statusSelection)
        }.autoDispose()
        binding.inStoreCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            binding.allCheckBox.isChecked = false
            binding.pickupCheckBox.isChecked = false
            binding.deliveryCheckBox.isChecked = false
            binding.inStoreCheckBox.isChecked = true
            onCheckboxClicked()
        }.autoDispose()
        val arrayAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.statusArray,
            android.R.layout.simple_spinner_dropdown_item
        )
        binding.autoCompleteStatus.setAdapter(arrayAdapter)
        binding.autoCompleteStatus.throttleClicks().subscribeAndObserveOnMainThread {
            binding.autoCompleteStatus.showDropDown()
        }.autoDispose()
        binding.autoCompleteStatus.onItemClickListener = OnItemClickListener { parent, _, position, _ ->
            statusSelection = when (position) {
                0 -> {
                    null
                }
                4 -> {
                    resources.getString(R.string.cancelled_refunded).lowercase()
                }
                else -> {
                    parent.getItemAtPosition(position).toString().lowercase()
                }
            }
            orderViewModel.loadOrderData(calenderSelectedDate, isCheck, statusSelection)
        }
        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = true
            binding.relativeLayout.isVisible = false
            Observable.timer(2000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                onResume()
                binding.swipeRefreshLayout.isRefreshing = false
                binding.relativeLayout.isVisible = true
            }.autoDispose()
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
                    orderAdapter.listOfOrder = orderListFilter
                }
            }
            //it.customerFullName().contains(item.searchText)
        }.autoDispose()
    }

    private fun initAdapter() {
        orderAdapter = OrderAdapter(requireContext()).apply {
            orderActionState.subscribeAndObserveOnMainThread {
                if (it.orderType == getString(R.string.delivery)|| it.orderTypeId == 20) {
                    val trans: FragmentTransaction = parentFragmentManager.beginTransaction()
                    trans.replace(R.id.frameLayout, DeliveriesOrderDetailsFragment.newInstance(it.id,it.orderUserId))
                    trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    trans.commit()
                    onResume()
                } else {
                    val trans: FragmentTransaction = requireFragmentManager().beginTransaction()
                    trans.replace(R.id.frameLayout, OrderDetailsFragment.newInstance(it.id, it.orderUserId, it.orderCartGroupId))
                    trans.addToBackStack(null)
                    trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    trans.commit()
                    onResume()
                }

            }.autoDispose()
        }
        binding.rvOrderView.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.rvOrderView.apply {
            adapter = orderAdapter
        }
        orderAdapter.headerInfo = SectionInfo(
            getString(R.string.order_id),
            getString(R.string.guest),
            getString(R.string.total),
            getString(R.string.membership),
            getString(R.string.status),
            getString(R.string.promised_time_text),
            getString(R.string.order_Placed)
        )
    }

    private fun onCheckboxClicked() {
        isCheck = when {
            binding.allCheckBox.isChecked -> {
                resources.getString(R.string.all)
            }
            binding.pickupCheckBox.isChecked -> {
                resources.getString(R.string.pickup)
            }
            binding.deliveryCheckBox.isChecked -> {
                resources.getString(R.string.delivery)
            }
            binding.inStoreCheckBox.isChecked -> {
                resources.getString(R.string.in_store_text)
            }
            else -> {
                resources.getString(R.string.all)
            }
        }
        orderViewModel.loadOrderData(calenderSelectedDate, isCheck, statusSelection)
    }

    private fun openCalender() {
//        val datePickerDialog = DatePickerDialog(requireContext(), { _, year, monthOfYear, dayOfMonth ->
//            val calenderMonth = monthOfYear + 1
//            val calendar = Calendar.getInstance()
//            val yearf = calendar.get(Calendar.YEAR)
//            val month: Int = calendar.get(Calendar.MONTH)
//            val day = calendar.get(Calendar.DAY_OF_MONTH)
//            currentDate = "${month + 1}/$day/$yearf".toDate("MM/dd/yyyy")?.formatTo("MM/dd/yyyy")
//            println("currentDate : $currentDate")
//            calenderSelectedDate = "$year-$calenderMonth-$dayOfMonth".toDate("yyyy-MM-dd")?.formatTo("yyyy-MM-dd").toString()
//            calenderDate = "$calenderMonth/$dayOfMonth/$year".toDate("MM/dd/yyyy")?.formatTo("MM/dd/yyyy").toString()
//            orderViewModel.loadOrderData(calenderSelectedDate, isCheck, statusSelection)
//            binding.dateTextView.text = if (calenderDate == currentDate) {
//                resources.getText(R.string.today)
//            } else {
//                calenderDate
//            }
//        }, year, month - 1, day)
//        datePickerDialog.show()
//        val maximumDate = Calendar.getInstance()
//        maximumDate.set(Calendar.DAY_OF_MONTH, day)
//        maximumDate.set(Calendar.MONTH, month - 1)
//        maximumDate.set(Calendar.YEAR, year)
//        datePickerDialog.datePicker.maxDate = System.currentTimeMillis() - 1000
    }

    override fun onResume() {
        super.onResume()
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window, activity)
        binding.allCheckBox.isChecked = true
        binding.inStoreCheckBox.isChecked = false
        binding.deliveryCheckBox.isChecked = false
        binding.pickupCheckBox.isChecked = false
        orderViewModel.loadOrderData(calenderSelectedDate, isCheck, statusSelection)
    }
}