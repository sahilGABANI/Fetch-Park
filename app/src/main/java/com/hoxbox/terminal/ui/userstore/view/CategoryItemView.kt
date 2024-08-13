package com.hoxbox.terminal.ui.userstore.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.menu.model.MenusItem
import com.hoxbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.throttleClicks
import com.hoxbox.terminal.databinding.MenuViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class CategoryItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val userStoreCategoryStateSubject: PublishSubject<MenusItem> = PublishSubject.create()
    val userStoreCategoryActionState: Observable<MenusItem> = userStoreCategoryStateSubject.hide()

    private var binding: MenuViewBinding? = null

    init {
        initUi()
    }

    private fun initUi() {
        val view = View.inflate(context, R.layout.menu_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = MenuViewBinding.bind(view)
    }

    fun bind(menusItem: MenusItem, position: Int) {
        binding?.apply {
            if(menusItem.isSelected) {
                menuTextView.isSelected = true
                selectionView.isVisible = true
            } else {
                menuTextView.isSelected = false
                selectionView.isVisible = false
            }
            notificationCountTextView.isVisible = false
            menuTextView.text = menusItem.categoryName?.toUpperCase()
            menuConstraintLayout.throttleClicks().subscribeAndObserveOnMainThread {
                userStoreCategoryStateSubject.onNext(menusItem)
            }.autoDispose()
        }
    }
}