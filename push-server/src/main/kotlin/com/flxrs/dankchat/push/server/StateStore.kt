package com.flxrs.dankchat.push.server

import com.flxrs.dankchat.push.PushConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class StateStore(
    private val path: Path,
    private val json: Json = Json { prettyPrint = true },
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(load())
    val state = _state.asStateFlow()

    suspend fun updateConfiguration(configuration: PushConfiguration): Boolean =
        mutex.withLock {
            val previous = _state.value.configuration
            if (previous != null && configuration.revision <= previous.revision) {
                return false
            }
            persist(_state.value.copy(configuration = configuration))
            true
        }

    suspend fun addDevice(token: String) =
        mutex.withLock {
            persist(_state.value.copy(devices = _state.value.devices + token))
        }

    suspend fun removeDevice(token: String) =
        mutex.withLock {
            persist(_state.value.copy(devices = _state.value.devices - token))
        }

    suspend fun updateTwitchTokens(tokens: TwitchTokens) =
        mutex.withLock {
            persist(_state.value.copy(twitchTokens = tokens))
        }

    private fun load(): ServerState {
        if (!Files.exists(path)) {
            return ServerState()
        }
        return json.decodeFromString(Files.readString(path))
    }

    private fun persist(value: ServerState) {
        Files.createDirectories(path.parent)
        val temporary = path.resolveSibling("${path.fileName}.tmp")
        Files.writeString(temporary, json.encodeToString(ServerState.serializer(), value))
        try {
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
        }
        _state.value = value
    }
}
