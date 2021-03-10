package com.diploma

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


interface EventType

enum class NormalEvent(val event: String) : EventType {
    CREATE("create"),
    JOIN("join"),
    ADD_QUESTION("add-question"),
    START_ROUND("start-round"),
    START("start"),
    PLAYER_CONNECTED("pl-con"),
    GET_ANSWERS("ans"),
    GET_TEAMS("get-t"),
    JOIN_TEAM("join-t"),
    FAKE_ANS("wr-ans"),
    REAL_ANS("r-ans"),
    QUEST_FOR_FAKE_ANS("wr-qst"),
    TIMER_ELAPSED("timer-elapsed"),
}

enum class ErrorEvent(val event: String) : EventType {
    ERROR("error"),
    NULL("null"),
}


@Serializable
abstract class WSMessage

@Serializable
abstract class ReceivedAdminMessage : WSMessage()

@Serializable
class ReceivedAdminBasicMessage: ReceivedAdminMessage()

@Serializable
@SerialName("create")
class ReceivedAdminCreateMessage: ReceivedAdminMessage()

@Serializable
@SerialName("join")
data class ReceivedAdminJoinMessage(val admin_id: String) : ReceivedAdminMessage()

@Serializable
@SerialName("add_question")
data class ReceivedAdminAddQstMessage(val question: String, val answer: String) : ReceivedAdminMessage()

@Serializable
@SerialName("start")
data class ReceivedAdminStartRoundMessage(val num: Int, val timer: Int) : ReceivedAdminMessage()


//---------------------------------------------------------------------------//


@Serializable
abstract class SentAdminMessage : WSMessage()

@Serializable
class SentAdminBasicMessage: SentAdminMessage()

@Serializable
@SerialName("create")
data class SentAdminCreateMessage(val code: String, val admin_id: String) : SentAdminMessage()

@Serializable
@SerialName("join")
data class SentAdminJoinMessage(val code: String) : SentAdminMessage()

@Serializable
@SerialName("add_question")
data class SentAdminAddQstMessage(val question_id: String) : SentAdminMessage()

@Serializable
@SerialName("start")
data class SentAdminStartMessage(val num: Int, val max_ans: Int) : SentAdminMessage()

@Serializable
@SerialName("pl_con")
data class SentAdminPlayerConnectedMessage(val team_id: Int, val pl_id: Int) : SentAdminMessage()

@Serializable
@SerialName("ans")
data class SentAdminGetAnswersMessage(val team_id: Int, val question: Int, val answers: List<String>) : SentAdminMessage()


//return WSMessage( when () {
//    ErrorEvent.ERROR.event -> ErrorEvent.ERROR
//    NormalEvent.CREATE.event -> NormalEvent.CREATE
//    NormalEvent.JOIN.event -> NormalEvent.JOIN
//    NormalEvent.ADD_QUESTION.event -> NormalEvent.ADD_QUESTION
//    NormalEvent.START_ROUND.event -> NormalEvent.START_ROUND
//    NormalEvent.START.event -> NormalEvent.START
//    NormalEvent.PLAYER_CONNECTED.event -> NormalEvent.PLAYER_CONNECTED
//    NormalEvent.GET_ANSWERS.event -> NormalEvent.GET_ANSWERS
//    NormalEvent.GET_TEAMS.event -> NormalEvent.GET_TEAMS
//    NormalEvent.JOIN_TEAM.event -> NormalEvent.JOIN_TEAM
//    NormalEvent.FAKE_ANS.event -> NormalEvent.FAKE_ANS
//    NormalEvent.REAL_ANS.event -> NormalEvent.REAL_ANS
//    NormalEvent.QUEST_FOR_FAKE_ANS.event -> NormalEvent.QUEST_FOR_FAKE_ANS
//    NormalEvent.TIMER_ELAPSED.event -> NormalEvent.TIMER_ELAPSED
//    else -> ErrorEvent.ERROR
//})