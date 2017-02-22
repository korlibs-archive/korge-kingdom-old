package com.games.soywiz.korgekingdom

import com.soywiz.korge.Korge
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korio.net.ws.WebSocketClient
import java.net.URI

fun main(args: Array<String>) = Korge(KorgeKingdomModule(), args)

class KorgeKingdomModule : Module() {
    override val mainScene: Class<out Scene> get() = KorgeKingdomMainScene::class.java
}

class KorgeKingdomMainScene : Scene() {
    suspend override fun init() {
        super.init()

        val client = WebSocketClient(URI("ws://127.0.0.1:8080/"))
    }
}