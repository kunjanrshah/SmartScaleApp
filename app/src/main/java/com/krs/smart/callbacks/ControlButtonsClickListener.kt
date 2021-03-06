package com.krs.smart.callbacks

interface ControlButtonsClickListener {
    fun onTitleClick(position: Int)
    fun onTareClick(position: Int)
    fun onModeClick(position: Int)
    fun onMPlusIncClick(position: Int)
    fun onMRShiftClick(position: Int)
    fun onRefreshScaleFABClick(position: Int)
    fun onRemoveClick(position: Int)
}