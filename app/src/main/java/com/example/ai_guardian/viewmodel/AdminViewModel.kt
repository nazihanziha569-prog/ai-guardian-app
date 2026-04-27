package com.example.ai_guardian.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.ai_guardian.data.model.User
import com.example.ai_guardian.data.model.Alert
import com.example.ai_guardian.data.model.Stats
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Collections.list

class AdminViewModel : ViewModel() {
    var stats by mutableStateOf(Stats())
        private set

    // ================= USERS =================
    var users by mutableStateOf(listOf<User>())
    var search by mutableStateOf("")
    var selectedRole by mutableStateOf("all")

    // ================= ALERTS =================
    var alerts by mutableStateOf(listOf<Alert>())
        private set

    private val db = FirebaseFirestore.getInstance()

    init {
        loadUsers()
        loadAlerts()
    }

    // ================= LOAD USERS =================
    private fun loadUsers() {
        db.collection("Users")
            .addSnapshotListener { snap, error ->

                if (error != null) return@addSnapshotListener

                val usersList = snap?.documents?.mapNotNull {
                    it.toObject(User::class.java)
                } ?: emptyList()

                users = usersList

                updateStats(usersList, alerts)
            }
    }

    // ================= LOAD ALERTS =================
    private fun loadAlerts() {
        db.collection("Alerts")
            .addSnapshotListener { snap, error ->

                if (error != null) return@addSnapshotListener

                val alertsList = snap?.documents?.mapNotNull {
                    it.toObject(Alert::class.java)
                } ?: emptyList()

                alerts = alertsList.sortedByDescending { it.timestamp }

                updateStats(users, alertsList)
            }
    }

    // ================= FILTER =================
    val filteredUsers: List<User>
        get() = users.filter {
            (selectedRole == "all" || it.role == selectedRole) &&
                    it.nom.contains(search, ignoreCase = true)
        }

    // ================= ACTIONS =================
    fun deleteUser(uid: String) {
        db.collection("Users").document(uid).delete()
    }

    fun toggleBlock(user: User) {
        db.collection("Users").document(user.uid)
            .update("blocked", !user.blocked)
    }

    // (اختياري) حذف alert
    fun deleteAlert(id: String) {
        db.collection("Alerts").document(id).delete()
    }
    private fun updateStats(users: List<User>, alerts: List<Alert>) {

        val admins = users.count { it.role == "admin" }
        val sup = users.count { it.role == "superviseur" }
        val surv = users.count { it.role == "surveille" }

        val active = users.count { !it.blocked && it.isOnline == true }
        val blocked = users.count { it.blocked }
        val inactive = users.count { it.isOnline == false }

        val danger = alerts.count { it.type == "danger" }
        val normal = alerts.count { it.type == "normal" }

        stats = Stats(
           total = admins + sup + surv,
            admins = admins,
            superviseurs = sup,
            surveilles = surv,

            active = active,
            blocked = blocked,
            inactive = inactive,

            danger = danger,
            normal = normal
        )
    }
    fun refreshData() {
        loadUsers()
        loadAlerts()
    }
    // ================= SETTINGS =================

    fun clearAllAlerts() {
        db.collection("Alerts")
            .get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach { it.reference.delete() }
            }
    }

    fun resetAllUsers() {
        db.collection("Users")
            .whereNotEqualTo("role", "admin")
            .get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach { it.reference.delete() }
            }
    }

    fun makeAdmin(uid: String) {
        db.collection("Users").document(uid)
            .update("role", "admin")
    }
}
