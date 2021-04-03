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
sealed class WSMessage

@Serializable
object WSErrorMessage : WSMessage()

@Serializable
sealed class ReceivedAdminMessage : WSMessage()

@Serializable
object ReceivedAdminBasicMessage : ReceivedAdminMessage()

@Serializable
@SerialName("create")
object ReceivedAdminCreateMessage : ReceivedAdminMessage()

@Serializable
@SerialName("close")
object ReceivedAdminCloseMessage : ReceivedAdminMessage()

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
sealed class SentAdminMessage : WSMessage()

@Serializable
object SentAdminBasicMessage : SentAdminMessage()

@Serializable
@SerialName("create")
data class SentAdminCreateMessage(val code: String, val admin_id: String) : SentAdminMessage()

@Serializable
@SerialName("close")
object SentAdminCloseMessage : SentAdminMessage()

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
data class SentAdminPlayerConnectedMessage(val team_id: String, val pl_id: String) : SentAdminMessage()

@Serializable
@SerialName("ans")
data class SentAdminGetAnswersMessage(val team_id: String, val question_id: String, val answers: List<String>) : SentAdminMessage()

//---------------------------------------------------------------------------//


@Serializable
sealed class ReceivedUserMessage : WSMessage()

@Serializable
object ReceivedUserBasicMessage : ReceivedUserMessage()

@Serializable
@SerialName("join")
data class ReceivedUserJoinMessage(val nick: String, val code: String) : ReceivedUserMessage()


@Serializable
@SerialName("get_t")
object ReceivedUserGetTeamsMessage : ReceivedUserMessage()

@Serializable
@SerialName("join_t")
data class ReceivedUserJoinTeamMessage(val team_id: String) : ReceivedUserMessage()


@Serializable
@SerialName("wr_ans")
data class ReceivedUserWrongAnswerMessage(val pl_id: String, val question_id: String,val string: String) : ReceivedUserMessage()

@Serializable
@SerialName("r_ans")
data class ReceivedUserRightAnswerMessage(val pl_id: String, val question_id: String,val string: String) : ReceivedUserMessage()

//---------------------------------------------------------------------------//


@Serializable
sealed class SentUserMessage : WSMessage()

@Serializable
object SentUserBasicMessage : SentUserMessage()

@Serializable
@SerialName("join")
data class SentUserJoinMessage(val pl_id: String) : SentUserMessage()

@Serializable
@SerialName("get_t")
data class SentUserGetTeamsMessage(val team_ids: List<String>) : SentUserMessage()


@Serializable
@SerialName("join_t")
data class SentUserJoinTeamMessage(val team_id: String) : SentUserMessage()

@Serializable
@SerialName("wr_qst")
data class SentUserWrongQstMessage(val question_id: List<String>,val string: List<String>) : SentUserMessage()


@Serializable
@SerialName("timer_elapsed")
object SentUserTimerElapsedMessage : SentUserMessage()


@Serializable
@SerialName("ans")
data class SentUserGetAnswersMessage(val question: String, val question_id: String,val answers: List<Answer>) : SentUserMessage()



//---------------------------------------------------------------------------//

@Serializable
data class Answer(val key: Int, val value: String)


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