package com.dibujaron.distanthorizon.command

import com.dibujaron.distanthorizon.player.Player

interface CommandHandler {
    fun handle(sender: Player, args: List<String>)
}