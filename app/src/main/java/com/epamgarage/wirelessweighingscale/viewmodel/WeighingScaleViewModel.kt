package com.epamgarage.wirelessweighingscale.viewmodel

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.epamgarage.wirelessweighingscale.datalayer.WeighingScalesRepository
import com.epamgarage.wirelessweighingscale.room.model.WeighingScale
import kotlinx.coroutines.launch

class WeighingScaleViewModel(private val repository: WeighingScalesRepository) : ViewModel() {
    val showEmptyView = ObservableBoolean(false)

    val getAllScales = repository.getAllWeighingScales.asLiveData()
    fun insertScale(scale: WeighingScale) = viewModelScope.launch {
        repository.insertScale(scale)
    }
    fun deleteScale(scale: WeighingScale) = viewModelScope.launch {
        repository.deleteScale(scale)
    }
}