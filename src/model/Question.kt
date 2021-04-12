package com.diploma.model

import kotlinx.serialization.Serializable
import java.util.*


data class Question(val text:String, val answers: MutableList<Answer>, val id:String = UUID.randomUUID().toString())

@Serializable
data class Answer(val text: String, val valid: Boolean)
