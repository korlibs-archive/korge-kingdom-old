package com.games.soywiz.korgekingdom

interface Packet
interface LoginPacket : Packet

data class LoginChallenge(val key: String) : LoginPacket
data class LoginRequest(val user: String, val challengedHash: String) : LoginPacket