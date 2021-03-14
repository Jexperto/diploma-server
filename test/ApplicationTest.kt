package com.diploma

import com.diploma.service.Game
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.content.*
import io.ktor.http.content.*
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import java.time.*
import io.ktor.gson.*
import io.ktor.features.*
import kotlin.test.*
import io.ktor.server.testing.*

class ApplicationTest {
    @Test
    fun testGame() {
        withTestApplication({ module(testing = true) }) {
                val game = Game("test")
                game.storage.createTeam("team")
                game.storage.addToTeam("team","user")
                println(game.storage.teams["team"].toString())
                assertEquals(1,game.storage.teams.size,"Must be 1")

        }
    }

    fun testRoot() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("HELLO WORLD!", response.content)
            }
        }
    }
}
