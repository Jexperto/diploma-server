package com.diploma

class ConnectionManager { //TODO: clear data upon game closure
    enum class Type{
        ADMIN,
         USER,
    }
    private val adminToUserMap = hashMapOf<String,MutableList<String>>()
    private val userToAdmin = hashMapOf<String,String>()
    private val idToConnection = hashMapOf<String,Connection>()

    fun add(thisConnection: Connection, type:Type, optUUID: String? = null) {
        when(type){
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
    fun remove(thisConnection: Connection){
        idToConnection.remove(thisConnection.uuid)
    }

    fun getUsersIDS(adminID:String):List<String>?{
        return adminToUserMap[adminID]
    }
    fun getAdminID(userID:String): String? {
        return userToAdmin[userID]
    }

    fun getConnectionByID(uuid: String):Connection?{
        return idToConnection[uuid]
    }

    fun count(): Int {
        return idToConnection.count()
    }

    operator fun minusAssign(thisConnection: Connection) {
        remove(thisConnection)
    }
}