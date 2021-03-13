package com.diploma

import io.ktor.http.cio.websocket.*
import java.util.*
import java.util.concurrent.atomic.*

class Connection(val session: DefaultWebSocketSession, var uuid: UUID?) {

}