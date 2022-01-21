package com.epamgarage.wirelessweighingscale.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.epamgarage.wirelessweighingscale.room.model.WeighingScale
import kotlinx.coroutines.flow.Flow

@Dao
interface WeighingScaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scale: WeighingScale)

    @Query("DELETE FROM weighing_scale")
    suspend fun deleteAllWeighingScale()

    @Query("DELETE FROM weighing_scale where id = :id")
    suspend fun deleteWeighingScaleById(id: Int)

    @Query("SELECT * FROM weighing_scale ORDER BY id ASC")
    fun getAllWeighingScales(): Flow<MutableList<WeighingScale>>
}