package com.hoxbox.terminal.base

import com.hoxbox.terminal.api.userstore.model.CreateOrderResponse
import com.hoxbox.terminal.api.userstore.model.OrderPrice
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

object RxBus {

    private val publisher = PublishSubject.create<Any>()

    fun publish(event: Any) {
        publisher.onNext(event)
    }

    fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)
}

class RxEvent {
    data class EventOrderCountListen(val count: Int)
    data class EventDeliveryCountListen(val count: Int)
    data class EventCartGroupIdListen(val cartGroupId: Int)
    data class EventPaymentButtonEnabled(val enable :Boolean)
    object EventDismissLoyaltyDialog
    object EventDismissLoyaltyRegistrationSuccess
    object EventGotoStartButton
    data class EventTotalPayment(val orderPrice: OrderPrice)
    data class EventTotalCheckOut(val orderPrice: OrderPrice)
    data class QRCodeText(val data :String)
    object EventCheckValidation
    object DismissedPrinterDialog
    object EventValidation
    data class EventGoToPaymentScreen(val data :Boolean)
    data class EventGoToBack(val data :Boolean)
    data class AddGiftCart(val giftCardAmount: Int,val giftCardId :Int)
    data class AddPromoCode(val promocodeAmount: Double,val couponCodeId :Int)
    data class AddCredit(val credit: Double)
    data class RemoveGiftCart(val giftCard: Boolean)
    data class RemoveCredit(val credit: Boolean)
    data class RemovePromoCode(val giftCard: Boolean)
    data class PassPromocodeAndGiftCard(val couponCodeId: Int,val giftCardAmount :Int,val giftCardId : Int, val orderInstructions :String?)
    data class PassCreditAmount(val creditAmount: Int)
    data class PaymentIsNotVisible(val boolean :Boolean)
    data class AddEmployeeDiscount(val discount: Double)
    data class AddMHSMemberDiscount(val discount: Double)
    data class AddAdjustmentDiscount(val discount: Double)
    data class OpenOrderSuccessDialog(val orderId: CreateOrderResponse?)
    data class SearchOrderFilter(val searchText : String)
}