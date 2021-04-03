package com.diploma.service

import com.diploma.*
import com.diploma.model.Answer
import com.diploma.model.Question
import kotlin.concurrent.schedule
import com.diploma.store.Storage
import io.ktor.http.cio.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*


class Game(val storage: Storage, val connections: ConnectionManager) {
    companion object {
        fun getRandomString(length: Int): String {
            val allowedChars = ('A'..'Z')
            return (1..length)
                .map { allowedChars.random() }
                .joinToString("")
        }
    }

    //  val storage: GameStorageInterface = GameStorageInterface(uuid, strg)
    private val timer: Timer = Timer("GameTimer", false) //TODO: Use threadpool
    private val stateListeners = mutableListOf<(String, GameState) -> Unit>()


    init {
        storage += { gameID, cur ->
            when (cur) {
                GameState.ROUND1 -> {
                    storage.shuffleQuestionsByTeams(gameID)
                }
                GameState.MID -> TODO()
                GameState.ROUND2 -> TODO()
            }
        }
    }


    fun createGame(admin_uuid: String, code: String): String? {
        val id = UUID.randomUUID().toString()
        if (storage.createGame(id, admin_uuid, code)) {
            storage.createTeam(id, "1", "1")
            storage.createTeam(id, "2", "2")
            return id
        }
        return null

    }


    private fun startRound(game_uuid: String, roundNumber: Int, timer: Long?) {
        when (roundNumber) {
            1 -> {
                storage.setState(game_uuid, GameState.ROUND1)
                this.timer.schedule(timer?.times(1000) ?: 5000) {
                    storage.setState(game_uuid, GameState.MID)
                }
            }
            2 -> {
                storage.setState(game_uuid, GameState.ROUND2)
            }
        }
    }


    suspend fun processUserMessage(message: WSMessage, thisConnection: Connection): String? {
        when (message) {
            is ReceivedUserJoinMessage -> {
                if (thisConnection.uuid != null) {
                    throw Exception("You already have an active session")
                }
                thisConnection.uuid = UUID.randomUUID().toString()
                if (!joinGame(message.code, thisConnection.uuid.toString(), message.nick, null)) {
                    throw Exception("Couldn't join user to a game")
                }
                val admin_uuid = storage.findGameByCode(message.code)?.let { storage.getAdmin(it) }
                connections.add(
                    thisConnection, ConnectionManager.Type.USER,
                    admin_uuid
                )
                return (Json.encodeToString(SentUserJoinMessage(thisConnection.uuid!!) as SentUserMessage))

            }

            else -> {
                if (thisConnection.uuid == null) {
                    throw Exception("You don't have any active sessions")
                }
                val game_uuid = com.diploma.storage.findGameByUser(thisConnection.uuid!!) ?: throw ConnectionException(
                    "Game not found. Closing connection"
                )
                when (storage.getState(game_uuid)) {
                    GameState.WAITING -> when (message) {

                        is ReceivedUserJoinTeamMessage -> {
                            if (storage.addToTeam(game_uuid, message.team_id, thisConnection.uuid!!)) {

                                val adminConnection =
                                    connections.getConnectionByID(connections.getAdminID(thisConnection.uuid!!)!!)
                                adminConnection?.session?.send(
                                    Frame.Text(Json.encodeToString(
                                        SentAdminPlayerConnectedMessage(
                                            message.team_id,
                                            thisConnection.uuid!!
                                        ) as SentAdminMessage
                                    ).also { println("Reply: $it") })
                                )


                                return Json.encodeToString(SentUserJoinTeamMessage(message.team_id) as SentUserMessage)
                            }
                            return null
                        }

                        is ReceivedUserGetTeamsMessage -> return Json.encodeToString(
                            storage.getTeams(game_uuid)?.values.toList()?.let {
                                SentUserGetTeamsMessage(
                                    it
                                )
                            } as SentUserMessage)
                    }
                    GameState.ROUND1 -> {

                    }
                    GameState.ROUND2 -> {

                    }
                    GameState.MID -> {

                    }
                }

                println(thisConnection.uuid.toString())

            }
        }
        return null
    }

    fun processAdminMessage(message: WSMessage, thisConnection: Connection): String? {
        when (message) {


            is ReceivedAdminCreateMessage -> {
                if (thisConnection.uuid != null) {
                    throw Exception("You already have an active session")
                }
                thisConnection.uuid = UUID.randomUUID().toString()
                connections.add(thisConnection, ConnectionManager.Type.ADMIN)
                val gameID =
                    createGame(thisConnection.uuid!!, getRandomString(6)) ?: throw Exception("Couldn't create a game")
                val reply =
                    Json.encodeToString(storage.getGameCode(gameID).also { println("CODE: $it") }?.let { code ->
                        SentAdminCreateMessage(
                            code, thisConnection.uuid!!
                        )
                    } as SentAdminMessage)

                println("admin_id: $thisConnection.uuid")
                return reply
            }


            is ReceivedAdminJoinMessage -> {
                if (thisConnection.uuid != null) {
                    throw Exception("You already have an active session")
                }
                val game = com.diploma.storage.findGameByAdmin(message.admin_id)
                    ?: throw ConnectionException("Game not found. Closing connection")
                thisConnection.uuid = message.admin_id
                connections.add(thisConnection, ConnectionManager.Type.ADMIN)
                return Json.encodeToString(
                    SentAdminJoinMessage(com.diploma.storage.getGameCode(game)!!)
                            as SentAdminMessage
                )
            }


            else -> {
                if (thisConnection.uuid == null) {
                    throw Exception("You don't have any active sessions")
                }
                val game_uuid = com.diploma.storage.findGameByAdmin(thisConnection.uuid!!) ?: throw ConnectionException(
                    "Game not found. Closing connection"
                )
                when (storage.getState(game_uuid)) {
                    GameState.WAITING -> {
                        when (message) {
                            is ReceivedAdminAddQstMessage -> {
                                println(message)
                                val q = Question(message.question, mutableListOf(Answer(message.answer, true)))
                                val id = storage.addQuestion(
                                    game_uuid,
                                    q.id, q.answers!!.first().text, q.text
                                ) ?: return null
                                return Json.encodeToString(SentAdminAddQstMessage(id.toString()) as SentAdminMessage)
                            }
                            is ReceivedAdminStartRoundMessage -> {
                                startRound(game_uuid, message.num, message.timer.toLong())
                                return Json.encodeToString(SentAdminStartMessage(message.num, 20) as SentAdminMessage)
                            }
                            is ReceivedAdminCloseMessage -> {
                                storage.removeGame(game_uuid)
                                return Json.encodeToString(SentAdminCloseMessage as SentAdminMessage)
                            }
                            else -> return null
                        }


                    }
                    GameState.ROUND1 -> {

                    }
                    GameState.ROUND2 -> {

                    }
                    GameState.MID -> {

                    }
                }


            }


        }
        return null
    }


//    inner class GameStorageInterface(val uuid: String, private val strg: Storage) {
//        fun createGame(): Boolean {
//            return strg.createGame(uuid)
//        }
//
//        fun getGameCode(): String? {
//            return strg.getGameCode(uuid)
//        }
//
//
//        fun saveEvent(message: WSMessage) {
//            strg.saveEvent(uuid, message)
//        }
//
//        fun getHistory(): List<WSMessage>? = strg.getHistory(uuid)
//
//        fun addAdmin(uuid: String): Boolean {
//            return strg.addAdmin(this.uuid, uuid)
//        }
//
//        fun getAdmin(): String? {
//            return strg.getAdmin(uuid)
//        }
//
//        fun removeAdmin(uuid: String): Boolean {
//            return strg.removeAdmin(this.uuid, uuid)
//        }
//
//        fun addUser(uuid: String): Boolean {
//            return strg.addUser(this.uuid, uuid)
//        }
//
//        fun removeUser(uuid: String): Boolean {
//            return strg.removeUser(this.uuid, uuid)
//        }
//
//        fun containsUser(uuid: String): Boolean {
//            return strg.containsUser(this.uuid, uuid)
//        }
//
//        fun addToTeam(team_uuid: String, user_uuid: String): Boolean {
//
//            return strg.addToTeam(uuid, team_uuid, user_uuid)
//        }
//
//        fun createTeam(uuid: String): Boolean {
//            return strg.createTeam(this.uuid, uuid)
//        }
//
//        fun removeTeam(uuid: String): Boolean {
//            return strg.removeTeam(this.uuid, uuid)
//        }
//
//        fun addQuestion(q: Question): Int? {
//            return strg.addQuestion(this.uuid, q)
//        }
//
//        fun getTeams(): Set<String>? {
//            return strg.getTeams(this.uuid)
//
//        }
//
//        fun containsTeam(game_uuid: String, uuid: String): Boolean {
//            return strg.containsTeam(this.uuid, uuid)
//        }
//    }


}
//ReceivedAdminBasicMessage -> TODO()
//


//ReceivedUserBasicMessage -> TODO()
//is ReceivedUserJoinMessage -> TODO()
//is ReceivedUserRightAnswerMessage -> TODO()
//is ReceivedUserWrongAnswerMessage -> TODO()
//is SentAdminAddQstMessage -> TODO()
//SentAdminBasicMessage -> TODO()
//is SentAdminCreateMessage -> TODO()
//is SentAdminGetAnswersMessage -> TODO()
//is SentAdminJoinMessage -> TODO()
//is SentAdminPlayerConnectedMessage -> TODO()
//is SentAdminStartMessage -> TODO()
//SentUserBasicMessage -> TODO()
//is SentUserGetAnswersMessage -> TODO()
//is SentUserGetTeamsMessage -> TODO()
//is SentUserJoinMessage -> TODO()
//is SentUserJoinTeamMessage -> TODO()
//SentUserTimerElapsedMessage -> TODO()
//is SentUserWrongQstMessage -> TODO()