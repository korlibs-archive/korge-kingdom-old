package com.games.soywiz.korgekingdom

import com.soywiz.korio.crypto.sha1Async
import com.soywiz.korio.util.toHexString

interface Packet
interface LoginPacket : Packet

//////////////////////////////////////////////////
// LOGIN
//////////////////////////////////////////////////
object Login {
    data class Challenge(val key: String) : LoginPacket {
        companion object {
            suspend fun hash(challenge: String, password: String): String {
                return "$challenge-$password".toByteArray().sha1Async().toHexString()
            }
        }
    }

    data class Request(val user: String, val challengedHash: String) : LoginPacket

    data class Result(val success: Boolean, val msg: String) : LoginPacket
}

//////////////////////////////////////////////////
// CHAT
//////////////////////////////////////////////////

object Chat {
    data class Say(val msg: String) : Packet
    data class Said(val user: String, val msg: String) : Packet
}
