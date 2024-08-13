package com.hoxbox.terminal.ui.main.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hoxbox.terminal.ui.main.settings.SettingsFragment
import com.hoxbox.terminal.ui.main.deliveries.DeliveriesFragment
import com.hoxbox.terminal.ui.main.giftcard.GiftCardFragment
import com.hoxbox.terminal.ui.main.menu.MenuFragment
import com.hoxbox.terminal.ui.main.order.OrdersFragment
import com.hoxbox.terminal.ui.main.store.StoreFragment
import com.hoxbox.terminal.ui.main.timemanagement.TimeManagementFragment

class SideNavigationAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount() = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                OrdersFragment.newInstance()
            }
            1 -> {
//                DeliveriesFragment.newInstance()
                MenuFragment.newInstance()
            }
            2 -> {
//                MenuFragment.newInstance()
                StoreFragment.newInstance()
            }
            3-> {
//                TimeManagementFragment.newInstance()
                GiftCardFragment.newInstance()
            }
//            4-> {
//                StoreFragment.newInstance()
//            }
//            5 -> {
//                GiftCardFragment.newInstance()
//            }
//            6 -> {
//                SettingsFragment.newInstance()
//            }
            else -> {
                OrdersFragment.newInstance()
            }
        }
    }
}