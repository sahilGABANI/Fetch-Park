package com.hoxbox.terminal.ui.userstore.guest.view

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.userstore.model.ModifiersItem
import com.hoxbox.terminal.api.userstore.model.OptionsItemRequest
import com.hoxbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hoxbox.terminal.base.extension.showToast
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.throttleClicks
import com.hoxbox.terminal.databinding.OrderSubitemLayoutBinding
import com.hoxbox.terminal.utils.doOnCollapse
import com.hoxbox.terminal.utils.doOnExpand

class SubProductItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: OrderSubitemLayoutBinding? = null
    private lateinit var subProductAdapter: SubProductAdapter


    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.order_subitem_layout, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = OrderSubitemLayoutBinding.bind(view)
    }

    fun bind(subOrderItemData: ModifiersItem) {
        binding?.apply {
            subOrderItemData.selectedOptionsItem.clear()
            subProductNumberAppCompatTextView.visibility = View.GONE
            subProductNameAppCompatTextView.text = subOrderItemData.modificationText
            dropDownMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
                if (expandable.isExpanded) {
                    downArrowImageView.isSelected = false
                    expandable.collapse()
                }
                else {
                    downArrowImageView.isSelected = true
                    expandable.expand()
                }
            }.autoDispose()

            expandable.doOnCollapse {
                subProductNameAppCompatTextView.isSelected = true
                subProductNumberAppCompatTextView.isSelected = true
                downArrowImageView.isSelected = false
            }
            expandable.doOnExpand {
                downArrowImageView.isSelected = true
            }
            subProductAdapter = SubProductAdapter(context).apply {
                subProductActionState.subscribeAndObserveOnMainThread { item ->
                    val list = subOrderItemData.selectedOptionsItem
                    if(subOrderItemData.selectMax == 1) {
                        val listofOption = subProductAdapter.listOfSubProduct
                        listofOption?.filter { it.isCheck }?.forEach {
                            it.isCheck = false
                            if(subOrderItemData.selectedOptionsItem.contains(OptionsItemRequest(it.optionPrice,it.modGroupId,it.active,it.optionName,it.id))) subOrderItemData.selectedOptionsItem!!.remove(OptionsItemRequest(it.optionPrice,it.modGroupId,it.active,it.optionName,it.id))
                        }
                        listofOption?.find { it.id == item.id }?.apply {
                            list.add(OptionsItemRequest(item.optionPrice,item.modGroupId,item.active,item.optionName,item.id))
                            subOrderItemData.selectedOption = 1
                            isCheck = true
                        }
                        subProductAdapter.listOfSubProduct = listofOption

                    }else {
                        val listOfOption = subProductAdapter.listOfSubProduct
                        if (item.isCheck) {
                            listOfOption?.find { it.id == item.id }?.apply {
                                isCheck = false
                                if(subOrderItemData.selectedOptionsItem.contains(OptionsItemRequest(item.optionPrice,item.modGroupId,item.active,item.optionName,item.id))) subOrderItemData.selectedOptionsItem!!.remove(OptionsItemRequest(item.optionPrice,item.modGroupId,item.active,item.optionName,item.id))
                            }
                        } else {
                            if (subOrderItemData.selectedOptionsItem.isEmpty()) {
                                if (subOrderItemData.selectedOptionsItem.size < subOrderItemData.selectMax!!) {
                                    listOfOption?.find { it.id == item.id }?.apply {
                                       isCheck = !isCheck
                                        if (isCheck) {
                                            subOrderItemData.selectedOptionsItem.add(OptionsItemRequest(item.optionPrice,item.modGroupId,item.active,item.optionName,item.id))
                                        } else {
                                            if(subOrderItemData.selectedOptionsItem.contains(OptionsItemRequest(item.optionPrice,item.modGroupId,item.active,item.optionName,item.id))) subOrderItemData.selectedOptionsItem!!.remove(OptionsItemRequest(item.optionPrice,item.modGroupId,item.active,item.optionName,item.id))
                                        }
                                    }
                                } else {
                                    context.showToast("you have select maximum option")
                                }
                            } else {
                                if (subOrderItemData.selectedOptionsItem.size < subOrderItemData.selectMax!!) {
                                    listOfOption?.find { it.id == item.id }?.apply {
                                        isCheck = !isCheck
                                        if (isCheck) {
                                            subOrderItemData.selectedOptionsItem.add(OptionsItemRequest(item.optionPrice,item.modGroupId,item.active,item.optionName,item.id))
                                        } else {
                                            if(subOrderItemData.selectedOptionsItem.contains(OptionsItemRequest(item.optionPrice,item.modGroupId,item.active,item.optionName,item.id))) subOrderItemData.selectedOptionsItem!!.remove(OptionsItemRequest(item.optionPrice,item.modGroupId,item.active,item.optionName,item.id))
                                        }
                                    }
                                } else {
                                    context.showToast("you have select maximum option")
                                }
                            }

                        }
                        subProductAdapter.listOfSubProduct = listOfOption
                    }

                }.autoDispose()
            }
            rvSubItem.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            rvSubItem.adapter = subProductAdapter
            subProductAdapter.listOfSubProduct = subOrderItemData.options
        }

    }
}