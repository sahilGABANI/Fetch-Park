package com.hoxbox.terminal.ui.userstore.deliveryaddress.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.userstore.model.FeaturesItem
import com.hoxbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.throttleClicks
import com.hoxbox.terminal.databinding.ViewDeliveryAddressBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ViewDeliveryAddress(context: Context) : ConstraintLayoutWithLifecycle(context) {


    private var binding: ViewDeliveryAddressBinding? = null
    private val addressStateSubject: PublishSubject<FeaturesItem> = PublishSubject.create()
    val addressActionState: Observable<FeaturesItem> = addressStateSubject.hide()
    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.view_delivery_address, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewDeliveryAddressBinding.bind(view)
    }

    @SuppressLint("SetTextI18n")
    fun bind(locationsItem: FeaturesItem) {
        binding?.apply {
            tvLocationAddress.text =  locationsItem.properties?.formatted.toString()
            tvLocationAddress.throttleClicks().subscribeAndObserveOnMainThread {
                addressStateSubject.onNext(locationsItem)
            }.autoDispose()
        }
    }
}