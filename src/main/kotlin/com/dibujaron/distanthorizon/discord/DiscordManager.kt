package com.dibujaron.distanthorizon.discord

import com.dibujaron.distanthorizon.event.EventHandler
import com.dibujaron.distanthorizon.event.EventManager
import com.dibujaron.distanthorizon.event.PlayerChatEvent
import com.dibujaron.distanthorizon.player.PlayerManager
import com.jessecorbett.diskord.dsl.bot
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.commands
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.sendMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

object DiscordManager : EventHandler {

    var CHANNEL_ID = ""
    var BOT_TOKEN = ""
    var BOT_USERNAME = ""
    fun moduleInit(properties: Properties) {
        BOT_TOKEN =
            properties.getProperty("discord.bot.token", "")
        CHANNEL_ID = properties.getProperty("discord.bot.channel.id", "")
        BOT_USERNAME = properties.getProperty("discord.bot.username", "KANE")
        if (BOT_TOKEN.isNotEmpty() && CHANNEL_ID.isNotEmpty()) {
            EventManager.registerEvents(this)
            GlobalScope.launch { initializeBot() }
        } else {
            println("Warning: Discord integration is disabled, bot token is not set.")
        }

    }

    private suspend fun initializeBot() {
        bot(BOT_TOKEN) {
            commands { //uses default prefix of "."
                command("ping") {
                    println("got bot command!")
                    reply("pong")
                    delete()
                }
                messageCreated {
                    if (it.channelId == CHANNEL_ID && it.author.username != BOT_USERNAME) {
                        PlayerManager.broadcast(it.author.username, it.content)
                    }
                }
            }
        }
    }

    override fun onPlayerChat(event: PlayerChatEvent) {
        if (BOT_TOKEN.isNotEmpty() && CHANNEL_ID.isNotEmpty()) {
            runBlocking { relayChat(event.player.getDisplayName(), event.message) }
        }
    }

    private suspend fun relayChat(sender: String, message: String) {
        ClientStore(BOT_TOKEN).channels[CHANNEL_ID].sendMessage("$sender: $message")
    }
}