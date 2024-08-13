package com.hoxbox.terminal.ui.main.menu

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.menu.model.MenuSectionInfo
import com.hoxbox.terminal.api.menu.model.MenusItem
import com.hoxbox.terminal.api.menu.model.ProductStateRequest
import com.hoxbox.terminal.api.menu.model.ProductsItem
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseFragment
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.base.extension.getViewModelFromFactory
import com.hoxbox.terminal.base.extension.showToast
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.throttleClicks
import com.hoxbox.terminal.databinding.FragmentMenuBinding
import com.hoxbox.terminal.ui.main.menu.view.MenuAdapter
import com.hoxbox.terminal.ui.main.menu.viewModel.MenuState
import com.hoxbox.terminal.ui.main.menu.viewModel.MenuViewModel
import com.hoxbox.terminal.utils.Constants
import com.hoxbox.terminal.utils.UserInteractionInterceptor
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MenuFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = MenuFragment()
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MenuViewModel>
    private lateinit var menuViewModel: MenuViewModel

    private var category: String? = null
    private var selectedCategory: String? = null
    private lateinit var menuAdapter: MenuAdapter
    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!
    private var isCheck: String = Constants.CHECK_ALL
    private var categorySelection: String = Constants.CATEGORY_FILTER_ALL
    private var spinnerArrayList = listOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        menuViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewModel() {
        menuViewModel.menuState.subscribeAndObserveOnMainThread {
            when (it) {
                is MenuState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is MenuState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is MenuState.ProductItemInfo -> {
                    setData(it.ProductsItem)
                }
                is MenuState.UpdatedProductItemResponse -> {
                    val list = menuAdapter.listOfMenu
                    var list2 = list?.find { item -> item.menuId == it.MenuItemInfo.id }?.apply {
                        this.active = it.MenuItemInfo.active
                        if (it.MenuItemInfo.priceOverride == null) {
                            this.price = null
                        } else {
                            this.price = it.MenuItemInfo.priceOverride
                        }
                    }
                    menuAdapter.listOfMenu = list
                }
                is MenuState.MenuItemInfo -> {
                    spinnerArrayList = listOf(Constants.CATEGORY_FILTER_ALL)
                    it.MenuItemInfo?.forEach { category ->
                        spinnerArrayList = spinnerArrayList + category.categoryName.toString()
                    }
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun setData(productsInfo: List<ProductsItem>? = null) {
        val listOfProduct =  ArrayList<ProductsItem>()
        listOfProduct.add(ProductsItem(productName = "Wild Leap Tail Chaser IPA 12oz", productId = 1, price = 800.0, active = 1))
        listOfProduct.add(ProductsItem(productName = "Tropicalia 12oz", productId = 1, price = 700.0, active = 1))
        listOfProduct.add(ProductsItem(productName = "Sierra Nevada 12oz", productId = 1, price = 700.0, active = 1))
        listOfProduct.add(ProductsItem(productName = "Scofflaw POG 16oz", productId = 1, price = 700.0, active = 1))
        listOfProduct.add(ProductsItem(productName = "Mango Cart 12oz", productId = 1, price = 700.0, active = 1))
        listOfProduct.add(ProductsItem(productName = "Georgia Field Party 12oz", productId = 1, price = 700.0, active = 1))
        val listOfMenu =  ArrayList<MenusItem>()
        listOfMenu.add(MenusItem(0,"Beer", products =  listOfProduct))
        listOfMenu.add(MenusItem(0,"Cocktails",products =  listOfProduct))
        listOfMenu.add(MenusItem(0,"Non-Alchoholic",products =  listOfProduct))
        listOfMenu.add(MenusItem(0,"Merchandise",products =  listOfProduct))
        spinnerArrayList = listOf(Constants.CATEGORY_FILTER_ALL)
        listOfMenu?.forEach { category ->
            spinnerArrayList = spinnerArrayList + category.categoryName.toString()
        }
        menuAdapter.listOfMenu = listOfProduct
//        menuAdapter.listOfMenu = productsInfo
    }

    @SuppressLint("SetTextI18n")
    private fun emptyMessageVisibility() {
        binding.emptyMessageAppCompatTextView.isVisible = true
        binding.emptyMessageAppCompatTextView.text = getString(R.string.no_added_any_one_menu)
    }

    @SuppressLint("ResourceType")
    private fun listenToViewEvent() {
        initAdapter()
        binding.allCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            binding.allCheckBox.isChecked = true
            binding.availableCheckBox.isChecked = false
            binding.unavailableCheckBox.isChecked = false
            onCheckboxClicked()
        }.autoDispose()
        binding.availableCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            binding.allCheckBox.isChecked = false
            binding.availableCheckBox.isChecked = true
            binding.unavailableCheckBox.isChecked = false
            onCheckboxClicked()
        }.autoDispose()
        binding.unavailableCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            binding.allCheckBox.isChecked = false
            binding.availableCheckBox.isChecked = false
            binding.unavailableCheckBox.isChecked = true
            onCheckboxClicked()
        }.autoDispose()

        category = getColoredSpanned(resources.getString(R.string.category), getColor(requireContext(), R.color.grey))
        selectedCategory = getColoredSpanned(resources.getString(R.string.all), getColor(requireContext(), R.color.black))
        binding.autoCompleteStatus.setText(Html.fromHtml("$category $selectedCategory"))
        binding.autoCompleteStatus.throttleClicks().subscribeAndObserveOnMainThread {
            val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, spinnerArrayList)
            binding.autoCompleteStatus.setAdapter(arrayAdapter)
            binding.autoCompleteStatus.showDropDown()
        }.autoDispose()
        binding.autoCompleteStatus.onItemClickListener = OnItemClickListener { parent, _, position, _ ->
            selectedCategory = getColoredSpanned(parent.getItemAtPosition(position).toString(), getColor(requireContext(), R.color.black))
            binding.autoCompleteStatus.setText(Html.fromHtml("$category $selectedCategory"))
            categorySelection = parent.getItemAtPosition(position).toString()
            menuViewModel.getMenuByLocation(isCheck, categorySelection)
        }
        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = true
            binding.relativeLayout.isVisible = false
            Observable.timer(2000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                onResume()
                binding.swipeRefreshLayout.isRefreshing = false
                binding.relativeLayout.isVisible = true
            }.autoDispose()
        }.autoDispose()
    }

    private fun initAdapter() {
        menuAdapter = MenuAdapter(requireContext()).apply {
            menuActionState.subscribeAndObserveOnMainThread {
                if (it.isActive == false) {
                    it.menuId?.let { item -> menuViewModel.updateMenuState(ProductStateRequest(false), item) }
                } else {
                    it.menuId?.let { item -> menuViewModel.updateMenuState(ProductStateRequest(true), item) }
                }
            }.autoDispose()
        }
        binding.rvMenuView.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.rvMenuView.apply {
            adapter = menuAdapter
        }
        menuAdapter.headerInfo = MenuSectionInfo(
            getString(R.string.product), getString(R.string.description), getString(R.string.price), getString(R.string.state)
        )
        setData()
    }

    private fun onCheckboxClicked() {
        isCheck = when {
            binding.allCheckBox.isChecked -> {
                Constants.CHECK_ALL
            }
            binding.availableCheckBox.isChecked -> {
                Constants.CHECK_AVAILABLE
            }
            binding.unavailableCheckBox.isChecked -> {
                Constants.CHECK_UNAVAILABLE
            }
            else -> {
                Constants.CHECK_ALL
            }
        }
        menuViewModel.getMenuByLocation(isCheck, categorySelection)
    }

    private fun onRefresh() {
        category = getColoredSpanned(resources.getString(R.string.category), getColor(requireContext(), R.color.grey))
        selectedCategory = getColoredSpanned(resources.getString(R.string.all), getColor(requireContext(), R.color.black))
        binding.autoCompleteStatus.setText(Html.fromHtml("$category $selectedCategory"))
        binding.allCheckBox.isChecked = true
        binding.availableCheckBox.isChecked = false
        binding.unavailableCheckBox.isChecked = false
        menuViewModel.getMenuByLocation(Constants.CHECK_ALL, Constants.CATEGORY_FILTER_ALL)
    }

    override fun onResume() {
        super.onResume()
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window, activity)
        onRefresh()
    }

    private fun getColoredSpanned(text: String, color: Int): String {
        return "<font color=$color>$text</font>"
    }
}