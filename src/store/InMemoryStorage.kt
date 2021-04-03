package com.diploma.store

import com.diploma.WSMessage
import com.diploma.game
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
    private var stateListeners = hashSetOf<(String, GameState, GameState) -> Unit>()

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


    override fun createGame(game_uuid: String, admin_uuid: String, code: String): Boolean {
        val game = GameStorage()
        games[game_uuid] = game
        codeToGame[game.code] = game_uuid
        return true
    }


    override fun removeGame(game_uuid: String): Boolean {
        //TODO: Synchronize 3 maps in a different way
        games.remove(game_uuid)
        adminToGame.values.remove(game_uuid)
        codeToGame.values.remove(game_uuid)
        return true
    }

    override fun setState(game_uuid: String, state: GameState): Boolean {
        if (!games.contains(game_uuid))
            return false
        val prev = games[game_uuid]!!.state
        games[game_uuid]!!.state = state
        for (listener in stateListeners) {
            listener(game_uuid, prev, state)
        }
        return true
    }

    override fun shuffleQuestionsByTeams(game_uuid: String) {
        val game = games[game_uuid]
        val teamCount = game?.teams?.count() ?: return
        if (teamCount < 1) return
        val k = game.questions?.count()?.div(teamCount) ?: return
        game.questions!!.shuffle()
        var i = 0
        for (team in game.teams) {
            val list = mutableListOf<Question>()
            list.addAll(game.questions!!.subList(i, (i + 1) * k))
            game.teamToQuestion[team.key] = list;
            i++
        }
        game.questions = null
    }

    override fun getUserTeam(player_id: String): String? {
        TODO("Not yet implemented")
    }

    override fun getUserName(player_id: String): String? {
        TODO("Not yet implemented")
    }

    override fun getState(game_uuid: String): GameState? {
        return games[game_uuid]?.state
    }

    override fun plusAssign(listener: (String, GameState, GameState) -> Unit) {
        stateListeners.add(listener);
    }

    override fun minusAssign(listener: (String, GameState, GameState) -> Unit) {
        stateListeners.remove(listener)
    }



    override fun getGameCode(game_uuid: String): String? {
        return games[game_uuid]?.code
    }


    override fun saveEvent(game_uuid: String, message: WSMessage) {
        games[game_uuid]?.events?.add(message)
    }

    override fun getHistory(game_uuid: String): List<WSMessage>? =
        games[game_uuid]?.events?.toList()


    override fun addAdmin(admin_uuid: String, name: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getAdminName(admin_uuid: String): String? {
        TODO("Not yet implemented")
    }

//    override fun addAdmin(game_uuid: String, uuid: String): Boolean {
//        val game = games[game_uuid] ?: return false
//        if (game.admin != null)
//            return false
//        game.admin = uuid
//        adminToGame[uuid] = game_uuid
//        //  adminGames[uuid] = game
//        return true
//    }

    override fun getAdmin(game_uuid: String): String? {
        return games[game_uuid]?.admin
    }

    override fun removeAdmin(game_uuid: String, uuid: String): Boolean {
        games[game_uuid]?.admin = null ?: return false
        return true
    }

    override fun addUser(game_uuid: String, player_id: String, name: String, team_id: String?): Boolean {
        TODO("Not yet implemented")
    }

//    override fun addUser(game_uuid: String, uuid: String): Boolean {
//        games[game_uuid]?.users?.add(uuid) ?: return false
//        return true
//    }

    override fun removeUser(game_uuid: String, uuid: String): Boolean {
        games[game_uuid]?.users?.remove(uuid) ?: return false
        return true
    }

    fun containsUser(game_uuid: String, uuid: String): Boolean {
        return games[game_uuid]?.users?.contains(uuid) ?: return false
    }

    override fun addToTeam(game_uuid: String, team_uuid: String, user_uuid: String): Boolean {
        val game = games[game_uuid]
        if (game?.teams?.containsKey(team_uuid) == true) {
            game.teams[team_uuid]?.add(user_uuid)
            return true
        }
        return false
    }

    override fun createTeam(game_uuid: String, team_uuid: String, name: String): Boolean {
        TODO("Not yet implemented")
    }

//    override fun createTeam(game_uuid: String, uuid: String): Boolean {
//        games[game_uuid]?.teams?.set(uuid, hashSetOf())
//        return true
//    }

    override fun removeTeam(game_uuid: String, uuid: String): Boolean {
        games[game_uuid]?.teams?.remove(uuid)
        return true
    }

    override fun getQuestions(game_uuid: String): List<Question?> {

        return emptyList()
    }

    override fun addQuestion(game_uuid: String, question_id: String, right_answer: String, question: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun addWrongAnswer(question_id: String, answer: String, player_id: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getTeams(game_uuid: String): HashMap<String, String> {
        TODO("Not yet implemented")
    }


//    override fun addQuestion(game_uuid: String, q: Question): Int? {
//        val qst = games[game_uuid]?.questions
//        qst?.add(q) ?: return null
//        return (qst.size.minus(1))
//    }

//    override fun getTeams(game_uuid: String): HashMap<String,String> {
//        return games[game_uuid]?.teams ?: return null
//
//    }

    fun containsTeam(game_uuid: String, uuid: String): Boolean {
        return games[game_uuid]?.teams?.containsKey(uuid) ?: return false
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


}