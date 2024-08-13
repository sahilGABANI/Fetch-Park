package com.hoxbox.terminal.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.text.SpannableStringBuilder
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.hoxbox.terminal.api.order.model.OrderDetail
import com.hoxbox.terminal.base.extension.showToast
import com.hoxbox.terminal.base.extension.toDollar
import com.hoxbox.terminal.base.extension.toMinusDollar
import com.hoxbox.terminal.utils.Constants
import timber.log.Timber

class FohPrinterHelper private constructor() {
    private lateinit var fohPrinter: Printer

    companion object {
        @Volatile
        private var instance: FohPrinterHelper? = null

        @SuppressLint("StaticFieldLeak")
        private lateinit var activity: Activity
        fun getInstance(activity: Activity): FohPrinterHelper {
            Companion.activity = activity
            return instance ?: synchronized(this) {
                instance ?: FohPrinterHelper().also { instance = it }
            }
        }
    }

    fun printerInitialize(context: Context) {
        try {
            fohPrinter = Printer(Printer.TM_T88, Printer.MODEL_ANK, context)
        } catch (e: Epos2Exception) {
            Timber.e(e)
            activity.runOnUiThread {
                context.showToast(e.message.toString())
            }
        }
    }

    fun printerConnect(bohPrintAddress: String?): Boolean {
        Timber.tag("Printer").i("BOH Connect Printer")
        if (bohPrintAddress != null){
            try {
                fohPrinter.connect("TCP:$bohPrintAddress", Printer.PARAM_DEFAULT)
            } catch (e: java.lang.Exception) {
                Timber.e(e, "connect : $e")
                return false
            }
            return true
        } else {
            return false
        }
    }

    fun isPrinterConnected(): Boolean {
        return fohPrinter.status.connection == 1
    }

    fun runPrintReceiptSequence(
        orderDetails: OrderDetail,
        fohPrintAddress: String,
        currentTime: String
    ): Boolean {
        val textData = SpannableStringBuilder("")
        if (orderDetails.locationName != null) {
            textData.append("HotBoxCookies (${orderDetails.locationName})\n")
        }
        if (!orderDetails.locationAddress1.isNullOrEmpty()) {
            textData.append("${orderDetails.locationAddress1}")
        }
        if (!orderDetails.locationAddress2.isNullOrEmpty()) {
            textData.append("${orderDetails.locationAddress2}")
        }
        if (!orderDetails.locationCity.isNullOrEmpty() && !orderDetails.locationState.isNullOrEmpty()) {
            textData.append("\n${orderDetails.locationCity},${orderDetails.locationState}")
        }
        if (!orderDetails.locationZip.isNullOrEmpty()) {
            textData.append(",${orderDetails.locationZip}")
        }
        textData.append("\n")
        if (!orderDetails.orderPromisedTime?.trim().isNullOrEmpty()) {
            textData.append(
                spaceBetweenProductAndPrice(
                    currentTime.toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("MMMM dd, yyyy").toString(),
                    currentTime.toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("hh:mm a").toString()
                )
            )
        }
        textData.append("\n------------------------------------------\n")
        if (orderDetails.id != 0 && orderDetails.id != null) {
            textData.append(spaceBetweenProductAndPrice(Constants.RECEIPT, "#${orderDetails.id}"))
        }
        textData.append("\n------------------------------------------\n")
        if (!orderDetails.items.isNullOrEmpty()) {
            orderDetails.items?.forEach {
                var productPrice: Double = 0.00
                if (it.menuItemPrice != null) {
                    productPrice = it.menuItemPrice ?: 0.00
                }
                it.menuItemModifiers?.forEach {
                    it.options?.forEach {
                        productPrice = productPrice.plus(it.optionPrice?.toInt() ?: 0)
                    }
                }
                textData.append("\n")
                if (!it.productName?.trim().isNullOrEmpty()) {
                    textData.append(
                        spaceBetweenProductAndPrice(
                            it.productName.toString().plus(" X ${it.menuItemQuantity}"), productPrice.div(100).toDollar()
                        )
                    )
                }
                if (!it.menuItemModifiers.isNullOrEmpty()) {
                    it.menuItemModifiers.forEach { item ->
                        item?.options?.forEach { item1 ->
                            if (item.options.firstOrNull()?.equals(item1) == true) {
                                if (item1.optionPrice != null && item1.optionPrice != 0.0 && !item1.optionName.isNullOrEmpty()) {
                                    textData.append("\n\t-${item1.optionName} ${(item1.optionPrice.toDouble().div(100) ?: 0).toDollar()}\n")
                                } else {
                                    if (!item1.optionName.isNullOrEmpty()) {
                                        textData.append("\n\t-${item1.optionName}\n")
                                    }
                                }
                            } else {
                                if (item1.optionPrice != null && item1.optionPrice != 0.0 && !item1.optionName.isNullOrEmpty()) {
                                    textData.append("\n\t-${item1.optionName} ${(item1.optionPrice.toDouble().div(100) ?: 0).toDollar()}\n")
                                } else {
                                    if (!item1.optionName.isNullOrEmpty()) {
                                        textData.append("\n\t-${item1.optionName}\n")
                                    }
                                }
                            }
                        }
                    }
                    it.menuItemInstructions?.trim()?.let {
                        textData.append("\nNote :$it\n")
                    }
                }
            }
        }
        textData.append("\n------------------------------------------\n")
        if (orderDetails.orderSubtotal != null) {
            textData.append(spaceBetweenProductAndPrice("Subtotal", orderDetails.orderSubtotal.div(100).toDollar()))
        }
        if (orderDetails.orderDeliveryFee != null && orderDetails.orderDeliveryFee != 0.00 && orderDetails.orderType == "Delivery") {
            textData.append("\n")
            textData.append(spaceBetweenProductAndPrice("Delivery Fee", orderDetails.orderDeliveryFee?.div(100).toDollar()))
        }
        if (orderDetails.orderTax != null) {
            textData.append("\n")
            textData.append(spaceBetweenProductAndPrice("Sales Tax", orderDetails.orderTax.div(100).toDollar()))
        }
        if (orderDetails.orderTip != null && orderDetails.orderTip != 0.00) {
            textData.append("\n")
            textData.append(spaceBetweenProductAndPrice("Tip", orderDetails.orderTip.div(100).toDollar()))
        }
        if (orderDetails.orderGiftCardAmount != null && orderDetails.orderGiftCardAmount != 0.00) {
            textData.append("\n")
            textData.append(
                spaceBetweenProductAndPrice(
                    "Gift Card", "-${(orderDetails.orderGiftCardAmount.div(100)).toMinusDollar().toDollar()}"
                )
            )
        }
        if (orderDetails.orderCouponCodeDiscount != null && orderDetails.orderCouponCodeDiscount != 0.00) {
            textData.append("\n")
            textData.append(
                spaceBetweenProductAndPrice(
                    "Promocode", "-${(orderDetails.orderCouponCodeDiscount.div(100)).toMinusDollar().toDollar()}"
                )
            )
        }
        textData.append("\n------------------------------------------\n")
        if (orderDetails.orderTotal != null) {
            textData.append(spaceBetweenProductAndPrice("Total", orderDetails.orderTotal?.div(100).toDollar()))
        }
        Timber.tag("OkHttpClient").i(textData.toString())
        if (!createReceiptData(orderDetails, currentTime)) {
            return false
        }
        return printData(fohPrintAddress)
    }

    private fun createReceiptData(
        orderDetails: OrderDetail, currentTime: String
    ): Boolean {
        if (fohPrinter == null) {
            return false
        }
        try {
            fohPrinter.clearCommandBuffer()
            fohPrinter.addTextLang(Printer.LANG_EN)
            fohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            if (orderDetails.locationName != null) {
                fohPrinter.addText("HotBoxCookies (${orderDetails.locationName})\n")
            }
            if (!orderDetails.locationAddress1.isNullOrEmpty()) {
                fohPrinter.addText("${orderDetails.locationAddress1}")
            }
            if (!orderDetails.locationAddress2.isNullOrEmpty()) {
                fohPrinter.addText("${orderDetails.locationAddress2}")
            }
            println("\n")
            if (!orderDetails.locationCity.isNullOrEmpty() && !orderDetails.locationState.isNullOrEmpty()) {
                fohPrinter.addText("\n${orderDetails.locationCity},${orderDetails.locationState}")
            }
            if (!orderDetails.locationZip.isNullOrEmpty()) {
                fohPrinter.addText(",${orderDetails.locationZip}")
            }
//        textData.append("\n${orderDetails.loc}")
            fohPrinter.addText("\n")
            if (!orderDetails.orderPromisedTime?.trim().isNullOrEmpty()) {
                fohPrinter.addText(
                    spaceBetweenProductAndPrice(
                        currentTime.toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("MMMM dd, yyyy").toString(),
                        currentTime.toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("hh:mm a").toString()
                    )
                )
            }
            fohPrinter.addText("\n------------------------------------------\n")
            if (orderDetails.id != 0 && orderDetails.id != null) {
                fohPrinter.addText(spaceBetweenProductAndPrice(Constants.RECEIPT, "#${orderDetails.id}"))
            }
//            if (orderDetails.orderTableNumber != 0 && orderDetails.orderTableNumber != null) {
//                printer.addText("\n")
//                printer.addText(spaceBetweenProductAndPrice(Constants.TABLE_NO, "${orderDetails.orderTableNumber}"))
//            }
            fohPrinter.addText("\n------------------------------------------\n")
            fohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            if (!orderDetails?.items.isNullOrEmpty()) {
                orderDetails.items?.forEach {
                    var productPrice: Double = 0.00
                    if (it.menuItemPrice != null) {
                        productPrice = it.menuItemPrice ?: 0.00
                    }
                    it.menuItemModifiers?.forEach {
                        it?.options?.forEach {
                            productPrice = productPrice.plus(it.optionPrice?.toInt() ?: 0)
                        }
                    }
                    fohPrinter.addText("\n")
                    if (!it.productName?.trim().isNullOrEmpty()) {
                        fohPrinter.addText(
                            spaceBetweenProductAndPrice(
                                it.productName.toString().plus(" X ${it.menuItemQuantity}"), productPrice.div(100).toDollar()
                            )
                        )
                    }
                    fohPrinter.addTextAlign(Printer.ALIGN_LEFT)
                    if (!it.menuItemModifiers.isNullOrEmpty()) {
                        it.menuItemModifiers.forEach { item ->
                            item?.options?.forEach { item1 ->
                                if (item.options.firstOrNull()?.equals(item1) == true) {
                                    if (item1.optionPrice != null && item1.optionPrice != 0.0 && !item1.optionName.isNullOrEmpty()) {
                                        fohPrinter.addText("\n\t-${item1.optionName} ${(item1.optionPrice.toDouble().div(100) ?: 0).toDollar()}\n")
                                    } else {
                                        if (!item1.optionName.isNullOrEmpty()) {
                                            fohPrinter.addText("\n\t-${item1.optionName}\n")
                                        }
                                    }
                                } else {
                                    if (item1.optionPrice != null && item1.optionPrice != 0.0 && !item1.optionName.isNullOrEmpty()) {
                                        fohPrinter.addText("\n\t-${item1.optionName} ${(item1.optionPrice.toDouble().div(100) ?: 0).toDollar()}\n")
                                    } else {
                                        if (!item1.optionName.isNullOrEmpty()) {
                                            fohPrinter.addText("\n\t-${item1.optionName}\n")
                                        }
                                    }
                                }
                            }
                        }
                        it.menuItemInstructions?.trim()?.let {
                            fohPrinter.addTextAlign(Printer.ALIGN_LEFT)
                            fohPrinter.addText("\nNote :$it\n")
                        }
                    }
                }
            }

            fohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            fohPrinter.addText("\n------------------------------------------\n")
            if (orderDetails.orderSubtotal != null) {
                fohPrinter.addText(spaceBetweenProductAndPrice("Subtotal", orderDetails.orderSubtotal.div(100).toDollar()))
            }
            if (orderDetails.orderDeliveryFee != null && orderDetails.orderDeliveryFee != 0.00 && orderDetails.orderType == "Delivery") {
                fohPrinter.addText("\n")
                fohPrinter.addText(spaceBetweenProductAndPrice("Delivery Fee", orderDetails.orderDeliveryFee.div(100).toDollar()))
            }
//            if (orderDetails.orderEmpDiscount != null && orderDetails.orderEmpDiscount != 0.00) {
//                printer.addText("\n")
//                printer.addText(spaceBetweenProductAndPrice("Employee Discount", "-${orderDetails.orderEmpDiscount.div(100).toDollar()}"))
//            }
//            if (orderDetails.orderDiscount != null && orderDetails.orderDiscount != 0.00) {
//                printer.addText("\n")
//                printer.addText(spaceBetweenProductAndPrice("Order Discount", "-${orderDetails.orderDiscount.div(100).toDollar()}"))
//            }
//            if (orderDetails.orderRefundAmount != null && orderDetails.orderRefundAmount != 0.00) {
//                printer.addText("\n")
//                printer.addText(spaceBetweenProductAndPrice("Refund", "-${orderDetails.orderRefundAmount.div(100).toDollar()}"))
//            }
            if (orderDetails.orderTax != null) {
                fohPrinter.addText("\n")
                fohPrinter.addText(spaceBetweenProductAndPrice("Sales Tax", orderDetails.orderTax.div(100).toDollar()))
            }
            if (orderDetails.orderTip != null && orderDetails.orderTip != 0.00) {
                fohPrinter.addText("\n")
                fohPrinter.addText(spaceBetweenProductAndPrice("Tip", orderDetails.orderTip.div(100).toDollar()))
            }
            if (orderDetails.orderGiftCardAmount != null && orderDetails.orderGiftCardAmount != 0.00) {
                fohPrinter.addText("\n")
                fohPrinter.addText(
                    spaceBetweenProductAndPrice(
                        "Gift Card", "-${(orderDetails.orderGiftCardAmount.div(100)).toMinusDollar().toDollar()}"
                    )
                )
            }
//            if (orderDetails.orderAdjustmentAmount != null && orderDetails.orderAdjustmentAmount != 0.00) {
//                if (orderDetails.orderAdjustmentAmount > 0) {
//                    printer.addText("\n")
//                    printer.addText(
//                        spaceBetweenProductAndPrice(
//                            "Adjustment", orderDetails.orderAdjustmentAmount.div(100).toDollar().toString()
//                        )
//                    )
//                } else {
//                    printer.addText("\n")
//                    printer.addText(
//                        spaceBetweenProductAndPrice(
//                            "Adjustment", "-${(abs(orderDetails.orderAdjustmentAmount).div(100)).toDollar()}"
//                        )
//                    )
//                }
//
//            }
            if (orderDetails.orderCouponCodeDiscount != null && orderDetails.orderCouponCodeDiscount != 0.00) {
                fohPrinter.addText("\n")
                fohPrinter.addText(
                    spaceBetweenProductAndPrice(
                        "Promocode", "-${(orderDetails.orderCouponCodeDiscount.div(100)).toMinusDollar().toDollar()}"
                    )
                )
            }
            fohPrinter.addText("\n------------------------------------------\n")
            if (orderDetails.orderTotal != null) {
                fohPrinter.addText(spaceBetweenProductAndPrice("Total", orderDetails.orderTotal?.div(100).toDollar()))
            }
            fohPrinter.addFeedLine(3)
            fohPrinter.addCut(Printer.CUT_FEED)
        } catch (e: Epos2Exception) {
            fohPrinter.clearCommandBuffer()
            return false
        }
        return true
    }

    private fun spaceBetweenProductAndPrice(product: String, price: String): String {
        val l = "${product}${price}".length;
        if (l < 42) {
            val s = 42 - l

            var space: String = ""
            for (i in 1..s) {
                space = "$space "
            }
            return product.plus(space).plus(price)
        } else {
            val s = 42 - price.length

            var space: String = ""
            for (i in 1..s) {
                space = "$space ";
            }
            return product.plus("\n$space").plus(price)
        }
    }

    private fun printData(fohPrintAddress: String): Boolean {
        if (fohPrinter == null) {
            return false
        }

        if (!connectPrinter(fohPrinter, fohPrintAddress)) {
            fohPrinter.clearCommandBuffer()
            return false
        }
        try {
            fohPrinter.sendData(Printer.PARAM_DEFAULT)
        } catch (e: Epos2Exception) {
            fohPrinter.clearCommandBuffer()
            try {
                fohPrinter.disconnect()
            } catch (ex: Epos2Exception) {
                return false
                // Do nothing
            }

        }
        return true
    }


    private fun connectPrinter(printer: Printer, printAddress: String): Boolean {
        Timber.tag("Printer").i("FOH Connect Printer")
        try {
            printer.connect("TCP:$printAddress", Printer.PARAM_DEFAULT)
        } catch (e: Epos2Exception) {
            Timber.e(e, "connect : $e")
            return false
        }
        return true
    }

}