package com.games.soywiz.korgekingdom

import com.soywiz.korio.async.Consumer
import com.soywiz.korio.async.ProduceConsumer
import com.soywiz.korio.async.Producer
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.util.Extra

interface CClient : Extra {
    suspend fun send(packet: Packet): Unit
    suspend fun read(): Packet
}

suspend inline fun Iterable<CClient>.send(packet: Packet): Unit {
    for (client in this) {
        try {
            client.send(packet)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

suspend inline fun <reified T : Packet> CClient.wait(): T = wait(T::class.java)

suspend fun <T : Packet> CClient.wait(clazz: Class<T>): T {
    //println("Waiting for!: $clazz")
    while (true) {
        val msg = read()
        if (!msg::class.java.isAssignableFrom(clazz)) invalidOp("Unexpected message $msg")
        return msg as T
    }
}

class ClientPair {
    val pc1 = ProduceConsumer<Packet>()
    val pc2 = ProduceConsumer<Packet>()

    val client: CClient = Client(pc1, pc2)
    val server: CClient = Client(pc2, pc1)
}

fun Client(producer: Producer<Packet>, consumer: Consumer<Packet>): CClient {
    val client = object : CClient, Extra by Extra.Mixin() {
        suspend override fun send(packet: Packet) = producer.produce(Packet.serializeDeserialize(packet))
        suspend override fun read(): Packet = consumer.consume()!!

    }
    return client
}
