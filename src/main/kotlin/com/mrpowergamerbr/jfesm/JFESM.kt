package com.mrpowergamerbr.jfesm

import com.github.salomonbrys.kotson.char
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.string
import com.github.salomonbrys.kotson.toJsonArray
import com.mrpowergamerbr.jfesm.entities.Player
import com.mrpowergamerbr.jfesm.entities.Sentence
import kotlinx.coroutines.*
import java.util.*

class JFESM(val jfesmServer: JFESMServer) {
    companion object {
        val RANDOM = SplittableRandom()
    }

    val players: MutableList<Player> = mutableListOf()
    var currentIndex = 0
    var sentence1: Sentence? = null
    var sentence2: Sentence? = null
    val alreadyGuessed = mutableSetOf<Char>()

    suspend fun start() {
        if (2 >= players.size)
            throw RuntimeException("Players insuficientes!")
        if (players.size > 5)
            throw RuntimeException("Players demais!")

        val clonedPlayers = players.toMutableList()
        val player1 = clonedPlayers.random()
        clonedPlayers.remove(player1)
        val player2 = clonedPlayers.random()

        val tasks = listOf(
            GlobalScope.async { chooseSentence(player1, 1) },
            GlobalScope.async { chooseSentence(player2, 2) }
        )

        val sentencedPlayers = tasks.awaitAll().shuffled()

        // Hora de sortear os players!
        players.shuffle() // Sortear a lista...
        players.removeAll(sentencedPlayers)
        players.addAll(sentencedPlayers)

        // Os players A, B sempre devem ficar no final da lista...
        // players.addAll(sentencedPlayers)

        // Vamos começar!
        gameLoop()
    }

    fun gameLoop() {
        while (true) {
            println(" ")
            println(" ")

            if (players.isEmpty()) {
                println("Parece que todos os jogadores perderam a partida!")
                return
            }

            if (currentIndex >= players.size)
                currentIndex = 0

            val currentPlayer = players[currentIndex]

            players.filterNot { it == currentPlayer }.forEach {
                GlobalScope.launch {
                    it.sendPayload(
                        jsonObject(
                            "code" to OpCode.UPDATE_STATUS,
                            "currentPlayer" to currentPlayer.name,
                            "sentence1" to sentence1!!.originalSentence,
                            "sentence2" to sentence2!!.originalSentence,
                            "alreadyGuessed" to alreadyGuessed.toJsonArray(),
                            "players" to players.map {
                                jsonObject(
                                    "name" to it.name,
                                    "cutLimbs" to it.hangman.cutLimbs
                                )
                            }.toJsonArray()
                        )
                    )
                }
            }
            println("PLAYER ${currentPlayer.name}, ainda existem ${players.size} jogadores jogando!")

            val actionPayload = runBlocking {
                currentPlayer.sendPayload(
                    jsonObject(
                        "code" to OpCode.YOUR_TURN,
                        "sentence1" to sentence1!!.originalSentence,
                        "sentence2" to sentence2!!.originalSentence,
                        "alreadyGuessed" to alreadyGuessed.toJsonArray(),
                        "cutLimbs" to currentPlayer.hangman.cutLimbs,
                        "players" to players.map {
                            jsonObject(
                                "name" to it.name,
                                "cutLimbs" to it.hangman.cutLimbs
                            )
                        }.toJsonArray()
                    )
                )
            }

            val action = actionPayload["action"].string

            when (action) {
                "guess" -> {
                    val letter = actionPayload["letter"].char
                    if (!sentence1!!.originalSentence.any { it == letter } && !sentence2!!.originalSentence.any { it == letter }) {
                        runBlocking {
                            currentPlayer.sendPayload(
                                jsonObject(
                                    "code" to OpCode.SEND_MESSAGE,
                                    "message" to "Pelo visto você adivinhou uma letra que não existe nas palavras... hora de cortar uma parte do seu corpo!"
                                )
                            )
                        }

                        val hangman = currentPlayer.hangman

                        hangman.cutLimbs += 1

                        if (hangman.cutLimbs == 5) {
                            // Perdeu o jogo
                            val tasks = players.map {
                                GlobalScope.async {
                                    it.sendPayload(
                                        jsonObject(
                                            "code" to OpCode.SEND_MESSAGE,
                                            "message" to "Parece que alguém chamado ${currentPlayer.name} foi morto... quem mandou ser burrinho?"
                                        )
                                    )
                                }
                            }

                            runBlocking {
                                currentPlayer.sendPayload(
                                    jsonObject(
                                        "code" to OpCode.SEND_MESSAGE,
                                        "message" to "E parece que só sobrou a sua cabeça, que triste, né?"
                                    )
                                )
                            }
                            players.remove(currentPlayer)
                        }
                    }

                    alreadyGuessed.add(letter)
                }
                OpCode.SENTENCE_GUESS -> {
                    val palpiteA = actionPayload["sentence1"].string
                    val palpiteB = actionPayload["sentence2"].string

                    val matchesSentence1 = sentence1!!.originalSentence == palpiteA
                    val matchesSentence2 = sentence2!!.originalSentence == palpiteB

                    if (matchesSentence1 && matchesSentence2) {
                        val wonGameResult = players.map {
                            GlobalScope.async {
                                it.sendPayload(
                                    jsonObject(
                                        "code" to OpCode.WON_GAME,
                                        "playerName" to currentPlayer.name,
                                        "sentence1" to palpiteA,
                                        "sentence2" to palpiteB
                                    )
                                )
                            }
                        }

                        runBlocking {
                            wonGameResult.awaitAll()
                        }

                        System.exit(0)
                    } else {
                        println("Mal palpite realizado!")
                        val tasks = players.map {
                            GlobalScope.async {
                                it.sendPayload(
                                    jsonObject(
                                        "code" to OpCode.SEND_MESSAGE,
                                        "message" to "Parece que alguém chamado ${currentPlayer.name} fez o palpite errado... meus pêsames para ele."
                                    )
                                )
                            }
                        }

                        runBlocking {
                            tasks.awaitAll()
                        }

                        println("Avisado para todos os players, OK")

                        runBlocking {
                            println("Enviando aviso")
                            currentPlayer.sendPayload(
                                jsonObject(
                                    "code" to OpCode.QUIT_GAME_DUE_TO_BAD_GUESS
                                )
                            )
                            println("Aviso enviado!")
                        }

                        players.remove(currentPlayer)
                    }
                }
            }

            /* players.filterNot { it == currentPlayer }.forEach {
                it as NetworkPlayer
                GlobalScope.launch {
                    it.sendPayload(
                        jsonObject(
                            "code" to OpCode.UPDATE_STATUS,
                            "currentPlayer" to currentPlayer.name,
                            "sentence1" to sentence1!!.originalSentence,
                            "sentence2" to sentence2!!.originalSentence,
                            "alreadyGuessed" to alreadyGuessed.toJsonArray(),
                            "players" to players.map {
                                jsonObject(
                                    "name" to it.name,
                                    "cutLimbs" to it.hangman.cutLimbs
                                )
                            }.toJsonArray()
                        )
                    )
                }
            } */

            currentIndex++
        }
    }

    suspend fun chooseSentence(player: Player, sentenceKind: Int): Player {
        // players.remove(player)

        val payload = player.sendPayload(
            jsonObject(
                "code" to OpCode.SELECT_SENTENCE,
                "sentence" to sentenceKind
            )
        )

        val sentence = payload["sentence"].string

        println("Recebeu $sentence")

        // TODO: Filtrar coisas inválidas
        if (sentenceKind == 1) {
            sentence1 = Sentence(sentence.toUpperCase())
        } else {
            sentence2 = Sentence(sentence.toUpperCase())
        }

        /* if (sentence1 == null) {
            println("${player.name}, qual será a palavra A a ser adivinhada?")
            sentence1 = Sentence(readLine()!!.toUpperCase()) // TODO: Filtrar coisas inválidas
        } else {
            println("${player.name}, qual será a palavra B a ser adivinhada?")
            sentence2 = Sentence(readLine()!!.toUpperCase()) // TODO: Filtrar coisas inválidas
        } */

        return player
    }
}