package com.mrpowergamerbr.jfesm

import com.mrpowergamerbr.jfesm.entities.Player

object JFESMLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        val jfesm = JFESM()
        jfesm.players.addAll(
            listOf(
                Player("Amarelo"),
                Player("Azul"),
                Player("Laranja")
            )
        )
        jfesm.start()
    }
}