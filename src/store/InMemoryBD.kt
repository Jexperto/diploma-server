package com.diploma.store

import com.diploma.WSMessage
import com.diploma.model.Answer
import com.diploma.model.Question
import com.diploma.service.GameState
import java.sql.DriverManager
import java.sql.Statement
import java.sql.ResultSet


private const val CONNECTION_URL = "jdbc:sqlite::memory:"

class InMemoryBD : Storage {
    private var con = DriverManager.getConnection(CONNECTION_URL)


    init {
        initBD()
    }

    private fun initBD() {
        val statement = con.createStatement()
        statement.addBatch(
            "PRAGMA foreign_keys = ON"
        )
        statement.addBatch(
            "CREATE TABLE IF NOT EXISTS Admins\n" +
                    "(\n" +
                    "    id   TEXT primary key,\n" +
                    "    name TEXT\n" +
                    ");"
        )
        statement.addBatch(
            "CREATE TABLE IF NOT EXISTS Rooms\n" +
                    "(\n" +
                    "    id       TEXT primary key,\n" +
                    "    admin_id TEXT,\n" +
                    "    code     TEXT    not null,\n" +
                    "    state    INTEGER not null,\n" +
                    "    foreign key (admin_id)\n" +
                    "        references Admins (id)\n" +
                    "        on delete cascade\n" +
                    ");"
        )
        statement.addBatch(
            "CREATE TABLE IF NOT EXISTS Teams\n" +
                    "(\n" +
                    "    id      TEXT primary key,\n" +
                    "    room_id TEXT not null,\n" +
                    "    name    TEXT not null,\n" +
                    "    points  INTEGER not null,\n" +
                    "    foreign key (room_id)\n" +
                    "        references Rooms (id)\n" +
                    "        on delete cascade\n" +
                    "\n" +
                    ");"
        )
        statement.addBatch(
            "CREATE TABLE IF NOT EXISTS Players\n" +
                    "(\n" +
                    "    id      TEXT primary key,\n" +
                    "    team_id TEXT,\n" +
                    "    room_id TEXT not null,\n" +
                    "    name    TEXT not null,\n" +
                    "    foreign key (team_id)\n" +
                    "        references Teams (id)\n" +
                    "        on delete cascade\n" +
                    ");"
        )
        statement.addBatch(
            "CREATE TABLE IF NOT EXISTS Questions\n" +
                    "(\n" +
                    "    id           TEXT primary key,\n" +
                    "    room_id      TEXT not null,\n" +
                    "    question     TEXT not null,\n" +
                    "    foreign key (room_id)\n" +
                    "        references Rooms (id)\n" +
                    "        on delete cascade\n" +
                    ");"
        )
        statement.addBatch(
            "CREATE TABLE IF NOT EXISTS Right_Answers\n" +
                    "(\n" +
                    "    question_id TEXT not null,\n" +
                    "    answer      TEXT not null,\n" +
                    "    primary key (question_id),\n" +
                    "    foreign key (question_id)\n" +
                    "        references Questions (id)\n" +
                    "        on delete cascade\n" +
                    ");"
        )
        statement.addBatch(
            "CREATE TABLE IF NOT EXISTS Wrong_Answers\n" +
                    "(\n" +
                    "    question_id TEXT not null,\n" +
                    "    answer      TEXT not null,\n" +
                    "    player_id   TEXT not null,\n" +
                    "    primary key (question_id, answer),\n" +
                    "    foreign key (player_id)\n" +
                    "        references Players (id)\n" +
                    "        on delete cascade\n" +
                    ");"
        )

        statement.addBatch(
            "CREATE TABLE IF NOT EXISTS PlayersToQuestionsResults\n" +
                    "(\n" +
                    "    player_id     TEXT not null,\n" +
                    "    question_id TEXT not null,\n" +
                    "    result      BOOLEAN,\n" +
                    "    primary key (player_id, question_id),\n" +
                    "    foreign key (player_id)\n" +
                    "        references Players (id)\n" +
                    "        on delete cascade,\n" +
                    "    foreign key (question_id)\n" +
                    "        references Questions (id)\n" +
                    "        on delete cascade\n" +
                    ");"
        )

        statement.addBatch(
            "CREATE TABLE IF NOT EXISTS TeamsToQuestions\n" +
                    "(\n" +
                    "    team_id     TEXT not null,\n" +
                    "    question_id TEXT not null,\n" +
                    "    primary key (team_id, question_id),\n" +
                    "    foreign key (team_id)\n" +
                    "        references Teams (id)\n" +
                    "        on delete cascade,\n" +
                    "    foreign key (question_id)\n" +
                    "        references Questions (id)\n" +
                    "        on delete cascade\n" +
                    ");"
        )

        statement.executeBatch()
        statement.close()
    }

    fun executeQuery(sql: String) {
        val statement = con.createStatement()
        val rs: ResultSet = statement.executeQuery(sql)
        val colCount = rs.metaData.columnCount
        while (rs.next()) {
            for (i in 1..colCount) {
                val obj = rs.getObject(i).also { println("${rs.metaData.getColumnLabel(i)} ---- $it") }
            }
            println("-----------------------")
        }
        statement.close()
    }

    fun execute(sql: String) {
        val stmt: Statement = con.createStatement()
        stmt.execute(sql)
        stmt.close()
    }

    override fun setState(gameUUID: String, state: GameState): Boolean {
        return try {
            val stmt = con.prepareStatement("update Rooms set state = ? where id = ?")
            stmt.setInt(1, state.ordinal)
            stmt.setString(2, gameUUID)
            stmt.executeUpdate()
            stmt.close()

            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getState(gameUUID: String): GameState? {
        return try {
            val statement = con.prepareStatement("select state from Rooms where id=?")
            statement.setString(1, gameUUID)
            val res = statement.executeQuery().getInt(1)
            statement.close()
            GameState.values()[res]
        } catch (e: Exception) {
            null
        }
    }


    override fun createGame(gameUUID: String, admin_uuid: String, code: String): Boolean {
        return try {
            val insertRoomSql = "insert into Rooms (id, admin_id, code,state) values (?,?,?,?)"
            val statement = con.prepareStatement(insertRoomSql)
            statement.setString(1, gameUUID)
            statement.setString(2, admin_uuid)
            statement.setString(3, code)
            statement.setInt(4, GameState.WAITING.ordinal)
            statement.execute()
            statement.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun removeGame(gameUUID: String): Boolean {
        return try {
            val statement = con.prepareStatement("delete from Rooms where id=?")
            statement.setString(1, gameUUID)
            statement.execute()
            statement.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getGameCode(gameUUID: String): String? {
        return try {
            val statement = con.prepareStatement("select code from Rooms where id=?")
            statement.setString(1, gameUUID)
            val res = statement.executeQuery().getString(1)
            statement.close()
            res
        } catch (e: Exception) {
            null
        }
    }


    override fun addAdmin(admin_uuid: String, name: String): Boolean {
        return try {
            val insertAdminSql = "insert into Admins (id, name) values (?,?)"
            val statement = con.prepareStatement(insertAdminSql)
            statement.setString(1, admin_uuid)
            statement.setString(2, name)
            statement.execute()
            statement.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getAdmin(gameUUID: String): String? {
        return try {
            val stmt = con.prepareStatement(("select admin_id from Rooms where id=?"))
            stmt.setString(1, gameUUID)
            val res = stmt.executeQuery().getString(1)
            stmt.close()
            res
        } catch (e: Exception) {
            null
        }
    }


    override fun getAdminName(admin_uuid: String): String? {
        return try {
            val stmt = con.prepareStatement(("select name from Admins where id=?"))
            stmt.setString(1, admin_uuid)
            val res = stmt.executeQuery().getString(1)
            stmt.close()
            res
        } catch (e: Exception) {
            null
        }
    }

    override fun removeAdmin(gameUUID: String, admin_uuid: String): Boolean {
        TODO("Not yet implemented")
    }


    override fun createTeam(gameUUID: String, team_uuid: String, name: String): Boolean {
        return try {
            val stmt = con.prepareStatement("insert into Teams (room_id, id, name, points) values (?,?,?,?)")
            stmt.setString(1, gameUUID)
            stmt.setString(2, team_uuid)
            stmt.setString(3, name)
            stmt.setInt(4, 0);
            stmt.execute()
            stmt.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun setTeamPoints(gameUUID: String, team_uuid: String, value: Int): Boolean {
        return try {
            val stmt = con.prepareStatement("update Teams set points = ? where id = ?")
            stmt.setInt(1, value)
            stmt.setString(2, team_uuid)
            stmt.executeUpdate()
            stmt.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getTeamPoints(gameUUID: String, team_uuid: String): Boolean {
        return try {
            val stmt = con.prepareStatement("select points from Teams where id=?")
            stmt.setString(1, team_uuid)
            stmt.executeUpdate()
            stmt.close()
            true
        } catch (e: Exception) {
            false
        }
    }


    override fun removeTeam(gameUUID: String, team_uuid: String): Boolean {
        return try {
            val stmt = con.prepareStatement("delete from Teams where id=?")
            stmt.setString(1, team_uuid)
            stmt.execute()
            stmt.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun addToTeam(gameUUID: String, team_uuid: String, user_uuid: String): Boolean {
        return try {
            //todo: check for id
            val stmt = con.prepareStatement("update Players set team_id = ? where id = ?")
            stmt.setString(1, team_uuid)
            stmt.setString(2, user_uuid)
            stmt.executeUpdate()
            stmt.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun addUser(gameUUID: String, player_id: String, name: String,room_id:String, team_id: String?): Boolean {
        return try {
            val stmt = con.prepareStatement("insert into Players (id, team_id, room_id, name) values (?,?,?,?)")
            stmt.setString(1, player_id)
            stmt.setString(2, team_id)
            stmt.setString(3, room_id)
            stmt.setString(4, name)
            stmt.execute()
            stmt.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getUserTeam(player_id: String): String? {
        return try {
            val stmt = con.prepareStatement("select team_id from Players where id=?")
            stmt.setString(1, player_id)
            val res = stmt.executeQuery().getString(1)
            stmt.close()
            res
        } catch (e: Exception) {
            null
        }
    }

    override fun getUserName(player_id: String): String? {
        return try {
            val stmt = con.prepareStatement("select name from Players where id=?")
            stmt.setString(1, player_id)
            val res = stmt.executeQuery().getString(1)
            stmt.close()
            res
        } catch (e: Exception) {
            null
        }
    }


    override fun removeUser(gameUUID: String, uuid: String): Boolean {
        return try {
            val stmt = con.prepareStatement("delete from Players where id=?")
            stmt.setString(1, uuid)
            stmt.execute()
            stmt.close()
            true
        } catch (e: Exception) {
            false
        }
    }


    override fun getQuestions(gameUUID: String): List<Question> {
        try {

            val stmt = con.prepareStatement("select id,question from Questions where room_id == :1")
            stmt.setString(1, gameUUID)
            val qstRes = stmt.executeQuery()
            val map = hashMapOf<String, Question>()
            while (qstRes.next()) {
                val qstId = qstRes.getString(1)
                map[qstId] = Question(qstRes.getString(2), mutableListOf(), qstId)
            }
            val statement =
                con.prepareStatement("select question_id, answer, false as valid from Wrong_Answers, Questions where Questions.room_id = :1 union select question_id, answer, true from Right_Answers, Questions where Questions.room_id = :1;")
            statement.setString(1, gameUUID)
            val res = statement.executeQuery()
            while (res.next()) {
                val qstId = res.getString(1)
                val answer = res.getString(2)
                val valid = res.getBoolean(3)
                map[qstId]?.answers?.add(Answer(answer, valid))
            }
            stmt.close()
            statement.close()
            return map.values.toList()

        } catch (e: Exception) {
            return listOf()
        }
    }

    override fun getQuestionIds(gameUUID: String): MutableList<String> {
        val questions = mutableListOf<String>()
        val qststmt = con.prepareStatement("select id from Questions where room_id == ?;")
        qststmt.setString(1, gameUUID)
        var res = qststmt.executeQuery()
        while (res.next())
            questions += res.getString(1)
        qststmt.close()
        return questions
    }

    override fun getUsersToQuestions(gameUUID: String): HashMap<String, List<String>> {
        TODO("Not yet implemented")
    }


    override fun addQuestion(gameUUID: String, question_id: String, right_answer: String, question: String): Boolean {
        return try {
            con.autoCommit = false
            val stmt1 = con.prepareStatement("insert into Right_Answers (question_id, answer) values (:1,:2)")
            val stmt =
                con.prepareStatement("insert into Questions (id, room_id, question) values (:1,:2,:3); insert into Right_Answers (question_id, answer) values (:1,:4)")
            stmt.setString(1, question_id)
            stmt.setString(2, gameUUID)
            stmt.setString(3, question)
            stmt1.setString(1, question_id)
            stmt1.setString(2, right_answer)
            stmt.execute()
            stmt1.execute()
            stmt.close()
            stmt1.close()
            con.commit()
            con.autoCommit = true
            true
        } catch (e: Exception) {
            con.rollback()
            con.autoCommit = true

            false
        }
    }

    override fun addWrongAnswer(question_id: String, answer: String, player_id: String): Boolean {
        return try {
            val stmt = con.prepareStatement("insert into Wrong_Answers (question_id, answer, player_id) values (?,?,?)")
            stmt.setString(1, question_id)
            stmt.setString(2, answer)
            stmt.setString(3, player_id)
            stmt.execute()
            stmt.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun addUserAnswerResult(question_id: String, player_id: String, correct: Boolean): Boolean {
        return try {
            val stmt =
                con.prepareStatement("insert into PlayersToQuestionsResults (player_id, question_id, result) values (?,?,?)")
            stmt.setString(1, player_id)
            stmt.setString(2, question_id)
            stmt.setBoolean(3, correct)
            stmt.addBatch()
            stmt.executeBatch()
            stmt.close()
            true
        } catch (e: Exception) {
            false
        }
    }


    override fun getTeamsWithPlayersAndNames(gameUUID: String): HashMap<String, MutableList<Pair<String,String>>> {
        return try {
            val map = hashMapOf<String, MutableList<Pair<String,String>>>()
            val stmt =
                con.prepareStatement("select Players.id, Players.team_id, Players.name from Players, Teams where Teams.room_id == ? and Players.team_id == Teams.id;")
            stmt.setString(1, gameUUID)
            val res = stmt.executeQuery()
            while (res.next()) {
                val playerID = res.getString(1)
                val teamID = res.getString(2)
                val name = res.getString(3)
                map[teamID]?.add(Pair(playerID,name)) ?: map.put(teamID, mutableListOf(Pair(playerID,name)))
            }
            stmt.close()
            return map
        } catch (e: Exception) {
            hashMapOf()
        }
    }

    override fun getTeamsWithPlayers(gameUUID: String): HashMap<String, MutableList<String>> {
        return try {
            val map = hashMapOf<String, MutableList<String>>()
            val stmt =
                con.prepareStatement("select Players.id, Players.team_id from Players, Teams where Teams.room_id == ? and Players.team_id == Teams.id;")
            stmt.setString(1, gameUUID)
            val res = stmt.executeQuery()
            while (res.next()) {
                val playerID = res.getString(1)
                val teamID = res.getString(2)
                map[teamID]?.add(playerID) ?: map.put(teamID, mutableListOf(playerID))
            }
            stmt.close()
            return map
        } catch (e: Exception) {
            hashMapOf()
        }
    }

    override fun getTeamIds(gameUUID: String): MutableList<String> {
        val teams = mutableListOf<String>()
        val teamstmt = con.prepareStatement("select id from Teams where room_id = ?;")
        teamstmt.setString(1, gameUUID)
        val res = teamstmt.executeQuery()
        while (res.next())
            teams += res.getString(1)
        teamstmt.close()
        return teams
    }

    override fun getTeams(gameUUID: String): HashMap<String, String> {
        return try {
            val map = hashMapOf<String, String>()
            val stmt = con.prepareStatement("select id,name from Teams where room_id = ?;")
            println("getTeams -- gameUUID: $gameUUID")
            stmt.setString(1, gameUUID)
            val res = stmt.executeQuery()
            while (res.next())
                map[res.getString(1)] = res.getString(2)
            stmt.close()
            return map
        } catch (e: Exception) {
            hashMapOf<String, String>()
        }
    }


    override fun findGameByAdmin(admin_uuid: String): String? {
        return try {
            val stmt = con.prepareStatement("select id from Rooms where admin_id = ?;")
            stmt.setString(1, admin_uuid)
            val res = stmt.executeQuery().getString(1)
            stmt.close()
            res
        } catch (e: Exception) {
            null
        }
    }

    override fun findGameByCode(code: String): String? {
        return try {
            val stmt = con.prepareStatement("select id from Rooms where code = ?;")
            stmt.setString(1, code)
            val res = stmt.executeQuery().getString(1)
            stmt.close()
            res
        } catch (e: Exception) {
            null
        }
    }

    override fun findGameByUser(uuid: String): String? {
        return try {
            val stmt =
                con.prepareStatement("select room_id from Players where Players.id==?;")
            stmt.setString(1, uuid)
            val resultSet = stmt.executeQuery()
            if (resultSet.next()) {
                val res = resultSet.getString(1)
                stmt.close()
                return res
            }
            null
        } catch (e: Exception) {
            null
        }

    }


    override fun getTeamsWithQuestions(gameUUID: String): HashMap<String, MutableList<String>> {
        return try {
            val res = hashMapOf<String, MutableList<String>>()
            val stmt =
                con.prepareStatement("select team_id,question_id from TeamsToQuestions, Teams where TeamsToQuestions.team_id == Teams.id and Teams.room_id == ?;")
            stmt.setString(1, gameUUID)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                val teamId = rs.getString(1)
                val questionId = rs.getString(2)
                if (!res.containsKey(teamId))
                    res[teamId] = mutableListOf(questionId)
                else
                    res[teamId]!!.add(questionId)
            }
            stmt.close()
            res
        } catch (e: Exception) {
            hashMapOf()
        }
    }


    override fun addQuestionsToTeams(teamsToQuestions: HashMap<String, MutableList<String>>): Boolean {
        return try {
            val stmt = con.prepareStatement("insert into TeamsToQuestions (team_id, question_id) values (?,?)")
            teamsToQuestions.forEach {
                for (q in it.value) {
                    stmt.setString(1, it.key)
                    stmt.setString(2, q)
                    stmt.addBatch()
                }
            }
            stmt.executeBatch()
            stmt.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun saveEvent(gameUUID: String, message: WSMessage) {
        TODO("Not yet implemented")
    }

    override fun getHistory(gameUUID: String): List<WSMessage>? {
        TODO("Not yet implemented")
    }


}

