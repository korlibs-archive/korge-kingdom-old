package com.games.soywiz.korgekingdom

import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.util.Dynamic

interface Packet {
    companion object {
        val loader = ClassLoader.getSystemClassLoader()

        fun serializeDeserialize(packet: Packet): Packet = deserialize(serialize(packet))

        fun serialize(packet: Packet): String {
            return Json.encode(mapOf("type" to packet::class.java.name, "payload" to packet))
        }

        fun deserialize(str: String): Packet {
            try {
                val info = Json.decode(str) as Map<String, Any?>
                val type = info["type"]!!.toString()
                val clazz = loader.loadClass(type)
                //val clazz = Packet::class.java.classLoader.loadClass(type)
                //val clazz = Class.forName(type)
                if (clazz.isAssignableFrom(Packet::class.java)) invalidOp("Invalid packet $type")
                return Dynamic.dynamicCast(info["payload"]!!, clazz) as Packet
            } catch (e: ClassNotFoundException) {
                throw e
            }
        }
    }
}
