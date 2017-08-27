package com.games.soywiz.korgekingdom

import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.go
import com.soywiz.korio.async.spawnAndForget
import com.soywiz.korio.ext.db.redis.Redis
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.vertx.vertx
import com.soywiz.korio.vfs.ResourcesVfs

fun main(args: Array<String>) = EventLoop.main {
    val resources = ResourcesVfs
    val serverInjector = AsyncInjector().map(Redis(listOf("127.0.0.1:6379")))
    val server = serverInjector.get<ServerHandler>()

    vertx.createHttpServer()
            .websocketHandler { ws ->
                go {
                    server.handleClient(VertxWebsocketClient(ws))
                }
            }
            .requestHandler { req ->
                go {
                    req.response().end(resources["ws.html"].readString())
                }
            }
            .listen(8080) {
                if (it.succeeded()) {
                    println("Listening at 8080")
                } else {
                    println("WAS NOT ABLE to bind at post 8080")
                }
            }
}
