package com.hoxbox.terminal.ui.userstore.cookies.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.menu.model.ProductsItem
import com.hoxbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.throttleClicks
import com.hoxbox.terminal.base.extension.toDollar
import com.hoxbox.terminal.databinding.ViewUserStoreItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class UserStoreProductView(context: Context) : ConstraintLayoutWithLifecycle(context) {
    private val userStoreProductStateSubject: PublishSubject<ProductsItem> = PublishSubject.create()
    val userStoreProductActionState: Observable<ProductsItem> = userStoreProductStateSubject.hide()

    private lateinit var binding: ViewUserStoreItemBinding

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_user_store_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewUserStoreItemBinding.bind(view)
    }

    fun bind(productInfo: ProductsItem) {
        binding.apply {
            orderItemPrize.text = ((productInfo.price)?.div(100)).toDollar()
            productNameTextView.text = productInfo.productName
            Glide.with(context).load(productInfo.productImage).placeholder(R.drawable.demo_box_img).error(R.drawable.demo_box_img)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(productImageView)
            productSizeAndWeightTextView.text = productInfo.productDescription
            productDetailsLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                userStoreProductStateSubject.onNext(productInfo)
            }.autoDispose()
        }
    }
}