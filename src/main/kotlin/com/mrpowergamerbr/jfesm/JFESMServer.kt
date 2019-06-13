package com.mrpowergamerbr.jfesm

import com.github.salomonbrys.kotson.char
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.nullString
import com.github.salomonbrys.kotson.obj
import com.mrpowergamerbr.jfesm.entities.Player
import java.net.ServerSocket
import java.util.*
import kotlin.concurrent.thread
import kotlin.coroutines.resume

class JFESMServer {
    val jfesm = JFESM(this)

    fun startServer() {
        val socket = ServerSocket(3322)
        println("Iniciando servidor JFESM...")

        val availableColors = Constants.colors.toMutableList()

        while (true) {
            val client = socket.accept()

            thread {
                println("Client conectado! $client")

                val color = availableColors.random()
                availableColors.remove(color)

                val networkPlayer = Player(
                    color,
                    client
                )

                jfesm.players.add(
                    networkPlayer
                )

                val inputStream = client.getInputStream().bufferedReader()
                val outputStream = client.getOutputStream().bufferedWriter()

                outputStream.write(
                    Constants.gson.toJson(
                        jsonObject(
                            "code" to OpCode.IDENTIFY,
                            "name" to networkPlayer.name
                        )
                    ) + "\n"
                )

                outputStream.flush()

                while (client.isConnected) {
                    try {
                        val line = inputStream.readLine()

                        println("Recebido de ${networkPlayer.name}: $line")

                        val payload = Constants.jsonParser.parse(line).obj
                        val opCode = payload["code"].nullString
                        val trackedId = payload["uniqueId"].nullString

                        if (trackedId != null) {
                            jfesm.players.forEach {
                                println(it)
                                println(it.callbacks[UUID.fromString(trackedId)])
                                it.callbacks[UUID.fromString(trackedId)]?.resume(payload)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        println("Saindo do socket main loop...")
                        // jfesm.players.remove(networkPlayer)
                        break
                    }
                }
                println("Client ${networkPlayer.name} desconectado, removendo...")
                jfesm.players.remove(networkPlayer)
            }
        }
    }
}