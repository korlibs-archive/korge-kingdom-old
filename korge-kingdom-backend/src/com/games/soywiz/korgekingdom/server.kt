package com.games.soywiz.korgekingdom

import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.ext.db.redis.Redis
import com.soywiz.korio.ext.db.redis.hget
import com.soywiz.korio.ext.db.redis.hset
import com.soywiz.korio.ext.db.redis.key
import com.soywiz.korio.inject.Prototype
import com.soywiz.korio.util.Extra
import java.util.*

@Prototype
class Server(
        private val redis: Redis
) {
    private val clients = hashMapOf<String, Channel>()
    private val logins = redis.key("korge_logins")

    // Extra properties for Channel
    var Channel.userName: String by Extra.Property("userName") { "" }

    suspend fun handleClient(channel: Channel) {
        channel.process()
    }

    suspend fun Channel.process() {
        val user = login()
        try {
            clients[user] = this
            userName = user
            processIngame()
        } finally {
            clients.remove(user)
        }
    }

    suspend fun Channel.processIngame() {
        while (true) {
            val packet = read()
            when (packet) {
                is Chat.Say -> {
                    clients.values.send(Chat.Said(userName, packet.msg))
                }
                else -> {
                    invalidOp("Unknown packet said")
                }
            }
        }
    }

    suspend fun Channel.login(): String {
        val challenge = UUID.randomUUID().toString()
        send(Login.Challenge(challenge))
        val req = wait<Login.Request>()

        try {
            val user = req.user
            val password = logins.hget(user) ?: invalidOp("Can't find user '$user'")
            val expectedHash = Login.Challenge.hash(challenge, password)
            if (req.challengedHash != expectedHash) invalidOp("Invalid challenge")
            send(Login.Result(true, "ok"))
            return user
        } catch (e: Throwable) {
            send(Login.Result(false, e.message ?: "error"))
            throw e
        }
    }

    suspend fun register(user: String, password: String, notify: Channel? = null) {
        logins.hset(user, password)
    }
}
