package com.diploma

import com.diploma.service.Game
import com.diploma.store.InMemoryBD
import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
sealed class Project {
    abstract val name: String
}

@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()


class ApplicationTest {


    @Test
    fun testSerial() {
        withTestApplication({ module(testing = true) }) {
            val data: SentUserMessage = SentUserJoinTeamMessage("test")
            println(Json.encodeToString(data))

        }

    }

//    @Test
//    fun testGame() {
//        withTestApplication({ module(testing = true) }) {
//            val game = Game("test", connections)
//            game.storage.createTeam("team")
//            game.storage.addToTeam("team", "user")
////            println(game.storage.teams["team"].toString())
////            assertEquals(1, game.storage.teams.size, "Must be 1")
//
//        }
//    }
//
//
//    @Test
//    fun testProcessUserMessage() {
//        withTestApplication({ module(testing = true) }) {
//            val game = Game("test", connections)
//           val msg = ReceivedUserGetTeamsMessage
//            val res = game.processUserMessage(msg, "uuid")
//            assert(res is SentUserGetTeamsMessage)
//            println(Json.encodeToString(res as? SentUserGetTeamsMessage))
//        }
//    }
//
//
//    fun testRoot() {
//        withTestApplication({ module(testing = true) }) {
//            handleRequest(HttpMethod.Get, "/").apply {
//                assertEquals(HttpStatusCode.OK, response.status())
//                assertEquals("HELLO WORLD!", response.content)
//            }
//        }
//    }
}
