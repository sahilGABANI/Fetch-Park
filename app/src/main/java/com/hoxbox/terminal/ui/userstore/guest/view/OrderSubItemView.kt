package com.hoxbox.terminal.ui.userstore.guest.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.order.model.OptionsItem
import com.hoxbox.terminal.api.userstore.model.OptionsItemRequest
import com.hoxbox.terminal.api.userstore.model.SubOrderItemData
import com.hoxbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hoxbox.terminal.base.extension.showToast
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.throttleClicks
import com.hoxbox.terminal.databinding.OrderSubitemLayoutBinding
import com.hoxbox.terminal.utils.doOnCollapse
import com.hoxbox.terminal.utils.doOnExpand
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class OrderSubItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: OrderSubitemLayoutBinding? = null
    private lateinit var subProductAdapter: SubProductAdapter
    private var groupBy = 0

    private val subProductStateSubject: PublishSubject<SubOrderItemData> = PublishSubject.create()
    val subProductActionState: Observable<SubOrderItemData> = subProductStateSubject.hide()
    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.order_subitem_layout, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = OrderSubitemLayoutBinding.bind(view)
    }

    @SuppressLint("SetTextI18n")
    fun bind(subOrderItemData: SubOrderItemData, position: Int) {
        binding?.apply {
            subOrderItemData.modifiers?.get(position)?.groupBy?.let {
                groupBy = it
            }

//            subProductNameAppCompatTextView.text = "${position + 1}.) ".plus(subOrderItemData.modifiers?.get(position)?.modificationText)
            subProductNameAppCompatTextView.text = subOrderItemData.modifiers?.get(position)?.modificationText
            subProductNumberAppCompatTextView.text = (position + 1).toString()
            if(subOrderItemData.modifiers?.get(position)?.isRequired == 1) {
                leftOptionCompatTextView.isVisible = true
                val listOfSelectedOption = subOrderItemData.modifiers[position].options?.filter { it.isCheck }
                var quntity = 0
                listOfSelectedOption?.forEach {
                    quntity += it.optionQuantity ?: 1
                }
                leftOptionCompatTextView.text  = "$quntity Selected"
            }
            subOrderItemData.optionImage?.let {
                subProductNameAppCompatTextView.isSelected = true
                subProductNumberAppCompatTextView.isSelected = true
                productImageView.isVisible = true
                Glide.with(context).load(it).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(productImageView)
            }
            expandable.doOnCollapse {
                subProductNameAppCompatTextView.isSelected = true
                subProductNumberAppCompatTextView.isSelected = true
                downArrowImageView.isSelected = false
            }
            expandable.doOnExpand {
                downArrowImageView.isSelected = true
            }
            dropDownMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
                if (expandable.isExpanded) {
                    downArrowImageView.isSelected = false
                    expandable.collapse()
                } else {
                    downArrowImageView.isSelected = true
                    expandable.expand()
                }
            }.autoDispose()
            if ((subOrderItemData.modifiers?.get(position)?.options?.size ?: 0) <= 2) {
                dropDownMaterialCardView.isVisible = false
                expandable.expand()
            } else {
                dropDownMaterialCardView.isVisible = true
                expandable.collapse()
            }
            subProductAdapter = SubProductAdapter(context).apply {
                subProductActionState.subscribeAndObserveOnMainThread { item ->
                    if (subOrderItemData.modifiers?.get(position)?.selectMax == 1) {
                        val listofOption = subProductAdapter.listOfSubProduct
                        listofOption?.filter { it.isCheck }?.forEach {
                            it.isCheck = false
                        }
                        listofOption?.find { it.id == item.id }?.apply {
                            isCheck = true
                        }
                        subProductAdapter.listOfSubProduct = listofOption
                        if(subOrderItemData.modifiers?.get(position)?.isRequired == 1) {
                            leftOptionCompatTextView.isVisible = true
                            val listOfSelectedOption = listofOption?.filter { it.isCheck }
                            var quntity = 0
                            listOfSelectedOption?.forEach {
                                quntity += it.optionQuantity ?: 1
                            }
                            leftOptionCompatTextView.text  = "$quntity Selected"
                        }
                    } else {
                        val listOfOption = subProductAdapter.listOfSubProduct
                        if (item.isCheck) {
                            listOfOption?.find { it.id == item.id }?.apply {
                                isCheck = false
                            }
                        } else {
                            val listOfSelectedOption = subProductAdapter.listOfSubProduct?.filter { it.isCheck }
                            if ((listOfSelectedOption?.size ?: 0) < subOrderItemData.modifiers?.get(position)?.selectMax!!) {
                                listOfOption?.find { it.id == item.id }?.apply {
                                    isCheck = !isCheck
                                }
                            } else {
                                context.showToast("you have select maximum option")
                            }
                        }
                        subProductAdapter.listOfSubProduct = listOfOption
                        if(subOrderItemData.modifiers?.get(position)?.isRequired == 1) {
                            leftOptionCompatTextView.isVisible = true
                            val listOfSelectedOption = listOfOption?.filter { it.isCheck }
                            var quntity = 0
                            listOfSelectedOption?.forEach {
                                quntity += it.optionQuantity ?: 1
                            }

                            leftOptionCompatTextView.text  = "$quntity Selected"
                        }
                    }
                    subProductStateSubject.onNext(subOrderItemData)
                }.autoDispose()

                subProductQuantitySubscriptionActionState.subscribeAndObserveOnMainThread {
                    val listofOption = subProductAdapter.listOfSubProduct
                    val quantity = quantityCount(listofOption)
                    subProductAdapter.selectedOptionQuantity = quantity
                    subProductAdapter.listOfSubProduct = listofOption
                    if(subOrderItemData.modifiers?.get(position)?.isRequired == 1) {
                        leftOptionCompatTextView.isVisible = true
                        val listOfSelectedOption = subProductAdapter.listOfSubProduct?.filter { it.isCheck }
                        var quntity = 0
                        listOfSelectedOption?.forEach {
                            quntity += it.optionQuantity ?: 1
                        }
                        leftOptionCompatTextView.text  = "$quntity Selected"
                    }
                    subProductStateSubject.onNext(subOrderItemData)
                }.autoDispose()


                subProductQuantityAdditionActionState.subscribeAndObserveOnMainThread {
                    val listofOption = subProductAdapter.listOfSubProduct
                    val quantity = quantityCount(listofOption)
                    subProductAdapter.selectedOptionQuantity = quantity
                    println("quantity :$quantity")
                    subProductAdapter.listOfSubProduct = listofOption
                    subProductStateSubject.onNext(subOrderItemData)
                    if(subOrderItemData.modifiers?.get(position)?.isRequired == 1) {
                        leftOptionCompatTextView.isVisible = true
                        val listOfSelectedOption = subProductAdapter.listOfSubProduct?.filter { it.isCheck }
                        var quntity = 0
                        listOfSelectedOption?.forEach {
                            quntity += it.optionQuantity ?: 1
                        }
                        leftOptionCompatTextView.text = "$quntity Selected"
                    }
                }.autoDispose()
            }
            rvSubItem.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            rvSubItem.adapter = subProductAdapter
            subOrderItemData.subProductList?.forEach { it.maximumSelectOption = subOrderItemData.modifiers?.get(position)?.selectMax }
            subProductAdapter.listOfSubProduct = subOrderItemData.subProductList
            subProductAdapter.groupBy = groupBy

            if ((subOrderItemData.subProductList?.size ?: 0) <= 2) {
                dropDownMaterialCardView.isVisible = false
                expandable.expand()
            } else {
                dropDownMaterialCardView.isVisible = true
                expandable.collapse()
            }
        }
    }

    fun quantityCount(listofOption: List<OptionsItem>?): Int {
        var quantity = 0
        listofOption?.forEach { it.optionQuantity?.let { it1 -> quantity = quantity.plus(it1) } }
        return quantity
    }
}