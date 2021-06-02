package com.diploma.store

import com.diploma.model.WSMessage
import com.diploma.model.Question
import com.diploma.service.GameState
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.random.Random


class InMemoryStorage : Storage {
    var games: ConcurrentHashMap<String, GameStorage> = ConcurrentHashMap()
    private var adminToGame: ConcurrentHashMap<String, String> = ConcurrentHashMap()
    private var codeToGame: ConcurrentHashMap<String, String> = ConcurrentHashMap()
    private var stateListeners = hashSetOf<(String, GameState) -> Unit>()

    //private var adminGames: HashMap<String,GameStorage> = hashMapOf()
    class GameStorage() {
        var code: String = Random.nextInt().hashCode().toString()
        var state: GameState = GameState.WAITING
        var events: MutableList<WSMessage> = mutableListOf()
        var admin: String? = null
        var users: HashSet<String> = hashSetOf()
        var teams: HashMap<String, HashSet<String>?> = hashMapOf()
        var questions: MutableList<Question>? = mutableListOf()
        var teamToQuestion: HashMap<String, MutableList<Question>> = hashMapOf()
    }

    override fun createGame(gameUUID: String, admin_uuid: String, code: String): Boolean {
        val game = GameStorage()
        games[gameUUID] = game
        codeToGame[game.code] = gameUUID
        return true
    }


    override fun removeGame(gameUUID: String): Boolean {
        //TODO: Synchronize 3 maps in a different way
        games.remove(gameUUID)
        adminToGame.values.remove(gameUUID)
        codeToGame.values.remove(gameUUID)
        return true
    }

    override fun setState(gameUUID: String, state: GameState): Boolean {
        if (!games.contains(gameUUID))
            return false
        //val prev = games[gameUUID]!!.state
        games[gameUUID]!!.state = state
        for (listener in stateListeners) {
            listener(gameUUID, state)
        }
        return true
    }

     fun shuffleQuestionsByTeams(gameUUID: String):Boolean {
        val game = games[gameUUID]
        val teamCount = game?.teams?.count() ?: return false
        if (teamCount < 1) return false
        val k = game.questions?.count()?.div(teamCount) ?: return false
        game.questions!!.shuffle()
        var i = 0
        for (team in game.teams) {
            val list = mutableListOf<Question>()
            list.addAll(game.questions!!.subList(i, (i + 1) * k))
            game.teamToQuestion[team.key] = list
            i++
        }
        game.questions = null
        return true
    }

    override fun getUserTeam(player_id: String): String? {
        TODO("Not yet implemented")
    }

    override fun getUserName(player_id: String): String? {
        TODO("Not yet implemented")
    }

    override fun setTeamPoints(gameUUID: String, team_uuid: String, value: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun getTeamPoints(gameUUID: String, team_uuid: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getState(gameUUID: String): GameState? {
        return games[gameUUID]?.state
    }



    override fun getGameCode(gameUUID: String): String? {
        return games[gameUUID]?.code
    }




    override fun addAdmin(admin_uuid: String, name: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getAdminName(admin_uuid: String): String? {
        TODO("Not yet implemented")
    }

//    override fun addAdmin(gameUUID: String, uuid: String): Boolean {
//        val game = games[gameUUID] ?: return false
//        if (game.admin != null)
//            return false
//        game.admin = uuid
//        adminToGame[uuid] = gameUUID
//        //  adminGames[uuid] = game
//        return true
//    }

    override fun getAdmin(gameUUID: String): String? {
        return games[gameUUID]?.admin
    }

    override fun removeAdmin(gameUUID: String, admin_uuid: String): Boolean {
        games[gameUUID]?.admin = null ?: return false
        return true
    }

    override fun addUser(
        gameUUID: String,
        player_id: String,
        name: String,
        room_id: String,
        team_id: String?
    ): Boolean {
        TODO("Not yet implemented")
    }

//    override fun addUser(gameUUID: String, player_id: String, name: String, team_id: String?): Boolean {
//        TODO("Not yet implemented")
//    }

//    override fun addUser(gameUUID: String, uuid: String): Boolean {
//        games[gameUUID]?.users?.add(uuid) ?: return false
//        return true
//    }

    override fun removeUser(gameUUID: String, uuid: String): Boolean {
        games[gameUUID]?.users?.remove(uuid) ?: return false
        return true
    }

    fun containsUser(gameUUID: String, uuid: String): Boolean {
        return games[gameUUID]?.users?.contains(uuid) ?: return false
    }

    override fun addToTeam(gameUUID: String, team_uuid: String, user_uuid: String): Boolean {
        val game = games[gameUUID]
        if (game?.teams?.containsKey(team_uuid) == true) {
            game.teams[team_uuid]?.add(user_uuid)
            return true
        }
        return false
    }

    override fun createTeam(gameUUID: String, team_uuid: String, name: String): Boolean {
        TODO("Not yet implemented")
    }

//    override fun createTeam(gameUUID: String, uuid: String): Boolean {
//        games[gameUUID]?.teams?.set(uuid, hashSetOf())
//        return true
//    }

    override fun removeTeam(gameUUID: String, team_uuid: String): Boolean {
        games[gameUUID]?.teams?.remove(team_uuid)
        return true
    }

    override fun getQuestions(gameUUID: String): List<Question> {

        return emptyList()
    }

    override fun getQuestionIds(gameUUID: String): MutableList<String> {
        TODO("Not yet implemented")
    }

    override fun getUsersToQuestions(gameUUID: String): HashMap<String, List<String>> {
        TODO("Not yet implemented")
    }

    override fun addQuestion(gameUUID: String, question_id: String, right_answer: String, question: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun addWrongAnswer(question_id: String, answer: String, player_id: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun addUserAnswerResult(
        question_id: String,
        player_id: String,
        string: String,
        correct: Boolean
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun getQuestionResultByMaxAnswered(question_id: String): Pair<String, Boolean>? {
        TODO("Not yet implemented")
    }

    override fun getTeamsWithPlayersAndNames(gameUUID: String): HashMap<String, MutableList<Pair<String, String>>> {
        TODO("Not yet implemented")
    }

    override fun getTeamsWithPlayers(gameUUID: String): HashMap<String, MutableList<String>> {
        TODO("Not yet implemented")
    }

    override fun getTeamIds(gameUUID: String): MutableList<String> {
        TODO("Not yet implemented")
    }

    override fun getTeams(gameUUID: String): HashMap<String, String> {
        TODO("Not yet implemented")
    }


//    override fun addQuestion(gameUUID: String, q: Question): Int? {
//        val qst = games[gameUUID]?.questions
//        qst?.add(q) ?: return null
//        return (qst.size.minus(1))
//    }

//    override fun getTeams(gameUUID: String): HashMap<String,String> {
//        return games[gameUUID]?.teams ?: return null
//
//    }

    fun containsTeam(gameUUID: String, uuid: String): Boolean {
        return games[gameUUID]?.teams?.containsKey(uuid) ?: return false
    }

    override fun findGameByAdmin(admin_uuid: String): String? {
        return adminToGame[admin_uuid]

    }

    override fun findGameByCode(code: String): String? {
        return codeToGame[code]
    }

    override fun findGameByUser(uuid: String): String? {
        for (game in games) {
            if (game.value.users.contains(uuid)) {
                return game.key
            }
        }
        return null
    }

    override fun getTeamsWithQuestions(gameUUID: String): HashMap<String, MutableList<String>> {
        TODO("Not yet implemented")
    }

    override fun addQuestionsToTeams(teamsToQuestions: HashMap<String, MutableList<String>>): Boolean {
        TODO("Not yet implemented")
    }


}