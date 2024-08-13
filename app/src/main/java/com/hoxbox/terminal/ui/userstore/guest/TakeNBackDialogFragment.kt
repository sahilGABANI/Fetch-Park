package com.hoxbox.terminal.ui.userstore.guest

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.gson.Gson
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.menu.model.ProductsItem
import com.hoxbox.terminal.api.order.model.OptionsItem
import com.hoxbox.terminal.api.userstore.model.*
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseDialogFragment
import com.hoxbox.terminal.base.RxBus
import com.hoxbox.terminal.base.RxEvent
import com.hoxbox.terminal.base.ViewModelFactory
import com.hoxbox.terminal.base.extension.*
import com.hoxbox.terminal.databinding.FragmentTakeNBackDialogBinding
import com.hoxbox.terminal.ui.userstore.guest.view.OrderSubItemAdapter
import com.hoxbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hoxbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import com.hoxbox.terminal.utils.Constants
import com.hoxbox.terminal.utils.UserInteractionInterceptor
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class TakeNBackDialogFragment : BaseDialogFragment() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var orderSubItemAdapter: OrderSubItemAdapter
        var listOfProduct: ProductsItem? = null
            set(value) {
                field = value
                updateItems()
            }

        private fun updateItems() {

        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserStoreViewModel>
    private lateinit var userStoreViewModel: UserStoreViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var _binding: FragmentTakeNBackDialogBinding? = null
    private val binding get() = _binding!!
    private val subOrderItem = ArrayList<SubOrderItemData>()
    private var productQuantity = 1
    private var isValid: Boolean = false
    private var productsItem: ProductsItem? = null
    private var menuItemInstructions: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        userStoreViewModel = getViewModelFromFactory(viewModelFactory)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTakeNBackDialogBinding.inflate(inflater, container, false)
        dialog?.window?.decorView?.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewModel() {
        userStoreViewModel.userStoreState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserStoreState.LoadingState -> {
                }
                is UserStoreState.AddToCartProductResponse -> {
                    var cartGroupId = 0
                    it.addToCartResponse.cart?.cartGroupId?.let { cartGroupId = it }
                    loggedInUserCache.setLoggedInUserCartGroupId(cartGroupId)
                    RxBus.publish(RxEvent.EventCartGroupIdListen(cartGroupId))
                    this.dismiss()

                }
                is UserStoreState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is UserStoreState.SubProductState -> {
                    productsItem = it.productsItem
                    binding.addToCartMaterialButton.isEnabled = true
                    initUI(it.productsItem)
                }
                is UserStoreState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun initUI(productsItem: ProductsItem) {
        binding.productNameTextView.text = productsItem.productName
        binding.orderPrizeTextView.text = (productsItem.price)?.div(100).toDollar()
        binding.productDescriptionTextView.text = productsItem.productDescription
        Glide.with(requireContext()).load(productsItem.productImage).placeholder(R.drawable.demo_box_img).error(R.drawable.demo_box_img)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(binding.productImageView)
        if (productsItem.modifiers?.firstOrNull()?.selectMax != null) {
            binding.view.isVisible = true
            initAdapter()
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun listenToViewEvent() {
        binding.addOneMoreAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            val leftItemCount = (productsItem?.modifiers?.firstOrNull()?.selectMax?.minus(subOrderItem.size + 1)!!)
            if (leftItemCount == 0) {
                binding.leftItemNumberTextView.text = resources.getString(R.string.left, leftItemCount.toString())
                subOrderItem.add(
                    SubOrderItemData(
                        resources.getString(R.string.cookies), productsItem?.modifiers?.firstOrNull()?.options, null, null, productsItem?.modifiers
                    )
                )
                orderSubItemAdapter.listOfOrderSubItem = subOrderItem
                binding.leftItemNumberTextView.isVisible = false
                binding.addOneMoreAppCompatTextView.isVisible = false
            } else {
                binding.leftItemNumberTextView.text = resources.getString(R.string.left, leftItemCount.toString())
                subOrderItem.add(
                    SubOrderItemData(
                        resources.getString(R.string.cookies), productsItem?.modifiers?.firstOrNull()?.options, null, null, productsItem?.modifiers
                    )
                )
                orderSubItemAdapter.listOfOrderSubItem = subOrderItem
            }
        }.autoDispose()
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
        binding.additionMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            productQuantity++
            binding.orderPrizeTextView.text = ((listOfProduct?.price)?.div(100)?.times(productQuantity)).toDollar()
            binding.productQuantityAppCompatTextView.text = productQuantity.toString()
        }.autoDispose()
        binding.subtractionMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            if (productQuantity != 1) {
                productQuantity--
                binding.orderPrizeTextView.text = ((listOfProduct?.price)?.div(100)?.times(productQuantity)).toDollar()
                binding.productQuantityAppCompatTextView.text = productQuantity.toString()
            }
        }.autoDispose()
        binding.addToCartMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            addToCartProcess()
        }.autoDispose()
    }

    private fun initAdapter() {
        orderSubItemAdapter = OrderSubItemAdapter(requireContext()).apply {
            subProductActionState.subscribeAndObserveOnMainThread {
                println("priceCalculation ")
                priceCalculation(it.modifiers)
            }.autoDispose()
        }
        binding.rvCookies.apply {
            adapter = orderSubItemAdapter
        }
        orderSubItemAdapter.listOfOrderSubItem = getOrderSubItem()
    }

    private fun priceCalculation(modifiers: List<ModifiersItem>?) {
        var productPrice =  productsItem?.price
        modifiers?.forEach {
            it.options?.forEach {
                if (it.isCheck){
                    productPrice = productPrice?.plus(it.optionPrice ?: 0.0)
                }
            }
        }

        binding.orderPrizeTextView.text = productPrice?.div(100).toDollar()
    }

    private fun getOrderSubItem(): List<SubOrderItemData> {
        productsItem?.modifiers?.forEach {
            subOrderItem.add(SubOrderItemData(resources.getString(R.string.cookies), it.options, modifiers = productsItem?.modifiers))
        }

        return subOrderItem.toList()
    }

    private fun addToCartProcess() {
        val userId = loggedInUserCache.getLoggedInUserId()
        val locationId = loggedInUserCache.getLocationInfo()?.id
        val menuId = listOfProduct?.menuId
        val now = Calendar.getInstance()
        now.add(Calendar.MINUTE, 30)
        val df = SimpleDateFormat("yyy-MM-dd HH:mm:ss")
        val promisedTime = df.format(now.time)
        val selectMin: Int? = productsItem?.modifiers?.firstOrNull()?.selectMin
        val listOfModifiers = arrayListOf<MenuItemModifiersItemRequest>()
        val selectedOptionList = mutableListOf<OptionsItemRequest>()
        val addToCartRequest: AddToCartRequest
        menuItemInstructions = if (binding.specialInstructionsEditText.text.toString() == "") {
            null
        } else {
            binding.specialInstructionsEditText.text.toString()
        }

        if (productsItem?.modifiers?.size != 0) {
            orderSubItemAdapter.listOfOrderSubItem?.firstOrNull()?.let {
                it.modifiers?.forEach { it ->
                    println("it.options : ${Gson().toJson(it)}")
                    selectedOptionList.clear()
                    it.options?.forEach { it1 ->
                        if (it1.isCheck) {
                            selectedOptionList.add(
                                OptionsItemRequest(
                                    it1.optionPrice, it1.modGroupId, it1.active, it1.optionName, it1.id, it1.optionQuantity
                                )
                            )
                        }
                    }
                    val menuItemModifiersItem: MenuItemModifiersItemRequest = MenuItemModifiersItemRequest(
                        it.selectMax,
                        it.isRequired,
                        it.pmgActive,
                        it.productId,
                        selectedOptionList.toList(),
                        it.active,
                        it.groupBy,
                        it.id,
                        it.modificationText,
                        it.selectMin
                    )
                    menuItemModifiersItem.let { it1 ->
                        if (it1.options?.isNotEmpty() == true) {
                            listOfModifiers.add(it1)
                            if (it.selectMin != null && it.isRequired == 1) {
                                if (it.groupBy != 0 && it.groupBy != null){
                                    val quantity = quantityCount(selectedOptionList)
                                    if (quantity >= it.selectMin) {
                                        isValid = true
                                    } else {
                                        isValid = false
                                        showToast("Please Select Minimum $selectMin Option in ${it.modificationText}")
                                    }
                                }else {
                                    if (selectedOptionList.size >= it.selectMin && selectedOptionList.size <= it.selectMax!!) {
                                        isValid = true
                                    } else {
                                        isValid = false
                                        showToast("Please Select Minimum $selectMin Option in ${it.modificationText}")
                                    }
                                }

                            }
                        } else if (it.options?.isNotEmpty() == true){
                            if (it.selectMin != null && it.isRequired == 1) {
                                if (it.groupBy != 0 && it.groupBy != null){
                                    val quantity = quantityCount(selectedOptionList)
                                    if (quantity >= it.selectMin) {
                                        isValid = true
                                        listOfModifiers.add(it1)
                                    } else {
                                        isValid = false
                                        showToast("Please Select Minimum $selectMin Option in ${it.modificationText}")
                                    }
                                } else {
                                    if (selectedOptionList.size >= it.selectMin && selectedOptionList.size <= it.selectMax!!) {
                                        isValid = true
                                        listOfModifiers.add(it1)
                                    } else {
                                        isValid = false
                                        showToast("Please Select Minimum $selectMin Option in ${it.modificationText}")
                                    }
                                }
                            }
                        }
                    }

                }

            }

        } else {
            isValid = true
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
                    menuId
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

        if (isValid) {
            userStoreViewModel.addToCartProduct(addToCartRequest)
        }
    }


    fun quantityCount(listofOption: List<OptionsItemRequest>?): Int {
        var quantity = 0
        listofOption?.forEach { it.modifierQyt?.let { it1 -> quantity = quantity.plus(it1) } }
        println("quantity :$quantity")
        return quantity
    }

    private fun isValidate(): Boolean {
        return when {
            binding.specialInstructionsEditText.isFieldBlank() -> {
                showToast(getText(R.string.blank_special).toString())
                false
            }

            else -> true
        }
    }

    override fun onResume() {
        super.onResume()
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window, activity)
        userStoreViewModel.getProductDetails(listOfProduct?.id, listOfProduct?.menuGroupId)
    }
}