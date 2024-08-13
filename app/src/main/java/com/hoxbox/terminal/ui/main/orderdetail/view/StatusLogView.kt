package com.hoxbox.terminal.ui.main.orderdetail.view

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.order.model.StatusItem
import com.hoxbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hoxbox.terminal.databinding.ViewStatusListBinding
import com.hoxbox.terminal.helper.formatTo
import com.hoxbox.terminal.helper.toDate
import java.util.*

class StatusLogView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private lateinit var binding: ViewStatusListBinding
    private lateinit var statuslogInfo: StatusItem

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_status_list, this)
        binding = ViewStatusListBinding.bind(view)
    }

    fun bind(statusItem: StatusItem) {
        statuslogInfo = statusItem

        binding.apply {
//            customerTextView.text = statusItem.customerName
            val date =  statusItem.timestamp?.toDate("yyyy-MM-dd hh:mm:ss a")?.formatTo("MM/dd/yyyy, hh:mm a")
//            val updatedTimeInMillis = date?.time?.plus((1 * 60 * 60 * 1000))
//            val formatter = SimpleDateFormat("MM/dd/yyyy, hh:mm a", Locale.getDefault())
//            val formattedTime  = formatter.format(updatedTimeInMillis)
//            println("formattedTime :$formattedTime")
            dateAndTimeTextView.text = date ?: ""
            orderStatusTextView.text = statusItem.orderStatus
            customerTextView.text = statusItem.fullName()
            if (!statusItem.roleName.isNullOrEmpty()){
                roleNameTextView.text = statusItem.roleName
                roleNameTextView.isVisible = true
            } else {
                roleNameTextView.isVisible = false
            }

            when (statusItem.orderStatus) {
                resources.getString(R.string.received).lowercase() -> statusCardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.color_FFE0C2
                    )
                )
                resources.getString(R.string.packaging).lowercase() -> statusCardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.color_C0F2EC
                    )
                )
                resources.getString(R.string.completed).lowercase() -> statusCardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.color_CFE2FE
                    )
                )
                resources.getString(R.string.assigned).lowercase() -> statusCardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.color_FAEDBF
                    )
                )
                resources.getString(R.string.dispatched).lowercase() -> statusCardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.color_C0F2EC
                    )
                )
                resources.getString(R.string.delivered).lowercase() -> statusCardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.color_CFE2FE
                    )
                )
                resources.getString(R.string.cancelled_refunded).lowercase() -> statusCardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.color_E7ECED
                    )
                )
                else -> statusCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.light_green))
            }
        }
    }
}