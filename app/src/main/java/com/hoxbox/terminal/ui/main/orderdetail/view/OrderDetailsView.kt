package com.hoxbox.terminal.ui.main.orderdetail.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.order.model.MenuItemModifiersItem
import com.hoxbox.terminal.api.order.model.OrderDetailItem
import com.hoxbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.throttleClicks
import com.hoxbox.terminal.base.extension.toDollar
import com.hoxbox.terminal.databinding.OrderItemLayoutBinding

class OrderDetailsView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private lateinit var binding: OrderItemLayoutBinding

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.order_item_layout, this)
        binding = OrderItemLayoutBinding.bind(view)
        binding.apply {
            downArrowBackgroundMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {

            }.autoDispose()
        }
    }

    @SuppressLint("SetTextI18n", "CutPasteId")
    fun bind(orderDetailsInfo: OrderDetailItem) {
        binding.apply {
            Glide.with(context).load(orderDetailsInfo.productImage).placeholder(R.drawable.ic_app_logo).error(R.drawable.ic_app_logo)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(productImageView)
            productQuantityTextview.text = "X ${orderDetailsInfo.menuItemQuantity.toString()}"
            productPrizeTextView.text = ((orderDetailsInfo.menuItemPrice)?.div(100)).toDollar()
            productNameTextView.text = orderDetailsInfo.productName
            orderDetailsInfo.menuItemModifiers?.firstOrNull()?.options?.let {
                downArrowBackgroundMaterialCardView.isVisible = true
            }
            cardAndBowLinearLayout.removeAllViews()
            orderDetailsInfo.menuItemModifiers?.forEach { item ->
                item.modificationText?.let {
                    cardAndBowLinearLayout.isVisible = true
                    item.modificationText.let {
                        cardAndBowLinearLayout.isVisible = true
                        val v: View = View.inflate(context, R.layout.modification_text_view, null)
                        v.findViewById<AppCompatTextView>(R.id.productTextview).text = "$it"
                        v.findViewById<AppCompatTextView>(R.id.productTextDescription).isVisible = false
                        v.findViewById<AppCompatTextView>(R.id.productPriceTextView).isVisible = false
                        cardAndBowLinearLayout.addView(v)
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
                            v.findViewById<AppCompatTextView>(R.id.productPriceTextView).text = "(${it.optionPrice.div(100).toDollar()})"
                            cardAndBowLinearLayout.addView(v)
                        } else {
                            val v: View = View.inflate(context, R.layout.modification_text_view, null)
                            v.findViewById<AppCompatTextView>(R.id.productTextview).isVisible = false
                            v.findViewById<AppCompatTextView>(R.id.productPriceTextView).isVisible = false
                            if (it.modifierQyt != null) {
                                v.findViewById<AppCompatTextView>(R.id.productTextDescription).text = "- ${it.optionName} (${it.modifierQyt})"
                            } else {
                                v.findViewById<AppCompatTextView>(R.id.productTextDescription).text = "- ${it.optionName}"
                            }
                            cardAndBowLinearLayout.addView(v)
                        }
                    }
                    cardAndBowLinearLayout.visibility = View.VISIBLE


                }
            }
            orderDetailsInfo.menuItemInstructions?.let {
                textSpecialLinear.isVisible = true
                orderSpecialInstructionsAppCompatTextView.text = orderDetailsInfo.menuItemInstructions.toString()
            }
        }
    }
}
