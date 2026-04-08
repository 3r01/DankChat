package com.flxrs.dankchat.ui.log

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.flxrs.dankchat.data.repo.log.LogLevel
import com.flxrs.dankchat.data.repo.log.LogLine
import com.flxrs.dankchat.data.repo.log.LogLineParser
import com.flxrs.dankchat.data.repo.log.LogRepository
import com.flxrs.dankchat.ui.main.LogViewer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import java.io.File

@KoinViewModel
class LogViewerViewModel(
    savedStateHandle: SavedStateHandle,
    private val logRepository: LogRepository,
) : ViewModel() {
    val searchFieldState = TextFieldState()

    private val _levelFilter = MutableStateFlow<LogLevel?>(null)
    private val _selectedFileName = MutableStateFlow("")
    private val _selectedIndices = MutableStateFlow<Set<Int>>(emptySet())
    val selectedIndices: StateFlow<Set<Int>> = _selectedIndices.asStateFlow()

    init {
        val route = savedStateHandle.toRoute<LogViewer>()
        val initialFile = route.fileName.takeIf { it.isNotEmpty() }
        _selectedFileName.value = initialFile ?: logRepository
            .getLogFiles()
            .firstOrNull()
            ?.name
            .orEmpty()

        viewModelScope.launch {
            combine(_levelFilter, snapshotFlow { searchFieldState.text.toString() }) { _, _ -> }.collect {
                _selectedIndices.value = emptySet()
            }
        }
    }

    val state: StateFlow<LogViewerState> = combine(
        _selectedFileName,
        _levelFilter,
        snapshotFlow { searchFieldState.text.toString() },
    ) { fileName, levelFilter, searchQuery ->
        val files = logRepository.getLogFiles()
        val lines = if (fileName.isNotEmpty()) {
            LogLineParser.parseAll(logRepository.readLogFile(fileName)).toImmutableList()
        } else {
            emptyList<LogLine>().toImmutableList()
        }
        val filtered = filterLines(lines, levelFilter, searchQuery)
        LogViewerState(
            lines = filtered,
            levelFilter = levelFilter,
            availableFiles = files.map { it.name }.toImmutableList(),
            selectedFileName = fileName,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LogViewerState())

    fun selectFile(fileName: String) {
        _selectedFileName.value = fileName
    }

    fun setLevelFilter(level: LogLevel?) {
        _levelFilter.update { current ->
            if (current == level) null else level
        }
    }

    fun toggleSelection(index: Int) {
        _selectedIndices.update { current ->
            when (index) {
                in current -> current - index
                else -> current + index
            }
        }
    }

    fun clearSelection() {
        _selectedIndices.value = emptySet()
    }

    fun getSelectedLinesText(): String {
        val lines = state.value.lines
        return _selectedIndices.value
            .sorted()
            .mapNotNull { lines.getOrNull(it) }
            .joinToString("\n") { it.raw }
    }

    fun getShareableLogFile(): File? {
        val fileName = _selectedFileName.value
        if (fileName.isEmpty()) return null
        return logRepository.getLogFile(fileName)
    }

    private fun filterLines(
        lines: ImmutableList<LogLine>,
        levelFilter: LogLevel?,
        searchQuery: String,
    ): ImmutableList<LogLine> {
        if (levelFilter == null && searchQuery.isBlank()) return lines

        return lines
            .filter { line ->
                val matchesLevel = levelFilter == null || line.level >= levelFilter
                val matchesSearch = searchQuery.isBlank() || line.raw.contains(searchQuery, ignoreCase = true)
                matchesLevel && matchesSearch
            }.toImmutableList()
    }
}
