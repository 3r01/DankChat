package com.flxrs.dankchat.data.debug

import com.flxrs.dankchat.data.repo.chat.ChatMessageRepository
import com.flxrs.dankchat.data.repo.data.DataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Single

@Single
class ErrorsDebugSection(
    private val dataRepository: DataRepository,
    private val chatMessageRepository: ChatMessageRepository,
) : DebugSection {

    override val order = 9
    override val baseTitle = "Errors"

    override fun entries(): Flow<DebugSectionSnapshot> {
        return combine(dataRepository.dataLoadingFailures, chatMessageRepository.chatLoadingFailures) { dataFailures, chatFailures ->
            val totalFailures = dataFailures.size + chatFailures.size
            val entries = buildList {
                add(DebugEntry("Total failures", "$totalFailures"))
                dataFailures.forEach { failure ->
                    add(DebugEntry(failure.step::class.simpleName ?: "Unknown", failure.failure.message ?: "Unknown error"))
                }
                chatFailures.forEach { failure ->
                    add(DebugEntry(failure.step::class.simpleName ?: "Unknown", failure.failure.message ?: "Unknown error"))
                }
            }
            DebugSectionSnapshot(title = baseTitle, entries = entries)
        }
    }
}
