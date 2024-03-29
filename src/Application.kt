package com.diploma

import com.diploma.model.*
import com.diploma.service.Game
import com.diploma.store.DataBaseStorage
import com.diploma.store.Storage
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
import org.sqlite.JDBC
import java.io.File
import java.sql.DriverManager
import java.time.Duration

val storage: Storage = DataBaseStorage()
val connections = ConnectionManager()
val game = Game(storage, connections)
fun main(args: Array<String>) {
    DriverManager.registerDriver(JDBC())
    io.ktor.server.netty.EngineMain.main(args)
}

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.server(testing: Boolean = false) {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {

        val errorSerializer = SerializersModule {
            polymorphic(WSMessage::class) {
                subclass(WSErrorMessage::class)
            }
        }
        fun getJsonErrorString(e: String): String{
            return Json{serializersModule = errorSerializer}.encodeToString(WSErrorMessage(GenericError(e))as WSMessage)
        }
        static("/static") {
            resources("static")
        }

        //ws://localhost:8080/admin
        webSocket("/user") {
            val thisConnection = Connection(this, null)
            println("Adding connection $thisConnection");
            try {
                //send("You are connected! There are ${game.connections.count()} users here.")
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
                        send(Frame.Text(getJsonErrorString(e.message.toString())))
                        game.connections -= thisConnection
                    } catch (e: IllegalArgumentException) {
                        val repl = "Failed to process the message"
                        println(repl)
                        send(Frame.Text(getJsonErrorString(repl)))
                    }
                    catch (e: WrongAnswerErrorSoftException) {
                        println("WrongAnswerErrorSoftException: ${e.message}, ${e.question_id}, ${e.code}")
                        send(Json{serializersModule = errorSerializer}.encodeToString(WSErrorMessage(WrongAnswerError(e.question_id,e.code,e.message),ErrorPayload.wr_ans)as WSMessage))
                    }
                    catch (e: SoftException) {
                        println(e.message)
                        send(Frame.Text(getJsonErrorString(e.message.toString())))
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
            println("Created $thisConnection")
            try {
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val msg = frame.readText()
                    try {
                        val message = Json.decodeFromString<ReceivedAdminMessage>(msg)
                        val reply = game.processAdminMessage(message, thisConnection)
                        println("Reply: $reply")
                        reply?.let { Frame.Text(it) }?.let { send(it) }

                    } catch (e: ConnectionException) {
                        println(e.message)
                        val repl = getJsonErrorString(e.message.toString())
                        send(Frame.Text(repl))
                        game.connections -= thisConnection
                    } catch (e: IllegalArgumentException) {
                        val repl = "Failed to process the message\n ${e.message}"
                        println(repl)
                        send(Frame.Text(getJsonErrorString(e.message.toString())))
                    }

                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                println("Removing $thisConnection! with ${thisConnection.uuid}")
                game.connections -= thisConnection
            }
        }



        get("/json/gson") {
            call.respond(mapOf("hello" to "world"))
        }
    }


}


class ConnectionException(message: String?) : Exception(message) {}
open class SoftException(message: String?) : Exception(message) {}
class WrongAnswerErrorSoftException(val question_id:String, val code:Int? = null, text: String? = null) : SoftException(text) {}