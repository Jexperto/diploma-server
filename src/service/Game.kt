package com.diploma.service

import com.diploma.model.Question
import com.diploma.store.InMemoryStorage
import kotlin.random.Random


class Game(val uuid: String) {
    val storage = InMemoryStorage()
    var state: GameState = GameState.WAITING
    var code: String
    init {
        code = Random.nextInt().hashCode().toString()
    }

    constructor(uuid: String, adminUUID: String) : this(uuid) {
        addAdmin(adminUUID)
    }
    fun addAdmin(uuid: String){
        storage.admins.add(uuid)
    }
    fun containsAdmin(uuid: String){
        storage.admins.contains(uuid)
    }
    fun removeAdmin(uuid: String){}

    fun addUser(uuid: String){}
    fun removeUser(uuid: String){}

    fun startRound(roundNumber: Int, timer: Int?){}

    fun addToTeam(team_uuid: String, user_uuid:String){}

    fun createTeam(){}
    fun removeTeam(uuid: String){}

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