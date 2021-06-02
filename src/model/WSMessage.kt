package com.diploma.model

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

@SerialName("error")
@Serializable
data class WSErrorMessage(var err_desc: String) : WSMessage()

@Serializable
sealed class ReceivedAdminMessage : WSMessage()

@Serializable
object ReceivedAdminBasicMessage : ReceivedAdminMessage()

@Serializable
@SerialName("create")
data class ReceivedAdminCreateMessage(val nick: String) : ReceivedAdminMessage()

@Serializable
@SerialName("close")
object ReceivedAdminCloseMessage : ReceivedAdminMessage()

@Serializable
@SerialName("get_twq")
object ReceivedGetTeamsWithQuestionsMessage : ReceivedAdminMessage()

@Serializable
@SerialName("join")
data class ReceivedAdminJoinMessage(val admin_id: String) : ReceivedAdminMessage()

@Serializable
@SerialName("add_question")
data class ReceivedAdminAddQstMessage(val question: String, val answer: String) : ReceivedAdminMessage()

@Serializable
@SerialName("add_t")
data class ReceivedAdminAddTeamsMessage(val team_names: List<String>) : ReceivedAdminMessage()

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
@SerialName("add_t")
data class SentAdminAddTeamsMessage(val team_ids: List<String>) : SentAdminMessage()

@Serializable
@SerialName("start")
data class SentAdminStartMessage(val num: Int) : SentAdminMessage()

@Serializable
@SerialName("max_ans")
data class SentAdminMaxAnsMessage(val teams: List<TeamMaxAns>) : SentAdminMessage()

@Serializable
@SerialName("pl_con")
data class SentAdminPlayerConnectedMessage(val team_id: String, val nick: String, val pl_id: String) :
    SentAdminMessage()

@Serializable
@SerialName("wr_ans")
data class SentAdminWrongAnswerMessage(val pl_id: String, val question_id: String, val answer: String) :
    SentAdminMessage()

@Serializable
@SerialName("ans")
data class SentAdminGetAnswersMessage(val team_id: String, val question_id: String, val answers: List<String>) :
    SentAdminMessage()

@Serializable
@SerialName("get_twq")
data class SentAdminGetTeamsWithQuestionsMessage(val values: List<TeamsWithQuestions>) : SentAdminMessage()

@Serializable
@SerialName("rend")
data class SentAdminRoundEndedMessage(val num: Int) : SentAdminMessage()

@Serializable
@SerialName("points")
data class SentAdminPointsUpdatedMessage(val team_id: String, val num: Int) : SentAdminMessage()

@Serializable
@SerialName("tres")
data class SentAdminTeamResultMessage(
    val question_id: String,
    val answer: String,
    val team_id: String,
    val correct: Boolean,
) : SentAdminMessage()

@Serializable
@SerialName("plres")
data class SentAdminPlayerResultMessage(
    val question_id: String,
    val answer: String,
    val pl_id: String,
    val correct: Boolean,
) : SentAdminMessage()

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
data class ReceivedUserWrongAnswerMessage(val pl_id: String, val question_id: String, val string: String) :
    ReceivedUserMessage()

@Serializable
@SerialName("r_ans")
data class ReceivedUserRightAnswerMessage(val pl_id: String, val question_id: String, val answer_id: Int) :
    ReceivedUserMessage()


//---------------------------------------------------------------------------//


@Serializable
sealed class SentUserMessage : WSMessage()

@Serializable
object SentUserBasicMessage : SentUserMessage()

@Serializable
@SerialName("join")
data class SentUserJoinMessage(val pl_id: String, val players: List<Player>) : SentUserMessage()

@Serializable
@SerialName("get_t")
data class SentUserGetTeamsMessage(val teams: List<Team>) : SentUserMessage()

@Serializable
@SerialName("pl_con")
data class SentUserPlayerConnectedMessage(val team_id: String, val nick: String, val pl_id: String) : SentUserMessage()

@Serializable
@SerialName("join_t")
data class SentUserJoinTeamMessage(val team_id: String) : SentUserMessage()

@Serializable
@SerialName("wr_qst")
data class SentUserWrongQstMessage(val questions: List<QuestionMsg>) : SentUserMessage()

@Serializable
@SerialName("wr_ans")
data class SentUserWrongAnswerSubmittedMessage(val pl_id: String, val question_id: String, val string: String) :
    SentUserMessage()

@Serializable
@SerialName("start")
data class SentUserStartMessage(val num: Int) : SentUserMessage()

@Serializable
@SerialName("timer_elapsed")
object SentUserTimerElapsedMessage : SentUserMessage()


@Serializable
@SerialName("ans")
data class SentUserGetAnswersMessage(val question: String, val question_id: String, val answers: List<Ans>) :
    SentUserMessage()

@Serializable
@SerialName("rend")
data class SentUserRoundEndedMessage(val num: Int) : SentUserMessage()

//---------------------------------------------------------------------------//
@Serializable
data class Ans(val key: Int, val value: String)

@Serializable
data class Player(val pl_id: String, val nick: String, val team_id: String)

@Serializable
data class Team(val team_id: String, val team_name: String)

@Serializable
data class TeamMaxAns(val team_id: String, val value: Int)

@Serializable
data class QuestionMsg(val question_id: String, val string: String)

@Serializable
data class TeamsWithQuestions(val team_id: String, val question_ids: List<String>)

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