package com.sureja.accountant.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sureja.accountant.data.IssueRepository
import com.sureja.accountant.data.network.IssueDetailResponse
import com.sureja.accountant.data.network.IssueDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IssuesState(
    val issues: List<IssueDto> = emptyList(),
    val loading: Boolean = false,
    val submitting: Boolean = false,
    val submitted: Boolean = false,
    val detail: IssueDetailResponse? = null,
    val screenshots: Map<String, Bitmap> = emptyMap(),
    val error: String? = null,
)

@HiltViewModel
class IssuesViewModel @Inject constructor(private val repository: IssueRepository) : ViewModel() {
    private val mutable = MutableStateFlow(IssuesState())
    val state = mutable.asStateFlow()
    init { refresh() }

    fun refresh() = viewModelScope.launch {
        mutable.value = mutable.value.copy(loading = true, error = null)
        try {
            mutable.value = mutable.value.copy(issues = repository.list(), loading = false)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutable.value = mutable.value.copy(loading = false, error = "Could not load reports. Check your connection.")
        }
    }

    fun submit(category: String, title: String, description: String, uris: List<Uri>) = viewModelScope.launch {
        mutable.value = mutable.value.copy(submitting = true, submitted = false, error = null)
        try {
            repository.create(category, title, description, uris)
            mutable.value = mutable.value.copy(submitting = false, submitted = true)
            refresh()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            mutable.value = mutable.value.copy(submitting = false, error = error.message?.takeIf { error is IllegalArgumentException } ?: "Could not submit the report. Try again when online.")
        }
    }

    fun clearSubmitted() { mutable.value = mutable.value.copy(submitted = false) }
    fun clearError() { mutable.value = mutable.value.copy(error = null) }
    fun closeDetail() { mutable.value = mutable.value.copy(detail = null, screenshots = emptyMap()) }

    fun openIssue(id: String) = viewModelScope.launch {
        mutable.value = mutable.value.copy(loading = true, error = null)
        try {
            val detail = repository.detail(id)
            mutable.value = mutable.value.copy(detail = detail, screenshots = emptyMap(), loading = false)
            detail.screenshots.forEach { screenshot ->
                val bitmap = repository.screenshot(id, screenshot.id)
                if (bitmap != null && mutable.value.detail?.issue?.id == id) {
                    mutable.value = mutable.value.copy(screenshots = mutable.value.screenshots + (screenshot.id to bitmap))
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutable.value = mutable.value.copy(loading = false, error = "Could not open this report.")
        }
    }
}
