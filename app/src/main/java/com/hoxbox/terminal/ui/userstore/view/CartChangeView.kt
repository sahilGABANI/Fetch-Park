package com.hoxbox.terminal.ui.userstore.view

import android.content.Context
import android.view.View
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.userstore.model.CartItem
import com.hoxbox.terminal.base.ConstraintLayoutWithLifecycle

class CartChangeView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.view_cart_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    fun bind(cartInfo: CartItem) {

    }
}