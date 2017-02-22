package com.games.soywiz.korgekingdom

import com.soywiz.korio.async.ProduceConsumer
import com.soywiz.korio.util.Extra
import io.vertx.core.http.ServerWebSocket

open class VertxClientChannel(
        private val ws: ServerWebSocket
) : Channel, Extra by Extra.Mixin() {
    val messageQueue = ProduceConsumer<Packet>()

    init {
        ws.handler { messageQueue.produce(Packet.deserialize(it.toString(Charsets.UTF_8))) }
    }

    suspend override fun send(packet: Packet): Unit = Unit.apply { ws.writeFinalTextFrame(Packet.serialize(packet)) }
    suspend override fun read(): Packet = messageQueue.consume() as Packet
}