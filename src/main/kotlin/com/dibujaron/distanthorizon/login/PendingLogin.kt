package com.dibujaron.distanthorizon.login

class PendingLogin(val clientKey: String){

    //so the deal is this:
    //LOGIN PATH:
    //users presses 'log in with discord'
    //discord authenticates
    //balancer makes call to
    var stage = 0
    fun hasClientInitialConnection(): Boolean
    {
        return stage >= 1
    }

    fun hasWebSocketConnection(): Boolean
    {
        return stage >= 2
    }
}