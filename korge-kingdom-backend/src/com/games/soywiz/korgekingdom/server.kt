package com.games.soywiz.korgekingdom

import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.ext.db.redis.Redis
import com.soywiz.korio.ext.db.redis.hget
import com.soywiz.korio.ext.db.redis.hset
import com.soywiz.korio.ext.db.redis.key
import com.soywiz.korio.inject.Prototype
import java.util.*

@Prototype
class Server(
        private val redis: Redis
) {
    private val logins = redis.key("korge_logins")

    suspend fun handleClient(client: Channel) {
        client.login()
    }

    suspend fun Channel.login() {
        val challenge = UUID.randomUUID().toString()
        send(LoginChallenge(challenge))
        val req = wait<LoginRequest>()

        try {
            val user = req.user
            val password = logins.hget(user) ?: invalidOp("Can't find user '$user'")
            val expectedHash = LoginChallenge.hash(challenge, password)
            if (req.challengedHash != expectedHash) invalidOp("Invalid challenge")
            send(LoginResult(true, "ok"))
        } catch (e: Throwable) {
            send(LoginResult(false, e.message ?: "error"))
        }
    }

    suspend fun register(user: String, password: String, notify: Channel? = null) {
        logins.hset(user, password)
    }
}
