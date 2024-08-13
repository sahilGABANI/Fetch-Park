package com.hoxbox.terminal.ui.userstore.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hoxbox.terminal.api.userstore.model.CartItem
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class CartAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val userStoreCartStateSubject: PublishSubject<CartItem> = PublishSubject.create()
    val userStoreCartActionState: Observable<CartItem> = userStoreCartStateSubject.hide()

    private val userStoreCartQuantitySubscriptionStateSubject: PublishSubject<CartItem> = PublishSubject.create()
    val userStoreCartQuantitySubscriptionActionState: Observable<CartItem> = userStoreCartQuantitySubscriptionStateSubject.hide()

    private val deleteCartItemStateSubject: PublishSubject<CartItem> = PublishSubject.create()
    val deleteCartItemActionState: Observable<CartItem> = deleteCartItemStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfProductDetails: List<CartItem>? = null
        set(value) {
            field = value
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()

        listOfProductDetails?.forEach {
            adapterItem.add(AdapterItem.UserStoreProductCartItem(it))
//            if (it.isChanging == true) {
//                listOfProductDetails?.forEach {
//                    adapterItem.add(AdapterItem.UserStoreProductCartItem(it))
//                }
//            } else {
//                listOfProductDetails?.forEach {
//                    adapterItem.add(AdapterItem.UserStoreProductChangeItem(it))
//                }
//            }
        }



        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.UserStoreCartViewType.ordinal -> {
                UserStoreCartViewHolder(CartItemView(context).apply {
                    userStoreCartActionState.subscribe { userStoreCartStateSubject.onNext(it) }
                    userStoreCartQuantitySubscriptionActionState.subscribe { userStoreCartQuantitySubscriptionStateSubject.onNext(it) }
                    deleteCartItemActionState.subscribe { deleteCartItemStateSubject.onNext(it) }
                })
            }
            ViewType.UserStoreChangeViewType.ordinal -> {
                UserStoreCartViewHolder(CartChangeView(context))
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.UserStoreProductCartItem -> {
                (holder.itemView as CartItemView).bind(adapterItem.cartInfo)
            }
            is AdapterItem.UserStoreProductChangeItem -> {
                (holder.itemView as CartChangeView).bind(adapterItem.cartInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class UserStoreCartViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class UserStoreProductCartItem(val cartInfo: CartItem) :
            AdapterItem(ViewType.UserStoreCartViewType.ordinal)

        data class UserStoreProductChangeItem(val cartInfo: CartItem):
                AdapterItem(ViewType.UserStoreChangeViewType.ordinal)
    }


    private enum class ViewType {
        UserStoreCartViewType,
        UserStoreChangeViewType
    }
}