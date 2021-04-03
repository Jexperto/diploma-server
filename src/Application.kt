package com.diploma

import com.diploma.service.Game
import com.diploma.store.InMemoryStorage
import com.diploma.store.Storage
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.sqlite.JDBC
import java.sql.DriverManager
import java.time.Duration

val storage: Storage = InMemoryStorage()
val connections = ConnectionManager()
val game = Game(storage, connections)
fun main(args: Array<String>): Unit {
    DriverManager.registerDriver(JDBC())
    io.ktor.server.netty.EngineMain.main(args)
}

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(io.ktor.websocket.WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {

        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/static") {
            resources("static")
        }


        webSocket("/user") {
            val thisConnection = Connection(this, null)
            try {
                send("You are connected! There are ${game.connections.count()} users here.")
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val msg = frame.readText()
                    try {
                        val message = Json.decodeFromString<ReceivedUserMessage>(msg)
                        val reply = game.processUserMessage(message, thisConnection)
                        println("Reply: $reply")
                        reply?.let { Frame.Text(it) }?.let { send(it) }

                    } catch (e: ConnectionException) {
                        println(e.message)
                        game.connections -= thisConnection
                    } catch (e: IllegalArgumentException) {
                        val repl = "Failed to process the message"
                        println(repl)
                        send(Frame.Text(repl))
                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                println("Removing $thisConnection!")
                game.connections -= thisConnection
            }
        }


// ws://localhost:8080/admin
        webSocket("/admin") {
            val thisConnection = Connection(this, null)

            try {
                send("You are connected! There are ${game.connections.count()} users here.")
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val msg = frame.readText()
                    try {
                        val message = Json.decodeFromString<ReceivedAdminMessage>(msg)
                        val reply = game.processAdminMessage(message, thisConnection)
                        println("Reply: $reply")
                        reply?.let { Frame.Text(it) }?.let { send(it) }

                    } catch (e: ConnectionException) {
                        println(e.message).also { send(Frame.Text(it.toString())) }
                        game.connections -= thisConnection
                    } catch (e: IllegalArgumentException) {
                        val repl = "Failed to process the message"
                        println(repl)
                        send(Frame.Text(repl))
                    }

                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                println("Removing $thisConnection!")
                game.connections -= thisConnection
            }
        }



        get("/json/gson") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}

fun joinGame(code: String, player_uuid: String, name: String, team_uuid: String?): Boolean {
    val gameID = storage.findGameByCode(code) ?: return false
    storage.addUser(gameID,player_uuid,name,team_uuid)
    return true
}

class ConnectionException(message: String?) : Exception(message) {}