package com.diploma.service

import com.diploma.*
import com.diploma.model.*
import com.diploma.store.Storage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.util.*
import kotlin.collections.HashMap


class Game(
    private val storage: Storage,
    val connections: ConnectionManager,
    private val wrongAnswerTargetCount: Int = 3
) {
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

    data class AnsCorr(val value: String, val correct: Boolean);
    private var jobs = Collections.synchronizedMap<String, HashSet<Job>>(LinkedHashMap())
    private var gameToQuestionAnswers =
        Collections.synchronizedMap<String, HashMap<String, List<AnsCorr>>>(LinkedHashMap())
    private var gameToQuestions = Collections.synchronizedMap<String, HashMap<String, Question>>(LinkedHashMap())
    private var teamToPlayers = Collections.synchronizedMap<String, MutableList<String>>(LinkedHashMap())

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
                GameState.ROUND1 -> println("ROUND1")
                GameState.MID -> println("MID")
                GameState.ROUND2 -> println("ROUND2")
                GameState.FINISH -> println("FINISH")
                GameState.WAITING -> println("WAITING")
            }
        }
    }

    private fun createGame(admin_uuid: String, admin_name: String, code: String): String? {
        val id = UUID.randomUUID().toString()
        jobs[id] = hashSetOf()
        storage.addAdmin(admin_uuid, admin_name)
        if (storage.createGame(id, admin_uuid, code)) {
            return id
        }
        return null

    }


    private suspend fun startRound(gameUUID: String, roundNumber: Int, timerInSeconds: Long?) {
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
            val (playerToQuest, teamToQuest) = distributeQuestions(gameUUID)
            println("playerToQuest -- $playerToQuest")
            println("teamToQuest -- $teamToQuest")
            val job = GlobalScope.launch {
                delay(timerInSeconds!! * 1000)
                 storage.setStateWithListener(gameUUID, GameState.MID)
                for (user in playerToQuest) {
                    connections.sendToUser(
                        user.key,
                        getJsonUserMessageString(SentUserRoundEndedMessage(1))
                    )
                }
                connections.sendToAdmin(
                    playerToQuest.keys.first(),
                    getJsonAdminMessageString(SentAdminRoundEndedMessage(1)),
                    ConnectionManager.Type.USER
                )

            }
            jobs[gameUUID]?.add(job)
            connections.sendToAdmin(
                storage.getAdmin(gameUUID)!!,
                getJsonAdminMessageString(
                    SentAdminMaxAnsMessage(
                        teamToQuest.toList().map { TeamMaxAns(it.first, it.second.count() * (wrongAnswerTargetCount)) })
                ),
                ConnectionManager.Type.ADMIN
            )
            for (user in playerToQuest) {
                val questions = user.value

                connections.sendToUser(
                    user.key,
                    getJsonUserMessageString(
                        SentUserWrongQstMessage(
                            questions.map { QuestionMsg(it.id, it.text) })
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
            }.encodeToString(WSErrorMessage(GenericError(e.message.toString())) as WSMessage)), ConnectionManager.Type.ADMIN)
        }
    }

    data class QuestionIDnText(val id: String, val text: String)

    private fun distributeQuestions(gameUUID: String): Pair<HashMap<String, MutableList<QuestionIDnText>>, HashMap<String, MutableList<String>>> {
        println("DISTRIBUTE QUESTIONS CALLED")
        val playerToQuest = hashMapOf<String, MutableList<QuestionIDnText>>()
        val questions = storage.getQuestions(gameUUID).shuffled()
        val teams = storage.getTeamsWithPlayers(gameUUID)
        val ids = teams.keys
        if (teams.count() == 0) throw ArithmeticException("There are no teams or teams are empty")
        val questPerTeam = questions.size / ids.size
        teams.forEach { teamToPlayers[it.key] = it.value } //????
        println("FR TEAMS: $teams")
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
                for (ansCount in 0 until wrongAnswerTargetCount) {
                    val currPlayer = players[currentPlayerID]
                    if (!playerToQuest.contains(currPlayer))
                        playerToQuest[currPlayer] = mutableListOf()
                    val qList = playerToQuest[currPlayer]!!
                    qList.add(QuestionIDnText(q.id, q.text))
                    currentPlayerID++
                    if (currentPlayerID == players.size)
                        currentPlayerID = 0
                }
            }
            teamToQuest[team] = questionIds
        }
        storage.addQuestionsToTeams(teamToQuest)
        //teams.forEach{ teamToPlayers.remove(it.key) }
        return Pair(playerToQuest, teamToQuest)
    }

    private suspend fun playSecondRound(gameUUID: String, timerInSeconds: Long?) {

        if (!gameToQuestions.containsKey(gameUUID)) {
            val hashMap = hashMapOf<String, Question>()
            storage.getQuestions(gameUUID).forEach {
                hashMap[it.id] = it
            }
            gameToQuestions[gameUUID] = hashMap
            println("gameToQuestions2ndRound -- ${gameToQuestions[gameUUID]}")
        }
        if (!gameToQuestionAnswers.containsKey(gameUUID))
            gameToQuestionAnswers[gameUUID] = hashMapOf()
        val adminUUID = storage.getAdmin(gameUUID)!!
        val teams = storage.getTeamsWithPlayers(gameUUID)
        val teamsWithQuestions =
            storage.getTeamsWithQuestions(gameUUID).toList().onEach { println("teamWithQuestion2ndRound -- $it") }
        val questionCount =
            teamsWithQuestions.minOf { it.second.count() }.also { println("questionCount2ndRound -- $it") }
        for (i in 0 until questionCount) {
            teamsWithQuestions.forEachIndexed { index, pair ->
                val tIdx: Int = if (index + 1 < teamsWithQuestions.count())
                    index + 1
                else
                    0
                val teamUUID = teamsWithQuestions[tIdx].first
                val questionUUIDs = pair.second
                val questions = gameToQuestions[gameUUID]
                val question = questions?.get(questionUUIDs[i]) ?: throw SoftException("Couldn't find question")
                println("question2ndRound -- $question")
                val rightAnswerId = (0 until question.answers.size).random()
                val ans = formAnswers(question.answers, rightAnswerId) ?: throw SoftException("Couldn't form answers")
                println("ans2ndRound -- $ans")
                gameToQuestionAnswers[gameUUID]?.put(
                    question.id,
                    ans.map { AnsCorr(it.value, it.key == rightAnswerId) })
                sendDistributedQuestion(teams[teamUUID] as List<String>, adminUUID, teamUUID, question, ans)
            }
            //Delay between sending questions
            delay(timerInSeconds?.times(1000) ?: 0)
            for ((index, value) in teamsWithQuestions.withIndex()) {
                val tIdx: Int = if (index + 1 < teamsWithQuestions.count())
                    index + 1
                else
                    0
                val team = teamsWithQuestions[tIdx].first
                val question = value.second[i]
                val res = storage.getQuestionResultByMaxAnswered(question)
                connections.sendToAdmin(
                    adminUUID,
                    getJsonAdminMessageString(
                        SentAdminTeamResultMessage(
                            question, res?.first ?: "", //TODO: fix
                            team, res?.second ?: false
                        )
                    ), ConnectionManager.Type.ADMIN
                )
            }
        }

        //Finishing Round
        connections.sendToAdmin(
            adminUUID, getJsonAdminMessageString(SentAdminRoundEndedMessage(2)), ConnectionManager.Type.ADMIN
        )
        connections.sendAllUsers(
            adminUUID, getJsonUserMessageString(SentUserRoundEndedMessage(2))
        )
        storage.setStateWithListener(gameUUID, GameState.FINISH)

    }

    private suspend fun sendDistributedQuestion(playerUUIDs: List<String>, adminUUID: String, teamUUID: String, question: Question, answers: List<Ans>) {
        println("playerUUIDs $playerUUIDs")
        println("question $question")
        connections.sendToUsers(
            playerUUIDs, getJsonUserMessageString(
                SentUserGetAnswersMessage(
                    question.text,
                    question.id,
                    answers
                )
            ).also { println(it) }
        )
        connections.sendToAdmin(
            adminUUID, getJsonAdminMessageString(
                SentAdminGetAnswersMessage(
                    teamUUID,
                    question.id,
                    answers.map { it.value })
            ).also { println(it) }, ConnectionManager.Type.ADMIN
        )
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
                    throw SoftException("You don't have any active sessions")
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
                                    teams.toList().map { Team(it.first, it.second) }
                                )
                            )
                        }

                    }
                    GameState.ROUND1 -> when (message) {
                        is ReceivedUserWrongAnswerMessage -> handleUserWrongAnswerMessage(gameUuid, message)
                    }
                    GameState.ROUND2 -> when (message) {
                        is ReceivedUserRightAnswerMessage -> handleUserRightAnswerMessage(gameUuid, message)
                    }
                    GameState.MID -> {
                    }
                    GameState.FINISH -> {
                    }
                }

//                println(thisConnection.uuid.toString())

            }
        }
        return null
    }

    private suspend fun handleUserRightAnswerMessage(gameUUID: String, message: ReceivedUserRightAnswerMessage) {
        //TODO: Check if user has a right to add an answer
        val answerText = gameToQuestionAnswers[gameUUID]?.get(message.question_id)
            ?.get(message.answer_id)?.value.toString()
        if (gameToQuestionAnswers[gameUUID]?.get(message.question_id)?.get(message.answer_id)?.correct == true) {
            storage.addUserAnswerResult(message.question_id, message.pl_id, answerText, true)
            connections.sendToAdmin(
                message.pl_id,
                getJsonAdminMessageString(
                    SentAdminPlayerResultMessage(
                        message.question_id, answerText,
                        message.pl_id, true
                    )
                ), ConnectionManager.Type.USER
            )
        } else {
            storage.addUserAnswerResult(message.question_id, message.pl_id, answerText, false)
            connections.sendToAdmin(
                message.pl_id,
                getJsonAdminMessageString(
                    SentAdminPlayerResultMessage(
                        message.question_id,
                        answerText,
                        message.pl_id,
                        false
                    )
                ), ConnectionManager.Type.USER
            )
        }
    }

    private suspend fun handleUserWrongAnswerMessage(gameUUID: String, message: ReceivedUserWrongAnswerMessage) {
        //TODO: Check if user has a right to add an answer
        if (message.string.isEmpty()) throw WrongAnswerErrorSoftException(message.question_id).also { println("WrongAnswerStringEmpty") }
        if (!storage.addWrongAnswer(message.question_id, message.string, message.pl_id)) throw WrongAnswerErrorSoftException(message.question_id).also { println("WrongAnswerDatabaseError") }
        val team = storage.getUserTeam(message.pl_id)
        val adminUUID: String = storage.getAdmin(gameUUID).toString()
        connections.sendToAdmin(
            adminUUID,
            getJsonAdminMessageString(SentAdminWrongAnswerMessage(message.pl_id, message.question_id, message.string)),
            ConnectionManager.Type.ADMIN
        )
        teamToPlayers[team]?.let { players ->
            connections.sendToUsers(
                players,
                getJsonUserMessageString(
                    SentUserWrongAnswerSubmittedMessage(
                        message.pl_id,
                        message.question_id,
                        message.string
                    )
                )
            )
        }
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
            connections.sendAllUsers(
                connections.getAdminID(thisConnection.uuid!!).toString(), getJsonUserMessageString(
                    SentUserPlayerConnectedMessage(
                        message.team_id,
                        storage.getUserName(thisConnection.uuid!!).toString(),
                        thisConnection.uuid!!
                    )
                )
            )

            return getJsonUserMessageString(SentUserJoinTeamMessage(message.team_id))
        }
        throw SoftException("Couldn't join a team")
    }

    private fun handleUserJoinMessage(thisConnection: Connection, message: ReceivedUserJoinMessage): String {
        if (thisConnection.uuid != null) {
            throw SoftException("You already have an active session")
        }
        val gameID =
            com.diploma.storage.findGameByCode(message.code) ?: throw SoftException("Game code does not exist")
        thisConnection.uuid = UUID.randomUUID().toString()
        val players = storage.getTeamsWithPlayersAndNames(gameID)
        if (!joinGame(gameID, thisConnection.uuid.toString(), message.nick, null)) {
            throw SoftException("Couldn't join user to a game")
        }
        val adminUuid = gameID.let { storage.getAdmin(it) }
        connections.add(
            thisConnection, ConnectionManager.Type.USER,
            adminUuid
        )
        val res = mutableListOf<Player>()
        players.forEach { (team, players) ->
            players.forEach { (id, name) ->
                res.add(Player(id, name, team))
            }
        }

        return getJsonUserMessageString(SentUserJoinMessage(thisConnection.uuid!!, res))

    }


    suspend fun processAdminMessage(message: WSMessage, thisConnection: Connection): String? {
        when (message) {
            is ReceivedAdminCreateMessage -> return handleAdminCreateMessage(thisConnection, message)
            is ReceivedAdminJoinMessage -> return handleAdminJoinMessage(thisConnection, message)
            else -> {
                if (thisConnection.uuid == null) {
                    throw SoftException("You don't have any active sessions")
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
                            is ReceivedAdminAddTeamsMessage -> handleAdminAddTeamsMessage(gameUUID, message)
                            is ReceivedAdminStartRoundMessage -> handleAdminStartRoundMessage(
                                gameUUID, message,
                                thisConnection.uuid!!
                            )
                            is ReceivedAdminCloseMessage -> handleAdminCloseMessage(gameUUID)
                            is ReceivedGetTeamsWithQuestionsMessage -> handleAdminErrorMessage(
                                gameUUID,
                                "Questions are not yet distributed"
                            )
                            else -> null
                        }

                    }
                    GameState.ROUND1 -> {
                        return when (message) {
                            is ReceivedAdminCloseMessage -> handleAdminCloseMessage(gameUUID)
                            is ReceivedAdminSkipRoundMessage -> handleAdminSkipMessage(gameUUID,1);
                            is ReceivedGetTeamsWithQuestionsMessage -> handleAdminErrorMessage(
                                gameUUID,
                                "Questions are not yet distributed"
                            )
                            else -> null
                        }
                    }
                    GameState.ROUND2 -> {
                        return when (message) {
                            is ReceivedAdminSkipRoundMessage -> handleAdminSkipMessage(gameUUID,2);
                            is ReceivedAdminCloseMessage -> handleAdminCloseMessage(gameUUID)
                            is ReceivedGetTeamsWithQuestionsMessage -> handleAdminTeamsWithQuestionsMessage(gameUUID)
                            else -> null
                        }
                    }
                    GameState.MID -> {
                        return when (message) {
                            is ReceivedAdminStartRoundMessage -> handleAdminStartRoundMessage(
                                gameUUID,
                                message,
                                thisConnection.uuid!!
                            )
                            is ReceivedAdminCloseMessage -> return handleAdminCloseMessage(gameUUID)
                            is ReceivedGetTeamsWithQuestionsMessage -> handleAdminTeamsWithQuestionsMessage(gameUUID)
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

    private fun handleAdminErrorMessage(gameUUID: String, description: String): String {
        return (Json {
            serializersModule = SerializersModule {
                polymorphic(WSMessage::class) {
                    subclass(WSErrorMessage::class)
                }
            }
        }.encodeToString(WSErrorMessage(GenericError(description)) as WSMessage))
    }

    private fun handleAdminTeamsWithQuestionsMessage(gameUUID: String): String? {
        val twq = storage.getTeamsWithQuestions(gameUUID)
        return getJsonAdminMessageString(
            SentAdminGetTeamsWithQuestionsMessage(
                twq.toList().map { TeamsWithQuestions(it.first, it.second) })
        )
    }

    private fun handleAdminAddTeamsMessage(gameUUID: String, message: ReceivedAdminAddTeamsMessage): String {
        val ids = mutableListOf<String>()
        message.team_names.forEach { teamName ->
            val teamUUID = UUID.randomUUID().toString()
            if (storage.createTeam(gameUUID, teamUUID, teamName))
                ids.add(teamUUID)
        }
        return getJsonAdminMessageString(SentAdminAddTeamsMessage(ids))
    }


    private fun handleAdminJoinMessage(thisConnection: Connection, message: ReceivedAdminJoinMessage): String {
        if (thisConnection.uuid != null) {
            throw SoftException("You already have an active session")
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
            throw SoftException("You already have an active session")
        }
        thisConnection.uuid = UUID.randomUUID().toString()
        connections.add(thisConnection, ConnectionManager.Type.ADMIN)
        val code = getRandomString(6).also { println(it) }
        createGame(thisConnection.uuid!!, message.nick, code) ?: throw SoftException("Couldn't create a game")
        val reply = SentAdminCreateMessage(
            code, thisConnection.uuid!!
        )

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
        message: ReceivedAdminStartRoundMessage,
        adminUUID: String
    ): String {
        return getJsonAdminMessageString(SentAdminStartMessage(message.num))
            .also {
                connections.sendAllUsers(adminUUID, getJsonUserMessageString(SentUserStartMessage(message.num)))
                startRound(gameUUID, message.num, message.timer.toLong())
            }
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

    private suspend fun handleAdminSkipMessage(gameUUID: String, roundNumber: Int): String? {

        if (roundNumber == 1){
            jobs[gameUUID]?.forEach { it.cancel() }
            storage.setStateWithListener(gameUUID, GameState.MID)
            for (user in storage.getPlayerIds(gameUUID)) {
                connections.sendToUser(
                    user,
                    getJsonUserMessageString(SentUserRoundEndedMessage(1))
                )
            }
            return getJsonAdminMessageString(SentAdminRoundEndedMessage(1))

        }
        if (roundNumber == 2){
            jobs[gameUUID]?.forEach { it.cancel() }
            storage.setStateWithListener(gameUUID, GameState.FINISH)
            for (user in storage.getPlayerIds(gameUUID)) {
                connections.sendToUser(
                    user,
                    getJsonUserMessageString(SentUserRoundEndedMessage(2))
                )
            }
            return getJsonAdminMessageString(SentAdminRoundEndedMessage(2))

        }
        return null
    }

    private fun joinGame(gameID: String, player_uuid: String, name: String, team_uuid: String?): Boolean {
        storage.addUser(gameID, player_uuid, name, gameID, team_uuid)
        return true
    }


}


