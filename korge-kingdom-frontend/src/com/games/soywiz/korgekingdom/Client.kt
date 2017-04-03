package com.games.soywiz.korgekingdom

import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.bitmapfont.FontDescriptor
import com.soywiz.korge.input.component.onClick
import com.soywiz.korge.render.Texture
import com.soywiz.korge.resources.Path
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.Easing
import com.soywiz.korge.tween.rangeTo
import com.soywiz.korge.tween.tween
import com.soywiz.korge.util.IntArray2
import com.soywiz.korge.view.*
import com.soywiz.korge.view.tiles.TileSet
import com.soywiz.korge.view.tiles.tileMap
import com.soywiz.korio.async.AsyncThread
import com.soywiz.korio.async.ProduceConsumer
import com.soywiz.korio.async.sleepNextFrame
import com.soywiz.korio.async.spawnAndForget
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.inject.Inject
import com.soywiz.korio.inject.Optional
import com.soywiz.korio.net.ws.WebSocketClient
import com.soywiz.korio.util.Extra
import com.soywiz.korio.util.clamp
import com.soywiz.korma.geom.Point2d
import java.net.URI

class KorgeKingdomModule : Module() {
	override val title = "Korge Kingdom"
	override val icon = "icon.png"
	override val mainScene get() = KorgeKingdomMainScene::class.java
}

class KorgeKingdomMainScene(
	@FontDescriptor(face = "Arial", size = 16, chars = "abcdefghijklmnopqrstuvwxyz") val font: BitmapFont,
	@Path("avatar.png") private val avatarTexture: Texture
) : Scene() {
	@Inject lateinit var injector: AsyncInjector
	@Inject @Optional var userInfo: UserInfo? = null
	lateinit var ch: CClient
	lateinit var map: Container
	lateinit var roomNameText: Text

	val entities = hashMapOf<Long, Container>()

	suspend override fun init() {
		super.init()

		val tileset = TileSet(avatarTexture, 32, 32)

		map = root.container {
			this.tileMap(IntArray2(32, 32), tileset)
		}

		ch = injector.getOrNull(CClient::class.java) ?: WebSocketClient(URI("ws://127.0.0.1:8080/"), debug = true).toClient()

		roomNameText = root.text(font, "Room")

		println("[CLIENT] Waiting challenge...")
		val challenge = ch.wait<LoginPacket.Server.Challenge>()
		println("[CLIENT] Got challenge: $challenge")

		val actualUserInfo = userInfo ?: UserInfo("test", "test")

		ch.send(LoginPacket.Client.Request(user = actualUserInfo.user, challengedHash = ProtocolChallenge.hash(challenge.key, actualUserInfo.password)))
		val result = ch.wait<LoginPacket.Server.Result>()
		println("[CLIENT] Got result: $result")

		spawnAndForget { ch.messageHandlers() }

		if (result.success) {
			ch.send(ChatPackets.Client.Say("HELLO!"))
		}

		map.onClick {
			spawnAndForget {
				println("click! ${it.currentPos}")
				ch.send(EntityPackets.Client.Move(it.currentPos))
			}
		}
	}

	val CClient.queue by Extra.Property { AsyncThread() }

	suspend fun CClient.messageHandlers() {
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
					entities.getOrPut(it.id) { map.container() }.apply {
						children.clear()
						image(avatarTexture) {
							x = 0.0
							y = 0.0
						}
						text(font, it.name, textSize = 22.0) {
							x = 10.0
							y = 10.0
						}
						x = it.pos.x
						y = it.pos.y
					}
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

private fun WebSocketClient.toClient(): CClient {
	val pc = ProduceConsumer<String>()
	val ws = this

	ws.onStringMessage {
		println("[CLIENT] [WS-RECV-RAW] $it")
		pc.produce(it)
	}

	val client = object : CClient, Extra by Extra.Mixin() {
		suspend override fun send(packet: Packet) {
			val str = Packet.serialize(packet)
			println("[CLIENT] [WS-SEND] $str")
			ws.send(str)
		}

		suspend override fun read(): Packet {
			while (true) {
				val str = pc.consume()!!
				println("[CLIENT] [WS-RECV] $str")
				try {
					return Packet.deserialize(str)
				} catch (e: ClassNotFoundException) {
					System.err.println("${e::class.java.name}: ${e.message}")
				}
			}
		}
	}
	return client
}

suspend fun frame() = sleepNextFrame()