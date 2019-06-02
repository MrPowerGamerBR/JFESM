package com.mrpowergamerbr.jfesm

import com.mrpowergamerbr.jfesm.entities.Player
import com.mrpowergamerbr.jfesm.entities.Sentence
import java.util.*

class JFESM {
    companion object {
        val RANDOM = SplittableRandom()
    }

    val players: MutableList<Player> = mutableListOf()
    var currentIndex = 0
    var sentence1: Sentence? = null
    var sentence2: Sentence? = null
    val alreadyGuessed = mutableSetOf<Char>()

    fun start() {
        if (2 >= players.size)
            throw RuntimeException("Players insuficientes!")

        val playerA = chooseSentence()
        val playerB = chooseSentence()

        // Hora de sortear os players!
        players.shuffle() // Sortear a lista...

        // Os players A, B sempre devem ficar no final da lista...
        if (RANDOM.nextBoolean()) {
            players.add(playerA)
            players.add(playerB)
        }  else {
            players.add(playerB)
            players.add(playerA)
        }

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

            println("PLAYER ${currentPlayer.name}, ainda existem ${players.size} jogadores jogando!")

            displayHangmanStatus(currentPlayer)

            var increaseIndex = true

            println("Você irá querer [c]hutar uma palavra, ou fazer um [p]alpite?")
            inputLoop@while (true) {
                val input = readLine()!!.toLowerCase()

                when (input) {
                    "p" -> {
                        println("${currentPlayer.name}, qual é o seu palpite da palavra A?")
                        val palpiteA = readLine()!!.toUpperCase()
                        println("${currentPlayer.name}, qual é o seu palpite da palavra B?")
                        val palpiteB = readLine()!!.toUpperCase()

                        val matchesSentence1 = sentence1!!.originalSentence == palpiteA
                        val matchesSentence2 = sentence2!!.originalSentence == palpiteB

                        if (matchesSentence1 && matchesSentence2) {
                            println("Você ganhou o jogo!")
                            return
                        } else {
                            println("Você errou o palpite... você perdeu!")
                            players.remove(currentPlayer)
                            increaseIndex = false
                        }
                        break@inputLoop
                    }
                    "c" -> {
                        println("${currentPlayer.name}, qual é a letra que você irá adivinhar?")
                        val letter = readLine()!![0].toUpperCase() // TODO: Filtrar coisas inválidas

                        if (!sentence1!!.originalSentence.any { it == letter } && !sentence2!!.originalSentence.any { it == letter }) {
                            println("Pelo visto você adivinhou uma letra que não existe nas palavras... hora de cortar uma parte do seu corpo!")

                            val hangman = currentPlayer.hangman

                            hangman.cutLimbs += 1

                            if (hangman.cutLimbs == 5) {
                                // Perdeu o jogo
                                println("E parece que só sobrou a sua cabeça, que triste, né?")
                                increaseIndex = false
                                players.remove(currentPlayer)
                            }
                        }

                        alreadyGuessed.add(letter)
                        break@inputLoop
                    }
                }
            }

            if (increaseIndex)
                currentIndex++
        }
    }

    fun displayHangmanStatus(player: Player) {
        val sentence1 = sentence1!!
        val sentence2 = sentence2!!

        println("Seu carinha:")
        println("Quantas partes do corpo foram cortadas? (Máximo: 5): ${player.hangman.cutLimbs}")

        println("Palavra A: ${sentence1.buildSecretSentence(alreadyGuessed)}")
        println(" ")
        println("Palavra B: ${sentence2.buildSecretSentence(alreadyGuessed)}")
        println(" ")
        println("Letras já adivinhadas: ${alreadyGuessed.joinToString(", ")}")
    }


    fun chooseSentence(): Player {
        val player = players.random()

        if (sentence1 == null) {
            println("${player.name}, qual será a palavra A a ser adivinhada?")
            sentence1 = Sentence(readLine()!!.toUpperCase()) // TODO: Filtrar coisas inválidas
        } else {
            println("${player.name}, qual será a palavra B a ser adivinhada?")
            sentence2 = Sentence(readLine()!!.toUpperCase()) // TODO: Filtrar coisas inválidas
        }

        players.remove(player)

        return player
    }
}