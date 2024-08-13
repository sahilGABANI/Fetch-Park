package com.hoxbox.terminal.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.text.SpannableStringBuilder
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.hoxbox.terminal.api.giftcard.model.VirtualGiftCardInfo
import com.hoxbox.terminal.api.order.model.OrderDetail
import com.hoxbox.terminal.api.store.model.StoreResponse
import com.hoxbox.terminal.base.extension.showToast
import com.hoxbox.terminal.base.extension.toDollar
import com.hoxbox.terminal.utils.Constants
import timber.log.Timber

class BohPrinterHelper private constructor() : com.epson.epos2.printer.ReceiveListener {
    private lateinit var bohPrinter: Printer

    companion object {
        @Volatile
        private var instance: BohPrinterHelper? = null

        @SuppressLint("StaticFieldLeak")
        private lateinit var activity: Activity
        fun getInstance(activity: Activity): BohPrinterHelper {
            Companion.activity = activity
            return instance ?: synchronized(this) {
                instance ?: BohPrinterHelper().also { instance = it }
            }
        }
    }

    fun printerInitialize(context: Context) {
        try {
            bohPrinter = Printer(Printer.TM_T88, Printer.MODEL_ANK, context)
        } catch (e: Epos2Exception) {
            Timber.e(e)
            activity.runOnUiThread {
                context.showToast(e.message.toString())
            }
        }
    }

    fun printerConnect(bohPrintAddress: String?): Boolean {
        Timber.tag("Printer").i("BOH Connect Printer")
        try {
            bohPrinter.connect("TCP:$bohPrintAddress", Printer.PARAM_DEFAULT)
        } catch (e: java.lang.Exception) {
            Timber.e(e, "connect : $e")
            return false
        }
        return true
    }

    fun isPrinterConnected(): Boolean {
        return bohPrinter.status.connection == 1
    }

    fun runPrintBOHReceiptSequence(listOfOderDetails: OrderDetail, bohPrintAddress: String?): Boolean {
        bohPrinter.clearCommandBuffer()
        Timber.tag("OkHttpClient").i("Main Receipt")
        val textData = SpannableStringBuilder("")
        if (listOfOderDetails.guestName?.isNullOrEmpty() == false) {
            textData.append("\n${listOfOderDetails.guestName}")
        } else if (listOfOderDetails.fullName().isNotEmpty()) {
            textData.append("\n${listOfOderDetails.fullName()}")
        }
        textData.append("\n")

        if (!listOfOderDetails.orderPromisedTime?.trim().isNullOrEmpty()) {
            textData.append("\n")
            textData.append(
                spaceBetweenProductAndPrice(
                    listOfOderDetails.orderPromisedTime?.toDate("yyyy-MM-dd HH:mm")?.formatTo("MMMM dd, yyyy").toString(),
                    listOfOderDetails.orderPromisedTime?.toDate("yyyy-MM-dd HH:mm")?.formatTo("hh:mm:ss a").toString()
                )
            )
        }
        textData.append("\n------------------------------------------\n")
        if (listOfOderDetails.id != 0 && listOfOderDetails.id != null) {
            textData.append(spaceBetweenProductAndPrice(Constants.RECEIPT, "#${listOfOderDetails.id}"))
        }
        if (listOfOderDetails.orderType?.isNotEmpty() == true) {
            textData.append("\n")
            textData.append(spaceBetweenProductAndPrice("Order Type :", listOfOderDetails.orderType))
        }
        if (listOfOderDetails.orderDeliveryAddress?.isNullOrEmpty() == false && (listOfOderDetails.orderType == "Delivery" || listOfOderDetails.orderTypeId == 20)) {
            textData.append("\n")
            textData.append(printMultilineText(listOfOderDetails.orderDeliveryAddress.toString(), 42))
        }
        if (listOfOderDetails.orderType == "Delivery" || listOfOderDetails.orderTypeId == 20) {
            if (listOfOderDetails.customerPhone?.isNullOrEmpty() == false) {
                textData.append("\n")
                textData.append(spaceBetweenProductAndPrice("Phone Number :", listOfOderDetails.customerPhone.toString()))
            } else if (listOfOderDetails.guestPhone?.isNullOrEmpty() == false) {
                textData.append("\n")
                textData.append("Phone Number :", listOfOderDetails.guestPhone.toString())
            }
        }
        textData.append("\n------------------------------------------\n")
        if (listOfOderDetails.items?.isNotEmpty() == true) {
            listOfOderDetails.items.forEach {
                bohPrinter.addHPosition(0)
                if (it.menuItemQuantity != null && !it.productName.isNullOrEmpty()) {
                    textData.append("\n  ${it.menuItemQuantity} x ${it.productName} \n")
                }
                if (!it.menuItemModifiers.isNullOrEmpty()) {
                    it.menuItemModifiers.forEach { item ->
                        item.options?.forEach { item1 ->
                            if ((item1.modifierQyt ?: 0) > 0) {
                                textData.append("\t${item1.modifierQyt} x ${item1.optionName}\n")
                            } else {
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
        if (!createBohReceiptData(listOfOderDetails)) {
            return false
        }
        return printBohData(bohPrintAddress)
    }

    private fun printBohData(bohPrintAddress: String?): Boolean {
        if (bohPrinter == null) {
            return false
        }

        if (!isPrinterConnected()) {
            bohPrinter.clearCommandBuffer()
            return false
        }
        try {
            bohPrinter.sendData(Printer.PARAM_DEFAULT)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private fun createBohReceiptData(orderDetails: OrderDetail): Boolean {
        if (bohPrinter == null) {
            return false
        }
        try {
            bohPrinter.addTextLang(Printer.LANG_EN)
            bohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            bohPrinter.addTextSize(2, 3)
            bohPrinter.addTextSmooth(1)
            bohPrinter.addTextStyle(1, 0, 0, 0)
            if (orderDetails.guestName?.isNullOrEmpty() == false) {
                bohPrinter.addText("\n${orderDetails.guestName}")
            } else if (orderDetails.fullName().isNotEmpty()) {
                bohPrinter.addText("\n${orderDetails.fullName()}")
            }
            bohPrinter.addFeedLine(3)
            bohPrinter.addTextStyle(0, 0, 0, 0)
            bohPrinter.addTextSize(1, 1)
            bohPrinter.addText("\n\n")
            bohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            if (!orderDetails.orderPromisedTime?.trim().isNullOrEmpty()) {
                bohPrinter.addText("\n")
                bohPrinter.addText(
                    spaceBetweenProductAndPrice(
                        orderDetails.orderPromisedTime?.toDate("yyyy-MM-dd HH:mm")?.formatTo("MMMM dd, yyyy").toString(),
                        orderDetails.orderPromisedTime?.toDate("yyyy-MM-dd HH:mm")?.formatTo("hh:mm:ss a").toString()
                    )
                )
            }
            bohPrinter.addTextAlign(Printer.ALIGN_RIGHT)
            bohPrinter.addText("\n------------------------------------------\n")
            bohPrinter.addTextAlign(Printer.ALIGN_CENTER)
            if (orderDetails.id != 0 && orderDetails.id != null) {
                bohPrinter.addTextSize(1, 2)
                bohPrinter.addText(spaceBetweenProductAndPrice(Constants.RECEIPT, "#${orderDetails.id}"))
            }
//            if (orderDetails.orderTableNumber != 0 && orderDetails.orderTableNumber != null) {
//                bohPrinter.addText("\n")
//                bohPrinter.addText(spaceBetweenProductAndPrice(Constants.TABLE_NO, "${orderDetails.orderTableNumber}"))
//            }
//            bohPrinter.addTextSize(1, 1)
            if (orderDetails.orderType?.isEmpty() == false) {
                bohPrinter.addText("\n")
                bohPrinter.addText(spaceBetweenProductAndPrice("Order Type :", orderDetails.orderType))
            }
            if (orderDetails.orderDeliveryAddress?.isNullOrEmpty() == false && (orderDetails.orderType == "Delivery" || orderDetails.orderTypeId == 20)) {
                bohPrinter.addText("\n")
                bohPrinter.addText(printMultilineText(orderDetails.orderDeliveryAddress.toString(), 42))
            }
            if (orderDetails.orderType == "Delivery" || orderDetails.orderTypeId == 20) {
                if (orderDetails.customerPhone?.isNullOrEmpty() == false) {
                    bohPrinter.addText("\n")
                    bohPrinter.addText(spaceBetweenProductAndPrice("Phone Number :", orderDetails.customerPhone.toString()))
                } else if (orderDetails.guestPhone?.isNullOrEmpty() == false) {
                    bohPrinter.addText("\n")
                    bohPrinter.addText(spaceBetweenProductAndPrice("Phone Number :", orderDetails.guestPhone.toString()))
                }
            }
            bohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            bohPrinter.addText("\n------------------------------------------\n")
            bohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            bohPrinter.addTextSize(1, 2)

            bohPrinter.addPulse(Printer.DRAWER_HIGH, Printer.PULSE_100);
            if (orderDetails?.items?.isNotEmpty() == true) {
                orderDetails.items.forEach {
                    bohPrinter.addHPosition(0)
                    if (it.menuItemQuantity != null && !it?.productName.isNullOrEmpty()) {
                        bohPrinter.addTextSmooth(1)
                        bohPrinter.addTextStyle(1, 0, 0, 0)
                        bohPrinter.addText("\n  ${it.menuItemQuantity} x ${it?.productName} \n")
                    }
                    bohPrinter.addTextStyle(0, 0, 0, 0)
                    if (!it.menuItemModifiers.isNullOrEmpty()) {
                        it.menuItemModifiers.forEach { item ->
                            item?.options?.forEach { item1 ->
                                bohPrinter.addHPosition(40)
                                if (!item1.optionName.isNullOrEmpty()) {
                                    if ((item1.modifierQyt ?: 0) > 0) {
                                        bohPrinter.addText("\t${item1.modifierQyt} x ${item1.optionName}\n")
                                    } else {
                                        bohPrinter.addText("\t${item1.optionName}\n")
                                    }

                                }
                            }
                        }

                    }
                    it.menuItemInstructions?.trim()?.let {
                        bohPrinter.addText("Note :$it")
                    }
                }
            }
            bohPrinter.addFeedLine(3)
            bohPrinter.addCut(Printer.CUT_FEED)
        } catch (e: java.lang.Exception) {
            return false
        }
        return true
    }

    private fun createBohGiftCardReceipt(orderDetails: VirtualGiftCardInfo, storeResponse: StoreResponse): Boolean {
        if (bohPrinter == null) {
            return false
        }
        try {
            bohPrinter.addTextLang(Printer.LANG_EN)
            bohPrinter.addTextAlign(Printer.ALIGN_CENTER)
            bohPrinter.addTextStyle(0, 0, 0, 0)
            bohPrinter.addTextSize(1, 1)
            bohPrinter.addText("\n")
            bohPrinter.addText("Gift Card Receipt")
            bohPrinter.addText("\n")
            bohPrinter.addTextAlign(Printer.ALIGN_LEFT)
            storeResponse.let {
                if (it.locationName != null) {
                    bohPrinter.addText("HotBoxCookies (${it.locationName})\n")
                }
                if (!it.locationAddress1.isNullOrEmpty()) {
                    bohPrinter.addText("${it.locationAddress1}")
                }
                if (!it.locationAddress2.isNullOrEmpty()) {
                    bohPrinter.addText("${it.locationAddress2}")
                }
                if (!it.locationCity.isNullOrEmpty() && !it.locationState.isNullOrEmpty()) {
                    bohPrinter.addText("\n${it.locationCity},${it.locationState}")
                }
                if (!it.locationZip.isNullOrEmpty()) {
                    bohPrinter.addText(",${it.locationZip}")
                }
            }
            bohPrinter.addText("\n------------------------------------------\n")
            if (!orderDetails.orderCreationDate?.trim().isNullOrEmpty()) {
                bohPrinter.addText("\n")
                bohPrinter.addText(
                    spaceBetweenProductAndPrice(
                        orderDetails.orderCreationDate?.toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("MMMM dd, yyyy").toString(),
                        orderDetails.orderCreationDate?.toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("hh:mm a").toString()
                    )
                )
            }
            bohPrinter.addTextAlign(Printer.ALIGN_RIGHT)
            bohPrinter.addText("\n------------------------------------------\n")
            if (orderDetails.giftCardId != null) {
                bohPrinter.addText("\n")
                bohPrinter.addText(spaceBetweenProductAndPrice("Gift Card ID:", orderDetails.giftCardId.toString()))
            }
            if (orderDetails.orderTotal != null) {
                bohPrinter.addText("\n")
                bohPrinter.addText(spaceBetweenProductAndPrice("Gift Total Amount:", orderDetails.orderTotal.toDouble().div(100).toDollar()))
            }
            bohPrinter.addTextAlign(Printer.ALIGN_CENTER)
            bohPrinter.addFeedLine(3)
            bohPrinter.addCut(Printer.CUT_FEED)
        } catch (e: java.lang.Exception) {
            return false
        }
        return true
    }

    private fun createBohReceiptsData(listOfOderDetails: List<OrderDetail>): Boolean {
        listOfOderDetails.forEach {
            createBohReceiptData(it)
        }
        return true
    }

    fun runPrintBOHReceiptForGiftCard(
        listOfOderDetails: VirtualGiftCardInfo, bohPrintAddress: String?, storeResponse: StoreResponse
    ): Boolean {
        bohPrinter.clearCommandBuffer()
        Timber.tag("OkHttpClient").i("======== Gift Card Receipt ========")
        val textData = SpannableStringBuilder("")
        textData.append("Gift Card Receipt")
        textData.append("\n")
        storeResponse.let {
            if (it.locationName != null) {
                textData.append("HotBoxCookies (${it.locationName})\n")
            }
            if (!it.locationAddress1.isNullOrEmpty()) {
                textData.append("${it.locationAddress1}")
            }
            if (!it.locationAddress2.isNullOrEmpty()) {
                textData.append("${it.locationAddress2}")
            }
            if (!it.locationCity.isNullOrEmpty() && !it.locationState.isNullOrEmpty()) {
                textData.append("\n${it.locationCity},${it.locationState}")
            }
            if (!it.locationZip.isNullOrEmpty()) {
                textData.append(",${it.locationZip}")
            }

        }
        textData.append("\n------------------------------------------\n")
        if (!listOfOderDetails.orderCreationDate?.trim().isNullOrEmpty()) {
            textData.append(
                spaceBetweenProductAndPrice(
                    listOfOderDetails.orderCreationDate?.toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("MMMM dd, yyyy").toString(),
                    listOfOderDetails.orderCreationDate?.toDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")?.formatTo("hh:mm a").toString()
                )
            )
        }

        textData.append("\n------------------------------------------")
        if (listOfOderDetails.giftCardId != null) {
            textData.append("\n")
            textData.append(spaceBetweenProductAndPrice("Gift Card ID:", listOfOderDetails.giftCardId.toString()))
        }

        if (listOfOderDetails.orderTotal != null) {
            textData.append("\n")
            textData.append(spaceBetweenProductAndPrice("Gift Total Amount:", listOfOderDetails.orderTotal.toDouble().div(100).toDollar()))
        }

        Timber.tag("OkHttpClient").i(textData.toString())
        if (!createBohGiftCardReceipt(listOfOderDetails,storeResponse)) {
            return false
        }
        return printBohData(bohPrintAddress)
    }

    private fun spaceBetweenProductAndPrice(product: String, price: String): String {
        val l = "${product}${price}".length;
        if (l < 42) {
            val s = 42 - l;

            var space: String = ""
            for (i in 1..s) {
                space = "$space ";
            }
            return product.plus(space).plus(price)
        } else {
            val s = 42 - price.length;

            var space: String = ""
            for (i in 1..s) {
                space = "$space ";
            }
            return product.plus("\n$space").plus(price)
        }
    }

    fun printMultilineText(text: String, wordsPerLine: Int): String? {
        val words = text.split("\\s+".toRegex()) // Split text into words
        var formattedText = ""
        var currentLine = ""
        var wordCount = 0

        for (word in words) {
            if (wordCount + word.length <= wordsPerLine) {
                currentLine += "$word "
                wordCount += word.length + 1 // +1 for the space after the word
            } else {
                formattedText += currentLine.trim() + "\n"
                currentLine = "$word "
                wordCount = word.length + 1
            }
        }

        // Add the last line
        formattedText += currentLine.trim()

        return formattedText
    }


    override fun onPtrReceive(printerObj: Printer?, code: Int, status: PrinterStatusInfo?, printJobId: String?) {

    }
}