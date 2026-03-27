package com.flxrs.dankchat.data.api.helix

import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Single
class HelixApiStats {
    private val _totalRequests = AtomicInteger(0)
    private val _statusCounts = ConcurrentHashMap<Int, AtomicInteger>()

    val totalRequests: Int get() = _totalRequests.get()
    val statusCounts: Map<Int, Int> get() = _statusCounts.mapValues { it.value.get() }

    fun recordResponse(statusCode: Int) {
        _totalRequests.incrementAndGet()
        _statusCounts.getOrPut(statusCode) { AtomicInteger(0) }.incrementAndGet()
    }
}
