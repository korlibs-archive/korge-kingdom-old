package com.games.soywiz.korgekingdom

import com.soywiz.korio.async.ProduceConsumer
import com.soywiz.korio.util.Extra
import io.vertx.core.http.ServerWebSocket
import java.io.EOFException

open class VertxWebsocketClient(
        private val ws: ServerWebSocket
) : CClient, Extra by Extra.Mixin() {
    val messageQueue = ProduceConsumer<Packet>()

    init {
        ws.handler { messageQueue.produce(Packet.deserialize(it.toString(Charsets.UTF_8))) }
        ws.closeHandler {
            messageQueue.close()
        }
    }

    suspend override fun send(packet: Packet): Unit = Unit.apply { ws.writeFinalTextFrame(Packet.serialize(packet)) }
    suspend override fun read(): Packet = messageQueue.consume() ?: throw EOFException()
}