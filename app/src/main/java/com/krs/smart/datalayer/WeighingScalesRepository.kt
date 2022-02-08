package com.krs.smart.datalayer

import androidx.annotation.WorkerThread
import com.krs.smart.room.dao.WeighingScaleDao
import com.krs.smart.room.model.WeighingScale
import kotlinx.coroutines.flow.Flow

class WeighingScalesRepository(private val weighingScaleDao: WeighingScaleDao) {
    val getAllWeighingScales: Flow<List<WeighingScale>> = weighingScaleDao.getAllWeighingScales()
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insertScale(scale: WeighingScale) {
        weighingScaleDao.insert(scale)
    }
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun deleteScale(scale: WeighingScale) {
        weighingScaleDao.deleteWeighingScaleById(scale.id)
    }
}