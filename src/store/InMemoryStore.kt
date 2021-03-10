package com.diploma.store

import com.diploma.ReceivedAdminMessage

class InMemoryStore: Store {
    var events: MutableList<ReceivedAdminMessage> = mutableListOf()

    override fun save(message: ReceivedAdminMessage) {
        events.add(message)
    }

    override fun getHistory(): List<ReceivedAdminMessage> = events.toList()

}