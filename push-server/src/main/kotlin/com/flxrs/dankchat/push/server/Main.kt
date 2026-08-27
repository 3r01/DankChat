package com.flxrs.dankchat.push.server

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val config = ServerConfig.fromEnvironment()
    embeddedServer(Netty, host = config.host, port = config.port) {
        pushServer(config)
    }.start(wait = true)
}
