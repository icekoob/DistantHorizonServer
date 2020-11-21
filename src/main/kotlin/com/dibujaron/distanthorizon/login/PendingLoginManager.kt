package com.dibujaron.distanthorizon.login

import com.dibujaron.distanthorizon.DHServer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

//so the deal is this:
//LOGIN PATH:
//users presses 'log in with discord'
//discord authenticates, redirects client to start.
//client starts, requests to balancer at /client_start_login
//balancer asks discord for username
//balancer selects a server
//balancer requests server at /prep_login with username.
//server registers pending login for username, generates a token (pending login ID), sends this token back to balancer.
//balancer forwards this token to the client along with the server url.
//client pings to the server at the url at /confirm_client_login and sends the token.
//client works with character selection menu, this doesn't require auth.
//client starts game, this requires opening WS connection
//first WS message includes the token. Server gives client username. At this point the pending login is considered complete and it's removed from the list.

//GUEST PATH:
//user presses 'log in as guest'
//client starts, requests to balancer at /client_start_login
//balancer gets no username from discord
//balancer selects server
//balancer does NOT prep login with server.
//balancer gives client a server url.
//client does not ping server at /confirm_client_login
//first WS message does not include token. Server gives client guest username.
//..rest is the same.

//debug path
//nothing happens until the client pings to the server and sends a fake token, which the server accepts in debug mode.

object PendingLoginManager {

    private val unconfirmedLogins = ConcurrentHashMap<String, PendingLogin>()

    class PendingLogin(val username: String, val expiry: Long)

    fun registerPendingLoginGenerateToken(username: String): String
    {
        cleanup()
        val token = generateToken(username)
        unconfirmedLogins[token] = PendingLogin(username, System.currentTimeMillis() + 1000 * 60 * 60 * 10)
        println("registered pending login for user $username, token is $token")
        return token
    }

    fun completeLogin(token: String): String? {
        println("completing login for token $token...")
        return if (token == "debug") {
            println("token is debug. Server in debug mode=${DHServer.debug}")
            if (DHServer.debug) "Debug0000" else null
        } else {
            cleanup()
            val pendingLogin = unconfirmedLogins[token]
            return if (pendingLogin == null || System.currentTimeMillis() > pendingLogin.expiry) {
                println("no pending login found or login is expired.")
                null
            } else {
                unconfirmedLogins.remove(token)
                println("login completed for ${pendingLogin.username}")
                pendingLogin.username
            }
        }
    }

    private fun cleanup(): Int{
        val t = System.currentTimeMillis()
        var count = 0
        for(k in unconfirmedLogins.keys){
            val pl = unconfirmedLogins[k]
            if(pl != null && t > pl.expiry){
                unconfirmedLogins.remove(k)
                count++
            }
        }
        if(count > 0){
            println("cleaned up $count unfulfilled pending logins.")
        }
        return count
    }

    private fun generateToken(username: String): String
    {
        return "tok" + Objects.hash(username, System.currentTimeMillis())
    }
}