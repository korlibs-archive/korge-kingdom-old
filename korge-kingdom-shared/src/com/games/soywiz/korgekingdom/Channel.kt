package com.games.soywiz.korgekingdom

import com.soywiz.korio.async.Consumer
import com.soywiz.korio.async.ProduceConsumer
import com.soywiz.korio.async.Producer
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.util.Extra

interface Channel : Extra {
    suspend fun send(packet: Packet): Unit
    suspend fun read(): Packet
}

suspend inline fun Iterable<Channel>.send(packet: Packet): Unit {
    for (channel in this) channel.send(packet)
}

suspend inline fun <reified T : Packet> Channel.wait(): T = wait(T::class.java)

suspend fun <T : Packet> Channel.wait(clazz: Class<T>): T {
    //println("Waiting for!: $clazz")
    while (true) {
        val msg = read()
        if (!msg::class.java.isAssignableFrom(clazz)) invalidOp("Unexpected message $msg")
        return msg as T
    }
}

class ChannelPair {
    val pc1 = ProduceConsumer<Packet>()
    val pc2 = ProduceConsumer<Packet>()

    val client = Channel(pc1, pc2)
    val server = Channel(pc2, pc1)
}

fun Channel(producer: Producer<Packet>, consumer: Consumer<Packet>): Channel {
    val channel = object : Channel, Extra by Extra.Mixin() {
        suspend override fun send(packet: Packet) = producer.produce(packet)
        suspend override fun read(): Packet = consumer.consume()!!

    }
    return channel
}
