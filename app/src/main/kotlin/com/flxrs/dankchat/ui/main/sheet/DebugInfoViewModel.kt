package com.flxrs.dankchat.ui.main.sheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.debug.DebugSectionRegistry
import com.flxrs.dankchat.data.debug.DebugSectionSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class DebugInfoViewModel(
    debugSectionRegistry: DebugSectionRegistry,
) : ViewModel() {

    val sections: StateFlow<List<DebugSectionSnapshot>> = debugSectionRegistry.allSections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
