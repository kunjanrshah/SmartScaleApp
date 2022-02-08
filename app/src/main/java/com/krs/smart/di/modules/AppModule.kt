package com.krs.smart.di.modules

import android.content.Context
import com.krs.smart.WeighingScaleApplication
import com.krs.smart.datalayer.WeighingScalesRepository
import com.krs.smart.room.dao.WeighingScaleDao
import com.krs.smart.room.database.WeighingScaleDatabase
import com.krs.smart.viewmodel.WeighingScaleViewModelFactory
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