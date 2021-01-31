package com.dibujaron.distanthorizon.event

import com.dibujaron.distanthorizon.player.Player

class PlayerChatEvent(val player: Player, val message: String) : Event()
