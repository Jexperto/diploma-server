package com.diploma.service

enum class GameState {
    WAITING, // пока не наберется нужное кол-во людей
   // STARTING, // пока не истечет таймер показа списка игроком и все клиенты не пришлют 'ready'
    ROUND1, // пока не кончатся вопросы первого раунда
    MID, //  пока не истечет таймер показа списка игроком и все клиенты не пришлют 'ready'
    ROUND2, // пока не кончатся вопросы второго раунда

}

