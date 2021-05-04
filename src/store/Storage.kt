package com.diploma.store

import com.diploma.WSMessage
import com.diploma.model.Question
import com.diploma.service.GameState

interface Storage {

    fun setState(gameUUID: String, state: GameState): Boolean
    fun getState(gameUUID: String): GameState?

    fun createGame(gameUUID: String, admin_uuid: String, code: String): Boolean
    fun removeGame(gameUUID: String): Boolean

    fun getGameCode(gameUUID: String): String?
    fun saveEvent(gameUUID: String, message: WSMessage)
    fun getHistory(gameUUID: String): List<WSMessage>?
    fun addAdmin(admin_uuid: String,name: String): Boolean
    fun getAdminName(admin_uuid: String): String?
    fun getAdmin(gameUUID: String,) : String?
    fun removeAdmin(gameUUID: String,admin_uuid: String) : Boolean

    fun addUser(gameUUID: String, player_id: String, name: String,room_id:String, team_id: String?): Boolean
    fun removeUser(gameUUID: String, uuid: String) : Boolean

    fun addToTeam(gameUUID: String, team_uuid: String, user_uuid:String) : Boolean

    fun createTeam(gameUUID: String, team_uuid: String, name: String): Boolean
    fun removeTeam(gameUUID: String, team_uuid: String): Boolean

    fun getQuestions(gameUUID: String) : List<Question>
    fun getQuestionIds(gameUUID: String) : MutableList<String>
    fun getUsersToQuestions(gameUUID: String): HashMap<String,List<String>>

    fun addQuestion(gameUUID: String, question_id: String, right_answer: String, question: String): Boolean
    fun addWrongAnswer(question_id: String, answer: String, player_id: String): Boolean
    fun addUserAnswerResult(question_id: String, player_id: String, correct: Boolean): Boolean


    fun getTeamsWithPlayersAndNames(gameUUID: String): HashMap<String, MutableList<Pair<String,String>>>
    fun getTeamsWithPlayers(gameUUID: String) : HashMap<String, MutableList<String>>
    fun getTeamIds(gameUUID: String): MutableList<String>
    fun getTeams(gameUUID: String): HashMap<String,String>
    fun findGameByAdmin(admin_uuid: String): String?
    fun findGameByCode(code: String): String?
    fun findGameByUser(uuid: String): String?
    fun getTeamsWithQuestions(gameUUID: String): HashMap<String,MutableList<String>>

    fun addQuestionsToTeams(teamsToQuestions: HashMap<String, MutableList<String>>): Boolean

    fun getUserTeam(player_id: String): String?
    fun getUserName(player_id: String): String?

}