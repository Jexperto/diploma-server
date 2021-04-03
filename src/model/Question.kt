package com.diploma.model

import java.util.*


data class Question(val text:String, val answers: MutableList<Answer>, val id:String = UUID.randomUUID().toString())

data class Answer(val text: String, val valid: Boolean)
