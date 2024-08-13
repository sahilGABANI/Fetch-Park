package com.hoxbox.terminal.ui.main.order.view

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.view.View
import androidx.core.content.ContextCompat
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.order.model.OrdersInfo
import com.hoxbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.throttleClicks
import com.hoxbox.terminal.base.extension.toDollar
import com.hoxbox.terminal.databinding.OrderDetailItemBinding
import com.hoxbox.terminal.helper.formatTo
import com.hoxbox.terminal.helper.toDate
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.*

class OrderDetailItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: OrderDetailItemBinding? = null
    private lateinit var orderinfo: OrdersInfo
    private var orderTotal: Double? = 0.00

    init {
        inflateUi()
    }

    private val orderStateSubject: PublishSubject<OrdersInfo> = PublishSubject.create()
    val orderActionState: Observable<OrdersInfo> = orderStateSubject.hide()

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.order_detail_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = OrderDetailItemBinding.bind(view)
    }

    @SuppressLint("SetTextI18n")
    fun bind(orderInfo: OrdersInfo) {
        orderinfo = orderInfo
        binding?.apply {
            orderIdTextView.text = "#${orderInfo.id}"
            if (orderInfo.guestName != null) {
                customerNameTextView.text = orderInfo.guestName
            } else {
                customerNameTextView.text = orderInfo.fullName()
            }
            orderTotal = orderInfo.orderTotal
            if (orderInfo.orderAdjustmentAmount != null && orderInfo.orderAdjustmentAmount != 0) {
                orderTotal = orderInfo.orderAdjustmentAmount.let { orderTotal?.plus(it) }
            }
            orderTotal?.let {
                val price = (orderTotal?.div(100)).toDollar()
                orderTotalAppCompatTextView.text = price
            }
//            val date =  orderInfo.orderCreationDate?.toDate("yyyy-MM-dd hh:mm:ss a")
//            val updatedTimeInMillis = date?.time?.plus((1 * 60 * 60 * 1000))
//            val formatter = SimpleDateFormat("MM/dd/yyyy, hh:mm a", Locale.getDefault())
//            val formattedTime  = formatter.format(updatedTimeInMillis)
//            orderDateAndTimeTextView.text = formattedTime
                orderDateAndTimeTextView.text =
                    orderInfo.orderCreationDate?.toDate("yyyy-MM-dd hh:mm:ss a")?.formatTo("MM/dd/yyyy, hh:mm a")
            promisedTimeTextView.text = orderInfo.orderPromisedTime?.toDate()?.formatTo("MM/dd/yyyy, hh:mm a")
            if (orderInfo.orderTypeId == 20) {
                orderTypeTextView.text = "Delivery " + orderInfo.orderType
            } else {
                orderTypeTextView.text = orderInfo.orderType
            }
            statusTextView.text = orderInfo.orderStatus
            orderIdDotBackgroundImageView.isActivated = orderInfo.orderModeId == 1
            orderinfo = orderInfo
            when (orderInfo.orderStatus?.lowercase()) {
                resources.getString(R.string.completed).lowercase() -> statusBackgroundCardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context, R.color.color_CFE2FE
                    )
                )
                resources.getString(R.string.packaging).lowercase() -> statusBackgroundCardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context, R.color.color_C0F2EC
                    )
                )
                resources.getString(R.string.delivered).lowercase() -> statusBackgroundCardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context, R.color.color_CFE2FE
                    )
                )
                resources.getString(R.string.received).lowercase() -> statusBackgroundCardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context, R.color.color_FFE0C2
                    )
                )
                resources.getString(R.string.dispatched).lowercase() -> statusBackgroundCardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context, R.color.color_C0F2EC
                    )
                )

                resources.getString(R.string.assigned).lowercase() -> statusBackgroundCardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context, R.color.color_FAEDBF
                    )
                )
                else -> statusBackgroundCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.light_green))
            }
            orderDetailLinearLayout.isSelected = orderInfo.isSelected
            nextArrowImageView.isSelected = orderInfo.isSelected
            orderDetailLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                orderDetailLinearLayout.isSelected = true
                nextArrowImageView.isSelected = true
                orderStateSubject.onNext(orderinfo)
            }.autoDispose()
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}