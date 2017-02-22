package com.games.soywiz.korgekingdom

import com.soywiz.korio.async.ProduceConsumer
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.util.Dynamic
import io.vertx.core.http.ServerWebSocket

open class Client(
        private val ws: ServerWebSocket
) {
    val messageQueue = ProduceConsumer<Any>()
    val unprocessedMessageQueue = ProduceConsumer<Any>()

    init {
        ws.handler {
            val info = Json.decode(it.toString(Charsets.UTF_8)) as Map<String, Any?>
            val type = info["type"]!!.toString()
            val clazz = Class.forName(type)
            if (clazz.isAssignableFrom(Packet::class.java)) invalidOp("Invalid packet $type")
            val typedObject = Dynamic.dynamicCast(info["payload"]!!, clazz)
            messageQueue.produce(typedObject!!)
            //println("$it:$typedObject")
        }
    }

    suspend open fun send(packet: Any): Unit {
        val str = Json.encode(mapOf("type" to packet::class.java.canonicalName, "payload" to packet))
        ws.writeFinalTextFrame(str)
    }

    suspend fun read(): Packet {
        return messageQueue.consume() as Packet
    }

    suspend fun <T : Packet> wait(clazz: Class<T>): T {
        println("Waiting for!: $clazz")
        while (true) {
            val msg = read()
            if (msg::class.java.isAssignableFrom(clazz)) {
                return msg as T
            } else {
                unprocessedMessageQueue.produce(msg)
            }
        }
    }
}