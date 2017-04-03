package com.games.soywiz.korgekingdom

import com.soywiz.korio.crypto.sha1Async
import com.soywiz.korio.util.toHexString
import com.soywiz.korma.geom.Point2d

object ProtocolChallenge {
	suspend fun hash(challenge: String, password: String): String {
		return "$challenge-$password".toByteArray().sha1Async().toHexString()
	}
}

interface LoginPacket : Packet {
	interface Client {
		data class Request(val user: String, val challengedHash: String) : LoginPacket
	}

	interface Server {
		data class Challenge(val key: String) : LoginPacket
		data class Result(val success: Boolean, val msg: String) : LoginPacket
	}
}

interface ChatPackets : Packet {
	interface Client {
		data class Say(val msg: String) : ChatPackets
	}

	interface Server {
		data class Said(val user: String, val msg: String) : ChatPackets
	}
}

interface RoomPackets : Packet {
	interface Client {
		data class Join(val name: String) : RoomPackets
	}

	interface Server {
		data class Joined(val self: Long, val name: String) : RoomPackets
	}
}

interface EntityPackets : Packet {
	//enum class Type { PLAYER, NPC, ENEMY }
	object Type {
		val PLAYER = "player"
		val NPC = "npc"
		val ENEMY = "enemy"
	}

	interface Client {
		data class Move(val pos: Point2d) : EntityPackets
	}

	interface Server {
		data class Set(val id: Long, val name: String, val type: String, val pos: Point2d) : EntityPackets
		data class Moved(val id: Long, val pos: Point2d) : EntityPackets
	}
}
