package com.epamgarage.wirelessweighingscale.room.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weighing_scale")
data class WeighingScale(
    var visible: Int,
    var weights: String,
    var name: String,
    val type: ScaleType,
    val addedAt: Long) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}
