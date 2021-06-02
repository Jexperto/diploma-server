package com.diploma

import com.diploma.model.Answer
import com.diploma.model.Question
import com.diploma.service.Game
import com.diploma.service.GameState
import com.diploma.store.DataBaseStorage
import kotlin.test.*
import java.util.*

class DBTests {
    val bd = DataBaseStorage()

    @Test
    fun testBD() {
        val game = UUID.randomUUID().toString()
        var succ = bd.createGame(game, UUID.randomUUID().toString(), "123")
        bd.executeQuery("SELECT * FROM Rooms;")
        succ = bd.removeGame(game)
        println("-----------------------")
        bd.executeQuery("SELECT * FROM Rooms;")

    }

    @Test
    fun testQuery() {
        val code = "123"
        val admin = UUID.randomUUID().toString()
        val game = UUID.randomUUID().toString()
        val player1 = UUID.randomUUID().toString()
        val player2 = UUID.randomUUID().toString()
        val team1 = UUID.randomUUID().toString()
        val team2 = UUID.randomUUID().toString()
        var succ = bd.addAdmin(admin, "Kek")
        succ = bd.createGame(game, admin, code)
        succ = bd.createTeam(game, team1, "Team1")
        succ = bd.createTeam(game, team2, "Team2")
        succ = bd.addUser(game, player2, "Player2", game, team2)
        succ = bd.addUser(game, player1, "Player1", game, team1)
        val question = Question("question", mutableListOf(Answer("Answer", true)))
        val question2 = Question("question2", mutableListOf(Answer("Answer2", true)))
        succ = bd.addQuestion(game, question.id, question.answers.first().text, question.text)
        succ = bd.addQuestion(game, question2.id, question2.answers.first().text, question2.text)
        succ = bd.addWrongAnswer(question.id, "wrong", player1)
        succ = bd.addWrongAnswer(question2.id, "wrongqst2", player1)
        succ = bd.addWrongAnswer(question.id, "wrong2", player2)
        bd.executeQuery("select question_id, answer, false as valid from Wrong_Answers, Questions where Questions.room_id = '$game' union select question_id, answer, true from Right_Answers, Questions where Questions.room_id = '$game';")
//        bd.executeQuery("select Questions.id as question_id, question, answer, valid from Questions inner join  (\n" +
//                "select id, right_answer as answer, true as valid from Questions where room_id == '$game'\n" +
//                "union\n" +
//                "select question_id, answer, false from Wrong_Answers, Questions where question_id == Questions.id and  Questions.room_id = '$game') SEL on Questions.id == SEL.id where room_id = '$game';")

    }

    @Test
    fun testCode() {
        val code = "123"
        val admin = UUID.randomUUID().toString()
        val game = UUID.randomUUID().toString()
        var succ = bd.addAdmin(admin, "Kek")
        succ = bd.createGame(game, admin, code)
        val bdCode = bd.getGameCode(game)
        println(bdCode)
        assertEquals(code, bdCode)
        val bdGame = bd.findGameByCode(code)
        assertEquals(game, bdGame)
    }

    @Test
    fun testAdmin() {
        val code = "123"
        val admin = UUID.randomUUID().toString()
        val game = UUID.randomUUID().toString()
        var succ = bd.addAdmin(admin, "Kek")
        succ = bd.createGame(game, admin, code)
        val dbAdmin = bd.getAdmin(game)
        println(dbAdmin)
        assertEquals(admin, dbAdmin)
        val bdGame = bd.findGameByAdmin(admin)
        assertEquals(game, bdGame)
    }

    @Test
    fun testUser() {
        val test = {
            val code = "123"
            val admin = UUID.randomUUID().toString()
            val user = UUID.randomUUID().toString()
            val game = UUID.randomUUID().toString()
            val team = UUID.randomUUID().toString()
            var succ = bd.addAdmin(admin, "Kek")
            succ = bd.createGame(game, admin, code)
            succ = bd.createTeam(game, team, "kek")
            bd.addUser(game, user, "Kek", game, team)
            val dbUserTeam = bd.getUserTeam(user)
            val dbUserName = bd.getUserName(user)
            assertEquals(team, dbUserTeam)
            assertEquals("Kek", dbUserName)
            val bdGame = bd.findGameByUser(user)
            assertEquals(game, bdGame)
        }
        test()
        test()


    }

    @Test
    fun testQuestion() {
        val code = "123"
        val admin = UUID.randomUUID().toString()
        val game = UUID.randomUUID().toString()
        val team1 = UUID.randomUUID().toString()
        val team2 = UUID.randomUUID().toString()
        val player1 = UUID.randomUUID().toString()
        val player2 = UUID.randomUUID().toString()
        var succ = bd.addAdmin(admin, "Kek")
        succ = bd.createGame(game, admin, code)
        succ = bd.createTeam(game, team1, "Team1")
        succ = bd.createTeam(game, team2, "Team2")
        succ = bd.addUser(game, player2, "Player2", game, team2)
        succ = bd.addUser(game, player1, "Player1", game, team1)
        val question = Question("question", mutableListOf(Answer("Answer", true)))
        val question2 = Question("question2", mutableListOf(Answer("Answer2", true)))
        succ = bd.addQuestion(game, question.id, question.answers.first().text, question.text)
        succ = bd.addQuestion(game, question2.id, question2.answers.first().text, question2.text)
        succ = bd.addWrongAnswer(question.id, "wrong", player1)
        succ = bd.addWrongAnswer(question2.id, "wrongqst2", player1)
        succ = bd.addWrongAnswer(question.id, "wrong2", player2)
        val qsts = bd.getQuestions(game)
        qsts.forEach { q ->
            println(q?.id)
            println(q?.text)
            q?.answers?.forEach { a ->
                println("${a.text} -- ${a.valid}")
            }
            println("--------------")
        }
    }


    @Test
    fun testTeamsWithPlayers() {
        val code = "123"
        val admin = UUID.randomUUID().toString()
        val game = UUID.randomUUID().toString()
        val team1 = UUID.randomUUID().toString()
        val team2 = UUID.randomUUID().toString()
        val team3 = UUID.randomUUID().toString()
        val player1 = UUID.randomUUID().toString()
        val player2 = UUID.randomUUID().toString()
        var succ = bd.addAdmin(admin, "Kek")
        succ = bd.createGame(game, admin, code)
        succ = bd.createTeam(game, team1, "Yankees")
        succ = bd.createTeam(game, team2, "Wolves")
        succ = bd.createTeam(game, team3, "Bears")
        succ = bd.addUser(game, player2, "Player2", game, team2)
        succ = bd.addUser(game, player1, "Player1", game, team1)
        val map = bd.getTeams(game)
        map.forEach { t, u -> println("key: $t -- value: $u") }
        assertEquals(map[team1], "Yankees")
        assertEquals(map[team2], "Wolves")
        assertEquals(map[team3], "Bears")
        println("--------------")
        val twp = bd.getTeamsWithPlayers(game)
        twp.onEach { println(it) }
        val twpn = bd.getTeamsWithPlayersAndNames(game)
        twpn.onEach { println(it) }

    }

    @Test
    fun testTeams() {
        val code = "123"
        val admin = UUID.randomUUID().toString()
        val game = UUID.randomUUID().toString()
        val team1 = UUID.randomUUID().toString()
        val team2 = UUID.randomUUID().toString()
        val team3 = UUID.randomUUID().toString()
        var succ = bd.addAdmin(admin, "Kek")
        succ = bd.createGame(game, admin, code)
        succ = bd.createTeam(game, team1, "Yankees")
        succ = bd.createTeam(game, team2, "Wolves")
        succ = bd.createTeam(game, team3, "Bears")
        val map = bd.getTeams(game)
        map.forEach { t, u -> println("key: $t -- value: $u") }
        assertEquals(map[team1], "Yankees")
        assertEquals(map[team2], "Wolves")
        assertEquals(map[team3], "Bears")

    }

    @Test
    fun testShuffle() {
        val gameInstance = Game(bd, connections)
        val code = "123"
        val admin = UUID.randomUUID().toString()
        val game = UUID.randomUUID().toString()
        val team1PlayerList = List(4) { UUID.randomUUID().toString() }
        val team2PlayerList = List(4) { UUID.randomUUID().toString() }
        val team1 = UUID.randomUUID().toString().also { println(it) }
        val team2 = UUID.randomUUID().toString().also { println(it) }
        println("---------------")
        var succ = bd.addAdmin(admin, "Kek")
        succ = bd.createGame(game, admin, code)
        succ = bd.createTeam(game, team1, "Team1")
        succ = bd.createTeam(game, team2, "Team2")
        team1PlayerList.forEachIndexed { index, s ->
            succ = bd.addUser(game, s, "Player$index", game, team1)
            //println(s)
        }
        println("---------------")
        team2PlayerList.forEachIndexed { index, s ->
            succ = bd.addUser(game, s, "Player$index", game, team2)
            //println(s)
        }
        println("---------------")

        for (i in 1..8) {
            val question = Question("question$i", mutableListOf(Answer("Answer$i", true)))
            succ = bd.addQuestion(game, question.id, question.answers.first().text, question.text)
//            succ = bd.addWrongAnswer(question.id, "WrongAnswer$i", team1PlayerList[0])
//            succ = bd.addWrongAnswer(question.id, "Wrong1Answer$i", team1PlayerList[1])
//            succ = bd.addWrongAnswer(question.id, "Wrong2Answer$i", team1PlayerList[2])
        }

        //val (a, b) = gameInstance.distributeQuestions(game)
//        a.forEach { println("${it.key} -- ${it.value}") }
//        b.forEach { println("${it.key} -- ${it.value}") }
        bd.executeQuery("select * from TeamsToQuestions;")

    }


    @Test
    fun testState() {
        val code = "123"
        val admin = UUID.randomUUID().toString()
        val game = UUID.randomUUID().toString()
        var succ = bd.addAdmin(admin, "Kek")
        succ = bd.createGame(game, admin, code)
        bd.setState(game, GameState.MID)
        val state = bd.getState(game)
        assertEquals(state, GameState.MID)
    }

    @Test
    fun testGame() {
        val code = "123"
        val admin = UUID.randomUUID().toString()
        val game = UUID.randomUUID().toString()
        var succ = bd.addAdmin(admin, "Kek")
        succ = bd.createGame(game, admin, code)
        bd.setState(game, GameState.MID)
        val state = bd.getState(game)
        assertEquals(state, GameState.MID)
    }


}
