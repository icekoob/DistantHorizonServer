package com.dibujaron.distanthorizon.player.wallet

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.database.persistence.ActorInfo

class AccountWallet(myActorInitial: ActorInfo) : Wallet
{
    var actorCurrent = myActorInitial
    override fun getBalance(): Int {
        return actorCurrent.balance
    }

    override fun setBalance(newBal: Int): Int {
        actorCurrent = DHServer.getDatabase().getPersistenceDatabase().updateActorBalance(actorCurrent, newBal)!!
        return actorCurrent.balance
    }

}