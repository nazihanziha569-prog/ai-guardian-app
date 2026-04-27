package com.example.ai_guardian.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ai_guardian.data.datasource.FirebaseDataSource
import com.example.ai_guardian.data.model.Rappel
import com.example.ai_guardian.data.repository.AlarmRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


class RappelViewModel(
    private val firebase: FirebaseDataSource,
    private val alarmRepo: AlarmRepository
) : ViewModel() {

    private val _rappels = MutableStateFlow<List<Rappel>>(emptyList())
    val rappels = _rappels.asStateFlow()


    fun listenRappels() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("Rappels")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, _ ->

                if (snapshot != null) {
                    _rappels.value = snapshot.documents.mapNotNull {
                        it.toObject(Rappel::class.java)?.copy(id = it.id)
                    }
                }
            }
    }

    fun addRappel(message: String, time: Long) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val data = hashMapOf(
            "message" to message,
            "time" to time,
            "userId" to uid
        )

        FirebaseFirestore.getInstance()
            .collection("Rappels")
            .add(data)

        alarmRepo.scheduleAlarm(time, message)
    }

    fun deleteRappel(id: String) {
        FirebaseFirestore.getInstance()
            .collection("Rappels")
            .document(id)
            .delete()
    }
}