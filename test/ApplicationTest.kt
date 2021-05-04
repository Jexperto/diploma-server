import com.diploma.server
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.junit.Test
import kotlin.test.assertEquals

class MyAppTest {
    @Test
    fun test(){
        data class Player (val pl_id: String , val pl_name: String, val team_id: String)
        val numbersMap = hashMapOf("key1" to listOf(Pair("id1","name1"),Pair("id1","name1"),Pair("id1","name1")), "key2" to listOf(Pair("id2","name2"),Pair("id2","name2"),Pair("id2","name2")), "key3" to listOf(Pair("id3","name3"),Pair("id3","name3"),Pair("id3","name3")))

        val res = mutableListOf<Player>()
        numbersMap.forEach{ (team, players) ->
            println(team)
            println(players)
            players.forEach{(id,name) ->
                res.add(Player(id,name,team))
            }
        }
        println(res)
    }

    @Test
    fun testConversation() {
        withTestApplication(moduleFunction = { server() }) {

            for (i in 1..10)
                println("\n")

            handleWebSocketConversation("/admin") { incoming, outgoing ->

                val textMessages = listOf(
                    """{
        "type": "create",
		"nick": "AdminNick"
      }""",
                    """{
        "type": "add_question",
        "question": "question1",
        "answer": "answer1"
      }""",
                    """{
        "type": "add_question",
        "question": "question2",
        "answer": "answer2"
      }"""
                )
                outgoing.send(Frame.Text(textMessages[0]))
                var othersMessage = incoming.receive() as? Frame.Text
                othersMessage?.readText().also { println("----- $it") }




                outgoing.send(Frame.Text(textMessages[1]))
                othersMessage = incoming.receive() as? Frame.Text
                othersMessage?.readText().also { println("----- $it") }

                outgoing.send(Frame.Text(textMessages[1]))

                othersMessage = incoming.receive() as? Frame.Text
                othersMessage?.readText().also { println("----- $it") }
                //assertEquals(msg, (incoming.receive() as Frame.Text).readText())

                // assertEquals(receivedMessages, received)
            }
        }
    }
}
