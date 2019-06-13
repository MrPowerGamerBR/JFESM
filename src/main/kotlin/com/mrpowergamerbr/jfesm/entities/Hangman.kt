package com.mrpowergamerbr.jfesm.entities

class Hangman(var cutLimbs: Int) {
    fun getCutPartsAsString(): String {
        val list = mutableListOf<String>()

        when (cutLimbs) {
            5 -> list.add("cabeça")
            4 -> list.add("tronco")
            3 -> list.add("braço")
            2 -> list.add("perna direita")
            1 -> list.add("perna esquerda")
        }

        return list.joinToString(", ")
    }
}