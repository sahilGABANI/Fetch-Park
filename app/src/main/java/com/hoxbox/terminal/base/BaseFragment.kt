package com.hoxbox.terminal.base

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import timber.log.Timber

abstract class BaseFragment : Fragment() {

    private val compositeDisposable = CompositeDisposable()

    lateinit var baseActivity: Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        context.let {
            baseActivity = (context as Activity)
        }
    }

    fun Disposable.autoDispose() {
        compositeDisposable.add(this)
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    @CallSuper
    open fun onBackPressed() {
        if (isAdded) {
            val view = baseActivity.currentFocus

        }
    }
}