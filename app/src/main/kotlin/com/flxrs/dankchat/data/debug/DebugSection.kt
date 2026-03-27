package com.flxrs.dankchat.data.debug

import kotlinx.coroutines.flow.Flow

interface DebugSection {
    val baseTitle: String
    val order: Int
    fun entries(): Flow<DebugSectionSnapshot>
}

data class DebugSectionSnapshot(val title: String, val entries: List<DebugEntry>)

data class DebugEntry(val label: String, val value: String)
