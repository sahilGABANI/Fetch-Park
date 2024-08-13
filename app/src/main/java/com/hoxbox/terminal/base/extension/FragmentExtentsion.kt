package com.hoxbox.terminal.base.extension

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

fun Fragment.showToast(message: String) {
    Toast.makeText(this.context, message, Toast.LENGTH_SHORT).show()
}

fun Fragment.showLongToast(message: String) {
    Toast.makeText(this.requireContext(), message, Toast.LENGTH_LONG).show()
}

inline fun <reified T : ViewModel> Fragment.getViewModelFromFactory(vmFactory: ViewModelProvider.Factory): T {
    return ViewModelProvider(this, vmFactory)[T::class.java]
}