package com.mrpowergamerbr.jfesm

import com.github.salomonbrys.kotson.*
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mrpowergamerbr.jfesm.entities.Hangman
import com.mrpowergamerbr.jfesm.entities.Sentence
import kotlinx.coroutines.runBlocking
import java.net.Socket
import java.util.*
import kotlin.coroutines.Continuation

class JFESMClient(val ip: String, val port: Int) {
    lateinit var socket: Socket
    val callbacks = mutableMapOf<UUID, Continuation<JsonObject>>()
    var playerName = "???"

    fun connect() {
        socket = Socket(ip, port)

        println("Conectado!")

        val inputStream = socket.getInputStream().bufferedReader()
        val outputStream = socket.getOutputStream().bufferedWriter()

        while (socket.isConnected) {
            try {
                val line = inputStream.readLine()

                // println(line)

                val payload = Constants.jsonParser.parse(line).obj
                val opCode = payload["code"].nullString
                val trackedId = payload["uniqueId"].nullString

                when (opCode) {
                    OpCode.IDENTIFY -> {
                        val playerName = payload["name"].string
                        this.playerName = playerName

                        println("Seu nome é $playerName! :3")
                        println("Espere começar a partida, obrigado ^-^")
                    }

                    OpCode.WON_GAME -> {
                        val playerName = payload["playerName"].string
                        val sentence1 = payload["sentence1"].string
                        val sentence2 = payload["sentence2"].string

                        println("$playerName venceu o jogo!")
                        println("Palavra A: $sentence1")
                        println("Palavra B: $sentence2")
                        sendPayloadDoNotTrack(
                            jsonObject(
                                "uniqueId" to trackedId
                            )
                        )
                        socket.close()
                        System.exit(0)
                    }
                    OpCode.QUIT_GAME_DUE_TO_BAD_GUESS -> {
                        println("Oopsie poopsie! Você errou o papite, seu tosqueira. Tentou arriscar para petiscar mas perdeu.")
                        sendPayloadDoNotTrack(
                            jsonObject(
                                "uniqueId" to trackedId
                            )
                        )
                        socket.close()
                        System.exit(0)
                    }
                    OpCode.UPDATE_STATUS -> {
                        println(" ")
                        println(" ")
                        println(" ")
                        val _sentence1 = payload["sentence1"].string
                        val _sentence2 = payload["sentence2"].string

                        val sentence1 = Sentence(_sentence1)
                        val sentence2 = Sentence(_sentence2)

                        displayStatusUpdateHeader(payload["currentPlayer"].string)

                        displayHangmanStatus(
                            sentence1,
                            sentence2,
                            payload["alreadyGuessed"].array.map { it.char }.toSet(),
                            payload["players"].array
                        )
                    }
                    OpCode.SEND_MESSAGE -> {
                        println(payload["message"].string)
                        sendPayloadDoNotTrack(
                            jsonObject(
                                "uniqueId" to trackedId
                            )
                        )
                    }
                    OpCode.SELECT_SENTENCE -> {
                        val kind = if (payload["sentence"].int == 1)
                            "A"
                        else
                            "B"

                        println("Você, qual será a palavra $kind a ser adivinhada?")

                        var line: String? = null

                        do {
                            line = readLine()!!.toUpperCase()

                            if (line.contains(" ") || line.contains("á", true) || line.contains(
                                    "é",
                                    true
                                ) || line.contains(
                                    "í",
                                    true
                                ) || line.contains("ó", true) || line.contains("ã", true)
                            ) {
                                println("Mensagem inválida! Por favor coloque algo válido")
                                line = null
                            }
                        } while (line == null)

                        sendPayloadDoNotTrack(
                            jsonObject(
                                "uniqueId" to trackedId,
                                "sentence" to line
                            )
                        )
                    }

                    OpCode.YOUR_TURN -> {
                        println(" ")
                        println(" ")
                        println(" ")
                        val _sentence1 = payload["sentence1"].string
                        val _sentence2 = payload["sentence2"].string

                        val sentence1 = Sentence(_sentence1)
                        val sentence2 = Sentence(_sentence2)

                        displayYourHangmanStatus(payload["cutLimbs"].int)

                        displayHangmanStatus(
                            sentence1,
                            sentence2,
                            payload["alreadyGuessed"].array.map { it.char }.toSet(),
                            payload["players"].array
                        )

                        println("Você irá querer [c]hutar uma palavra, ou fazer um [p]alpite?")

                        inputLoop@ while (true) {
                            val input = readLine()!!.toLowerCase()

                            when (input) {
                                "p" -> {
                                    println("Você, qual é o seu palpite da palavra A?")
                                    val palpiteA = readLine()!!.toUpperCase()
                                    println("Você, qual é o seu palpite da palavra B?")
                                    val palpiteB = readLine()!!.toUpperCase()

                                    val matchesSentence1 = sentence1!!.originalSentence == palpiteA
                                    val matchesSentence2 = sentence2!!.originalSentence == palpiteB

                                    val status = runBlocking {
                                        sendPayloadDoNotTrack(
                                            jsonObject(
                                                "uniqueId" to trackedId,
                                                "action" to OpCode.SENTENCE_GUESS,
                                                "sentence1" to palpiteA,
                                                "sentence2" to palpiteB
                                            )
                                        )
                                    }
                                    break@inputLoop
                                }
                                "c" -> {
                                    println("Você, qual é a letra que você irá adivinhar?")
                                    val letter = readLine()!![0].toUpperCase() // TODO: Filtrar coisas inválidas

                                    val status = runBlocking {
                                        sendPayloadDoNotTrack(
                                            jsonObject(
                                                "uniqueId" to trackedId,
                                                "action" to OpCode.GUESS,
                                                "letter" to letter
                                            )
                                        )
                                    }
                                    break@inputLoop
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                System.exit(0)
            }

            // val trackedId = UUID.fromString(payload["uniqueId"].string)

            /* machine?.process(
                this,
                Constants.jsonParser.parse(line).obj
            ) */
        }
    }

    fun displayStatusUpdateHeader(playerName: String) {
        println("Quem é agora? $playerName")
        println("---")
    }

    fun displayYourHangmanStatus(cutLimbs: Int) {
        println("Agora é você! Você tem ${Hangman(cutLimbs).getCutPartsAsString()} partes do corpo cortadas! (wow! Quem mandou ser burro!)")
        println("---")
    }

    fun displayHangmanStatus(sentence1: Sentence, sentence2: Sentence, alreadyGuessed: Set<Char>, players: JsonArray) {
        println("Palavra A: ${sentence1.buildSecretSentence(alreadyGuessed)}")
        println(" ")
        println("Palavra B: ${sentence2.buildSecretSentence(alreadyGuessed)}")
        println(" ")
        println("Letras já adivinhadas: ${alreadyGuessed.joinToString(", ")}")
        println("---")
        println("Players:")
        players.forEach {
            println("${it["name"].string} - ${Hangman(it["cutLimbs"].int).getCutPartsAsString()} partes do corpo cortadas")
        }
    }

    suspend fun sendPayload(jsonObject: JsonObject): JsonObject {
        val trackedId = UUID.randomUUID()

        jsonObject["uniqueId"] = trackedId

        val bufWriter = socket.getOutputStream().bufferedWriter()

        return kotlin.coroutines.suspendCoroutine { cont ->
            callbacks[trackedId] = cont

            bufWriter.write(
                Constants.gson.toJson(jsonObject) + "\n"
            )
        }
    }

    fun sendPayloadDoNotTrack(jsonObject: JsonObject) {
        println(jsonObject)

        val bufWriter = socket.getOutputStream().bufferedWriter()
        bufWriter.write(
            Constants.gson.toJson(jsonObject) + "\n"
        )
        bufWriter.flush()
    }
}