package com.hoxbox.terminal.ui.userstore.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.menu.model.MenusItem
import com.hoxbox.terminal.api.userstore.model.LocationsItem
import com.hoxbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.throttleClicks
import com.hoxbox.terminal.databinding.MenuViewBinding
import com.hoxbox.terminal.databinding.ViewNearByLocationBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NearByLocationView(context: Context) : ConstraintLayoutWithLifecycle(context) {


    private var binding: ViewNearByLocationBinding? = null

    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.view_near_by_location, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewNearByLocationBinding.bind(view)
    }

    @SuppressLint("SetTextI18n")
    fun bind(locationsItem: LocationsItem, position: Int) {
        binding?.apply {
            menuTextView.text = (position+1).toString()
            tvLocation.text = locationsItem.locationName
            tvLocationAddress.text = locationsItem.getSafeAddressName()
        }
    }
}