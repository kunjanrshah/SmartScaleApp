package com.krs.smart

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.krs.smart.di.component.AppComponent
import com.krs.smart.di.component.DaggerAppComponent
import com.krs.smart.di.modules.AppModule

class WeighingScaleApplication: Application() {
    lateinit var appComponent: AppComponent
    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent
            .builder()
            .appModule(AppModule(this))
            .build()

        registerActivityLifecycleCallbacks(object :ActivityLifecycleCallbacks{
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                appInForeground = true
            }

            override fun onActivityPaused(activity: Activity) {
                appInForeground = false
            }

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}

        })
    }

    companion object {
        @kotlin.jvm.JvmField
        var appInForeground: Boolean = false
    }
}