package com.diploma.store

import com.diploma.WSMessage
import com.diploma.model.Question

class InMemoryStorage: Storage {
    var events: MutableList<WSMessage> = mutableListOf()
    var admins: HashSet<String> = hashSetOf()
    var users: HashSet<String> = hashSetOf()
    var teams: HashMap<String,HashSet<String>?> = hashMapOf()
    var questions: MutableList<Question> = mutableListOf()

    override fun saveEvent(message: WSMessage) {
        events.add(message)
    }

    override fun getHistory(): List<WSMessage> = events.toList()

    override fun addAdmin(uuid: String) {
        admins.add(uuid)
    }

    override fun containsAdmin(uuid: String) {
        admins.contains(uuid)
    }

    override fun removeAdmin(uuid: String) {
       admins.remove(uuid)
    }

    override fun addUser(uuid: String) {
        users.add(uuid)
    }

    override fun removeUser(uuid: String) {
        users.remove(uuid)
    }

    override fun containsUser(uuid: String) {
            users.contains(uuid)
    }

    override fun addToTeam(team_uuid: String, user_uuid: String) {
            if (teams.containsKey(team_uuid)){
                teams[team_uuid]?.add(user_uuid)
            }
    }

    override fun createTeam(uuid: String) {
        teams[uuid] = hashSetOf()
    }

    override fun removeTeam(uuid: String) {
        teams.remove(uuid)
    }

    override fun addQuestion(q: Question) {
        questions.add(q)
    }

}