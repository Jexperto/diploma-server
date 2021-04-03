package com.diploma.store

import com.diploma.WSMessage
import com.diploma.game
import com.diploma.model.Answer
import com.diploma.model.Question
import com.diploma.service.Game
import com.diploma.service.GameState
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Statement
import java.sql.ResultSet


private val CONNECTION_URL = "jdbc:sqlite::memory:";

class InMemoryBD : Storage {
    private var con = DriverManager.getConnection(CONNECTION_URL)
    private var stateListeners = hashSetOf<(String, GameState) -> Unit>()

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
            "CREATE TABLE IF NOT EXISTS TeamsToQuestions\n" +
                    "(\n" +
                    "    team_id     TEXT not null,\n" +
                    "    question_id TEXT not null,\n" +
                    "    result BOOLEAN,\n" +
                    "    primary key (team_id, question_id),\n" +
                    "    foreign key (team_id)\n" +
                    "        references Teams (id)\n" +
                    "        on delete cascade,\n" +
                    "    foreign key (question_id)\n" +
                    "        references Questions (id)\n" +
                    "        on delete cascade\n" +
                    "\n" +
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
                rs.getObject(i).also { println("${rs.metaData.getColumnLabel(i)} ---- $it") }
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

    override fun setState(game_uuid: String, state: GameState): Boolean {
        return try {
            val stmt = con.prepareStatement("update Rooms set state = ? where id = ?")
            stmt.setInt(1, state.ordinal)
            stmt.setString(2, game_uuid)
            stmt.executeUpdate()
            for (listener in stateListeners) {
                listener(game_uuid, state)
            }
            stmt.close()

            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getState(game_uuid: String): GameState? {
        return try {
            val statement = con.prepareStatement("select state from Rooms where id=?")
            statement.setString(1, game_uuid)
            val res = statement.executeQuery().getInt(1)
            statement.close()
            GameState.values()[res]
        } catch (e: Exception) {
            null
        }
    }
    override fun plusAssign(listener: (String, GameState) -> Unit) {
        stateListeners.add(listener);
    }

    override fun minusAssign(listener: (String, GameState) -> Unit) {
        stateListeners.remove(listener)
    }


    override fun createGame(game_uuid: String, admin_uuid: String, code: String): Boolean {
        return try {
            val insertRoomSql = "insert into Rooms (id, admin_id, code,state) values (?,?,?,?)"
            val statement = con.prepareStatement(insertRoomSql)
            statement.setString(1, game_uuid)
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

    override fun removeGame(game_uuid: String): Boolean {
        return try {
            val statement = con.prepareStatement("delete from Rooms where id=?")
            statement.setString(1, game_uuid)
            statement.execute()
            statement.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getGameCode(game_uuid: String): String? {
        return try {
            val statement = con.prepareStatement("select code from Rooms where id=?")
            statement.setString(1, game_uuid)
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

    override fun getAdmin(game_uuid: String): String? {
        return try {
            val stmt = con.prepareStatement(("select admin_id from Rooms where id=?"))
            stmt.setString(1, game_uuid)
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

    override fun removeAdmin(game_uuid: String, admin_uuid: String): Boolean {
        TODO("Not yet implemented")
    }


    override fun createTeam(game_uuid: String, team_uuid: String, name: String): Boolean {
        return try {
            val stmt = con.prepareStatement("insert into Teams (room_id, id, name) values (?,?,?)")
            stmt.setString(1, game_uuid)
            stmt.setString(2, team_uuid)
            stmt.setString(3, name)
            stmt.execute()
            stmt.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun removeTeam(game_uuid: String, team_uuid: String): Boolean {
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

    override fun addToTeam(game_uuid: String, team_uuid: String, user_uuid: String): Boolean {
        return try {
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

    override fun addUser(game_uuid: String, player_id: String, name: String, team_id: String?): Boolean {
        return try {
            val stmt = con.prepareStatement("insert into Players (id, team_id, name) values (?,?,?)")
            stmt.setString(1, player_id)
            stmt.setString(2, team_id)
            stmt.setString(3, name)
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


    override fun removeUser(game_uuid: String, uuid: String): Boolean {
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


    override fun getQuestions(game_uuid: String): List<Question?> {
        try {

            val stmt = con.prepareStatement("select id,question from Questions where room_id == :1")
            stmt.setString(1, game_uuid)
            val qstRes = stmt.executeQuery()
            val map = hashMapOf<String, Question>()
            while (qstRes.next()) {
                val qstId = qstRes.getString(1)
                map[qstId] = Question(qstRes.getString(2), mutableListOf(), qstId)
            }
            val statement =
                con.prepareStatement("select question_id, answer, false as valid from Wrong_Answers, Questions where Questions.room_id = :1 union select question_id, answer, true from Right_Answers, Questions where Questions.room_id = :1;")
            statement.setString(1, game_uuid)
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

    override fun addQuestion(game_uuid: String, question_id: String, right_answer: String, question: String): Boolean {
        return try {
            con.autoCommit = false
            val stmt1 = con.prepareStatement("insert into Right_Answers (question_id, answer) values (:1,:2)")
            val stmt =
                con.prepareStatement("insert into Questions (id, room_id, question) values (:1,:2,:3); insert into Right_Answers (question_id, answer) values (:1,:4)")
            stmt.setString(1, question_id)
            stmt.setString(2, game_uuid)
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

    override fun getTeams(game_uuid: String): HashMap<String, String> {
        return try {
            val map = hashMapOf<String, String>()
            val stmt = con.prepareStatement("select id,name from Teams where room_id = ?;")
            stmt.setString(1, game_uuid)
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
                con.prepareStatement("select room_id from Teams inner join (select (team_id) from Players where id == ?);")
            stmt.setString(1, uuid)
            val res = stmt.executeQuery().getString(1)
            stmt.close()
            res
        } catch (e: Exception) {
            null
        }

    }

    override fun shuffleQuestionsByTeams(game_uuid: String) {
        TODO("Not yet implemented")
    }

    override fun saveEvent(game_uuid: String, message: WSMessage) {
        TODO("Not yet implemented")
    }

    override fun getHistory(game_uuid: String): List<WSMessage>? {
        TODO("Not yet implemented")
    }


}

