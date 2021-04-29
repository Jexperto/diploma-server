package com.diploma.service

import com.diploma.*
import com.diploma.model.Answer
import com.diploma.model.Question
import com.diploma.store.Storage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
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

    private fun getJsonAdminMessageString(e: SentAdminMessage): String {
        return Json.encodeToString(e)
    }

    private fun getJsonUserMessageString(e: SentUserMessage): String {
        return Json.encodeToString(e)
    }

    private var jobs = Collections.synchronizedMap<String, HashSet<Job>>(LinkedHashMap())
    private var gameToQuestionAnswers = Collections.synchronizedMap<String, HashMap<String, Int>>(LinkedHashMap())
    private var gameToQuestions = Collections.synchronizedMap<String, HashMap<String, Question>>(LinkedHashMap())

    //  val storage: GameStorageInterface = GameStorageInterface(uuid, strg)
    private var stateListeners = hashSetOf<suspend (String, GameState) -> Unit>()

    operator fun Storage.plusAssign(listener: suspend (String, GameState) -> Unit) {
        stateListeners.add(listener)
    }

    operator fun Storage.minusAssign(listener: (String, GameState) -> Unit) {
        stateListeners.remove(listener)
    }

    private suspend fun Storage.setStateWithListener(gameUUID: String, state: GameState): Boolean {
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
                GameState.FINISH -> println("FINISH")
                GameState.WAITING -> TODO()
            }
        }
    }

    private fun createGame(admin_uuid: String, admin_name: String, code: String): String? {
        val id = UUID.randomUUID().toString()
        val team1 = UUID.randomUUID().toString()
        val team2 = UUID.randomUUID().toString()
        jobs[id] = hashSetOf()
        storage.addAdmin(admin_uuid, admin_name)
        if (storage.createGame(id, admin_uuid, code)) {
            storage.createTeam(id, team1, "Red")
            storage.createTeam(id, team2, "White")
            return id
        }
        return null

    }


    suspend fun startRound(gameUUID: String, roundNumber: Int, timerInSeconds: Long?) {
        when (roundNumber) {
            1 -> {
                storage.setStateWithListener(gameUUID, GameState.ROUND1)
                val job = GlobalScope.launch { playFirstRound(gameUUID, timerInSeconds) }
                jobs[gameUUID]?.add(job)
            }
            2 -> {
                storage.setStateWithListener(gameUUID, GameState.ROUND2)
                val job = GlobalScope.launch { playSecondRound(gameUUID, timerInSeconds) }
                jobs[gameUUID]?.add(job)
            }
        }
    }

    private suspend fun playFirstRound(gameUUID: String, timerInSeconds: Long?) {
        try {
            val map = distributeQuestions(gameUUID)
            val job = GlobalScope.launch {
                delay(timerInSeconds!! * 1000)
                storage.setStateWithListener(gameUUID, GameState.MID)
                println("Changed round")
                for (user in map) {
                    connections.sendToUser(
                        user.key,
                        getJsonUserMessageString(SentUserRoundEndedMessage(1))
                    )
                }
                connections.sendToAdmin(
                    map.keys.first(),
                    getJsonAdminMessageString(SentAdminRoundEndedMessage(1)),
                    ConnectionManager.Type.USER
                )

            }
            jobs[gameUUID]?.add(job)
            for (user in map) {
                val questions = user.value
                connections.sendToUser(
                    user.key,
                    getJsonUserMessageString(
                        SentUserWrongQstMessage(
                            listOf(questions.map { it.id }.toString()),
                            listOf(questions.map { it.text }.toString())
                        )
                    )
                )
            }
        } catch (e: Exception) {
            storage.setStateWithListener(gameUUID, GameState.WAITING)
            connections.sendToAdmin(storage.getAdmin(gameUUID).toString(), (Json {
                serializersModule = SerializersModule {
                    polymorphic(WSMessage::class) {
                        subclass(WSErrorMessage::class)
                    }
                }
            }.encodeToString(WSErrorMessage(e.message.toString()) as WSMessage)), ConnectionManager.Type.ADMIN)
        }


    }

    data class QuestionIDnText(val id: String, val text: String)

    private fun distributeQuestions(gameUUID: String): HashMap<String, MutableList<QuestionIDnText>> {
        val playerToQuest = hashMapOf<String, MutableList<QuestionIDnText>>()

        val questions = storage.getQuestions(gameUUID).shuffled()
        val teams = storage.getTeamsWithPlayers(gameUUID)
        val ids = teams.keys
        if (teams.count() == 0) throw ArithmeticException("There are no teams or teams are empty")
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
                    currentPlayerID = 0
            }
            teamToQuest[team] = questionIds
        }
        storage.addQuestionsToTeams(teamToQuest)

        return playerToQuest
    }

    private suspend fun playSecondRound(gameUUID: String, timerInSeconds: Long?) {
        if (!gameToQuestions.containsKey(gameUUID)) {
            val hashMap = hashMapOf<String, Question>()
            storage.getQuestions(gameUUID).forEach {
                hashMap[it.id] = it
            }
            gameToQuestions[gameUUID] = hashMap
        }
        if (!gameToQuestionAnswers.containsKey(gameUUID))
            gameToQuestionAnswers[gameUUID] = hashMapOf()
        val adminUUID = storage.getAdmin(gameUUID)
        val teams = storage.getTeamsWithPlayers(gameUUID)
        val kek = storage.getTeamsWithQuestions(gameUUID)
        val teamsWithQuestions = kek.toList()
        val questionCount = teamsWithQuestions.minOf { it.second.count() }
        for (i in 0 until questionCount) {
            teamsWithQuestions.forEachIndexed { index, pair ->
                val tIdx: Int = if (index + 1 < teamsWithQuestions.count())
                    index + 1
                else
                    0
                val teamUUID = teamsWithQuestions[tIdx].first
                val questionUUIDs = pair.second
                val questions = gameToQuestions[gameUUID]
                val question = questions?.get(questionUUIDs[i]) ?: throw Exception("Couldn't find question")
                val rightAnswerId = (0 until question.answers.size).random()
                val ans = formAnswers(question.answers, rightAnswerId) ?: throw Exception("Couldn't form answers")
                gameToQuestionAnswers[gameUUID]?.put(question.id, rightAnswerId)
                connections.sendToUsers(
                    teams[pair.first] as List<String>, getJsonUserMessageString(
                        SentUserGetAnswersMessage(
                            question.text,
                            question.id,
                            ans
                        )
                    ).also { println(it) }
                )
                connections.sendToAdmin(
                    adminUUID.toString(), getJsonAdminMessageString(
                        SentAdminGetAnswersMessage(
                            teamUUID,
                            question.id,
                            ans.map { it.value })
                    ).also { println(it) }, ConnectionManager.Type.ADMIN
                )
            }
            //Delay between sending questions
            delay(timerInSeconds?.times(1000) ?: 0)
        }
        //Finishing Round
        connections.sendToAdmin(
            adminUUID.toString(), getJsonAdminMessageString(SentAdminRoundEndedMessage(2)), ConnectionManager.Type.ADMIN
        )
        connections.sendAllUsers(
            adminUUID.toString(), getJsonUserMessageString(SentUserRoundEndedMessage(2))
        )
        storage.setStateWithListener(gameUUID, GameState.FINISH)

    }

    private fun formAnswers(answers: List<Answer>, rightAnswerId: Int): List<Ans>? {
        val mutableList = answers.toMutableList()
        mutableList.shuffle()
        var succ = false
        for (i in mutableList.indices) {
            if (mutableList[i].valid) {
                mutableList[rightAnswerId] = mutableList[i].also { mutableList[i] = mutableList[rightAnswerId] }
                succ = true
            }
        }
        if (!succ)
            return null
        return List(mutableList.size) {
            Ans(it, mutableList[it].text)
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
                        is ReceivedUserGetTeamsMessage -> {
                            val teams = storage.getTeams(gameUuid)
                            return getJsonUserMessageString(
                                SentUserGetTeamsMessage(
                                    teams.keys.toList(),
                                    teams.values.toList()
                                )
                            )
                        }

                    }
                    GameState.ROUND1 -> when (message) {
                        is ReceivedUserWrongAnswerMessage -> handleUserWrongAnswerMessage(message)
                    }
                    GameState.ROUND2 -> when (message) {
                        is ReceivedUserRightAnswerMessage -> handleUserRightAnswerMessage(gameUuid, message)
                    }
                    GameState.MID -> {
                    }
                    GameState.FINISH -> {
                    }
                }

                println(thisConnection.uuid.toString())

            }
        }
        return null
    }

    private fun handleUserRightAnswerMessage(gameUUID: String, message: ReceivedUserRightAnswerMessage) {
        //TODO: Check if user has a right to add an answer
        if (message.answer_id == gameToQuestionAnswers[gameUUID]?.get(message.question_id))
            storage.addUserAnswerResult(message.question_id, message.pl_id, true)
        else
            storage.addUserAnswerResult(message.question_id, message.pl_id, false)
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
                thisConnection.uuid!!, getJsonAdminMessageString(
                    SentAdminPlayerConnectedMessage(
                        message.team_id,
                        storage.getUserName(thisConnection.uuid!!).toString(),
                        thisConnection.uuid!!
                    )
                ), ConnectionManager.Type.USER
            )

            return getJsonUserMessageString(SentUserJoinTeamMessage(message.team_id))
        }
        throw Exception("Couldn't join a team")
    }

    private fun handleUserJoinMessage(thisConnection: Connection, message: ReceivedUserJoinMessage): String {
        if (thisConnection.uuid != null) {
            throw SoftException("You already have an active session")
        }
        val gameID =
            com.diploma.storage.findGameByCode(message.code) ?: throw SoftException("Game code does not exist")
        thisConnection.uuid = UUID.randomUUID().toString()
        if (!joinGame(gameID, thisConnection.uuid.toString(), message.nick, null)) {
            throw SoftException("Couldn't join user to a game")
        }
        val adminUuid = gameID.let { storage.getAdmin(it) }
        connections.add(
            thisConnection, ConnectionManager.Type.USER,
            adminUuid
        )
        return getJsonUserMessageString(SentUserJoinMessage(thisConnection.uuid!!))

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
                    ) //TODO: Make it faster
                val state = storage.getState(gameUUID)
                when (state) {
                    GameState.WAITING -> {
                        return when (message) {
                            is ReceivedAdminAddQstMessage -> handleAdminAddQuestionMessage(gameUUID, message)
                            is ReceivedAdminStartRoundMessage -> handleAdminStartRoundMessage(gameUUID, message)
                            is ReceivedAdminCloseMessage -> handleAdminCloseMessage(gameUUID)
                            else -> null
                        }

                    }
                    GameState.ROUND1 -> {
                        return when (message) {
                            is ReceivedAdminCloseMessage -> handleAdminCloseMessage(gameUUID)
                            else -> null
                        }
                    }
                    GameState.ROUND2 -> {
                        return when (message) {
                            is ReceivedAdminCloseMessage -> handleAdminCloseMessage(gameUUID)
                            else -> null
                        }
                    }
                    GameState.MID -> {
                        return when (message) {
                            is ReceivedAdminStartRoundMessage -> handleAdminStartRoundMessage(gameUUID, message)
                            is ReceivedAdminCloseMessage -> return handleAdminCloseMessage(gameUUID)
                            else -> null
                        }
                    }
                    GameState.FINISH -> {

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
        return getJsonAdminMessageString(SentAdminJoinMessage(com.diploma.storage.getGameCode(game)!!))
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
        val code = getRandomString(6).also { println(it) }
        createGame(thisConnection.uuid!!, message.nick, code) ?: throw Exception("Couldn't create a game")
        val reply = SentAdminCreateMessage(
            code, thisConnection.uuid!!
        )

        println("admin_id: $thisConnection.uuid")
        return getJsonAdminMessageString(reply)
    }


    private fun handleAdminAddQuestionMessage(gameUUID: String, message: ReceivedAdminAddQstMessage): String {
        val q = Question(message.question, mutableListOf(Answer(message.answer, true)))
        storage.addQuestion(
            gameUUID,
            q.id, q.answers.first().text, q.text
        )
        return getJsonAdminMessageString(SentAdminAddQstMessage(q.id))
    }

    private suspend fun handleAdminStartRoundMessage(
        gameUUID: String,
        message: ReceivedAdminStartRoundMessage
    ): String {
        return getJsonAdminMessageString(SentAdminStartMessage(message.num, 20))
            .also { startRound(gameUUID, message.num, message.timer.toLong()) }
    }

    private fun handleAdminCloseMessage(gameUUID: String): String {
        val admin = storage.getAdmin(gameUUID)
        if (admin != null) {
            connections.removeAdminWithUsers(admin)
        }
        storage.removeGame(gameUUID)
        if (jobs.containsKey(gameUUID)) {
            jobs[gameUUID]!!.forEach { it.cancel() }
            jobs.remove(gameUUID)
        }
        gameToQuestions.remove(gameUUID)
        gameToQuestionAnswers.remove(gameUUID)

        return getJsonAdminMessageString(SentAdminCloseMessage)
    }

    private fun joinGame(gameID: String, player_uuid: String, name: String, team_uuid: String?): Boolean {
        storage.addUser(gameID, player_uuid, name, team_uuid)
        return true
    }


}


