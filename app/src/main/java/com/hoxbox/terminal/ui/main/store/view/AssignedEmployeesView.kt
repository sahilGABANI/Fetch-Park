package com.hoxbox.terminal.ui.main.store.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.store.model.AssignedEmployeeInfo
import com.hoxbox.terminal.base.ConstraintLayoutWithLifecycle
import com.hoxbox.terminal.databinding.AssignedEmployeesDetailsBinding
import com.hoxbox.terminal.helper.formatTo
import com.hoxbox.terminal.helper.toDate

class AssignedEmployeesView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: AssignedEmployeesDetailsBinding? = null

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.assigned_employees_details, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = AssignedEmployeesDetailsBinding.bind(view)
    }

    @SuppressLint("SetTextI18n")
    fun bind(assignedEmployeeInfo: AssignedEmployeeInfo) {
        binding?.apply {
            employeeDateAppCompatTextView.text = assignedEmployeeInfo.assigned?.toDate()?.formatTo("MM/dd/yyyy")
            employeeNameAppCompatTextView.text = assignedEmployeeInfo.firstName + " " + assignedEmployeeInfo.lastName
            employeePositionTextView.text = assignedEmployeeInfo.roleName
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}