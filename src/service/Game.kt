package com.diploma.service

import com.diploma.*
import com.diploma.model.Answer
import com.diploma.model.Question
import com.diploma.store.Storage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.HashMap


class Game(val storage: Storage, val connections: ConnectionManager) {
    companion object {
        fun getRandomString(length: Int): String {
            val allowedChars = ('A'..'Z')
            return (1..length)
                .map { allowedChars.random() }
                .joinToString("")
        }
    }

    private var gameToQuestions = Collections.synchronizedMap<String, HashMap<String,Question>>(LinkedHashMap())

    //  val storage: GameStorageInterface = GameStorageInterface(uuid, strg)
    private var stateListeners = hashSetOf<suspend (String, GameState) -> Unit>()

    operator fun Storage.plusAssign(listener: suspend (String, GameState) -> Unit) {
        stateListeners.add(listener);
    }

    operator fun Storage.minusAssign(listener: (String, GameState) -> Unit) {
        stateListeners.remove(listener)
    }

    suspend fun Storage.setStateWithListener(gameUUID: String, state: GameState): Boolean {
        if (!this.setState(gameUUID, state))
            return false
        for (listener in stateListeners) {
            listener(gameUUID, state)
        }
        return true
    }

    init {
        storage += { gameID, cur ->
            when (cur) {
                GameState.ROUND1 -> {
                    println("ROUND1")
                    //storage.shuffleQuestionsByTeams(gameID)
                }
                GameState.MID -> println("MID")
                GameState.ROUND2 -> println("ROUND2")
            }
        }
    }

    private fun createGame(admin_uuid: String, admin_name: String, code: String): String? {
        val id = UUID.randomUUID().toString()
        storage.addAdmin(admin_uuid, admin_name)
        if (storage.createGame(id, admin_uuid, code)) {
            storage.createTeam(id, "1", "1")
            storage.createTeam(id, "2", "2")
            return id
        }
        return null

    }


    private suspend fun startRound(gameUUID: String, roundNumber: Int, timerInSeconds: Long?) {
        when (roundNumber) {
            1 -> {
                storage.setStateWithListener(gameUUID, GameState.ROUND1)
                val job = GlobalScope.launch {
                    launch { playFirstRound(gameUUID) }
                    launch {
                        delay(timerInSeconds!! * 1000)
                        storage.setStateWithListener(gameUUID, GameState.MID)
                    }
                }
            }
            2 -> {
                storage.setStateWithListener(gameUUID, GameState.ROUND2)
                playSecondRound(gameUUID)
            }
        }
    }

    private suspend fun playFirstRound(gameUUID: String) {
        val map = distributeQuestions(gameUUID)
        for (user in map) {
            val questions = user.value
            connections.sendToUser(
                user.key,
                Json.encodeToString(
                    SentUserWrongQstMessage(
                        listOf(questions.map { it.id }.toString()),
                        listOf(questions.map { it.text }.toString())
                    )
                )
            )

        }

    }

    data class QuestionIDnText(val id: String, val text: String)

    fun distributeQuestions(gameUUID: String): HashMap<String, MutableList<QuestionIDnText>> {
        val playerToQuest = hashMapOf<String, MutableList<QuestionIDnText>>()

        val questions = storage.getQuestions(gameUUID).shuffled()
        val teams = storage.getTeamsWithPlayers(gameUUID)
        val ids = teams.keys
        val questPerTeam = questions.size / ids.size

//            if(questions.await().size % ids.size != 0)
//                throw IllegalArgumentException("It`s impossible to divide questions evenly")
        val questIter = questions.listIterator()
        val teamToQuest = hashMapOf<String, MutableList<String>>()

        for (team in ids) {
            val questionIds = mutableListOf<String>()
            val players = teams[team]!!
            var currentPlayerID = 0
            for (j in 0 until questPerTeam) {
                if (!questIter.hasNext())
                    break
                val q = questIter.next()
                questionIds.add(q.id)
                val currPlayer = players[currentPlayerID]
                if (!playerToQuest.contains(currPlayer))
                    playerToQuest[currPlayer] = mutableListOf()
                val qList = playerToQuest[currPlayer]!!
                qList.add(QuestionIDnText(q.id, q.text))
                currentPlayerID++
                if (currentPlayerID == players.size)
                    currentPlayerID = 0;
            }
            teamToQuest[team] = questionIds
        }
        storage.addQuestionsToTeams(teamToQuest)

        return playerToQuest
    }


    private fun playSecondRound(gameUUID: String) {
        if (!gameToQuestions.containsKey(gameUUID))
            storage.getQuestions(gameUUID).forEach { gameToQuestions[gameUUID]?.put(it.id,it)}
        val adminUUID = storage.getAdmin(gameUUID)
        val teams = storage.getTeamsWithPlayers(gameUUID)
        val teamsWithQuestins = storage.getTeamsWithQuestions(gameUUID).toList()
        GlobalScope.launch {
            var questionIter = 0
            var hasQuestions = true
            while (hasQuestions) {
                val teamCount = teamsWithQuestins.count()
                teamsWithQuestins.forEachIndexed { index, pair ->
                    if (index % 2 == 0) {
                        if (index + 1 >= teamCount)
                            return@forEachIndexed
                        teams[pair.first]?.forEach { playerUUID ->
                            val questionId = teamsWithQuestins[index + 1].second[questionIter]
                            val question = gameToQuestions[gameUUID]!![questionId]
                            connections.sendToUser(
                                playerUUID, Json.encodeToString(
                                    SentUserGetAnswersMessage(
                                       question!!.text, questionId,question.answers)
                                )
                            )
                            connections.sendToAdmin(
                                adminUUID.toString(),
                                Json.encodeToString(SentAdminGetAnswersMessage(pair.first, questionId, question.answers.map { it.text })),
                                ConnectionManager.Type.ADMIN
                            )
                        }

                    } else {
                        (index >= teamCount)
                        return@forEachIndexed

                    }
                }
                questionIter++
            }


        }
    }


    suspend fun processUserMessage(message: WSMessage, thisConnection: Connection): String? {
        when (message) {
            is ReceivedUserJoinMessage -> return handleUserJoinMessage(thisConnection, message)

            else -> {
                if (thisConnection.uuid == null) {
                    throw Exception("You don't have any active sessions")
                }
                val gameUuid =
                    com.diploma.storage.findGameByUser(thisConnection.uuid!!) ?: throw ConnectionException(
                        "Game not found. Closing connection"
                    )
                when (storage.getState(gameUuid)) {
                    GameState.WAITING -> when (message) {

                        is ReceivedUserJoinTeamMessage -> return handleUserJoinTeamMessage(
                            gameUuid,
                            thisConnection,
                            message
                        )
                        is ReceivedUserGetTeamsMessage -> return Json.encodeToString(
                            storage.getTeams(gameUuid).values.toList().let {
                                SentUserGetTeamsMessage(
                                    it
                                )
                            } as SentUserMessage)
                    }
                    GameState.ROUND1 -> when (message) {
                        is ReceivedUserWrongAnswerMessage -> handleUserWrongAnswerMessage(message)
                    }
                    GameState.ROUND2 -> when (message) {
                        is ReceivedUserRightAnswerMessage -> handleUserRightAnswerMessage(gameUuid, message)
                    }
                    GameState.MID -> {

                    }
                }

                println(thisConnection.uuid.toString())

            }
        }
        return null
    }

    private fun handleUserRightAnswerMessage(gameUUID: String, message: ReceivedUserRightAnswerMessage) {
        val q = gameToQuestions[gameUUID]!!.find { it?.id == message.question_id }
        // if (message.)
    }

    private fun handleUserWrongAnswerMessage(message: ReceivedUserWrongAnswerMessage) {
        //TODO: Check if user has a right to add an answer
        storage.addWrongAnswer(message.question_id, message.string, message.pl_id)
    }


    private suspend fun handleUserJoinTeamMessage(
        gameUUID: String,
        thisConnection: Connection,
        message: ReceivedUserJoinTeamMessage
    ): String {
        if (storage.addToTeam(gameUUID, message.team_id, thisConnection.uuid!!)) {

            connections.sendToAdmin(
                thisConnection.uuid!!, Json.encodeToString(
                    SentAdminPlayerConnectedMessage(
                        message.team_id,
                        thisConnection.uuid!!
                    ) as SentAdminMessage
                ), ConnectionManager.Type.USER
            )

            return Json.encodeToString(SentUserJoinTeamMessage(message.team_id) as SentUserMessage)
        }
        throw Exception("Couldn't join a team")
    }

    private fun handleUserJoinMessage(thisConnection: Connection, message: ReceivedUserJoinMessage): String {
        if (thisConnection.uuid != null) {
            throw Exception("You already have an active session")
        }
        thisConnection.uuid = UUID.randomUUID().toString()
        val gameID =
            com.diploma.storage.findGameByCode(message.code) ?: throw Exception("Game code does not exist")
        if (!joinGame(gameID, thisConnection.uuid.toString(), message.nick, null)) {
            throw Exception("Couldn't join user to a game")
        }
        val adminUuid = gameID.let { storage.getAdmin(it) }
        connections.add(
            thisConnection, ConnectionManager.Type.USER,
            adminUuid
        )
        return (Json.encodeToString(SentUserJoinMessage(thisConnection.uuid!!) as SentUserMessage))

    }


    suspend fun processAdminMessage(message: WSMessage, thisConnection: Connection): String? {
        when (message) {
            is ReceivedAdminCreateMessage -> return handleAdminCreateMessage(thisConnection, message)
            is ReceivedAdminJoinMessage -> return handleAdminJoinMessage(thisConnection, message)
            else -> {
                if (thisConnection.uuid == null) {
                    throw Exception("You don't have any active sessions")
                }
                val gameUUID =
                    com.diploma.storage.findGameByAdmin(thisConnection.uuid!!) ?: throw ConnectionException(
                        "Game not found. Closing connection"
                    )
                when (storage.getState(gameUUID)) {
                    GameState.WAITING -> {
                        return when (message) {
                            is ReceivedAdminAddQstMessage -> handleAdminAddQuestionMessage(gameUUID, message)
                            is ReceivedAdminStartRoundMessage -> handleAdminStartRoundMessage(gameUUID, message)
                            is ReceivedAdminCloseMessage -> handleAdminCloseMessage(gameUUID, message)
                            else -> null
                        }

                    }
                    GameState.ROUND1 -> {

                    }
                    GameState.ROUND2 -> {

                    }
                    GameState.MID -> {
                        return when (message) {
                            is ReceivedAdminStartRoundMessage -> handleAdminStartRoundMessage(gameUUID, message)
                            else -> null
                        }
                    }
                }


            }


        }
        return null
    }


    private fun handleAdminJoinMessage(thisConnection: Connection, message: ReceivedAdminJoinMessage): String {
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

    private fun handleAdminCreateMessage(
        thisConnection: Connection,
        message: ReceivedAdminCreateMessage
    ): String {
        if (thisConnection.uuid != null) {
            throw Exception("You already have an active session")
        }
        thisConnection.uuid = UUID.randomUUID().toString()
        connections.add(thisConnection, ConnectionManager.Type.ADMIN)
        val gameID =
            createGame(thisConnection.uuid!!, message.nick, getRandomString(6))
                ?: throw Exception("Couldn't create a game")
        val reply =
            Json.encodeToString(storage.getGameCode(gameID).also { println("CODE: $it") }?.let { code ->
                SentAdminCreateMessage(
                    code, thisConnection.uuid!!
                )
            } as SentAdminMessage)

        println("admin_id: $thisConnection.uuid")
        return reply
    }


    private fun handleAdminAddQuestionMessage(gameUUID: String, message: ReceivedAdminAddQstMessage): String {
        val q = Question(message.question, mutableListOf(Answer(message.answer, true)))
        storage.addQuestion(
            gameUUID,
            q.id, q.answers.first().text, q.text
        )
        return Json.encodeToString(SentAdminAddQstMessage(q.id) as SentAdminMessage)
    }

    private suspend fun handleAdminStartRoundMessage(
        gameUUID: String,
        message: ReceivedAdminStartRoundMessage
    ): String {
        return Json.encodeToString(SentAdminStartMessage(message.num, 20) as SentAdminMessage)
            .also { startRound(gameUUID, message.num, message.timer.toLong()) }
    }

    private fun handleAdminCloseMessage(gameUUID: String, message: ReceivedAdminCloseMessage): String {
        storage.removeGame(gameUUID)
        return Json.encodeToString(SentAdminCloseMessage as SentAdminMessage)
    }

    fun joinGame(gameID: String, player_uuid: String, name: String, team_uuid: String?): Boolean {
        com.diploma.storage.addUser(gameID, player_uuid, name, team_uuid)
        return true
    }


}