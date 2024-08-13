package com.hoxbox.terminal.ui.userstore.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.userstore.model.CartItem
import com.hoxbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.throttleClicks
import com.hoxbox.terminal.base.extension.toDollar
import com.hoxbox.terminal.databinding.ViewCartItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class CartItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: ViewCartItemBinding? = null
    private lateinit var subItemAdapter: SubItemAdapter
    private var productQuantity = 1

    private val userStoreCartStateSubject: PublishSubject<CartItem> = PublishSubject.create()
    val userStoreCartActionState: Observable<CartItem> = userStoreCartStateSubject.hide()

    private val userStoreCartQuantitySubscriptionStateSubject: PublishSubject<CartItem> = PublishSubject.create()
    val userStoreCartQuantitySubscriptionActionState: Observable<CartItem> = userStoreCartQuantitySubscriptionStateSubject.hide()

    private val deleteCartItemStateSubject: PublishSubject<CartItem> = PublishSubject.create()
    val deleteCartItemActionState: Observable<CartItem> = deleteCartItemStateSubject.hide()


    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.view_cart_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewCartItemBinding.bind(view)
    }

    @SuppressLint("CutPasteId")
    fun bind(cartItem: CartItem) {
        binding?.apply {
            productNameTextView.text = cartItem.productName
            cartItem.menuItemQuantity?.let { productQuantity = it }

            val productQuantity = cartItem.menuItemQuantity
            productQuantityAppCompatTextView.text = productQuantity.toString()
            tvProductQuantity.text = productQuantity.toString()
            if (productQuantity != 0 && productQuantity != 1) {
                productPrize.text = (productQuantity?.let { cartItem.menuItemPrice?.times(it) })?.div(100).toDollar()
                tvProductPrize.text = (productQuantity?.let { cartItem.menuItemPrice?.times(it) })?.div(100).toDollar()
            } else {
                productPrize.text = (cartItem.menuItemPrice)?.div(100).toDollar()
                tvProductPrize.text = (cartItem.menuItemPrice)?.div(100).toDollar()
            }
            Glide.with(context).load(cartItem.productImage).into(productImageView)
            downArrowBackgroundMaterialCardView.isVisible = false
            if (cartItem.menuItemModifiers != null) {
                downArrowBackgroundMaterialCardView.isVisible = true
            }
            subItemAdapter = SubItemAdapter(context)
            cartItem.menuItemInstructions?.let {
                specialTextRelativeLayout.isVisible = true
                orderSpecialInstructionsAppCompatTextView.text = it
            }
            modificationLinearLayout.removeAllViews()
            cartItem.menuItemModifiers?.forEach { item ->
                item.modificationText?.let {
                    modificationLinearLayout.isVisible = true
                    val v: View = View.inflate(context, R.layout.modification_text_view, null)
                    v.findViewById<AppCompatTextView>(R.id.productTextview).text = "$it"
                    v.findViewById<AppCompatTextView>(R.id.productTextDescription).isVisible = false
                    modificationLinearLayout.addView(v)
                }
                item.options?.forEach {
                    if (it.optionPrice != 0.0 && it.optionPrice != null) {
                        val v: View = View.inflate(context, R.layout.modification_text_view, null)
                        v.findViewById<AppCompatTextView>(R.id.productTextview).isVisible = false
                        v.findViewById<AppCompatTextView>(R.id.productPriceTextView).isVisible = true
                        if (it.modifierQyt != null) {
                            v.findViewById<AppCompatTextView>(R.id.productTextDescription).text = "- ${it.optionName} (${it.modifierQyt})"
                        } else {
                            v.findViewById<AppCompatTextView>(R.id.productTextDescription).text = "- ${it.optionName}"
                        }
                        v.findViewById<AppCompatTextView>(R.id.productPriceTextView).text = it.optionPrice.div(100).toDollar()
                        modificationLinearLayout.addView(v)
                    } else {
                        val v: View = View.inflate(context, R.layout.modification_text_view, null)
                        v.findViewById<AppCompatTextView>(R.id.productTextview).isVisible = false
                        v.findViewById<AppCompatTextView>(R.id.productPriceTextView).isVisible = false
                        if (it.modifierQyt != null) {
                            v.findViewById<AppCompatTextView>(R.id.productTextDescription).text = "- ${it.optionName} (${it.modifierQyt})"
                        } else {
                            v.findViewById<AppCompatTextView>(R.id.productTextDescription).text = "- ${it.optionName}"
                        }
                        modificationLinearLayout.addView(v)
                    }
                }
            }
            subItemRecycleView.apply {
                adapter = subItemAdapter
            }
            subItemAdapter.listOfSubProductDetails = cartItem.menuItemModifiers?.get(0)?.options
            additionMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
                userStoreCartStateSubject.onNext(cartItem)
            }.autoDispose()
            subtractionMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
                userStoreCartQuantitySubscriptionStateSubject.onNext(cartItem)
            }.autoDispose()
            iconCloseAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                deleteCartItemStateSubject.onNext(cartItem)
            }.autoDispose()
            downArrowBackgroundMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
                if (expandable.isExpanded) {
                    downArrowImageView.isSelected = false
                    downArrowBackgroundMaterialCardView.isSelected = false
                    expandable.collapse()
                } else {
                    downArrowImageView.isSelected = true
                    downArrowBackgroundMaterialCardView.isSelected = true
                    expandable.expand()
                }
            }.autoDispose()
            if (cartItem.isChanging == true) {
                additionMaterialCardView.visibility = View.VISIBLE
                subtractionMaterialCardView.visibility = View.VISIBLE
                iconCloseAppCompatImageView.visibility = View.VISIBLE
                productPrize.visibility = View.VISIBLE
                productQuantityAppCompatTextView.visibility = View.VISIBLE
                tvProductQuantity.visibility = View.GONE
                tvProductPrize.visibility = View.GONE
                tvMultiply.visibility = View.GONE
            }
            if (cartItem.isChanging == false) {
                additionMaterialCardView.visibility = View.GONE
                subtractionMaterialCardView.visibility = View.GONE
                iconCloseAppCompatImageView.visibility = View.GONE
                productPrize.visibility = View.GONE
                productQuantityAppCompatTextView.visibility = View.GONE
                tvProductQuantity.visibility = View.VISIBLE
                tvProductPrize.visibility = View.VISIBLE
                tvMultiply.visibility = View.VISIBLE
            }
        }

    }

}