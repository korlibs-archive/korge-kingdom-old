package com.games.soywiz.korgekingdom

import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.bitmapfont.FontDescriptor
import com.soywiz.korge.render.Texture
import com.soywiz.korge.resources.Path
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.Easing
import com.soywiz.korge.tween.rangeTo
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.*
import com.soywiz.korim.geom.Point2d
import com.soywiz.korio.async.AsyncThread
import com.soywiz.korio.async.sleepNextFrame
import com.soywiz.korio.async.spawnAndForget
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.inject.Inject
import com.soywiz.korio.util.Extra
import com.soywiz.korio.util.clamp

class KorgeKingdomModule : Module() {
    override val mainScene: Class<out Scene> get() = KorgeKingdomMainScene::class.java
}

class KorgeKingdomMainScene(
        @FontDescriptor(face = "Arial", size = 16, chars = "abcdefghijklmnopqrstuvwxyz") val font: BitmapFont,
        @Path("avatar.png") private val avatarTexture: Texture
) : Scene() {
    @Inject lateinit var injector: AsyncInjector
    @Inject lateinit var userInfo: UserInfo
    lateinit var ch: Channel
    lateinit var map: Container
    lateinit var roomNameText: Text

    val entities = hashMapOf<Long, View>()

    suspend override fun init() {
        super.init()

        val channel = injector.getOrNull(Channel::class.java)

        map = views.container()
        root += map

        ch = channel ?: ChannelPair().client // @TODO: WebSocketClient

        roomNameText = views.text(font, "Room")
        root += roomNameText

        println("[CLIENT] Waiting challenge...")
        val challenge = ch.wait<Login.Server.Challenge>()
        println("[CLIENT] Got challenge: $challenge")

        ch.send(Login.Client.Request(user = userInfo.user, challengedHash = Login.Server.Challenge.hash(challenge.key, userInfo.password)))
        val result = ch.wait<Login.Server.Result>()
        println("[CLIENT] Got result: $result")

        spawnAndForget { ch.messageHandlers() }
        spawnAndForget { inputHandler() }

        if (result.success) {
            ch.send(ChatPackets.Client.Say("HELLO!"))
        }

//val client = WebSocketClient(URI("ws://127.0.0.1:8080/"))
    }

    suspend fun inputHandler() {
        while (true) {
            //println(views.input.mouse)
            val mouse = views.input.mouse
            ch.send(EntityPackets.Client.Move(mouse))
            frame()
        }
    }

    val Channel.queue by Extra.Property("queue") { AsyncThread() }

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
                is EntityPackets.Server.Set -> {
                    val view = entities.getOrPut(it.id) { views.image(avatarTexture) }
                    map += view
                    view.x = it.pos.x
                    view.y = it.pos.y
                }
                is EntityPackets.Server.Moved -> {
                    val packet = it
                    val view = entities[it.id]
                    if (view != null) {
                        val dist = packet.pos - Point2d(view.x, view.y)
                        val time = (dist.length * 10.0).clamp(0.0, 300.0)
                        if (time > 0.0) {
                            queue { view.tween(View::x..packet.pos.x, View::y..packet.pos.y, time = time.toInt(), easing = Easing.EASE_IN_OUT_QUAD) }
                        }
                    }
                }
                else -> {
                    println("Unhandled packet: $it")
                }
            }
        }
    }
}

suspend fun frame() = sleepNextFrame()