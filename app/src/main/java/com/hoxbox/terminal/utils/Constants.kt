package com.hoxbox.terminal.utils

object Constants {

    const val CHECK_ALL = "All"
    const val CHECK_AVAILABLE = "Available"
    const val CHECK_UNAVAILABLE = "Unavailable"

    const val CATEGORY_FILTER_ALL = "All"
    const val TIER_1_DELIVERY_FEE = "tier_1_delivery_fee"
    const val MODE_ID = 4
    const val ORDER_TYPE_ID = 19
    const val ORDER_MODE_ID = 1

    const val CONNECTED_LOCATION_ID = "tml_EreAPwHzehWwZx"
    const val QR_CODE_TYPE_LOYALTY = "loyalty"
    const val QR_CODE_TYPE_GIFT_CARD = "giftcard"
    const val LIVE_READER = "tmr_ErwQjwmEiCCNdo"
    const val DEMO_READER = "tmr_EreA8w0pYIhBij"
    const val STRIPE_SK_TOKEN = "sk_test_51H7ofqHBh9c4S8JHZ2Uncluam52UeNXDBtsxsqQSSl3lh5n417UgKTIbwz4olMJXHjUZ91SfKMLelfpmpWRheCZC00rvxVj5pa"
    const val CAPTURE_ERROR ="Capture Error"
    const val CREATE_PAYMENT_ERROR ="Create Payment Error"
    const val PROCESS_PAYMENT_ERROR ="Process Payment Error"
    const val RETRIEVE_READER_ERROR ="Retrieve Reader Error"
    const val PAYMENT_METHOD_TYPE  = "card_present"
    const val CURRENCY = "usd"
    const val CAPTURE_METHOD  = "manual"
    const val ACTIVE =  "active"
    const val NEW =  "new"
    const val PAST =  "past"
    const val DELIVERY_ADDRESS_API_KEY =  "462b770dbfe347a6a35d6e9b4aa9a029"
    const val COUNTRYCODE =  "countrycode:us"
    const val DELIVERY_ORDER_TYPE_ID = 20
    const val ORDER_EMPLOYEE_DISCOUNT = 20
    const val MHS_MEMBER_DISCOUNT = 10

    const val RECEIPT = "Order :"
    const val TABLE_NO = "Table No :"
    const val ORDER_STATUS_RECEIVE = "received"
    const val EMAIL = "Email"
    const val PHONE = "Phone"
    var LOCATION_TAX_RATE = 7.5
    fun setTaxRate(taxRate : Double) {
        LOCATION_TAX_RATE = taxRate
    }

    fun getTaxRate(): Double {
        return LOCATION_TAX_RATE
    }
}