package com.krs.smart.viewmodel

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.krs.smart.datalayer.WeighingScalesRepository
import com.krs.smart.room.model.WeighingScale
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