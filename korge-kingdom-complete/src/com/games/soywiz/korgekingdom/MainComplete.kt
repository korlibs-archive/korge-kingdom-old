package com.games.soywiz.korgekingdom

import com.soywiz.korge.Korge
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.spawn
import com.soywiz.korio.ext.db.redis.Redis
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.util.Extra

fun main(args: Array<String>) = EventLoop {
    val pair = ClientPair()
    val userInfo = UserInfo(user = "test", password = "test")
    val serverInjector = AsyncInjector()
            .map(Redis(listOf("127.0.0.1:6379")))
    val clientInjector = AsyncInjector()
            .map(CClient::class.java, pair.client.clientLog("client"))
            .map(userInfo)

    val server = serverInjector.get<ServerHandler>()
    server.register(userInfo.user, userInfo.password)
    spawn { server.handleClient(pair.server) }
    spawn { Korge(KorgeKingdomModule(), args = args, injector = clientInjector) }
}

fun CClient.clientLog(name: String): CClient {
    val client = this
    val decoratedClient = object : CClient, Extra by Extra.Mixin() {
        suspend override fun send(packet: Packet) {
            println("Client[$name][SEND]: ${Packet.serialize(packet)}")
            client.send(packet)
        }

        suspend override fun read(): Packet {
            val res = client.read()
            println("Client[$name][RECV]: ${Packet.serialize(res)}")
            return res
        }

    }
    return decoratedClient
}