package com.epamgarage.wirelessweighingscale.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.epamgarage.wirelessweighingscale.datalayer.WeighingScalesRepository

@Suppress("UNCHECKED_CAST")
class WeighingScaleViewModelFactory(private val repository: WeighingScalesRepository): ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return WeighingScaleViewModel(repository)  as T
    }
}