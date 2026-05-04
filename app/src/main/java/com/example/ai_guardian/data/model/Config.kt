package com.example.ai_guardian.data.model

data class Config(
    val userId        : String  = "",
    val age           : Int     = 0,
    val maladies      : String  = "",
    val localisation  : Boolean = true,
    val alertesActives: Boolean = true,
    val heureReveil   : String  = "07:00",
    val heureSommeil  : String  = "22:00"
)