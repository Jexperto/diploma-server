package com.diploma.service

import com.diploma.WSMessage
import kotlin.concurrent.schedule
import com.diploma.store.InMemoryStorage
import java.util.Timer
import kotlin.random.Random


class Game(val uuid: String) {
    val storage = InMemoryStorage()
    private var state: GameState = GameState.WAITING
    var code: String = Random.nextInt().hashCode().toString()

    constructor(uuid: String, adminUUID: String) : this(uuid) {
        storage.addAdmin(adminUUID)
    }

    fun startRound(roundNumber: Int, timer: Long?) {
        when(roundNumber){
            1 -> {state = GameState.ROUND1
                Timer("SettingUp", false).schedule(timer?.times(1000) ?: 5000) {
                    state = GameState.MID
                }
            }
            2 -> {state = GameState.ROUND2}
        }
    }



    fun processAdminMessage(message: WSMessage) {
        when(state) {
            GameState.WAITING -> {

            }
            GameState.ROUND1 -> {

            }
            GameState.ROUND2 -> {

            }
            GameState.MID->{

            }
        }
    }


    fun processUserMessage(message: WSMessage) {
        when(state) {
            GameState.WAITING -> {

            }
            GameState.ROUND1 -> {

            }
            GameState.ROUND2 -> {

            }
            GameState.MID->{

            }
        }
    }

}