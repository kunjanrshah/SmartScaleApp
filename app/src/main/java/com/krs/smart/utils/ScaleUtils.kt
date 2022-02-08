package com.krs.smart.utils

import com.krs.smart.R
import com.krs.smart.room.model.ScaleType


fun ScaleType.getImgResourceId(): Int =
    when (this) {
        ScaleType.WIFI -> R.mipmap.ic_wifi_round
        ScaleType.INTERNET -> R.mipmap.ic_internet_round
        ScaleType.BLUETOOTH -> R.mipmap.ic_ble_round
    }