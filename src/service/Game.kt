package com.diploma.service

import com.diploma.model.Question
import com.diploma.store.qa.InMemoryStore

class Game(val id: String) {
//    val qaStore = InMemoryStore()

    var state: GameState = GameState.WAITING

    fun addQuestion(q: Question): Error {
//        return qaStore.adminAddQuestion(q)
        return Error()
    }

    fun processMessage() {
        when(state) {
            GameState.WAITING -> {

            }
            GameState.ROUND1 -> {

            }
        }
    }

}