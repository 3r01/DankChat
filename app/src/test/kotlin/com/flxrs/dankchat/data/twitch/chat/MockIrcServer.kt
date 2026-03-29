package com.flxrs.dankchat.data.twitch.chat

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MockIrcServer : AutoCloseable {
    private val server = MockWebServer()
    private var serverSocket: WebSocket? = null
    val sentFrames = CopyOnWriteArrayList<String>()
    private val connectedLatch = CountDownLatch(1)

    private val listener =
        object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                serverSocket = webSocket
                connectedLatch.countDown()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                text.trimEnd('\r', '\n').split("\r\n").forEach { line ->
                    sentFrames.add(line)
                    handleIrcCommand(webSocket, line)
                }
            }
        }

    val url: String get() = server.url("/").toString().replace("http://", "ws://")

    fun start() {
        server.enqueue(MockResponse.Builder().webSocketUpgrade(listener).build())
        server.start()
    }

    fun awaitConnection(timeout: Long = 5, unit: TimeUnit = TimeUnit.SECONDS): Boolean = connectedLatch.await(timeout, unit)

    fun sendToClient(ircLine: String) {
        serverSocket?.send("$ircLine\r\n")
    }

    override fun close() {
        serverSocket?.close(1000, null)
        server.close()
    }

    private fun handleIrcCommand(webSocket: WebSocket, line: String) {
        when {
            line.startsWith("NICK ") -> {
                val nick = line.removePrefix("NICK ")
                sendMotd(webSocket, nick)
            }

            line.startsWith("JOIN ") -> {
                val channels = line.removePrefix("JOIN ").split(",")
                channels.forEach { channel ->
                    val ch = channel.trim()
                    webSocket.send(":$NICK!$NICK@$NICK.tmi.twitch.tv JOIN $ch\r\n")
                    webSocket.send(":tmi.twitch.tv 353 $NICK = $ch :$NICK\r\n")
                    webSocket.send(":tmi.twitch.tv 366 $NICK $ch :End of /NAMES list\r\n")
                }
            }

            line.startsWith("PING") -> {
                webSocket.send(":tmi.twitch.tv PONG tmi.twitch.tv\r\n")
            }
        }
    }

    private fun sendMotd(webSocket: WebSocket, nick: String) {
        webSocket.send(":tmi.twitch.tv 001 $nick :Welcome, GLHF!\r\n")
        webSocket.send(":tmi.twitch.tv 002 $nick :Your host is tmi.twitch.tv\r\n")
        webSocket.send(":tmi.twitch.tv 003 $nick :This server is rather new\r\n")
        webSocket.send(":tmi.twitch.tv 004 $nick :-\r\n")
        webSocket.send(":tmi.twitch.tv 375 $nick :-\r\n")
        webSocket.send(":tmi.twitch.tv 372 $nick :You are in a maze of twisty passages.\r\n")
        webSocket.send(":tmi.twitch.tv 376 $nick :>\r\n")
    }

    private companion object {
        const val NICK = "justinfan12781923"
    }
}
