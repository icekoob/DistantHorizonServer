package com.dibujaron.distanthorizon.database.persistence

import org.json.JSONArray
import org.json.JSONObject

open class AccountInfo(
    val accountName: String,
    val actors: List<ActorInfo>
){
    open fun toJSON(): JSONObject {
        val r = JSONObject()
        r.put("account_name", accountName)
        val arr = JSONArray()
        actors.asSequence().map{it.toJSON()}.forEach { arr.put(it) }
        r.put("actors", arr)
        return r
    }
}

