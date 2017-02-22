package com.games.soywiz.korgekingdom

import com.soywiz.korge.Korge
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.spawn
import com.soywiz.korio.ext.db.redis.Redis
import com.soywiz.korio.inject.AsyncInjector

fun main(args: Array<String>) = EventLoop {
    val pair = ChannelPair()
    val userInfo = UserInfo(user = "test", password = "test")
    val serverInjector = AsyncInjector()
            .map(Redis(listOf("127.0.0.1:6379")))
    val clientInjector = AsyncInjector()
            .map(Channel::class.java, pair.client)
            .map(userInfo)

    val server = serverInjector.get<Server>()
    server.register(userInfo.user, userInfo.password)
    spawn { server.handleClient(pair.server) }
    spawn { Korge(KorgeKingdomModule(), args = args, injector = clientInjector) }
}