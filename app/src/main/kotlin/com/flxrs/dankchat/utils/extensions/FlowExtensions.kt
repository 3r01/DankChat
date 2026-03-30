package com.flxrs.dankchat.utils.extensions

import com.flxrs.dankchat.data.UserName
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.transformLatest

fun <T> mutableSharedFlowOf(
    defaultValue: T,
    replayValue: Int = 1,
    extraBufferCapacity: Int = 0,
    onBufferOverflow: BufferOverflow = BufferOverflow.DROP_OLDEST,
): MutableSharedFlow<T> =
    MutableSharedFlow<T>(replayValue, extraBufferCapacity, onBufferOverflow).apply {
        tryEmit(defaultValue)
    }

inline fun <T, R> Flow<T?>.flatMapLatestOrDefault(
    defaultValue: R,
    crossinline transform: suspend (value: T) -> Flow<R>,
): Flow<R> =
    transformLatest {
        when (it) {
            null -> emit(defaultValue)
            else -> emitAll(transform(it))
        }
    }

inline val <T> SharedFlow<T>.firstValue: T
    get() = replayCache.first()

inline val <T> SharedFlow<T>.firstValueOrNull: T?
    get() = replayCache.firstOrNull()

fun MutableSharedFlow<MutableMap<UserName, Int>>.increment(
    key: UserName,
    amount: Int,
) = tryEmit(
    firstValue.apply {
        val count = get(key) ?: 0
        put(key, count + amount)
    },
)

fun MutableSharedFlow<MutableMap<UserName, Int>>.clear(key: UserName) =
    tryEmit(
        firstValue.apply {
            put(key, 0)
        },
    )

fun <T1, T2, T3, T4, T5, T6, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> R,
): Flow<R> =
    combine(flow1, flow2, flow3, flow4, flow5, flow6) { args: Array<*> ->
        @Suppress("UNCHECKED_CAST")
        transform(
            args[0] as T1,
            args[1] as T2,
            args[2] as T3,
            args[3] as T4,
            args[4] as T5,
            args[5] as T6,
        )
    }

fun <T> MutableSharedFlow<MutableMap<UserName, T>>.assign(
    key: UserName,
    value: T,
) = tryEmit(
    firstValue.apply {
        put(key, value)
    },
)
