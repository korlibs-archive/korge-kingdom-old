package com.games.soywiz.korgekingdom

import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.bitmapfont.FontDescriptor
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Text
import com.soywiz.korge.view.text
import com.soywiz.korio.async.spawnAndForget
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.inject.Inject
import com.soywiz.korui.ui.TextField

class KorgeKingdomModule : Module() {
    override val mainScene: Class<out Scene> get() = KorgeKingdomMainScene::class.java
}

class KorgeKingdomMainScene(
        @FontDescriptor(face = "Arial", size = 16, chars = "abcdefghijklmnopqrstuvwxyz") val font: BitmapFont
) : Scene() {
    @Inject lateinit var injector: AsyncInjector
    @Inject lateinit var userInfo: UserInfo
    lateinit var ch: Channel
    lateinit var roomNameText: Text

    suspend override fun init() {
        super.init()

        val channel = injector.getOrNull(Channel::class.java)

        ch = channel ?: ChannelPair().client // @TODO: WebSocketClient

        roomNameText = views.text(font, "Room")
        root += roomNameText

        println("[CLIENT] Waiting challenge...")
        val challenge = ch.wait<Login.Server.Challenge>()
        println("[CLIENT] Got challenge: $challenge")

        ch.send(Login.Client.Request(user = userInfo.user, challengedHash = Login.Server.Challenge.hash(challenge.key, userInfo.password)))
        val result = ch.wait<Login.Server.Result>()
        println("[CLIENT] Got result: $result")

        spawnAndForget {
            ch.messageHandlers()
        }

        if (result.success) {
            ch.send(ChatPackets.Client.Say("HELLO!"))
        }

//val client = WebSocketClient(URI("ws://127.0.0.1:8080/"))
    }

    suspend fun Channel.messageHandlers() {
        while (true) {
            val it = read()
            when (it) {
                is ChatPackets.Server.Said -> {
                    println(it)
                }
                is RoomPackets.Server.Joined -> {
                    println(it)
                    roomNameText.text = it.name
                }
                else -> {
                    println("Unhandled packet: $it")
                }
            }
        }
    }
}
