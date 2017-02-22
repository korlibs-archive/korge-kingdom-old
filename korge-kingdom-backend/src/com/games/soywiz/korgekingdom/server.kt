package com.games.soywiz.korgekingdom

import java.util.*

suspend fun serverHandleClient(client: Channel) {
    client.login()
}

suspend fun Channel.login() {
    val challenge = UUID.randomUUID().toString()
    send(LoginChallenge(challenge))
    val req = wait<LoginRequest>()

    val user = req.user
    val password = "test"
    val expectedHash = LoginChallenge.hash(challenge, password)

    println(req)

    if (req.challengedHash == expectedHash) {
        println("Login success!")
        send(LoginResult(true))
    } else {
        println("Login challenge hash mismatch!")
        send(LoginResult(false))
    }
}
