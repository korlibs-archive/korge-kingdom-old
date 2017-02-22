package com.games.soywiz.korgekingdom

import com.soywiz.korio.async.ProduceConsumer
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.util.Dynamic
import com.soywiz.korio.util.Extra
import io.vertx.core.http.ServerWebSocket

open class VertxClientChannel(
        private val ws: ServerWebSocket
) : Channel, Extra by Extra.Mixin() {
    val messageQueue = ProduceConsumer<Packet>()
    val unprocessedMessageQueue = ProduceConsumer<Packet>()

    init {
        ws.handler {
            val info = Json.decode(it.toString(Charsets.UTF_8)) as Map<String, Any?>
            val type = info["type"]!!.toString()
            val clazz = Class.forName(type)
            if (clazz.isAssignableFrom(Packet::class.java)) invalidOp("Invalid packet $type")
            val typedObject = Dynamic.dynamicCast(info["payload"]!!, clazz)
            messageQueue.produce(typedObject!! as Packet)
            //println("$it:$typedObject")
        }
    }

    suspend override fun send(packet: Packet): Unit {
        val str = Json.encode(mapOf("type" to packet::class.java.canonicalName, "payload" to packet))
        ws.writeFinalTextFrame(str)
    }

    suspend override fun read(): Packet {
        return messageQueue.consume() as Packet
    }
}