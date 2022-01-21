package com.epamgarage.wirelessweighingscale.utils

import com.epamgarage.wirelessweighingscale.R
import com.epamgarage.wirelessweighingscale.room.model.ScaleType


fun ScaleType.getImgResourceId(): Int =
    when (this) {
        ScaleType.WIFI -> R.mipmap.ic_wifi_round
        ScaleType.INTERNET -> R.mipmap.ic_internet_round
        ScaleType.BLUETOOTH -> R.mipmap.ic_ble_round
    }