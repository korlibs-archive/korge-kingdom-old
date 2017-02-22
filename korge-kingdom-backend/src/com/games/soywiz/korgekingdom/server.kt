package com.games.soywiz.korgekingdom

import java.util.*

suspend fun serverHandleClient(client: Channel) {
    login(client)
}

suspend fun login(client: Channel) {
    val uuid = UUID.randomUUID().toString()
    client.send(LoginChallenge(uuid))
    val req = client.wait<LoginRequest>()
    println(req)
}
