package com.hoxbox.terminal.ui.userstore

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.hoxbox.terminal.R
import com.hoxbox.terminal.api.clockinout.model.ClockType
import com.hoxbox.terminal.api.userstore.model.LocationsItem
import com.hoxbox.terminal.base.BaseBottomSheetDialogFragment
import com.hoxbox.terminal.base.extension.putEnum
import com.hoxbox.terminal.base.network.NetworkError
import com.hoxbox.terminal.databinding.NearByLocationBottomSheetBinding
import com.hoxbox.terminal.ui.main.CheckInDialogFragment
import com.hoxbox.terminal.ui.userstore.view.CategoryAdapter
import com.hoxbox.terminal.ui.userstore.view.NearByLocationAdapter

class NearByLocationBottomSheetFragment : BaseBottomSheetDialogFragment() {

    private var listOfLocation: ArrayList<LocationsItem>? = arrayListOf()
    private var _binding: NearByLocationBottomSheetBinding? = null
    private val binding get() = _binding!!
    private lateinit var nearByLocationAdapter: NearByLocationAdapter

    companion object {
        const val NEAR_LOCATION_INFO = "location_info"
        fun newInstance(clockType: ArrayList<LocationsItem>): NearByLocationBottomSheetFragment {
            val args = Bundle()
            args.putParcelableArrayList(NEAR_LOCATION_INFO, clockType)
            val fragment = NearByLocationBottomSheetFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = NearByLocationBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }
    @SuppressLint("SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }
        dialog?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        listOfLocation = arguments?.getParcelableArrayList<LocationsItem>(NEAR_LOCATION_INFO)
        initAdapter()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

    private fun initAdapter() {
        nearByLocationAdapter = NearByLocationAdapter(requireContext())
        nearByLocationAdapter.listOfLocation = listOfLocation
        binding.rvLocation.adapter = nearByLocationAdapter
    }

}