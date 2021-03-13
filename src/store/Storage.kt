package com.diploma.store

import com.diploma.WSMessage

interface Storage {
    fun saveEvent(message: WSMessage)
    fun getHistory(): List<WSMessage>?
}