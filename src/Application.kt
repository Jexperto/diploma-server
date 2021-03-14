package com.diploma

import com.diploma.service.Game
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.time.Duration
import java.util.*
import kotlin.collections.LinkedHashSet


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(io.ktor.websocket.WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    // val gameStore = GameInMemoryStore()


    routing {

        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/static") {
            resources("static")
        }


        val adminModule = SerializersModule {
            polymorphic(WSMessage::class) {
                subclass(ReceivedAdminCreateMessage::class)
                default { ReceivedAdminBasicMessage.serializer() }
            }
        }

        val userModule = SerializersModule {
            polymorphic(WSMessage::class) {
                subclass(ReceivedUserJoinMessage::class)
                default { ReceivedUserBasicMessage.serializer() }
            }
        }


        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        val games = Collections.synchronizedSet<Game?>(LinkedHashSet())
        webSocket("/user") {
            val thisConnection = Connection(this, null)
            connections += thisConnection
            try {
                send("You are connected! There are ${connections.count()} users here.")
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val msg = frame.readText()
                    try {

                        val format = Json { serializersModule = userModule }
                        val message = format.decodeFromString<WSMessage>(msg)
                        if (message is ReceivedUserJoinMessage) {
                            if (thisConnection.uuid != null) {
                                throw MyException("You already have an active session")
                            }
                            thisConnection.uuid = UUID.randomUUID().toString()
                                .also {
                                    if (joinGame(games, message.code, thisConnection.uuid.toString()) < 0) {
                                        throw MyException("Couldn't join user to a game")
                                    }
                                    send(Json.encodeToString(SentUserJoinMessage(it)))
                                }
                                .also { println(it) }

                        } else {

                            if (thisConnection.uuid == null) {
                                throw MyException("You don't have any active sessions")
                            }
                            //TODO: compartmentalize users by games
                            games.find { it.storage.users.contains(thisConnection.uuid) }?.processUserMessage(message)
                                ?: throw ConnectionException("Game not found. Closing connection")

                            println(thisConnection.uuid.toString())
                        }

                    } catch (e: MyException) {
                        println(e.message)
                    } catch (e: ConnectionException) {
                        println(e.message)
                        connections -= thisConnection
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
                connections -= thisConnection
            }
        }


// ws://localhost:8080/admin
        webSocket("/admin") {
            val thisConnection = Connection(this, null)
            connections += thisConnection
            try {
                send("You are connected! There are ${connections.count()} users here.")
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val msg = frame.readText()
                    try {

                        val format = Json { serializersModule = adminModule }
                        val message = format.decodeFromString<WSMessage>(msg)
                        if (message is ReceivedAdminCreateMessage) {
                            if (thisConnection.uuid != null) {
                                throw MyException("You already have an active session")
                            }
                            thisConnection.uuid = UUID.randomUUID().toString()
                                .also {
                                    val game = Game(UUID.randomUUID().toString(), it)
                                    val reply = """{
                                        "type": "create",
                                        "code": "${game.code}",
                                        "admin_id": "$it"
                                        }""".trimIndent()
                                    println("CODE: ${game.code}")
                                    send(Frame.Text(reply))
                                    games.add(game)

                                }
                                .also {
                                    println("admin_id: $it") }

                        } else {

                            if (thisConnection.uuid == null) {
                                throw MyException("You don't have any active sessions")
                            }
                            //TODO: compartmentalize admins by games
                            games.find { it.storage.admins.contains(thisConnection.uuid) }?.processAdminMessage(message)
                                ?: throw ConnectionException("Game not found. Closing connection")

                            println(thisConnection.uuid.toString())
                        }

                    } catch (e: MyException) {
                        println(e.message)
                    } catch (e: ConnectionException) {
                        println(e.message)
                        connections -= thisConnection
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
                connections -= thisConnection
            }
        }


//        try {
//            val module = SerializersModule {
//                polymorphic(WSMessage::class) {
//                    subclass(ReceivedAdminCreateMessage::class)
//                    subclass(ReceivedAdminJoinMessage::class)
//                    subclass(ReceivedAdminAddQstMessage::class)
//                    subclass(ReceivedAdminStartRoundMessage::class)
//                    default { ReceivedAdminBasicMessage.serializer() }
//                }
//            }
//
//            val format = Json { serializersModule = module }
//            val message = format.decodeFromString<WSMessage>(msg)
//            println("$thisConnection: $message")
//
//        } catch (e: IllegalArgumentException) {
//            val repl = "Failed to process the message"
//            println(repl)
//            send(Frame.Text(repl))
//        }
//


        get("/json/gson") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}

fun joinGame(games: Set<Game>, code: String, uuid: String): Int {
    games.find { it.code == code }?.storage?.addUser(uuid) ?: return -1
    return 0
}

fun processMessage(msg: String) {
    TODO("Not yet implemented")
}

class MyException(message: String?) : Exception(message) {}
class ConnectionException(message: String?) : Exception(message) {}