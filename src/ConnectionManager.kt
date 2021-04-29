package com.diploma

import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.cancel

class ConnectionManager { //TODO: clear data upon game closure
    enum class Type {
        ADMIN,
        USER,
    }

    private val adminToUserMap = hashMapOf<String, MutableList<String>>()
    private val userToAdmin = hashMapOf<String, String>()
    private val idToConnection = hashMapOf<String, Connection>()

    fun add(thisConnection: Connection, type: Type, optUUID: String? = null) {
        when (type) {
            Type.ADMIN -> {
                if (!adminToUserMap.contains(thisConnection.uuid))
                    adminToUserMap[thisConnection.uuid!!] = mutableListOf()
            }
            Type.USER -> {
                userToAdmin[thisConnection.uuid!!] = optUUID!!
                adminToUserMap[optUUID]!!.add(thisConnection.uuid!!)

            }
        }
        idToConnection[thisConnection.uuid!!] = thisConnection
    }

    fun getConnections(): Collection<Connection> {
        return idToConnection.values
    }
    fun removeUser(userConnection: Connection) {
        userConnection.uuid?.let { removeUser(it) }
    }

    fun removeUser(userUUID: String) {
        val adminUUID = userToAdmin[userUUID]
        userToAdmin.remove(userUUID)
        adminToUserMap[adminUUID]?.remove(userUUID)
        idToConnection[userUUID]!!.session.cancel()
        idToConnection.remove(userUUID)
    }

    fun removeAdminWithUsers(adminConnection: Connection) {
        adminConnection.uuid?.let { removeAdminWithUsers(it) }
    }

    fun removeAdminWithUsers(adminUUID: String) {
        adminToUserMap[adminUUID]?.forEach {
            userToAdmin.remove(it)
            idToConnection[it]!!.session.cancel()
        }
        adminToUserMap.remove(adminUUID)
        idToConnection[adminUUID]!!.session.cancel()
        idToConnection.remove(adminUUID)
    }

    fun getUsersIDS(adminID: String): List<String>? {
        return adminToUserMap[adminID]
    }

    suspend fun sendAllUsers(adminUUID: String, message: String) {
        adminToUserMap[adminUUID]?.forEach {
            idToConnection[it]!!.session.send(Frame.Text(message))
        }// ?: throw Exception("No such admin")
    }

    suspend fun sendToUser(uuid: String, message: String) {
        idToConnection[uuid]!!.session.send(Frame.Text(message))
    }

    suspend fun sendToUsers(uuids: List<String>, message: String) {
        for (uuid in uuids)
            idToConnection[uuid]?.session?.send(Frame.Text(message))
    }

    suspend fun sendToAdmin(uuid: String, message: String, type: Type) {
        when (type) {
            Type.ADMIN -> idToConnection[uuid]?.session?.send(Frame.Text(message))
            Type.USER -> idToConnection[userToAdmin[uuid]]?.session?.send(Frame.Text(message))
        }

    }

    fun getAdminID(userID: String): String? {
        return userToAdmin[userID]
    }

    fun getConnectionByID(uuid: String): Connection? {
        return idToConnection[uuid]
    }

    fun count(): Int {
        return idToConnection.count()
    }

    operator fun minusAssign(thisConnection: Connection) {
//        if (adminToUserMap.containsKey(thisConnection.uuid)){
//            removeAdminWithUsers(thisConnection)
//            return
//        }
//        if (userToAdmin.containsKey(thisConnection.uuid)){
//            removeUser(thisConnection)
//            return
//        }
        idToConnection[thisConnection.uuid]?.session?.cancel()
        idToConnection.remove(thisConnection.uuid)
    }


}