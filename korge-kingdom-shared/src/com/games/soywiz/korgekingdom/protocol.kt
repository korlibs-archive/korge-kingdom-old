package com.games.soywiz.korgekingdom

import com.soywiz.korio.crypto.sha1Async
import com.soywiz.korio.util.toHexString

interface Packet

interface Login : Packet {
    object Client {
        data class Request(val user: String, val challengedHash: String) : Login
    }

    object Server {
        data class Challenge(val key: String) : Login {
            companion object {
                suspend fun hash(challenge: String, password: String): String {
                    return "$challenge-$password".toByteArray().sha1Async().toHexString()
                }
            }
        }

        data class Result(val success: Boolean, val msg: String) : Login
    }
}

interface ChatPackets : Packet {
    object Client {
        data class Say(val msg: String) : ChatPackets
    }

    object Server {
        data class Said(val user: String, val msg: String) : ChatPackets
    }
}

interface RoomPackets : Packet {
    object Client {
        data class Join(val name: String) : RoomPackets
    }

    object Server {
        data class Joined(val self: Long, val name: String) : RoomPackets
    }
}

interface EntityPackets : Packet {
    enum class Type { PLAYER, NPC, ENEMY }

    object Client {
        data class Move(val x: Int, val y: Int) : EntityPackets
    }

    object Server {
        data class Set(val id: Long, val name: String, val type: Type, val x: Int, val y: Int) : EntityPackets
        data class Moved(val id: Long, val x: Int, val y: Int) : EntityPackets
    }
}
