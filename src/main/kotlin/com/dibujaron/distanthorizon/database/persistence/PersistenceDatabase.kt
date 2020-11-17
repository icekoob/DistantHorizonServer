package com.dibujaron.distanthorizon.database.persistence

import com.dibujaron.distanthorizon.orbiter.Station

interface PersistenceDatabase {
    fun selectOrCreateAccount(accountName: String): AccountInfo
    fun createNewActorForAccount(accountInfo: AccountInfo, actorDisplayName: String): AccountInfo?
    fun deleteActor(actorInfo: ActorInfo)
    fun updateShipOfActor(actor: ActorInfo, ship: ShipInfo): ActorInfo?
    fun updateActorBalance(actor: ActorInfo, newBal: Int): ActorInfo?
    fun updateActorLastDockedStation(actor: ActorInfo, station: Station): ActorInfo?
}