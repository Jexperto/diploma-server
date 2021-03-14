package com.diploma.store

import com.diploma.WSMessage
import com.diploma.model.Question

interface Storage {
    fun saveEvent(message: WSMessage)
    fun getHistory(): List<WSMessage>?
    fun addAdmin(uuid: String)
    fun containsAdmin(uuid: String)
    fun removeAdmin(uuid: String)

    fun addUser(uuid: String)
    fun removeUser(uuid: String)
    fun containsUser(uuid: String)

    fun addToTeam(team_uuid: String, user_uuid:String)

    fun createTeam(uuid: String)
    fun removeTeam(uuid: String)

    fun addQuestion(q: Question)

}