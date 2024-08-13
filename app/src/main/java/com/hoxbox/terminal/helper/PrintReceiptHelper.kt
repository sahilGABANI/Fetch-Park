package com.hoxbox.terminal.helper

import android.app.Activity
import android.content.Context
import android.text.SpannableStringBuilder
import com.epson.epos2.Epos2CallbackCode
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.hoxbox.terminal.api.authentication.LoggedInUserCache
import com.hoxbox.terminal.api.order.model.OrderDetail
import com.hoxbox.terminal.base.extension.toDollar
import com.hoxbox.terminal.base.extension.toMinusDollar
import com.hoxbox.terminal.utils.Constants
import timber.log.Timber
import kotlin.math.abs


class PrintReceiptHelper(val context: Context, val activity: Activity) : com.epson.epos2.printer.ReceiveListener {

    fun runPrintBOHReceiptSequence(printer: Printer, orderDetails: OrderDetail,bohPrintAddress :String): Boolean {
        val textData = SpannableStringBuilder("")
        if (orderDetails.fullName().isNotEmpty()) {
            textData.append("\n${orderDetails.fullName()}")
        }
        textData.append("\n\n")
        if (!orderDetails.orderCreationDate?.trim().isNullOrEmpty()) {
            textData.append("\n")
            textData.append(
                spaceBetweenProductAndPrice(
                    orderDetails.orderCreationDate?.toDate("yyyy-MM-dd HH:mm")?.formatTo("MMMM dd, yyyy").toString(),
                    orderDetails.orderCreationDate?.toDate("yyyy-MM-dd HH:mm")?.formatTo("hh:mm:ss a").toString()
                )
            )
        }
        textData.append("\n-----------------------------------\n")
        if (orderDetails.id != 0 && orderDetails.id != null) {
            textData.append(spaceBetweenProductAndPrice(Constants.RECEIPT, "#${orderDetails.id}"))
        }
        if (orderDetails.orderType?.isNotEmpty() == true) {
            textData.append("\n")
            textData.append(spaceBetweenProductAndPrice("Order Type :",orderDetails.orderType))
        }
        textData.append("\n-----------------------------------\n")
        if (orderDetails.items?.isNotEmpty() == true) {
            orderDetails.items.forEach {
//                bohPrinter.addHPosition(0)
                if (it.menuItemQuantity != null && !it?.productName.isNullOrEmpty()) {
                    textData.append("\n  ${it.menuItemQuantity} x ${it?.productName} \n")
                }
                if (!it.menuItemModifiers.isNullOrEmpty()) {
                    it.menuItemModifiers.forEach { item ->
                        item?.options?.forEach { item1 ->
                            if (!item1.optionName.isNullOrEmpty()) {
                                textData.append("\t${item1.optionName}\n")
                            }

                        }
                    }

                }
                it.menuItemInstructions?.trim()?.let {
                    textData.append("Note :$it")
                }
            }
        }

        Timber.tag("OkHttpClient").i(textData.toString())
        if (!createBohReceiptData(printer, orderDetails)) {
            return false
        }
        return printBohData(printer, bohPrintAddress)
    }

    private fun printBohData(printer: Printer, bohPrintAddress: String): Boolean {
        if (printer == null) {
            return false
        }

        if (!connectPrinter(printer, bohPrintAddress)) {
            printer.clearCommandBuffer()
            return false
        }
        try {
            printer.sendData(Printer.PARAM_DEFAULT)
        } catch (e: Epos2Exception) {
            printer.clearCommandBuffer()
            try {
                printer.disconnect()
            } catch (ex: Epos2Exception) {
                // Do nothing
            }
            return false
        }
        return true
    }

    private fun createBohReceiptData(printer: Printer, orderDetails: OrderDetail): Boolean {
        if (printer == null) {
            return false
        }
        try {
            printer.clearCommandBuffer()
            printer.addTextLang(Printer.LANG_EN)
            printer.addTextAlign(Printer.ALIGN_LEFT)
            printer.addTextSize(2, 3)
            printer.addTextSmooth(1)
            printer.addTextStyle(1, 0, 0, 0)
            if (orderDetails.fullName().isNotEmpty()) {
                printer.addText("\n${orderDetails.fullName()}")
            }
            printer.addFeedLine(3)
            printer.addTextStyle(0, 0, 0, 0)
            printer.addTextSize(1, 1)
            printer.addText("\n\n")
            printer.addTextAlign(Printer.ALIGN_LEFT)
            if (!orderDetails.orderCreationDate?.trim().isNullOrEmpty()) {
                printer.addText("\n")
                printer.addText(
                    spaceBetweenProductAndPrice(
                        orderDetails.orderCreationDate?.toDate("yyyy-MM-dd HH:mm")?.formatTo("MMMM dd, yyyy").toString(),
                        orderDetails.orderCreationDate?.toDate("yyyy-MM-dd HH:mm")?.formatTo("hh:mm:ss a").toString()
                    )
                )
            }
            printer.addTextAlign(Printer.ALIGN_LEFT)
            printer.addText("\n-----------------------------------\n")
            printer.addTextAlign(Printer.ALIGN_CENTER)
            if (orderDetails.id != 0 && orderDetails.id != null) {
                printer.addTextSize(1, 2)
                printer.addText(spaceBetweenProductAndPrice(Constants.RECEIPT, "#${orderDetails.id}"))
            }
            printer.addTextSize(1, 1)
            if (orderDetails.orderType?.isEmpty() == false) {
                printer.addText("\n")
                printer.addText(spaceBetweenProductAndPrice("Order Type :",orderDetails.orderType))
            }
            printer.addTextAlign(Printer.ALIGN_LEFT)
            printer.addText("\n-----------------------------------\n")
            printer.addTextAlign(Printer.ALIGN_LEFT)
            printer.addTextSize(1, 2)
            if (orderDetails.items?.isNotEmpty() == true) {
                orderDetails.items.forEach {
                    printer.addHPosition(0)
                    if (it.menuItemQuantity != null && !it?.productName.isNullOrEmpty()) {
                        printer.addTextSmooth(1)
                        printer.addTextStyle(1, 0, 0, 0)
                        printer.addText("\n  ${it.menuItemQuantity} x ${it?.productName} \n")
                    }
                    printer.addTextStyle(0, 0, 0, 0)
                    if (!it.menuItemModifiers.isNullOrEmpty()) {
                        it.menuItemModifiers.forEach { item ->
                            item?.options?.forEach { item1 ->
                                printer.addHPosition(40)
                                if (!item1.optionName.isNullOrEmpty()) {
                                    printer.addText("\t${item1.optionName}\n")
                                }

                            }
                        }

                    }
                    it.menuItemInstructions?.trim()?.let {
                        printer.addText("Note :$it")
                    }
                }
            }
            printer.addFeedLine(3)
            printer.addCut(Printer.CUT_FEED)
        } catch (e: Epos2Exception) {
            printer.clearCommandBuffer()
            try {
                printer.disconnect()
            } catch (ex: Epos2Exception) {
                // Do nothing
            }
            return false
        }
        return true
    }


    fun runPrintReceiptSequence(
        context: Context,
        printer: Printer,
        orderDetails: OrderDetail,
        loggedInUserCache: LoggedInUserCache,
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
//        textData.append("\n${orderDetails.loc}")
        textData.append("\n")
        if (!orderDetails.orderPromisedTime?.trim().isNullOrEmpty()) {
            textData.append(
                spaceBetweenProductAndPrice(
                    currentTime.toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("MMMM dd, yyyy").toString(),
                    currentTime.toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("hh:mm a").toString()
                )
            )
        }
        textData.append("\n-----------------------------------\n")
        if (orderDetails.id != 0 && orderDetails.id != null) {
            textData.append(spaceBetweenProductAndPrice(Constants.RECEIPT, "#${orderDetails.id}"))
        }
//        if (orderDetails.orderTableNumber != 0 && orderDetails.orderTableNumber != null) {
//            textData.append("\n")
//            textData.append(spaceBetweenProductAndPrice(Constants.TABLE_NO, "${orderDetails.orderTableNumber}"))
//        }
        textData.append("\n-----------------------------------\n")
        if (!orderDetails?.items.isNullOrEmpty()) {
            orderDetails?.items?.forEach {
                var productPrice: Double = 0.00
                if (it.menuItemPrice != null) {
                    productPrice = it.menuItemPrice ?: 0.00
                }
                it.menuItemModifiers?.forEach {
                    it?.options?.forEach {
                        productPrice = productPrice.plus(it.optionPrice?.toInt() ?: 0)
                    }
                }
                textData.append("\n")
                if (!it?.productName?.trim().isNullOrEmpty()) {
                    textData.append(
                        spaceBetweenProductAndPrice(
                            it?.productName.toString().plus(" X ${it.menuItemQuantity}"), productPrice.div(100).toDollar()
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
        textData.append("\n-----------------------------------\n")
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
//        if (orderDetails.orderAdjustmentAmount != null && orderDetails.orderAdjustmentAmount != 0.00) {
//            if (orderDetails.orderAdjustmentAmount > 0) {
//                textData.append("\n")
//                textData.append(
//                    spaceBetweenProductAndPrice(
//                        "Adjustment", orderDetails.orderAdjustmentAmount.div(100).toDollar().toString()
//                    )
//                )
//            } else {
//                textData.append("\n")
//                textData.append(
//                    spaceBetweenProductAndPrice(
//                        "Adjustment", "-${(abs(orderDetails.orderAdjustmentAmount).div(100)).toDollar()}"
//                    )
//                )
//            }
//
//        }

//        if (orderDetails.orderEmpDiscount != null && orderDetails.orderEmpDiscount != 0.00) {
//            textData.append("\n")
//            textData.append(spaceBetweenProductAndPrice("Employee Discount", "-${orderDetails.orderEmpDiscount?.div(100).toDollar()}"))
//        }
//        if (orderDetails.orderDiscount != null && orderDetails.orderDiscount != 0.00) {
//            textData.append("\n")
//            textData.append(spaceBetweenProductAndPrice("Order Discount", "-${orderDetails.orderDiscount?.div(100).toDollar()}"))
//        }
//        if (orderDetails.orderRefundAmount != null && orderDetails.orderRefundAmount != 0.00) {
//            textData.append("\n")
//            textData.append(spaceBetweenProductAndPrice("Refund", "-${orderDetails.orderRefundAmount?.div(100).toDollar()}"))
//        }
        if (orderDetails.orderCouponCodeDiscount != null && orderDetails.orderCouponCodeDiscount != 0.00) {
            textData.append("\n")
            textData.append(
                spaceBetweenProductAndPrice(
                    "Promocode", "-${(orderDetails.orderCouponCodeDiscount.div(100)).toMinusDollar().toDollar()}"
                )
            )
        }
        textData.append("\n-----------------------------------\n")
        printer.addText("\n-----------------------------------\n")
        if (orderDetails.orderTotal != null) {
            textData.append(spaceBetweenProductAndPrice("Total", orderDetails.orderTotal?.div(100).toDollar()))
        }
        Timber.tag("OkHttpClient").i(textData.toString())
        if (!createReceiptData(context, printer, orderDetails, loggedInUserCache, currentTime)) {
            return false
        }
        return printData(context, printer, fohPrintAddress)
    }

    private fun createReceiptData(
        context: Context, printer: Printer, orderDetails: OrderDetail, loggedInUserCache: LoggedInUserCache, currentTime: String
    ): Boolean {
        if (printer == null) {
            return false
        }
        try {
            printer.clearCommandBuffer()
            printer.addTextLang(Printer.LANG_EN)
            printer.addTextAlign(Printer.ALIGN_LEFT)
            if (orderDetails.locationName != null) {
                printer.addText("HotBoxCookies (${orderDetails.locationName})\n")
            }
            if (!orderDetails.locationAddress1.isNullOrEmpty()) {
                printer.addText("${orderDetails.locationAddress1}")
            }
            if (!orderDetails.locationAddress2.isNullOrEmpty()) {
                printer.addText("${orderDetails.locationAddress2}")
            }
            println("\n")
            if (!orderDetails.locationCity.isNullOrEmpty() && !orderDetails.locationState.isNullOrEmpty()) {
                printer.addText("\n${orderDetails.locationCity},${orderDetails.locationState}")
            }
            if (!orderDetails.locationZip.isNullOrEmpty()) {
                printer.addText(",${orderDetails.locationZip}")
            }
//        textData.append("\n${orderDetails.loc}")
            printer.addText("\n")
            if (!orderDetails.orderPromisedTime?.trim().isNullOrEmpty()) {
                printer.addText(
                    spaceBetweenProductAndPrice(
                        currentTime.toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("MMMM dd, yyyy").toString(),
                        currentTime.toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("hh:mm a").toString()
                    )
                )
            }
            printer.addText("\n-----------------------------------\n")
            if (orderDetails.id != 0 && orderDetails.id != null) {
                printer.addText(spaceBetweenProductAndPrice(Constants.RECEIPT, "#${orderDetails.id}"))
            }
//            if (orderDetails.orderTableNumber != 0 && orderDetails.orderTableNumber != null) {
//                printer.addText("\n")
//                printer.addText(spaceBetweenProductAndPrice(Constants.TABLE_NO, "${orderDetails.orderTableNumber}"))
//            }
            printer.addText("\n-----------------------------------\n")
            printer.addTextAlign(Printer.ALIGN_LEFT)
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
                    printer.addText("\n")
                    if (!it.productName?.trim().isNullOrEmpty()) {
                        printer.addText(
                            spaceBetweenProductAndPrice(
                                it.productName.toString().plus(" X ${it.menuItemQuantity}"), productPrice.div(100).toDollar()
                            )
                        )
                    }
                    printer.addTextAlign(Printer.ALIGN_LEFT)
                    if (!it.menuItemModifiers.isNullOrEmpty()) {
                        it.menuItemModifiers.forEach { item ->
                            item?.options?.forEach { item1 ->
                                if (item.options.firstOrNull()?.equals(item1) == true) {
                                    if (item1.optionPrice != null && item1.optionPrice != 0.0 && !item1.optionName.isNullOrEmpty()) {
                                        printer.addText("\n\t-${item1.optionName} ${(item1.optionPrice.toDouble().div(100) ?: 0).toDollar()}\n")
                                    } else {
                                        if (!item1.optionName.isNullOrEmpty()) {
                                            printer.addText("\n\t-${item1.optionName}\n")
                                        }
                                    }
                                } else {
                                    if (item1.optionPrice != null && item1.optionPrice != 0.0 && !item1.optionName.isNullOrEmpty()) {
                                        printer.addText("\n\t-${item1.optionName} ${(item1.optionPrice.toDouble().div(100) ?: 0).toDollar()}\n")
                                    } else {
                                        if (!item1.optionName.isNullOrEmpty()) {
                                            printer.addText("\n\t-${item1.optionName}\n")
                                        }
                                    }
                                }
                            }
                        }
                        it.menuItemInstructions?.trim()?.let {
                            printer.addTextAlign(Printer.ALIGN_LEFT)
                            printer.addText("\nNote :$it\n")
                        }
                    }
                }
            }

            printer.addTextAlign(Printer.ALIGN_LEFT)
            printer.addText("\n-----------------------------------\n")
            if (orderDetails.orderSubtotal != null) {
                printer.addText(spaceBetweenProductAndPrice("Subtotal", orderDetails.orderSubtotal.div(100).toDollar()))
            }
            if (orderDetails.orderDeliveryFee != null && orderDetails.orderDeliveryFee != 0.00 && orderDetails.orderType == "Delivery") {
                printer.addText("\n")
                printer.addText(spaceBetweenProductAndPrice("Delivery Fee", orderDetails.orderDeliveryFee.div(100).toDollar()))
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
                printer.addText("\n")
                printer.addText(spaceBetweenProductAndPrice("Sales Tax", orderDetails.orderTax.div(100).toDollar()))
            }
            if (orderDetails.orderTip != null && orderDetails.orderTip != 0.00) {
                printer.addText("\n")
                printer.addText(spaceBetweenProductAndPrice("Tip", orderDetails.orderTip.div(100).toDollar()))
            }
            if (orderDetails.orderGiftCardAmount != null && orderDetails.orderGiftCardAmount != 0.00) {
                printer.addText("\n")
                printer.addText(
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
                printer.addText("\n")
                printer.addText(
                    spaceBetweenProductAndPrice(
                        "Promocode", "-${(orderDetails.orderCouponCodeDiscount.div(100)).toMinusDollar().toDollar()}"
                    )
                )
            }
            printer.addText("\n-----------------------------------\n")
            if (orderDetails.orderTotal != null) {
                printer.addText(spaceBetweenProductAndPrice("Total", orderDetails.orderTotal?.div(100).toDollar()))
            }
            printer.addFeedLine(3)
            printer.addCut(Printer.CUT_FEED)
        } catch (e: Epos2Exception) {
            printer.clearCommandBuffer()
            return false
        }
        return true
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

    private fun printData(context: Context, printer: Printer, fohPrintAddress: String): Boolean {
        if (printer == null) {
            return false
        }

        if (!connectPrinter(printer, fohPrintAddress)) {
            printer.clearCommandBuffer()
            return false
        }
        try {
            printer.sendData(Printer.PARAM_DEFAULT)
        } catch (e: Epos2Exception) {
            printer.clearCommandBuffer()
            try {
                printer.disconnect()
            } catch (ex: Epos2Exception) {
                return false
                // Do nothing
            }

        }
        return true
    }

    private fun connectPrinter(printer: Printer, printAddress: String): Boolean {
        try {
            printer.setReceiveEventListener(this)
            printer.connect("TCP:$printAddress", Printer.PARAM_DEFAULT)
        } catch (e: Epos2Exception) {
            Timber.e(e, "connect : $e")
            return false
        }
        return true
    }

    override fun onPtrReceive(printerObj: Printer?, code: Int, status: PrinterStatusInfo?, printJobId: String?) {
        activity.runOnUiThread {
            disconnectPrinter(printerObj)
        }
    }

    private fun disconnectPrinter(printer: Printer?) {
        if (printer == null) {
            return
        }
        while (true) {
            try {
                printer.disconnect()
                break
            } catch (e: Epos2Exception) {
                //Note: If printer is processing such as printing and so on, the disconnect API returns ERR_PROCESSING.
                if (e.errorStatus == Epos2Exception.ERR_PROCESSING) {
                    try {
//                            Thread.sleep(DISCONNECT_INTERVAL.toLong())
                    } catch (ex: java.lang.Exception) {
                    }
                } else {
//                        requireActivity().runOnUiThread(Runnable { ShowMsg.showException(e, "disconnect", mContext) })
                    break
                }

            }
        }
    }

    private fun getCodeText(state: Int): String {
        var return_text = ""
        return_text = when (state) {
            Epos2CallbackCode.CODE_SUCCESS -> "PRINT_SUCCESS"
            Epos2CallbackCode.CODE_PRINTING -> "PRINTING"
            Epos2CallbackCode.CODE_ERR_AUTORECOVER -> "ERR_AUTO_RECOVER"
            Epos2CallbackCode.CODE_ERR_COVER_OPEN -> "ERR_COVER_OPEN"
            Epos2CallbackCode.CODE_ERR_CUTTER -> "ERR_CUTTER"
            Epos2CallbackCode.CODE_ERR_MECHANICAL -> "ERR_MECHANICAL"
            Epos2CallbackCode.CODE_ERR_EMPTY -> "ERR_EMPTY"
            Epos2CallbackCode.CODE_ERR_UNRECOVERABLE -> "ERR_UNRECOVERABLE"
            Epos2CallbackCode.CODE_ERR_FAILURE -> "ERR_FAILURE"
            Epos2CallbackCode.CODE_ERR_NOT_FOUND -> "ERR_NOT_FOUND"
            Epos2CallbackCode.CODE_ERR_SYSTEM -> "ERR_SYSTEM"
            Epos2CallbackCode.CODE_ERR_PORT -> "ERR_PORT"
            Epos2CallbackCode.CODE_ERR_TIMEOUT -> "ERR_TIMEOUT"
            Epos2CallbackCode.CODE_ERR_JOB_NOT_FOUND -> "ERR_JOB_NOT_FOUND"
            Epos2CallbackCode.CODE_ERR_SPOOLER -> "ERR_SPOOLER"
            Epos2CallbackCode.CODE_ERR_BATTERY_LOW -> "ERR_BATTERY_LOW"
            Epos2CallbackCode.CODE_ERR_TOO_MANY_REQUESTS -> "ERR_TOO_MANY_REQUESTS"
            Epos2CallbackCode.CODE_ERR_REQUEST_ENTITY_TOO_LARGE -> "ERR_REQUEST_ENTITY_TOO_LARGE"
            Epos2CallbackCode.CODE_CANCELED -> "CODE_CANCELED"
            Epos2CallbackCode.CODE_ERR_NO_MICR_DATA -> "ERR_NO_MICR_DATA"
            Epos2CallbackCode.CODE_ERR_ILLEGAL_LENGTH -> "ERR_ILLEGAL_LENGTH"
            Epos2CallbackCode.CODE_ERR_NO_MAGNETIC_DATA -> "ERR_NO_MAGNETIC_DATA"
            Epos2CallbackCode.CODE_ERR_RECOGNITION -> "ERR_RECOGNITION"
            Epos2CallbackCode.CODE_ERR_READ -> "ERR_READ"
            Epos2CallbackCode.CODE_ERR_NOISE_DETECTED -> "ERR_NOISE_DETECTED"
            Epos2CallbackCode.CODE_ERR_PAPER_JAM -> "ERR_PAPER_JAM"
            Epos2CallbackCode.CODE_ERR_PAPER_PULLED_OUT -> "ERR_PAPER_PULLED_OUT"
            Epos2CallbackCode.CODE_ERR_CANCEL_FAILED -> "ERR_CANCEL_FAILED"
            Epos2CallbackCode.CODE_ERR_PAPER_TYPE -> "ERR_PAPER_TYPE"
            Epos2CallbackCode.CODE_ERR_WAIT_INSERTION -> "ERR_WAIT_INSERTION"
            Epos2CallbackCode.CODE_ERR_ILLEGAL -> "ERR_ILLEGAL"
            Epos2CallbackCode.CODE_ERR_INSERTED -> "ERR_INSERTED"
            Epos2CallbackCode.CODE_ERR_WAIT_REMOVAL -> "ERR_WAIT_REMOVAL"
            Epos2CallbackCode.CODE_ERR_DEVICE_BUSY -> "ERR_DEVICE_BUSY"
            Epos2CallbackCode.CODE_ERR_IN_USE -> "ERR_IN_USE"
            Epos2CallbackCode.CODE_ERR_CONNECT -> "ERR_CONNECT"
            Epos2CallbackCode.CODE_ERR_DISCONNECT -> "ERR_DISCONNECT"
            Epos2CallbackCode.CODE_ERR_MEMORY -> "ERR_MEMORY"
            Epos2CallbackCode.CODE_ERR_PROCESSING -> "ERR_PROCESSING"
            Epos2CallbackCode.CODE_ERR_PARAM -> "ERR_PARAM"
            Epos2CallbackCode.CODE_RETRY -> "RETRY"
            Epos2CallbackCode.CODE_ERR_DIFFERENT_MODEL -> "ERR_DIFFERENT_MODEL"
            Epos2CallbackCode.CODE_ERR_DIFFERENT_VERSION -> "ERR_DIFFERENT_VERSION"
            Epos2CallbackCode.CODE_ERR_DATA_CORRUPTED -> "ERR_DATA_CORRUPTED"
            Epos2CallbackCode.CODE_ERR_JSON_FORMAT -> "ERR_JSON_FORMAT"
            Epos2CallbackCode.CODE_NO_PASSWORD -> "NO_PASSWORD"
            Epos2CallbackCode.CODE_ERR_INVALID_PASSWORD -> "ERR_INVALID_PASSWORD"
            else -> String.format("%d", state)
        }
        return return_text
    }
}