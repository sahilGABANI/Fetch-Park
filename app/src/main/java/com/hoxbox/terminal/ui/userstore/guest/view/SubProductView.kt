package com.hoxbox.terminal.ui.userstore.guest.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.order.model.OptionsItem
import com.hoxbox.terminal.api.userstore.model.CartItem
import com.hoxbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hoxbox.terminal.base.extension.showToast
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.throttleClicks
import com.hoxbox.terminal.base.extension.toDollar
import com.hoxbox.terminal.databinding.LayoutSubProductSelectedBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SubProductView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val subProductStateSubject: PublishSubject<OptionsItem> = PublishSubject.create()
    val subProductActionState: Observable<OptionsItem> = subProductStateSubject.hide()

    private val subProductQuantitySubscriptionStateSubject: PublishSubject<OptionsItem> = PublishSubject.create()
    val subProductQuantitySubscriptionActionState: Observable<OptionsItem> = subProductQuantitySubscriptionStateSubject.hide()


    private val subProductQuantityAdditionStateSubject: PublishSubject<OptionsItem> = PublishSubject.create()
    val subProductQuantityAdditionActionState: Observable<OptionsItem> = subProductQuantityAdditionStateSubject.hide()

    private var binding: LayoutSubProductSelectedBinding? = null

    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.layout_sub_product_selected, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = LayoutSubProductSelectedBinding.bind(view)
    }

    fun bind(subProductInfo: OptionsItem, groupBy: Int?, selectedOptionQuantity: Int?) {
        binding?.apply {
            if (subProductInfo.optionPrice != null) {
                val optionPrice =  subProductInfo.optionPrice.div(100).toDollar()
                itemNameTextView.text = subProductInfo.optionName.plus(" (${optionPrice})")
            } else {
                itemNameTextView.text = subProductInfo.optionName
            }


            itemSelectedRadioButton.isChecked = true
            println("groupBy :$groupBy")
            if (groupBy != 0 && groupBy != null) {
                itemSelectedRadioButton.isVisible = false
                llGroupBy.isVisible = true
                productQuantityAppCompatTextView.text = groupBy.let { subProductInfo.productQuantity!!.times(it).toString() }
                subProductInfo.optionQuantity = groupBy.let { it1 -> subProductInfo.productQuantity!!.times(it1) }
                println("product Quantity : ${subProductInfo.productQuantity} \n option Quantity : ${subProductInfo.optionQuantity}")
                additionMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
                    if (subProductInfo.maximumSelectOption != selectedOptionQuantity) {
                        subProductInfo.productQuantity = subProductInfo.productQuantity?.plus(1)
                        subProductInfo.isCheck = true
                        subProductInfo.optionQuantity = groupBy.let { it1 -> subProductInfo.productQuantity!!.times(it1) }
                        subProductQuantitySubscriptionStateSubject.onNext(subProductInfo)
                    } else {
                        context.showToast("You have selected the maximum options")
                    }

                }.autoDispose()
                subtractionMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
                    when (subProductInfo.productQuantity) {
                        0 -> {
                            context.showToast("this option is not selected")
                        }
                        1 -> {
                            subProductInfo.isCheck = false
                            subProductInfo.productQuantity = 0
                            productQuantityAppCompatTextView.text = groupBy.let { subProductInfo.productQuantity!!.times(it).toString() }
                            subProductInfo.optionQuantity = groupBy.let { subProductInfo.productQuantity!!.times(it) }
                            println("product Quantity : ${subProductInfo.productQuantity} \n option Quantity : ${subProductInfo.optionQuantity}")
                            subProductQuantityAdditionStateSubject.onNext(subProductInfo)
                        }
                        else -> {
                            subProductInfo.productQuantity = subProductInfo.productQuantity!! - 1
                            productQuantityAppCompatTextView.text = groupBy.let { subProductInfo.productQuantity!!.times(it).toString() }
                            subProductInfo.optionQuantity = groupBy.let { subProductInfo.productQuantity!!.times(it) }
                            println("product Quantity : ${subProductInfo.productQuantity} \n option Quantity : ${subProductInfo.optionQuantity}")
                            subProductQuantityAdditionStateSubject.onNext(subProductInfo)
                        }
                    }
                }.autoDispose()
            } else {
                llGroupBy.isVisible = false
                subProductInfo.productQuantity = 0
                subProductInfo.optionQuantity = 1
                itemSelectedRadioButton.isChecked = false
            }
            itemSelectedRadioButton.isChecked = subProductInfo.isCheck

            if (subProductInfo.optionImage != null) {
                subProductImageView.isVisible = true
                Glide.with(context).load(subProductInfo.optionImage)
                    .placeholder(R.drawable.demo_box_img)
                    .error(R.drawable.demo_box_img)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(subProductImageView)
            }

            itemSelectedRadioButton.throttleClicks().subscribeAndObserveOnMainThread {
                subProductStateSubject.onNext(subProductInfo)
            }.autoDispose()
        }

    }
}