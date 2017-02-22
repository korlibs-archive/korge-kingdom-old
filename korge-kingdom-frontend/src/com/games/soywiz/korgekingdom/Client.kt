package com.games.soywiz.korgekingdom

import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korio.crypto.sha1Async
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.inject.Inject
import com.soywiz.korio.util.toHexString

class KorgeKingdomModule : Module() {
    override val mainScene: Class<out Scene> get() = KorgeKingdomMainScene::class.java
}

class KorgeKingdomMainScene : Scene() {
    @Inject lateinit var injector: AsyncInjector
    lateinit var ch: Channel

    suspend override fun init() {
        super.init()

        val channel = injector.getOrNull(Channel::class.java)

        ch = channel ?: ChannelPair().client // @TODO: WebSocketClient

        val challenge = ch.wait<LoginChallenge>()
        val user = "test"
        val password = "test"

        ch.send(LoginRequest(user = user, challengedHash = "${challenge.key}-$password".toByteArray().sha1Async().toHexString()))

//val client = WebSocketClient(URI("ws://127.0.0.1:8080/"))
    }
}
