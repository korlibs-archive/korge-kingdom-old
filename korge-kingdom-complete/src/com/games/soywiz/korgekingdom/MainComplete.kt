package com.games.soywiz.korgekingdom

import com.soywiz.korge.Korge
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.spawn
import com.soywiz.korio.inject.AsyncInjector

fun main(args: Array<String>) = EventLoop {
    val pair = ChannelPair()

    spawn { serverHandleClient(pair.server) }
    spawn { Korge(KorgeKingdomModule(), args = args, injector = AsyncInjector().map(Channel::class.java, pair.client)) }
}