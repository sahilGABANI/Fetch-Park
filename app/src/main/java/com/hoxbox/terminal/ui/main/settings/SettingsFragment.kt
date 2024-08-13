package com.hoxbox.terminal.ui.main.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseFragment
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.throttleClicks
import com.hoxbox.terminal.databinding.FragmentSettingsBinding
import com.hoxbox.terminal.helper.BohPrinterHelper
import com.hoxbox.terminal.helper.TestPrintHelper
import timber.log.Timber
import javax.inject.Inject

class SettingsFragment : BaseFragment() {
    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var bohPrinterHelper: BohPrinterHelper
    private lateinit var printer: Printer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        val bohPrintAddress = loggedInUserCache.getLocationInfo()?.bohPrintAddress
        val fohPrintAddress = loggedInUserCache.getLocationInfo()?.printAddress
        fohOrBohSelection(false)
        binding.fohSelectLinear.throttleClicks().subscribeAndObserveOnMainThread {
//            fohOrBohSelection(true)
        }.autoDispose()
        binding.bohSelectLinear.throttleClicks().subscribeAndObserveOnMainThread {
            fohOrBohSelection(false)
        }.autoDispose()

        binding.fohPrint.printReceiptButton.throttleClicks().subscribeAndObserveOnMainThread {
            printerInitialize()
            if (fohPrintAddress != null) {
                TestPrintHelper(requireContext(), requireActivity()).runPrintBOHReceiptSequence(printer, fohPrintAddress)
            }
        }.autoDispose()

        binding.bohPrint.printReceiptButton.throttleClicks().subscribeAndObserveOnMainThread {
            printerInitialize()
            if (bohPrintAddress != null) {
                TestPrintHelper(requireContext(), requireActivity()).runPrintBOHReceiptSequence(printer, bohPrintAddress)
            }
        }.autoDispose()
    }

    private fun printerInitialize() {
        try {
            printer = Printer(Printer.TM_T88, Printer.MODEL_ANK, requireContext())
        } catch (e: Epos2Exception) {
            Timber.e(e)
        }
    }

    private fun fohOrBohSelection(isPhysical: Boolean) {
        binding.fohSelectLinear.isSelected = isPhysical
        binding.fohTextview.isSelected = isPhysical
        binding.fohPrint.root.isVisible = isPhysical
        binding.bohSelectLinear.isSelected = !isPhysical
        binding.bohTextview.isSelected = !isPhysical
        binding.bohPrint.root.isVisible = !isPhysical
    }


}