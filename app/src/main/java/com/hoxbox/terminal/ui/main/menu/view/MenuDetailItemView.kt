package com.hoxbox.terminal.ui.main.menu.view

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.menu.model.ProductsItem
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hoxbox.terminal.base.extension.showToast
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.throttleClicks
import com.hoxbox.terminal.base.extension.toDollar
import com.hoxbox.terminal.databinding.MenuDetailItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class MenuDetailItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: MenuDetailItemBinding? = null

    private val menuStateSubject: PublishSubject<ProductsItem> = PublishSubject.create()
    val menuActionState: Observable<ProductsItem> = menuStateSubject.hide()
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.menu_detail_item, this)
        HotBoxApplication.component.inject(this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = MenuDetailItemBinding.bind(view)
    }

    fun bind(productsItem: ProductsItem) {
        binding?.apply {
            Glide.with(context).load(productsItem.productImage).placeholder(R.drawable.ic_app_logo).error(R.drawable.ic_app_logo)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(productImageView)
            productNameTextView.text = productsItem.productName
            productsItem.productDescription?.let {
                if (it != "") {
                    descriptionTextView.text = it
                } else {
                    descriptionTextView.text = resources.getString(R.string._text)
                }
            }
            if (productsItem.price != null) {
                priceTextView.text = ((productsItem.price)?.div(100)).toDollar()
            } else {
                priceTextView.text = ""
            }

            if (productsItem.active == 1) {
                productImageView.alpha = 1F
                descriptionTextView.setTextColor(resources.getColor(R.color.black))
                productNameTextView.setTextColor(resources.getColor(R.color.black))
                priceTextView.setTextColor(resources.getColor(R.color.red))
                menuStateTextView.setTextColor(resources.getColor(R.color.black))
                productNameTextView.isSelected = true
                menuStateTextView.text = resources.getText(R.string.available)
                stateSwitchCompat.isChecked = true
                productsItem.isActive = true
            } else {
                productImageView.alpha = 0.5F
                descriptionTextView.setTextColor(resources.getColor(R.color.grey))
                productNameTextView.setTextColor(resources.getColor(R.color.grey))
                priceTextView.setTextColor(resources.getColor(R.color.grey))
                menuStateTextView.setTextColor(resources.getColor(R.color.grey))
                menuStateTextView.text = resources.getText(R.string.off_unavailable)
                stateSwitchCompat.isChecked = false
                productsItem.isActive = false
            }
            stateSwitchCompat.throttleClicks().subscribeAndObserveOnMainThread {
                if (!loggedInUserCache.isAdmin()) {
                    context.showToast("This feature requires elevated permissions")
                    stateSwitchCompat.isChecked = productsItem.isActive == true
                } else {
                    menuStateSubject.onNext(productsItem)
                    if (stateSwitchCompat.isChecked) {
                        menuStateTextView.text = resources.getText(R.string.available)
                        productsItem.isActive = true
                    } else {
                        productsItem.isActive = false
                    }
                }
            }.autoDispose()
        }
    }

    fun bindGiftCard(productsItem: ProductsItem) {
        binding?.apply {
            Glide.with(context).load(productsItem.productImage).placeholder(R.drawable.ic_app_logo).error(R.drawable.ic_app_logo)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(productImageView)
            productNameTextView.text = productsItem.productName
            productsItem.productDescription?.let {
                if (it != "") {
                    descriptionTextView.text = it
                } else {
                    descriptionTextView.text = resources.getString(R.string._text)
                }
            }
            if (productsItem.dateCreated != null) {
                priceTextView.text = productsItem.dateCreated
                priceTextView.setTextColor(ContextCompat.getColor(context, R.color.color_666666))
            } else {
                priceTextView.text = ""
            }

            if (productsItem.active == 1) {
                productImageView.alpha = 1F
                descriptionTextView.setTextColor(resources.getColor(R.color.black))
                productNameTextView.setTextColor(resources.getColor(R.color.black))
                priceTextView.setTextColor(resources.getColor(R.color.color_777776))
                menuStateTextView.setTextColor(resources.getColor(R.color.black))
                productNameTextView.isSelected = true
                menuStateTextView.text = resources.getText(R.string.active)
                stateSwitchCompat.isChecked = true
                productsItem.isActive = true
            } else {
                productImageView.alpha = 0.5F
                descriptionTextView.setTextColor(resources.getColor(R.color.grey))
                productNameTextView.setTextColor(resources.getColor(R.color.grey))
                priceTextView.setTextColor(resources.getColor(R.color.grey))
                menuStateTextView.setTextColor(resources.getColor(R.color.grey))
                menuStateTextView.text = resources.getText(R.string.inactive)
                stateSwitchCompat.isChecked = false
                productsItem.isActive = false
            }

            stateSwitchCompat.throttleClicks().subscribeAndObserveOnMainThread {
                if (!loggedInUserCache.isAdmin()) {
                    context.showToast("This feature requires elevated permissions")
                    stateSwitchCompat.isChecked = productsItem.isActive == true
                } else {
                    menuStateSubject.onNext(productsItem)
                    if (stateSwitchCompat.isChecked) {
                        productImageView.alpha = 1F
                        descriptionTextView.setTextColor(resources.getColor(R.color.black))
                        productNameTextView.setTextColor(resources.getColor(R.color.black))
                        priceTextView.setTextColor(resources.getColor(R.color.color_777776))
                        menuStateTextView.setTextColor(resources.getColor(R.color.black))
                        productNameTextView.isSelected = true
                        menuStateTextView.text = resources.getText(R.string.active)
                        stateSwitchCompat.isChecked = true
                        productsItem.isActive = true
                    } else {
                        menuStateTextView.text = resources.getText(R.string.inactive)
                        productImageView.alpha = 0.5F
                        descriptionTextView.setTextColor(resources.getColor(R.color.grey))
                        productNameTextView.setTextColor(resources.getColor(R.color.grey))
                        priceTextView.setTextColor(resources.getColor(R.color.grey))
                        menuStateTextView.setTextColor(resources.getColor(R.color.grey))
                        menuStateTextView.text = resources.getText(R.string.inactive)
                        stateSwitchCompat.isChecked = false
                        productsItem.isActive = false
                    }
                }
            }.autoDispose()
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}