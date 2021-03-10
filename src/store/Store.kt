package com.diploma.store

import com.diploma.WSMessage

interface Store {
    fun save(message: WSMessage)
    fun getHistory(): List<WSMessage>
}