package com.hoxbox.terminal.ui.main.orderdetail


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.view.isVisible
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.google.gson.Gson
import com.hoxbox.terminal.BuildConfig
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.order.model.OrderDetail
import com.hoxbox.terminal.application.HotBoxApplication
import com.hoxbox.terminal.base.BaseDialogFragment
import com.hoxbox.terminal.base.RxBus
import com.hoxbox.terminal.base.RxEvent
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.subscribeOnIoAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.throttleClicks
import com.hoxbox.terminal.databinding.PrintReceiptDialogBinding
import com.hoxbox.terminal.helper.*
import com.hoxbox.terminal.utils.UserInteractionInterceptor
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class PrintReceiptDialog : BaseDialogFragment() {

    private var _binding: PrintReceiptDialogBinding? = null
    private val binding get() = _binding!!

    private val REQUEST_PERMISSION = 100
    private val DISCONNECT_INTERVAL = 500
    private lateinit var bohPrinterHelper: BohPrinterHelper
    private lateinit var fohPrinterHelper: FohPrinterHelper

    private val TAG = PrintReceiptDialog::class.java.simpleName
    private val printReceiptSubject: PublishSubject<String> = PublishSubject.create()
    val printReceiptDismissed: Observable<String> = printReceiptSubject.hide()

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private lateinit var printer: Printer
    private lateinit var bohPrinter: Printer
    lateinit var orderDetails: OrderDetail

    companion object {
        const val INTENT_CART_GROUP = "Intent Cart Group"
        fun newInstance(messageInfo: OrderDetail?): PrintReceiptDialog {
            val args = Bundle()
            val gson = Gson()
            val json: String = gson.toJson(messageInfo)
            json.let { args.putString(INTENT_CART_GROUP, it) }
            val fragment = PrintReceiptDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = PrintReceiptDialogBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.decorView?.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewEvent()

    }

    private fun listenToViewEvent() {
        val productsDetails = arguments?.getString(INTENT_CART_GROUP)
        val gson = Gson()
        orderDetails = gson.fromJson(productsDetails, OrderDetail::class.java)
        val fohPrintAddress = loggedInUserCache.getLocationInfo()?.printAddress
        val bohPrintAddress = loggedInUserCache.getLocationInfo()?.bohPrintAddress
        binding.ipAddress.text = "FOH: $fohPrintAddress & BOH: $bohPrintAddress "
        binding.progressBar.isVisible = false
        try {
            printer = Printer(Printer.TM_T88, Printer.MODEL_ANK, requireContext())
        } catch (e: Epos2Exception) {
            showToast(e.message.toString())
        }
        try {
            bohPrinter = Printer(Printer.TM_T88, Printer.MODEL_ANK, requireContext())
        } catch (e: Epos2Exception) {
            showToast(e.message.toString())
        }

        binding.printBOHReceiptButton.throttleClicks().subscribeAndObserveOnMainThread {
            val t: Thread = object : Thread() {
                override fun run() {
                    if (bohPrintAddress != null) {
                        bohPrint(orderDetails,bohPrintAddress)
                    }
                }
            }
            t.start()
        }.autoDispose()

        binding.printFOHReceiptButton.throttleClicks().subscribeAndObserveOnMainThread {
            val currentTime = getCurrentsStoreTime().formatToStoreTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            if (loggedInUserCache.getLocationInfo()?.bothPrinterNotSame() == true) {
                val t: Thread = object : Thread() {
                    override fun run() {
                        if (fohPrintAddress != null) {
                            fohPrint(orderDetails,fohPrintAddress,currentTime)
                        }
                    }
                }
                t.start()
            } else {
                showToast("Foh & Boh Printer Address Is same")
            }
        }.autoDispose()

        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
    }
    fun bohPrint(orderDetail: OrderDetail, bohPrinterAddress: String) {
        bohPrinterHelper = BohPrinterHelper.getInstance(requireActivity())
        if (!bohPrinterHelper.isPrinterConnected() && bohPrinterAddress != null) {
            try {
                val isConnected = bohPrinterHelper.printerConnect(bohPrinterAddress)
                Timber.tag("AutoReceive").e("Printer connection response ========= $isConnected")
            } catch (e: java.lang.Exception) {
                Timber.tag("AutoReceive").e(e)
            }
        }
        if (BuildConfig.DEBUG) {
            try {
                bohPrinterHelper.runPrintBOHReceiptSequence(orderDetail, bohPrinterAddress)
            } catch (e: java.lang.Exception) {
                Timber.tag("runPrintBOHReceiptSequence").e(e)
            }
        }
        if (bohPrinterAddress != null && bohPrinterHelper.isPrinterConnected()) {
            try {
                bohPrinterHelper.runPrintBOHReceiptSequence(orderDetail, bohPrinterAddress)
            } catch (e: java.lang.Exception) {
                Timber.tag("runPrintBOHReceiptSequence").e(e)
            }
        } else {
            Timber.tag("AutoReceive").e("----------------- Printer not connected -----------------")
        }
    }

    fun fohPrint(orderDetail: OrderDetail, bohPrinterAddress: String, currentTime: String) {
        fohPrinterHelper = FohPrinterHelper.getInstance(requireActivity())
        if (!fohPrinterHelper.isPrinterConnected() && bohPrinterAddress != null) {
            try {
                val isConnected = fohPrinterHelper.printerConnect(bohPrinterAddress)
                Timber.tag("AutoReceive").e("Printer connection response ========= $isConnected")
            } catch (e: java.lang.Exception) {
                Timber.tag("AutoReceive").e(e)
            }
        }
        if (BuildConfig.DEBUG) {
            try {
                fohPrinterHelper.runPrintReceiptSequence(orderDetail, bohPrinterAddress,currentTime)
            } catch (e: java.lang.Exception) {
                Timber.tag("runPrintBOHReceiptSequence").e(e)
            }
        }
        if (bohPrinterAddress != null && fohPrinterHelper.isPrinterConnected()) {
            try {
                fohPrinterHelper.runPrintReceiptSequence(orderDetail, bohPrinterAddress,currentTime)
            } catch (e: java.lang.Exception) {
                Timber.tag("runPrintBOHReceiptSequence").e(e)
            }
        } else {
            Timber.tag("AutoReceive").e("----------------- Printer not connected -----------------")
        }
    }
    private fun spaceBetweenProductAndPrice(product: String, price: String): String {
        val l = "${product}${price}".length;
        if (l < 35) {
            val s = 35 - l;

            var space: String = ""
            for (i in 1..s) {
                space = "$space ";
            }
            return product.plus(space).plus(price)
        } else {
            val s = 35 - price.length;

            var space: String = ""
            for (i in 1..s) {
                space = "$space ";
            }
            return product.plus("\n$space").plus(price)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            printer.disconnect()
        } catch (e: Epos2Exception) {

        }
    }

    private fun connectFohPrinter(fohPrintAddress: String): Boolean {
        try {
            printer.connect("TCP:$fohPrintAddress", Printer.PARAM_DEFAULT)
        } catch (e: java.lang.Exception) {
            showToast(makeStatusMassage(printer.status))
            Timber.e(e, "connect : $e")
            return false
        }
        return true
    }

    private fun connectBohPrinter(fohPrintAddress: String): Boolean {
        try {
            bohPrinter.connect("TCP:$fohPrintAddress", Printer.PARAM_DEFAULT)
        } catch (e: java.lang.Exception) {
            showToast(makeStatusMassage(bohPrinter.status))
            Timber.e(e, "connect : $e")
            return false
        }
        return true
    }

    private fun makeStatusMassage(statusInfo: PrinterStatusInfo): String {
        var msg = ""
        msg += "connection:"
        when (statusInfo.connection) {
            Printer.TRUE -> msg += "CONNECT"
            Printer.FALSE -> msg += "DISCONNECT"
            Printer.UNKNOWN -> msg += "UNKNOWN"
            else -> {}
        }
        msg += "\n"
        msg += "online:"
        when (statusInfo.online) {
            Printer.TRUE -> msg += "ONLINE"
            Printer.FALSE -> msg += "OFFLINE"
            Printer.UNKNOWN -> msg += "UNKNOWN"
            else -> {}
        }
        return msg
    }


    override fun onResume() {
        super.onResume()
        UserInteractionInterceptor.wrapWindowCallback(requireActivity().window, activity)
        RxBus.listen(RxEvent.DismissedPrinterDialog::class.java).subscribeOnIoAndObserveOnMainThread({
            printReceiptSubject.onNext("")
        }, {
            Timber.e(it)
        }).autoDispose()
    }

}