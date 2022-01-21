package com.epamgarage.wirelessweighingscale.di.modules

import android.content.Context
import com.epamgarage.wirelessweighingscale.WeighingScaleApplication
import com.epamgarage.wirelessweighingscale.datalayer.WeighingScalesRepository
import com.epamgarage.wirelessweighingscale.room.dao.WeighingScaleDao
import com.epamgarage.wirelessweighingscale.room.database.WeighingScaleDatabase
import com.epamgarage.wirelessweighingscale.viewmodel.WeighingScaleViewModelFactory
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(private val application: WeighingScaleApplication) {

    @Provides
    @Singleton
    fun providesApplicationContext(): Context = application

    @Provides
    @Singleton
    internal fun getWeighingScaleDatabase(context: Context): WeighingScaleDatabase {
        return WeighingScaleDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    internal fun getWeighingScaleDao(database: WeighingScaleDatabase): WeighingScaleDao {
        return database.scaleDao()
    }

    @Provides
    @Singleton
    internal fun getWeighingScaleRepository(weighingScaleDao: WeighingScaleDao): WeighingScalesRepository {
        return WeighingScalesRepository(weighingScaleDao)
    }

    @Provides
    @Singleton
    internal fun getWeighingScaleViewModelFactory(scalesRepository: WeighingScalesRepository):
            WeighingScaleViewModelFactory {
        return WeighingScaleViewModelFactory(scalesRepository)
    }
}