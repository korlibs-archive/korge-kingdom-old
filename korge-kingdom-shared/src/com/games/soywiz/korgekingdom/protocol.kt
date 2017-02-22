package com.games.soywiz.korgekingdom

import com.soywiz.korio.crypto.sha1Async
import com.soywiz.korio.util.toHexString

interface Packet
interface LoginPacket : Packet

data class LoginChallenge(val key: String) : LoginPacket {
    companion object {
        suspend fun hash(challenge: String, password: String): String {
            return "$challenge-$password".toByteArray().sha1Async().toHexString()
        }
    }
}

data class LoginRequest(val user: String, val challengedHash: String) : LoginPacket

data class LoginResult(val success: Boolean, val msg: String) : LoginPacket