package com.hoxbox.terminal.api.giftcard.model

import com.google.gson.annotations.SerializedName


data class GiftCardRequest(


	@field:SerializedName("giftCards")
	val giftCards: List<BuyVirtualCardRequest>? = arrayListOf(),

	@field:SerializedName("transaction_amount")
	val transactionAmount: String? = null,

	@field:SerializedName("transaction_id_of_processor")
	val transactionIdOfProcessor: String? = null,

	@field:SerializedName("transaction_charge_id")
	val transactionChargeId: String? = null,
)
data class BuyVirtualCardRequest(


	@field:SerializedName("gift_card_recipient_email")
	val giftCardRecipientEmail: String? = null,

	@field:SerializedName("gift_card_amout")
	val giftCardAmout: Int? = null,

	@field:SerializedName("gift_card_recipient_first_name")
	val giftCardRecipientFirstName: String? = null,

	@field:SerializedName("gift_card_purchaser_last_name")
	val giftCardPurchaserLastName: String? = null,

	@field:SerializedName("gift_card_purchaser_email")
	val giftCardPurchaserEmail: String? = null,

	@field:SerializedName("gift_card_purchaser_first_name")
	val giftCardPurchaserFirstName: String? = null,

	@field:SerializedName("gift_card_personal_message")
	val giftCardPersonalMessage: String? = null,

	@field:SerializedName("gift_card_code")
	val giftCardCode: String? = null,

	@field:SerializedName("gift_card_recipient_last_name")
	val giftCardRecipientLastName: String? = null,

	@field:SerializedName("order_mode_id")
	val orderModeId: Int? = null,

)

data class BuyPhysicalCardRequest(
	@field:SerializedName("gift_card_amout")
	val giftCardAmout: Int? = null,

	@field:SerializedName("transaction_amount")
	val transactionAmount: String? = null,

	@field:SerializedName("gift_card_code")
	val giftCardCode: String? = null,

	@field:SerializedName("transaction_id_of_processor")
	val transactionIdOfProcessor: String? = null,

	@field:SerializedName("transaction_charge_id")
	val transactionChargeId: String? = null,
)

data class GiftCardData(

	@field:SerializedName("gift_card_recipient_email")
	val giftCardRecipientEmail: String? = null,

	@field:SerializedName("gift_card_amout")
	val giftCardAmout: Double? = null,

	@field:SerializedName("gift_card_redemption")
	val giftCardRedemption: Int? = null,

	@field:SerializedName("gift_card_recipient_first_name")
	val giftCardRecipientFirstName: String? = null,

	@field:SerializedName("gift_card_purchaser_last_name")
	val giftCardPurchaserLastName: String? = null,

	@field:SerializedName("gift_card_purchaser_email")
	val giftCardPurchaserEmail: String? = null,

	@field:SerializedName("gift_card_purchased")
	val giftCardPurchased: String? = null,

	@field:SerializedName("gift_card_purchaser_first_name")
	val giftCardPurchaserFirstName: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("gift_card_code")
	val giftCardCode: String? = null,

	@field:SerializedName("gift_card_recipient_last_name")
	val giftCardRecipientLastName: String? = null
)

data class GiftCardResponseInfo(

	@field:SerializedName("giftCard")
	val giftCards: List<GiftCardsItem>? = null
)

data class PhysicalGiftCardInfo(

	@field:SerializedName("giftCard")
	val giftCards: List<PhysicalGiftCardResponse>? = null
)

data class GiftCardsItem(

	@field:SerializedName("order_user_id")
	val orderUserId: Any? = null,

	@field:SerializedName("order_mode_id")
	val orderModeId: Int? = null,

	@field:SerializedName("order_transaction_id")
	val orderTransactionId: Int? = null,

	@field:SerializedName("order_tip")
	val orderTip: Int? = null,

	@field:SerializedName("order_type_id")
	val orderTypeId: Any? = null,

	@field:SerializedName("order_refund")
	val orderRefund: Any? = null,

	@field:SerializedName("order_cart_group_id")
	val orderCartGroupId: Any? = null,

	@field:SerializedName("credit_amount")
	val creditAmount: Any? = null,

	@field:SerializedName("order_adjustments")
	val orderAdjustments: Any? = null,

	@field:SerializedName("order_gift_card_amount")
	val orderGiftCardAmount: Any? = null,

	@field:SerializedName("order_instructions")
	val orderInstructions: Any? = null,

	@field:SerializedName("order_promised_time")
	val orderPromisedTime: Any? = null,

	@field:SerializedName("coupon_code_id")
	val couponCodeId: Any? = null,

	@field:SerializedName("order_delivery_fee")
	val orderDeliveryFee: Int? = null,

	@field:SerializedName("order_tax")
	val orderTax: Int? = null,

	@field:SerializedName("order_location_id")
	val orderLocationId: Any? = null,

	@field:SerializedName("order_coupon_code_discount")
	val orderCouponCodeDiscount: Any? = null,

	@field:SerializedName("order_creation_date")
	val orderCreationDate: String? = null,

	@field:SerializedName("order_total")
	val orderTotal: Int? = null,

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("order_subtotal")
	val orderSubtotal: Int? = null,

	@field:SerializedName("gift_card_id")
	val giftCardId: Int? = null
)

data class PhysicalGiftCardResponse(

	@field:SerializedName("gift_card_recipient_email")
	val giftCardRecipientEmail: Any? = null,

	@field:SerializedName("gift_card_amout")
	val giftCardAmout: Int? = null,

	@field:SerializedName("gift_card_redemption")
	val giftCardRedemption: Int? = null,

	@field:SerializedName("gift_card_recipient_first_name")
	val giftCardRecipientFirstName: Any? = null,

	@field:SerializedName("gift_card_purchaser_last_name")
	val giftCardPurchaserLastName: Any? = null,

	@field:SerializedName("gift_card_purchaser_email")
	val giftCardPurchaserEmail: Any? = null,

	@field:SerializedName("gift_card_purchased")
	val giftCardPurchased: String? = null,

	@field:SerializedName("gift_card_purchaser_first_name")
	val giftCardPurchaserFirstName: Any? = null,

	@field:SerializedName("active")
	val active: Int? = null,

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("gift_card_code")
	val giftCardCode: String? = null,

	@field:SerializedName("gift_card_recipient_last_name")
	val giftCardRecipientLastName: Any? = null
)

data class VirtualGiftCardResponse(

	@field:SerializedName("giftCards")
	val giftCards: List<VirtualGiftCardInfo>? = null
)

data class VirtualGiftCardInfo(

	@field:SerializedName("order_user_id")
	val orderUserId: Any? = null,

	@field:SerializedName("order_mode_id")
	val orderModeId: Int? = null,

	@field:SerializedName("order_transaction_id")
	val orderTransactionId: Int? = null,

	@field:SerializedName("order_tip")
	val orderTip: Int? = null,

	@field:SerializedName("order_type_id")
	val orderTypeId: Any? = null,

	@field:SerializedName("order_refund")
	val orderRefund: Any? = null,

	@field:SerializedName("order_cart_group_id")
	val orderCartGroupId: Any? = null,

	@field:SerializedName("credit_amount")
	val creditAmount: Any? = null,

	@field:SerializedName("order_adjustments")
	val orderAdjustments: Any? = null,

	@field:SerializedName("order_gift_card_amount")
	val orderGiftCardAmount: Any? = null,

	@field:SerializedName("order_instructions")
	val orderInstructions: Any? = null,

	@field:SerializedName("order_promised_time")
	val orderPromisedTime: Any? = null,

	@field:SerializedName("coupon_code_id")
	val couponCodeId: Any? = null,

	@field:SerializedName("order_delivery_fee")
	val orderDeliveryFee: Int? = null,

	@field:SerializedName("order_tax")
	val orderTax: Int? = null,

	@field:SerializedName("order_location_id")
	val orderLocationId: Any? = null,

	@field:SerializedName("order_coupon_code_discount")
	val orderCouponCodeDiscount: Any? = null,

	@field:SerializedName("order_creation_date")
	val orderCreationDate: String? = null,

	@field:SerializedName("order_total")
	val orderTotal: Int? = null,

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("order_subtotal")
	val orderSubtotal: Int? = null,

	@field:SerializedName("gift_card_id")
	val giftCardId: Int? = null
)