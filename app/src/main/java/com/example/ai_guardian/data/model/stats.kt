package com.example.ai_guardian.data.model

data class Stats(
    val total: Int = 0,
    val admins: Int = 0,
    val superviseurs: Int = 0,
    val surveilles: Int = 0,
    val newUsers: Int = 0,

    val danger: Int = 0,
    val normal: Int = 0,

    val active: Int = 0,
    val blocked: Int = 0,
    val inactive: Int = 0


)