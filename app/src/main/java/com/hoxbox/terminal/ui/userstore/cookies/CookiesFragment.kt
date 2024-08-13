package com.hoxbox.terminal.ui.userstore.cookies

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hoxbox.terminal.api.menu.model.ProductsItem
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseFragment
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.base.extension.getViewModelFromFactory
import com.hoxbox.terminal.base.extension.showToast
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.databinding.FragmentCookiesBinding
import com.hoxbox.terminal.ui.userstore.cookies.view.UserStoreProductAdapter
import com.hoxbox.terminal.ui.userstore.guest.GuestProductDetailsDialogFragment
import com.hoxbox.terminal.ui.userstore.guest.TakeNBackDialogFragment
import com.hoxbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hoxbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import com.hoxbox.terminal.utils.UserInteractionInterceptor
import javax.inject.Inject

class CookiesFragment : BaseFragment() {

    private lateinit var productInfo: ProductsItem
    private var _binding: FragmentCookiesBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserStoreViewModel>
    private lateinit var userStoreViewModel: UserStoreViewModel

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var userStoreProductAdapter: UserStoreProductAdapter

        var listOfProduct: List<ProductsItem>? = null
            set(value) {
                field = value
                updateItems()
            }

        private fun updateItems() {
            if (this::userStoreProductAdapter.isInitialized) {
                userStoreProductAdapter.listOfProductDetails = listOfProduct?.filter { it.active == 1 && it.productActive == 1 }
            }
        }

        @JvmStatic
        fun newInstance() = CookiesFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        userStoreViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCookiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        initAdapter()
    }

    private fun initAdapter() {
        userStoreProductAdapter = UserStoreProductAdapter(requireContext()).apply {
            userStoreProductActionState.subscribeAndObserveOnMainThread {
                userStoreViewModel.getProductDetails(it.id, it.menuGroupId)
                productInfo = it
//                val takeNBackDialogFragment = TakeNBackDialogFragment()
//                takeNBackDialogFragment.show(parentFragmentManager, CookiesFragment::class.java.name)
            }.autoDispose()
        }
        binding.productDetailsRecycleView.apply {
            adapter = userStoreProductAdapter
        }
        updateItems()
    }

    private fun listenToViewModel() {
        userStoreViewModel.userStoreState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserStoreState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is UserStoreState.SuccessMessage -> {

                }
                is UserStoreState.SubProductState -> {
//                    println("productsItem : ${it.productsItem.modifiers?.size}")
//                    TakeNBackDialogFragment.listOfProduct = productInfo
//                    val takeNBackDialogFragment = TakeNBackDialogFragment()
//                    takeNBackDialogFragment.show(parentFragmentManager, CookiesFragment::class.java.name)
//                    if (it.productsItem.modifiers?.size == 0 || it.productsItem.modifiers?.size == 1) {
                    TakeNBackDialogFragment.listOfProduct = productInfo
                    val takeNBackDialogFragment = TakeNBackDialogFragment()
                    takeNBackDialogFragment.show(parentFragmentManager, CookiesFragment::class.java.name)
//                    } else {
//                        GuestProductDetailsDialogFragment.listOfProduct = productInfo
//                        val guestProductDetailsDialogFragment = GuestProductDetailsDialogFragment()
//                        guestProductDetailsDialogFragment.show(parentFragmentManager, CookiesFragment::class.java.name)
//                    }
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    override fun onResume() {
        super.onResume()
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window, activity)
    }
}