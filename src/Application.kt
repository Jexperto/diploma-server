package com.diploma

import com.diploma.store.game.GameInMemoryStore
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

    val gameStore = GameInMemoryStore()

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/static") {
            resources("static")
        }

        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        webSocket("/user") {
            println("Adding user!")
            val thisConnection = Connection(this)
            connections += thisConnection
            try {
                send("You are connected! There are ${connections.count()} users here.")
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    val textWithUsername = "[${thisConnection.name}]: $receivedText"
                    connections.forEach {
                        it.session.send(textWithUsername)
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
            send(Frame.Text("Hi from server2"))
            while (true) {
                val frame = incoming.receive()
                if (frame is Frame.Text) {
                    val msg = frame.readText()
                    try {
                        val module = SerializersModule {
                            polymorphic(WSMessage::class) {
                                subclass(ReceivedAdminCreateMessage::class)
                                subclass(ReceivedAdminJoinMessage::class)
                                subclass(ReceivedAdminAddQstMessage::class)
                                subclass(ReceivedAdminStartRoundMessage::class)
                                default { ReceivedAdminBasicMessage.serializer() }
                            }
                        }

//                    val module = SerializersModule {
//                        polymorphic(SentAdminMessage::class) {
//                            subclass(SentAdminCreateMessage::class)
//                            subclass(SentAdminJoinMessage::class)
//                            subclass(SentAdminAddQstMessage::class)
//                            subclass(SentAdminStartMessage::class)
//                            subclass(SentAdminPlayerConnectedMessage::class)
//                            subclass(SentAdminGetAnswersMessage::class)
//                            default { SentAdminBasicMessage.serializer() }
//                        }
//                    }

                        val format = Json { serializersModule = module }
                        val message = format.decodeFromString<WSMessage>(msg)
                        println(message)
//                        when(message) {
//                            is ReceivedAdminCreateMessage -> {
//                                try {
//                                    val unit = gameStore.createGame()
//                                }
//                                catch (e: Exception) {
//                                    println("HUY a ne igra")
//                                }
//                                send("unit") // тут отправишь
//                            }
//                        }

                    } catch (e: IllegalArgumentException) {
                        val repl = "Failed to process the message"
                        println(repl)
                        send(Frame.Text(repl))
                    }

                }
            }
        }


        get("/json/gson") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}

fun processMessage(msg: String) {
    TODO("Not yet implemented")
}

