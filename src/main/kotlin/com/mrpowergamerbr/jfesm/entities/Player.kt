package com.mrpowergamerbr.jfesm.entities

import com.github.salomonbrys.kotson.set
import com.google.gson.JsonObject
import com.mrpowergamerbr.jfesm.Constants
import java.net.Socket
import java.util.*
import kotlin.coroutines.Continuation

open class Player(val name: String, val socket: Socket) {
    val callbacks = mutableMapOf<UUID, Continuation<JsonObject>>()
    val hangman = Hangman(0)

    suspend fun sendPayload(jsonObject: JsonObject): JsonObject {
        val trackedId = UUID.randomUUID()

        jsonObject["uniqueId"] = trackedId.toString()

        val bufWriter = socket.getOutputStream().bufferedWriter()

        println(jsonObject)

        return kotlin.coroutines.suspendCoroutine { cont ->
            callbacks[trackedId] = cont

            bufWriter.write(
                Constants.gson.toJson(jsonObject) + "\n"
            )
            bufWriter.flush()
        }
    }
}