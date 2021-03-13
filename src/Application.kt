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
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.time.Duration
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap
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

        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        val games = Collections.synchronizedSet<Game?>(LinkedHashSet())
        webSocket("/user") {
            println("Adding user!")
            val thisConnection = Connection(this, null)
            connections += thisConnection
            try {
                send("You are connected! There are ${connections.count()} users here.")
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    //val textWithUsername = "[${thisConnection.name}]: $receivedText"
//                    connections.forEach {
//                        it.session.send(textWithUsername)
//                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                println("Removing $thisConnection!")
                connections -= thisConnection
            }
        }

        val module = SerializersModule {
            polymorphic(WSMessage::class) {
                subclass(ReceivedAdminCreateMessage::class)
                subclass(ReceivedAdminJoinMessage::class)
                subclass(ReceivedAdminAddQstMessage::class)
                subclass(ReceivedAdminStartRoundMessage::class)
                default { ReceivedAdminBasicMessage.serializer() }
            }
        }
// ws://localhost:8080/admin
        webSocket("/admin") {
            println("Adding user!")
            val thisConnection = Connection(this, null)
            connections += thisConnection
            try {
                send("You are connected! There are ${connections.count()} users here.")
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val msg = frame.readText()
                    try {

                        val format = Json { serializersModule = module }
                        val message = format.decodeFromString<WSMessage>(msg)
                        // println("${thisConnection.session}: $message")
                        if (message is ReceivedAdminCreateMessage) {
                            if (thisConnection.uuid != null) {
                                throw MyException("You already have an active session")
                            }
                            thisConnection.uuid = UUID.randomUUID()
                                .also {
                                    val game = Game(UUID.randomUUID().toString(), it.toString())
                                    val reply = """{
                                        "type": "create",
                                        "code": "${game.code}",
                                        "admin_id": "${it.toString()}"
                                        }""".trimIndent()
                                    send(Frame.Text(reply))
                                    games.add(game)

                                }
                                .also { println(it.toString()) }

                        } else {

                            if (thisConnection.uuid == null) {
                                throw MyException("You don't have any active sessions")
                            }
                                when (message) {
                                    is ReceivedAdminAddQstMessage -> {
                                    }
                                    is ReceivedAdminJoinMessage -> {
                                    }
                                    is ReceivedAdminStartRoundMessage -> {
                                    }
                                    else -> {
                                        println("Removing $thisConnection!")
                                        connections -= thisConnection
                                    }
                                }

                            println(thisConnection.uuid.toString())
                        }

                    } catch (e: MyException) {
                        println(e.message)
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

fun processMessage(msg: String) {
    TODO("Not yet implemented")
}

class MyException(message: String?) : Exception(message) {

}