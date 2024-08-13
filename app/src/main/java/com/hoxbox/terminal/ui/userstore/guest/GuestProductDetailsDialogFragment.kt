package com.hoxbox.terminal.ui.userstore.guest

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.menu.model.ProductsItem
import com.hoxbox.terminal.api.userstore.model.AddToCartRequest
import com.hoxbox.terminal.api.userstore.model.MenuItemModifiersItemRequest
import com.hoxbox.terminal.api.userstore.model.ModifiersItem
import com.hoxbox.terminal.api.userstore.model.OptionsItemRequest
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseDialogFragment
import com.hoxbox.terminal.base.RxBus
import com.hoxbox.terminal.base.RxEvent
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.base.extension.*
import com.hoxbox.terminal.databinding.FragmentGuestProductDetailsDialogBinding
import com.hoxbox.terminal.ui.userstore.guest.view.OrderSubProductAdapter
import com.hoxbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hoxbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import com.hoxbox.terminal.utils.Constants
import com.hoxbox.terminal.utils.UserInteractionInterceptor
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class GuestProductDetailsDialogFragment : BaseDialogFragment() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var orderSubItemAdapter: OrderSubProductAdapter
        var listOfProduct: ProductsItem? = null
            set(value) {
                field = value
                updateItems()
            }

        private fun updateItems() {

        }
    }

    private var _binding: FragmentGuestProductDetailsDialogBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserStoreViewModel>
    private lateinit var userStoreViewModel: UserStoreViewModel
    private var productsItem: ProductsItem? = null
    private var productQuantity = 1
    private var menuItemInstructions: String? = null

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        userStoreViewModel = getViewModelFromFactory(viewModelFactory)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuestProductDetailsDialogBinding.inflate(inflater, container, false)
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
        println()
        listenToViewEvent()
        listenToViewModel()
    }

    private fun listenToViewModel() {
        userStoreViewModel.userStoreState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserStoreState.ErrorMessage -> {
                    Toast.makeText(requireContext(), it.errorMessage, Toast.LENGTH_SHORT).show()
                }
                is UserStoreState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is UserStoreState.SubProductState -> {
                    productsItem = it.productsItem
                    setData(it.productsItem.modifiers)
                    initUI(it.productsItem)
                }
                is UserStoreState.AddToCartProductResponse -> {
                    var cartGroupId = 0
                    it.addToCartResponse.cart?.cartGroupId?.let { cartGroupId = it }
                    loggedInUserCache.setLoggedInUserCartGroupId(cartGroupId)
                    RxBus.publish(RxEvent.EventCartGroupIdListen(cartGroupId))
                    this.dismiss()
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun setData(orderSubItem: List<ModifiersItem>?) {
        orderSubItemAdapter.listOfOrderSubItem = orderSubItem
    }

    private fun listenToViewEvent() {
        initAdapter()
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
        binding.additionMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            productQuantity++
            binding.orderPrizeTextView.text = ((TakeNBackDialogFragment.listOfProduct?.price)?.div(100)?.times(productQuantity)).toDollar()
            binding.productQuantityAppCompatTextView.text = productQuantity.toString()
        }.autoDispose()
        binding.subtractionMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            if (productQuantity != 1) {
                productQuantity--
                binding.orderPrizeTextView.text = ((TakeNBackDialogFragment.listOfProduct?.price)?.div(100)?.times(productQuantity)).toDollar()
                binding.productQuantityAppCompatTextView.text = productQuantity.toString()
            }
        }.autoDispose()

        binding.addToCartMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (validation()) {
                addToCartProcess()
            } else {
                showToast("Please Select Item")
            }
        }.autoDispose()

    }

    private fun validation(): Boolean {
        val list = orderSubItemAdapter.listOfOrderSubItem
        val array = arrayListOf<Boolean>()
        var boolean = false
        list?.forEach {
            if (it.selectMin!! <= it.selectedOptionsItem?.size!!) {
                array.add(true)
            } else {
                array.add(false)
            }
        }
        for (i in 0 until array.size - 1) {
            if (array[i]) {
                boolean = true
            } else {
                boolean = false
                break
            }
        }
        return boolean
    }

    private fun addToCartProcess() {
        val listOfModifiers = arrayListOf<MenuItemModifiersItemRequest>()
        val addToCartRequest: AddToCartRequest
        val userId = loggedInUserCache.getLoggedInUserId()

        println("id :${loggedInUserCache.getLoggedInUserCartGroupId()}")
        val locationId = loggedInUserCache.getLocationInfo()?.id
        val menuId = listOfProduct?.menuId
        val now = Calendar.getInstance()
        now.add(Calendar.MINUTE, 30)
        val df = SimpleDateFormat("yyy-MM-dd HH:mm:ss")
        val selectedOptionList = mutableListOf<OptionsItemRequest>()
        val selectedOptionListPassAPi = mutableListOf<OptionsItemRequest>()
        val promisedTime = df.format(now.time)
        menuItemInstructions = if (binding.specialInstructionsEditText.text.toString() == "") {
            null
        } else {
            binding.specialInstructionsEditText.text.toString()
        }
        if (productsItem?.modifiers?.size != 0) {
            orderSubItemAdapter.listOfOrderSubItem?.forEach {
                if (!it.selectedOptionsItem.isNullOrEmpty()) {
                    listOfModifiers.add(
                        MenuItemModifiersItemRequest(
                            it.selectMax,
                            it.isRequired,
                            it.pmgActive,
                            it.productId,
                            it.selectedOptionsItem,
                            it.active,
                            it.groupBy,
                            it.id,
                            it.modificationText,
                            it.selectMin
                        )
                    )
                }
            }


        }
        if (loggedInUserCache.getLoggedInUserCartGroupId() == 0) {
            addToCartRequest = if (listOfModifiers.size != 0) {
                AddToCartRequest(
                    promisedTime,
                    menuItemInstructions,
                    Constants.ORDER_TYPE_ID,
                    userId,
                    Constants.MODE_ID,
                    productQuantity,
                    listOfModifiers,
                    locationId,
                    menuId,
                )

            } else {
                AddToCartRequest(
                    promisedTime, menuItemInstructions, Constants.ORDER_TYPE_ID, userId, Constants.MODE_ID, productQuantity, null, locationId, menuId
                )

            }
        } else {
            if (listOfModifiers.size != 0) {
                addToCartRequest = AddToCartRequest(
                    promisedTime,
                    menuItemInstructions,
                    Constants.ORDER_TYPE_ID,
                    userId,
                    Constants.MODE_ID,
                    productQuantity,
                    listOfModifiers,
                    locationId,
                    menuId,
                    loggedInUserCache.getLoggedInUserCartGroupId()
                )

            } else {
                addToCartRequest = AddToCartRequest(
                    promisedTime,
                    menuItemInstructions,
                    Constants.ORDER_TYPE_ID,
                    userId,
                    Constants.MODE_ID,
                    productQuantity,
                    null,
                    locationId,
                    menuId,
                    loggedInUserCache.getLoggedInUserCartGroupId()
                )
            }
        }
        userStoreViewModel.addToCartProduct(addToCartRequest)
    }

    private fun initUI(productsItem: ProductsItem) {
        binding.productNameTextView.text = productsItem.productName
        binding.orderPrizeTextView.text = (productsItem.price)?.div(100).toDollar()
        binding.productDescriptionTextView.text = productsItem.productDescription
        Glide.with(requireContext()).load(productsItem.productImage).placeholder(R.drawable.demo_box_img).error(R.drawable.demo_box_img)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(binding.productImageView)

    }

    private fun initAdapter() {
        orderSubItemAdapter = OrderSubProductAdapter(requireContext()).apply {

        }
        binding.rvCookies.apply {
            adapter = orderSubItemAdapter
        }

    }

    override fun onResume() {
        super.onResume()
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window, activity)
        userStoreViewModel.getProductDetails(listOfProduct?.id, listOfProduct?.menuGroupId)
    }

}