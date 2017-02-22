package com.games.soywiz.korgekingdom

import com.soywiz.korim.geom.Point2d
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.ext.db.redis.Redis
import com.soywiz.korio.ext.db.redis.hget
import com.soywiz.korio.ext.db.redis.hset
import com.soywiz.korio.ext.db.redis.key
import com.soywiz.korio.inject.Prototype
import com.soywiz.korio.util.Extra
import java.io.EOFException
import java.util.*
import kotlin.collections.LinkedHashSet

@Prototype
class ServerHandler(
        private val redis: Redis
) {
    inner class Room(val name: String) {
        val clients = LinkedHashSet<CClient>()
    }

    suspend fun CClient.join(room: Room) {
        this.room.clients -= this
        this.room = room
        this.room.clients += this
        this.send(RoomPackets.Server.Joined(self = entityId, name = room.name))
        this.room.clients.send(EntityPackets.Server.Set(this.entityId, this.userName, EntityPackets.Type.PLAYER, this.pos))

        for (other in this.room.clients) {
            this.send(EntityPackets.Server.Set(other.entityId, other.userName, EntityPackets.Type.PLAYER, other.pos))
        }
    }

    private val clients = LinkedHashSet<CClient>()
    private val logins = redis.key("korge_logins")

    val defaultRoom = Room("default")

    val server = this

    // Extra properties for Client
    var CClient.userName: String by Extra.Property { "" }
    var CClient.entityId: Long by Extra.Property { 0L }
    var CClient.room: Room by Extra.Property { defaultRoom }
    val CClient.pos: Point2d by Extra.Property { Point2d() }

    var lastEntityId = 0L

    suspend fun handleClient(client: CClient) {
        client.process()
    }

    suspend fun CClient.process() {
        println("Connected $this")
        val user = login()
        try {
            println("Login $this: $user")
            clients += this
            userName = user
            entityId = lastEntityId++
            join(defaultRoom)
            processIngame()
        } catch (e: EOFException) {
            // Normal ending!
        } finally {
            println("Disconnecting $this")
            room.clients -= this
            clients -= this
        }
    }

    suspend fun CClient.processIngame() {
        while (true) {
            val it = read()
            when (it) {
                is ChatPackets.Client.Say -> {
                    server.clients.send(ChatPackets.Server.Said(userName, it.msg))
                }
                is EntityPackets.Client.Move -> {
                    room.clients.send(EntityPackets.Server.Moved(entityId, it.pos))
                }
                else -> {
                    invalidOp("Unknown packet said")
                }
            }
        }
    }

    suspend fun CClient.login(): String {
        val challenge = UUID.randomUUID().toString()
        send(LoginPacket.Server.Challenge(challenge))
        val req = wait<LoginPacket.Client.Request>()

        try {
            val user = req.user
            val password = logins.hget(user) ?: invalidOp("Can't find user '$user'")
            val expectedHash = ProtocolChallenge.hash(challenge, password)
            if (req.challengedHash != expectedHash) invalidOp("Invalid challenge")
            send(LoginPacket.Server.Result(true, "ok"))
            return user
        } catch (e: Throwable) {
            send(LoginPacket.Server.Result(false, e.message ?: "error"))
            throw e
        }
    }

    suspend fun register(user: String, password: String, notify: CClient? = null) {
        logins.hset(user, password)
    }
}
