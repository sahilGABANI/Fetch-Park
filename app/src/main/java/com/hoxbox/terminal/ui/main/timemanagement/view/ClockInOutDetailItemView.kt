package com.hoxbox.terminal.ui.main.timemanagement.view

import android.content.Context
import android.view.View
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.clockinout.model.TimeResponse
import com.hoxbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hoxbox.terminal.databinding.ClockinOutDetailsItemBinding

class ClockInOutDetailItemView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private lateinit var binding: ClockinOutDetailsItemBinding

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.clockin_out_details_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ClockinOutDetailsItemBinding.bind(view)
    }

    fun bind(clockInOutDetailsInfo: TimeResponse) {
        val date = clockInOutDetailsInfo.getActionFormattedTime("MM/dd/yyyy")
        val timeDate = clockInOutDetailsInfo.getActionFormattedTime()
        val clockOut = clockInOutDetailsInfo.getClockOutFormattedTime()
        binding.dateTextView.text = date
        binding.clockInTimeTextView.text = timeDate
        binding.timeTextView.text = if(clockOut.isNotEmpty()) clockOut else "-"
    }
}