package com.mrpowergamerbr.jfesm.entities

class Sentence(
    val originalSentence: String
) {
    fun buildSecretSentence(alreadyGuessed: Set<Char>): String {
        val builder = StringBuilder()

        for (char in originalSentence) {
            if (alreadyGuessed.contains(char)) {
                builder.append(char)
            } else {
                builder.append("_")
            }
            builder.append(" ")
        }

        return builder.toString().trim()
    }
}