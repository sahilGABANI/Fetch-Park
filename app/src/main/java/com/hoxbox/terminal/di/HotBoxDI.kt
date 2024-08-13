package com.hoxbox.terminal.di

import android.app.Application
import android.content.Context
import com.hoxbox.terminal.api.authentication.AuthenticationModule
import com.hoxbox.terminal.api.checkout.CheckOutModule
import com.hoxbox.terminal.api.clockinout.ClockInOutModule
import com.hoxbox.terminal.api.deliveries.DeliveriesModule
import com.hoxbox.terminal.api.giftcard.GiftCardModule
import com.hoxbox.terminal.api.menu.MenuModule
import com.hoxbox.terminal.api.order.OrderModule
import com.hoxbox.terminal.api.store.StoreModule
import com.hoxbox.terminal.api.stripe.PaymentAPIModule
import com.hoxbox.terminal.api.userstore.UserStoreModule
import com.hoxbox.terminal.api.viewmodelmodule.HotBoxViewModelProvider
import com.hoxbox.terminal.application.HotBox
import com.hoxbox.terminal.base.network.NetworkModule
import com.hoxbox.terminal.base.network.PaymentNetworkModule
import com.hoxbox.terminal.base.prefs.PrefsModule
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class HotboxAppModule(val app: Application) {

    @Provides
    @Singleton
    fun provideApplication(): Application {
        return app
    }

    @Provides
    @Singleton
    fun provideContext(): Context {
        return app
    }
}

@Singleton
@Component(
    modules = [
        HotboxAppModule::class,
        NetworkModule::class,
        AuthenticationModule::class,
        PrefsModule::class,
        OrderModule::class,
        HotBoxViewModelProvider::class,
        ClockInOutModule::class,
        StoreModule::class,
        DeliveriesModule::class,
        UserStoreModule::class,
        MenuModule::class,
        CheckOutModule::class,
        PaymentNetworkModule::class,
        PaymentAPIModule::class,
        GiftCardModule::class
    ]
)

interface HotBoxAppComponent : BaseAppComponent {
    fun inject(app: HotBox)
}
