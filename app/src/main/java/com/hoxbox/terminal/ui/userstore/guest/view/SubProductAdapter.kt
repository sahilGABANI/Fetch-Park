package com.hoxbox.terminal.ui.userstore.guest.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hoxbox.terminal.api.order.model.OptionsItem
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SubProductAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val subProductStateSubject: PublishSubject<OptionsItem> = PublishSubject.create()
    val subProductActionState: Observable<OptionsItem> = subProductStateSubject.hide()
    private val subProductQuantitySubscriptionStateSubject: PublishSubject<OptionsItem> = PublishSubject.create()
    val subProductQuantitySubscriptionActionState: Observable<OptionsItem> = subProductQuantitySubscriptionStateSubject.hide()

    private val subProductQuantityAdditionStateSubject: PublishSubject<OptionsItem> = PublishSubject.create()
    val subProductQuantityAdditionActionState: Observable<OptionsItem> = subProductQuantityAdditionStateSubject.hide()
    private var adapterItems = listOf<AdapterItem>()

    var listOfSubProduct: List<OptionsItem>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    var groupBy : Int ?=  null
        set(value) {
            field = value
            updateAdapterItems()
        }

    var selectedOptionQuantity : Int ?=  null
        set(value) {
            field = value
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfSubProduct?.forEach {
            adapterItem.add(AdapterItem.OrderSubProductItem(it))
        }

        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.OrderSubProductItemViewType.ordinal -> {
                OrderSubProductItemViewHolder(SubProductView(context).apply {
                    subProductActionState.subscribe { subProductStateSubject.onNext(it) }
                    subProductQuantitySubscriptionActionState.subscribe { subProductQuantitySubscriptionStateSubject.onNext(it)  }
                    subProductQuantityAdditionActionState.subscribe { subProductQuantityAdditionStateSubject.onNext(it)  }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.OrderSubProductItem -> {
                (holder.itemView as SubProductView).bind(adapterItem.optionsItem, groupBy,selectedOptionQuantity)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class OrderSubProductItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class OrderSubProductItem(val optionsItem: OptionsItem) : AdapterItem(ViewType.OrderSubProductItemViewType.ordinal)
    }

    private enum class ViewType {
        OrderSubProductItemViewType
    }

}