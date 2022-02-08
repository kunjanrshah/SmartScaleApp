package com.krs.smart.di.component

import com.krs.smart.MainActivity
import com.krs.smart.di.modules.AppModule
import com.krs.smart.viewmodel.WeighingScaleViewModelFactory
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {
    fun getUsersViewModelFactory(): WeighingScaleViewModelFactory
    fun inject(mainActivity: MainActivity)
}