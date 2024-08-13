package com.hoxbox.terminal.ui.userstore.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.hoxbox.terminal.base.BaseDialogFragment
import com.hoxbox.terminal.base.RxBus
import com.hoxbox.terminal.base.RxEvent
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.throttleClicks
import com.hoxbox.terminal.databinding.FragmentQrScannerBinding

class QrScannerFragment : BaseDialogFragment() {

    private var _binding: FragmentQrScannerBinding? = null
    private val binding get() = _binding!!
    private lateinit var codeScanner: CodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        val activity = requireActivity()
        codeScanner = CodeScanner(activity, binding.scannerView)

        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
        codeScanner.startPreview()
        codeScanner.decodeCallback = DecodeCallback {
            println("data :${it.text}")
            RxBus.publish(RxEvent.QRCodeText(it.text))
            dismiss()
            activity.runOnUiThread {
//                Toast.makeText(activity, it.text, Toast.LENGTH_LONG).show()
            }
        }
    }

}