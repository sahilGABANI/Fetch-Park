package com.hoxbox.terminal.di

import android.app.Application
import com.hoxbox.terminal.ui.userstore.guest.GuestProductDetailsDialogFragment
import com.hoxbox.terminal.ui.login.LoginActivity
import com.hoxbox.terminal.ui.main.CheckInDialogFragment
import com.hoxbox.terminal.ui.main.MainActivity
import com.hoxbox.terminal.ui.main.deliveries.AssignDriverDialogFragment
import com.hoxbox.terminal.ui.main.deliveries.DeliveriesFragment
import com.hoxbox.terminal.ui.main.deliveries.DeliveriesOrderDetailsFragment
import com.hoxbox.terminal.ui.main.giftcard.GiftCardFragment
import com.hoxbox.terminal.ui.main.menu.MenuFragment
import com.hoxbox.terminal.ui.main.menu.view.MenuDetailItemView
import com.hoxbox.terminal.ui.main.order.OrdersFragment
import com.hoxbox.terminal.ui.main.orderdetail.OrderDetailsFragment
import com.hoxbox.terminal.ui.main.orderdetail.PrintReceiptDialog
import com.hoxbox.terminal.ui.main.orderdetail.RefundFragmentDialog
import com.hoxbox.terminal.ui.main.settings.SettingsFragment
import com.hoxbox.terminal.ui.main.store.StoreFragment
import com.hoxbox.terminal.ui.main.timemanagement.TimeManagementFragment
import com.hoxbox.terminal.ui.splash.SplashActivity
import com.hoxbox.terminal.ui.userstore.CompAndAdjustmentActivity
import com.hoxbox.terminal.ui.userstore.deliveryaddress.DeliveryAddressActivity
import com.hoxbox.terminal.ui.userstore.UserStoreActivity
import com.hoxbox.terminal.ui.userstore.welcome.UserStoreWelcomeActivity
import com.hoxbox.terminal.ui.userstore.checkout.CheckOutFragment
import com.hoxbox.terminal.ui.userstore.cookies.CookiesFragment
import com.hoxbox.terminal.ui.userstore.guest.TakeNBackDialogFragment
import com.hoxbox.terminal.ui.userstore.loyaltycard.JoinLoyaltyProgramDialog
import com.hoxbox.terminal.ui.userstore.payment.PaymentFragment
import com.hoxbox.terminal.ui.wifi.AvailableWifiActivity
import com.hoxbox.terminal.ui.wifi.NoWifiActivity
import com.hoxbox.terminal.ui.wifi.SelectWifiActivity

abstract class BaseUiApp : Application() {

    abstract fun setAppComponent(baseAppComponent: BaseAppComponent)
    abstract fun getAppComponent(): BaseAppComponent
}

interface BaseAppComponent {
    fun inject(app: Application)
    fun inject(loginActivity: LoginActivity)
    fun inject(splashActivity: SplashActivity)
    fun inject(noWifiActivity: NoWifiActivity)
    fun inject(availableWifiActivity: AvailableWifiActivity)
    fun inject(selectWifiActivity: SelectWifiActivity)
    fun inject(checkInDialogFragment: CheckInDialogFragment)
    fun inject(mainActivity: MainActivity)
    fun inject(timeManagementFragment: TimeManagementFragment)
    fun inject(storeFragment: StoreFragment)
    fun inject(ordersFragment: OrdersFragment)
    fun inject(orderDetailsFragment: OrderDetailsFragment)
    fun inject(deliveriesFragment: DeliveriesFragment)
    fun inject(deliveriesOrderDetailsFragment: DeliveriesOrderDetailsFragment)
    fun inject(userStoreActivity: UserStoreActivity)
    fun inject(cookiesFragment: CookiesFragment)
    fun inject(menuFragment: MenuFragment)
    fun inject(guestProductDetailsDialogFragment: GuestProductDetailsDialogFragment)
    fun inject(takeNBackDialogFragment: TakeNBackDialogFragment)
    fun inject(paymentFragment: PaymentFragment)
    fun inject(checkOutFragment: CheckOutFragment)
    fun inject(joinLoyaltyProgramDialog: JoinLoyaltyProgramDialog)
    fun inject(giftCardFragment: GiftCardFragment)
    fun inject(userStoreWelcomeActivity: UserStoreWelcomeActivity)
    fun inject(deliveryAddressActivity: DeliveryAddressActivity)
    fun inject(printReceiptDialog: PrintReceiptDialog)
    fun inject(settingsFragment: SettingsFragment)
    fun inject(compAndAdjustmentActivity: CompAndAdjustmentActivity)
    fun inject(refundFragmentDialog: RefundFragmentDialog)
    fun inject(menuDetailItemView: MenuDetailItemView)
    fun inject(assignDriverDialogFragment: AssignDriverDialogFragment)
}

fun BaseUiApp.getComponent(): BaseAppComponent {
    return this.getAppComponent()
}