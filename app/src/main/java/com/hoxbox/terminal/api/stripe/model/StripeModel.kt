package com.hoxbox.terminal.api.stripe.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName


enum class  PaymentStatus {
    InProgress,
    Success,
    Failed,
    CancelledByUser,
    DeclineByHostOrCard,
    TimeoutOnUserInput
}


data class PaymentResponse(

    @field:SerializedName("response")
    val response: List<ResponseItem?>? = null,

    @field:SerializedName("iConnRESTResponse")
    val iConnRESTResponse: IConnRESTResponse? = null
)

data class Type(

    @field:SerializedName("code")
    val code: String? = null,

    @field:SerializedName("text")
    val text: String? = null
)

data class Card(

    @field:SerializedName("entry_mode")
    val entryMode: EntryMode? = null,

    @field:SerializedName("type")
    val type: Type? = null,

    @field:SerializedName("account_no")
    val accountNo: String? = null
)

data class IConnRESTResponse(

    @field:SerializedName("posAccessKey")
    val posAccessKey: String? = null,

    @field:SerializedName("transactionId")
    val transactionId: String? = null,

    @field:SerializedName("terminalAccessKey")
    val terminalAccessKey: String? = null
)

data class Data(

    @field:SerializedName("9F1A")
    val jsonMember9F1A: String? = null,

    @field:SerializedName("9A")
    val jsonMember9A: String? = null,

    @field:SerializedName("9F27")
    val jsonMember9F27: String? = null,

    @field:SerializedName("8A")
    val jsonMember8A: String? = null,

    @field:SerializedName("9F03")
    val jsonMember9F03: String? = null,

    @field:SerializedName("9F36")
    val jsonMember9F36: String? = null,

    @field:SerializedName("5F34")
    val jsonMember5F34: String? = null,

    @field:SerializedName("9F26")
    val jsonMember9F26: String? = null,

    @field:SerializedName("9F37")
    val jsonMember9F37: String? = null,

    @field:SerializedName("9F12")
    val jsonMember9F12: String? = null,

    @field:SerializedName("9F34")
    val jsonMember9F34: String? = null,

    @field:SerializedName("9F02")
    val jsonMember9F02: String? = null,

    @field:SerializedName("9F10")
    val jsonMember9F10: String? = null,

    @field:SerializedName("9F21")
    val jsonMember9F21: String? = null,

    @field:SerializedName("9F0E")
    val jsonMember9F0E: String? = null,

    @field:SerializedName("5F2A")
    val jsonMember5F2A: String? = null,

    @field:SerializedName("9F0F")
    val jsonMember9F0F: String? = null,

    @field:SerializedName("82")
    val jsonMember82: String? = null,

    @field:SerializedName("9F0D")
    val jsonMember9F0D: String? = null
)

data class ReceiptInformation(

    @field:SerializedName("header")
    val header: List<String?>? = null
)

data class EntryMode(

    @field:SerializedName("code")
    val code: String? = null,

    @field:SerializedName("text")
    val text: String? = null
)

data class ResponseItem(

    @field:SerializedName("host_response_code")
    val hostResponseCode: String? = null,

    @field:SerializedName("authorization_no")
    val authorizationNo: String? = null,

    @field:SerializedName("transaction_amount")
    val transactionAmount: String? = null,

    @field:SerializedName("host_response_text")
    val hostResponseText: String? = null,

    @field:SerializedName("merchant_id")
    val merchantId: String? = null,

    @field:SerializedName("trace_no")
    val traceNo: String? = null,

    @field:SerializedName("type")
    val type: String? = null,

    @field:SerializedName("emv")
    val emv: Emv? = null,

    @field:SerializedName("customer_card_description")
    val customerCardDescription: String? = null,

    @field:SerializedName("transaction_time")
    val transactionTime: String? = null,

    @field:SerializedName("customer_language")
    val customerLanguage: String? = null,

    @field:SerializedName("avs_result")
    val avsResult: String? = null,

    @field:SerializedName("terminal_id")
    val terminalId: String? = null,

    @field:SerializedName("transaction_date")
    val transactionDate: String? = null,

    @field:SerializedName("batch_no")
    val batchNo: String? = null,

    @field:SerializedName("host_transaction_reference")
    val hostTransactionReference: String? = null,

    @field:SerializedName("cvm_result")
    val cvmResult: String? = null,

    @field:SerializedName("host_response_isocode")
    val hostResponseIsocode: String? = null,

    @field:SerializedName("retrieval_reference_no")
    val retrievalReferenceNo: String? = null,

    @field:SerializedName("total_amount")
    val totalAmount: String? = null,

    @field:SerializedName("receipt_information")
    val receiptInformation: ReceiptInformation? = null,

    @field:SerializedName("commercial_card_indicator")
    val commercialCardIndicator: String? = null,

    @field:SerializedName("tip_amount")
    val tipAmount: String? = null,

    @field:SerializedName("card")
    val card: Card? = null,

    @field:SerializedName("reference_no")
    val referenceNo: String? = null,

    @field:SerializedName("status")
    val status: String? = null
)

data class Emv(

    @field:SerializedName("tvr")
    val tvr: String? = null,

    @field:SerializedName("data")
    val data: Data? = null,

    @field:SerializedName("aid")
    val aid: String? = null,

    @field:SerializedName("application_label")
    val applicationLabel: String? = null
)


@Keep
enum class EditType(val type: String, val displayType: String) {
    Email("email", "Email"),
    Phone("phone", "Phone")
}


