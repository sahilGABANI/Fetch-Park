package com.hoxbox.terminal.ui.userstore.deliveryaddress.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hoxbox.terminal.api.userstore.model.FeaturesItem
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class DeliveryAddressAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()
    private val addressStateSubject: PublishSubject<FeaturesItem> = PublishSubject.create()
    val addressActionState: Observable<FeaturesItem> = addressStateSubject.hide()

    var listOfLocation: List<FeaturesItem>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }


    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfLocation?.forEach {
            adapterItem.add(AdapterItem.UserStoreCategoryItem(it))
        }

        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.NearByLocationViewType.ordinal -> {
                NearByLocationViewHolder(ViewDeliveryAddress(context).apply {
                    addressActionState.subscribe { addressStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.UserStoreCategoryItem -> {
                (holder.itemView as ViewDeliveryAddress).bind(adapterItem.locationsItem)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    sealed class AdapterItem(val type: Int) {
        data class UserStoreCategoryItem(val locationsItem: FeaturesItem) : AdapterItem(ViewType.NearByLocationViewType.ordinal)
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class NearByLocationViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private enum class ViewType {
        NearByLocationViewType
    }
}