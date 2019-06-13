package com.mrpowergamerbr.jfesm

import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

object JFESMLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        println("Servidor ou Cliente?")
        val line = readLine()!!.toLowerCase()

        if (line == "servidor") {
            val server = JFESMServer()

            thread {
                while (true) {
                    val consoleLine = readLine()!!

                    if (consoleLine.startsWith("info")) {
                        server.jfesm.players.forEach {
                            println(it.name)
                        }
                    } else if (consoleLine.startsWith("start")) {
                        runBlocking {
                            server.jfesm.start()
                        }
                    }
                }
            }
            server.startServer()
        } else {
            println("Aonde irá conectar? IP:Porta")
            val where = readLine()!!

            val split = where.split(":")

            val jfesmClient = JFESMClient(split[0], split[1].toInt())
            jfesmClient.connect()
        }
    }
}