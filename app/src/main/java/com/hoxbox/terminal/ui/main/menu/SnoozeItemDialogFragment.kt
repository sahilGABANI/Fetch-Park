package com.hoxbox.terminal.ui.main.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.hoxbox.terminal.R
import com.hoxbox.terminal.base.BaseDialogFragment
import com.hoxbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hoxbox.terminal.base.extension.throttleClicks
import com.hoxbox.terminal.databinding.FragmentSnoozeItemDialogBinding

class SnoozeItemDialogFragment : BaseDialogFragment() {

    private var _binding: FragmentSnoozeItemDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSnoozeItemDialogBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        binding.confirmButton.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
        binding.turnOffUntilItTurnItOnRadioButton.throttleClicks().subscribeAndObserveOnMainThread {
            binding.snoozedOneHourRadioButton.isChecked = false
            binding.snoozedFourHourRadioButton.isChecked = false
            binding.snoozeUntilTomorrowRadioButton.isChecked = false
            binding.turnOffUntilItTurnItOnLinearLayout.isSelected = true
            binding.snoozedOneHourLinearLayout.isSelected = false
            binding.snoozedFourHourLinearLayout.isSelected = false
            binding.snoozeUntilTomorrowLinearLayout.isSelected = false
        }.autoDispose()

        binding.snoozedOneHourRadioButton.throttleClicks().subscribeAndObserveOnMainThread {
            binding.turnOffUntilItTurnItOnRadioButton.isChecked = false
            binding.snoozedOneHourRadioButton.isChecked = true
            binding.snoozedFourHourRadioButton.isChecked = false
            binding.snoozeUntilTomorrowRadioButton.isChecked = false
            binding.turnOffUntilItTurnItOnLinearLayout.isSelected = false
            binding.snoozedOneHourLinearLayout.isSelected = true
            binding.snoozedFourHourLinearLayout.isSelected = false
            binding.snoozeUntilTomorrowLinearLayout.isSelected = false
        }.autoDispose()
        binding.snoozedFourHourRadioButton.throttleClicks().subscribeAndObserveOnMainThread {
            binding.turnOffUntilItTurnItOnRadioButton.isChecked = false
            binding.snoozedOneHourRadioButton.isChecked = false
            binding.snoozedFourHourRadioButton.isChecked = true
            binding.snoozeUntilTomorrowRadioButton.isChecked = false
            binding.turnOffUntilItTurnItOnLinearLayout.isSelected = false
            binding.snoozedOneHourLinearLayout.isSelected = false
            binding.snoozedFourHourLinearLayout.isSelected = true
            binding.snoozeUntilTomorrowLinearLayout.isSelected = false
        }.autoDispose()
        binding.snoozeUntilTomorrowRadioButton.throttleClicks().subscribeAndObserveOnMainThread {
            binding.turnOffUntilItTurnItOnRadioButton.isChecked = false
            binding.snoozedOneHourRadioButton.isChecked = false
            binding.snoozedFourHourRadioButton.isChecked = false
            binding.snoozeUntilTomorrowRadioButton.isChecked = true
            binding.turnOffUntilItTurnItOnLinearLayout.isSelected = false
            binding.snoozedOneHourLinearLayout.isSelected = false
            binding.snoozedFourHourLinearLayout.isSelected = false
            binding.snoozeUntilTomorrowLinearLayout.isSelected = true
        }.autoDispose()
    }
}