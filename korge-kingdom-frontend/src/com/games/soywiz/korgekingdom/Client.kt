package com.games.soywiz.korgekingdom

import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korio.async.spawnAndForget
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.inject.Inject

class KorgeKingdomModule : Module() {
    override val mainScene: Class<out Scene> get() = KorgeKingdomMainScene::class.java
}

class KorgeKingdomMainScene : Scene() {
    @Inject lateinit var injector: AsyncInjector
    @Inject lateinit var userInfo: UserInfo
    lateinit var ch: Channel

    suspend override fun init() {
        super.init()

        val channel = injector.getOrNull(Channel::class.java)

        ch = channel ?: ChannelPair().client // @TODO: WebSocketClient

        println("[CLIENT] Waiting challenge...")
        val challenge = ch.wait<Login.Challenge>()
        println("[CLIENT] Got challenge: $challenge")

        ch.send(Login.Request(user = userInfo.user, challengedHash = Login.Challenge.hash(challenge.key, userInfo.password)))
        val result = ch.wait<Login.Result>()
        println("[CLIENT] Got result: $result")

        spawnAndForget {
            ch.messageHandlers()
        }

        if (result.success) {
            ch.send(Chat.Say("HELLO!"))
        }

//val client = WebSocketClient(URI("ws://127.0.0.1:8080/"))
    }

    suspend fun Channel.messageHandlers() {
        while (true) {
            val packet = read()
            when (packet) {
                is Chat.Said -> {
                    println(packet)
                }
                else -> {
                    println("Unhandled packet: $packet")
                }
            }
        }
    }
}
