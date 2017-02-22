package com.games.soywiz.korgekingdom

interface Packet

data class LoginChallenge(val key: String) : Packet
data class LoginRequest(val user: String, val challengedHash: String) : Packet