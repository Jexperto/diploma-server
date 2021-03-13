package com.diploma.store

import com.diploma.WSMessage
import com.diploma.service.Game

class InMemoryStorage: Storage {
    var events: MutableList<WSMessage> = mutableListOf()
    var admins: HashSet<String> = hashSetOf()

    override fun saveEvent(message: WSMessage) {
        events.add(message)
    }


    override fun getHistory(): List<WSMessage> = events.toList()

}