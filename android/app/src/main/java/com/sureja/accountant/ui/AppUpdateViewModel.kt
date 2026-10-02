package com.sureja.accountant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sureja.accountant.data.AppUpdateRepository
import com.sureja.accountant.domain.AppVersionPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUpdateState(val policy: AppVersionPolicy? = null, val checked: Boolean = false)
@HiltViewModel
class AppUpdateViewModel @Inject constructor(private val repository: AppUpdateRepository) : ViewModel() {
    private val _state = MutableStateFlow(AppUpdateState(repository.cached()))
    val state = _state.asStateFlow()
    private var check: Job? = null
    fun refresh() {
        if (check?.isActive == true) return
        check = viewModelScope.launch { _state.value = AppUpdateState(repository.refresh(), checked = true) }
    }
}
