package com.diploma.model

import java.util.*


data class Question(val id:String = UUID.randomUUID().toString(), val text:String, var answers: List<Answer>)

data class Answer(val text: String, val valid: Boolean)
