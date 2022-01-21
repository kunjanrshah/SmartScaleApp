package com.epamgarage.wirelessweighingscale

import android.app.Application
import com.epamgarage.wirelessweighingscale.di.component.AppComponent
import com.epamgarage.wirelessweighingscale.di.component.DaggerAppComponent
import com.epamgarage.wirelessweighingscale.di.modules.AppModule

class WeighingScaleApplication: Application() {
    lateinit var appComponent: AppComponent
    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent
            .builder()
            .appModule(AppModule(this))
            .build()
    }
}