package com.hoxbox.terminal.api.viewmodelmodule

import com.hoxbox.terminal.api.authentication.AuthenticationRepository
import com.hoxbox.terminal.api.checkout.CheckOutRepository
import com.hoxbox.terminal.api.clockinout.ClockInOutRepository
import com.hoxbox.terminal.api.deliveries.DeliveriesRepository
import com.hoxbox.terminal.api.giftcard.GiftCardRepository
import com.hoxbox.terminal.api.menu.MenuRepository
import com.hoxbox.terminal.api.order.OrderRepository
import com.hoxbox.terminal.api.store.StoreRepository
import com.hoxbox.terminal.api.stripe.PaymentRepository
import com.hoxbox.terminal.api.userstore.UserStoreRepository
import com.hoxbox.terminal.ui.login.viewmodel.LoginViewModel
import com.hoxbox.terminal.ui.main.deliveries.viewmodel.DeliveriesViewModel
import com.hoxbox.terminal.ui.main.giftcard.viewmodel.GiftCardViewModel
import com.hoxbox.terminal.ui.main.menu.viewModel.MenuViewModel
import com.hoxbox.terminal.ui.main.order.viewmodel.OrderViewModel
import com.hoxbox.terminal.ui.main.orderdetail.viewmodel.OrderDetailsViewModel
import com.hoxbox.terminal.ui.main.store.viewmodel.StoreViewModel
import com.hoxbox.terminal.ui.main.viewmodel.ClockInOutViewModel
import com.hoxbox.terminal.ui.splash.viewmodel.LocationViewModel
import com.hoxbox.terminal.ui.userstore.checkout.viewmodel.CheckOutViewModel
import com.hoxbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import com.hoxbox.terminal.ui.userstore.viewmodel.UserStoreWelcomeViewModel
import dagger.Module
import dagger.Provides

@Module
class HotBoxViewModelProvider {

    @Provides
    fun provideLoginViewModel(
        authenticationRepository: AuthenticationRepository,
        storeRepository: StoreRepository,
        orderRepository: OrderRepository
    ): LoginViewModel {
        return LoginViewModel(
            authenticationRepository,
            storeRepository,
            orderRepository
        )
    }

    @Provides
    fun provideStartViewModel(
        authenticationRepository: AuthenticationRepository,
        orderRepository : OrderRepository
    ): LocationViewModel {
        return LocationViewModel(
            authenticationRepository,
            orderRepository
        )
    }

    @Provides
    fun provideClockInOutViewModel(
        clockInOutRepository: ClockInOutRepository,
        storeRepository: StoreRepository
    ): ClockInOutViewModel {
        return ClockInOutViewModel(
            clockInOutRepository,
            storeRepository
        )
    }

    @Provides
    fun provideStoreViewModel(
        storeRepository: StoreRepository,
        orderRepository: OrderRepository
    ): StoreViewModel {
        return StoreViewModel(
            storeRepository,
            orderRepository
        )
    }

    @Provides
    fun provideOrderViewModel(
        orderRepository: OrderRepository
    ): OrderViewModel {
        return OrderViewModel(
            orderRepository
        )
    }

    @Provides
    fun provideOrderDetailsViewModel(
        orderRepository: OrderRepository,
        paymentRepository: PaymentRepository
    ): OrderDetailsViewModel {
        return OrderDetailsViewModel(
            orderRepository,
            paymentRepository
        )
    }

    @Provides
    fun provideDeliveriesViewModel(
        deliveriesRepository: DeliveriesRepository
    ): DeliveriesViewModel {
        return DeliveriesViewModel(
            deliveriesRepository
        )
    }

    @Provides
    fun provideUserStoreViewModel(
        userStoreRepository: UserStoreRepository,
        paymentRepository: PaymentRepository,
        storeRepository: StoreRepository,
        orderRepository: OrderRepository
    ): UserStoreViewModel {
        return UserStoreViewModel(
            userStoreRepository,
            paymentRepository,
            storeRepository,
            orderRepository
        )
    }

    @Provides
    fun provideMenuViewModel(
        menuRepository: MenuRepository
    ): MenuViewModel {
        return MenuViewModel(
            menuRepository
        )
    }

    @Provides
    fun provideCheckOutViewModel(
        checkOutRepository: CheckOutRepository
    ): CheckOutViewModel {
        return CheckOutViewModel(
            checkOutRepository
        )
    }

    @Provides
    fun provideUserStoreWelcomeViewModel(
        userStoreRepository: UserStoreRepository,
    ): UserStoreWelcomeViewModel {
        return UserStoreWelcomeViewModel(
            userStoreRepository
        )
    }

    @Provides
    fun provideGiftCardViewModel(
        giftCardRepository: GiftCardRepository,
        paymentRepository: PaymentRepository,
        storeRepository: StoreRepository
    ): GiftCardViewModel {
        return GiftCardViewModel(giftCardRepository,paymentRepository,storeRepository)
    }

}