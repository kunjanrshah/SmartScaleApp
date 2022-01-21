package com.epamgarage.wirelessweighingscale.di.component

import com.epamgarage.wirelessweighingscale.MainActivity
import com.epamgarage.wirelessweighingscale.di.modules.AppModule
import com.epamgarage.wirelessweighingscale.viewmodel.WeighingScaleViewModelFactory
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {
    fun getUsersViewModelFactory(): WeighingScaleViewModelFactory
    fun inject(mainActivity: MainActivity)
}