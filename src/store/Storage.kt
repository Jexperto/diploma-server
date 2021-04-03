package com.diploma.store

import com.diploma.WSMessage
import com.diploma.model.Question
import com.diploma.service.GameState
import com.sun.org.apache.xpath.internal.operations.Bool

interface Storage {

    fun setState(game_uuid: String, state: GameState): Boolean
    fun getState(game_uuid: String): GameState?

    operator fun plusAssign(listener: (String, GameState) -> Unit)
    operator fun minusAssign(listener: (String, GameState) -> Unit)


    fun createGame(game_uuid: String, admin_uuid: String, code: String): Boolean
    fun removeGame(game_uuid: String): Boolean

    fun getGameCode(game_uuid: String): String?
    fun saveEvent(game_uuid: String, message: WSMessage)
    fun getHistory(game_uuid: String): List<WSMessage>?
    fun addAdmin(admin_uuid: String,name: String): Boolean
    fun getAdminName(admin_uuid: String): String?
    fun getAdmin(game_uuid: String,) : String?
    fun removeAdmin(game_uuid: String,admin_uuid: String) : Boolean

    fun addUser(game_uuid: String, player_id: String, name: String, team_id: String? = null): Boolean
    fun removeUser(game_uuid: String, uuid: String) : Boolean

    fun addToTeam(game_uuid: String, team_uuid: String, user_uuid:String) : Boolean

    fun createTeam(game_uuid: String, team_uuid: String, name: String): Boolean
    fun removeTeam(game_uuid: String, team_uuid: String): Boolean

    fun getQuestions(game_uuid: String) : List<Question?>
    fun addQuestion(game_uuid: String, question_id: String, right_answer: String, question: String): Boolean
    fun addWrongAnswer(question_id: String, answer: String, player_id: String): Boolean

    fun getTeams(game_uuid: String): HashMap<String,String>
    fun findGameByAdmin(admin_uuid: String): String?
    fun findGameByCode(code: String): String?
    fun findGameByUser(uuid: String): String?

    fun shuffleQuestionsByTeams(game_uuid: String)


    fun getUserTeam(player_id: String): String?
    fun getUserName(player_id: String): String?
}