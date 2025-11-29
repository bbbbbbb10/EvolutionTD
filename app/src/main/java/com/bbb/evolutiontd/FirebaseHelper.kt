package com.bbb.evolutiontd

import com.google.firebase.Firebase
import com.google.firebase.database.*
data class UserRecord(
    val name: String = "",
    val bestWave: Int = 0
)

object FirebaseHelper {
    private val database = Firebase.database.reference

    var currentUserName: String = "Unknown"
    var currentBestWave: Int = 0

    // Проверка при авто-входе
    fun verifySavedUser(name: String, onResult: (Int) -> Unit) {
        val userRef = database.child("users").child(name)
        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val record = snapshot.getValue(UserRecord::class.java)
                currentUserName = record?.name ?: name
                currentBestWave = record?.bestWave ?: 0
                onResult(1) // Успех
            } else {
                onResult(2) // Юзера нет в базе
            }
        }.addOnFailureListener {
            currentUserName = name
            onResult(0) // Ошибка сети (пускаем офлайн)
        }
    }

    // Вход
    fun login(username: String?, onComplete: (Boolean, String?) -> Unit) {
        if (!username.isNullOrEmpty()) {
            checkAndSignIn(username, onComplete)
        } else {
            registerGuest(onComplete)
        }
    }

    private fun checkAndSignIn(name: String, onComplete: (Boolean, String?) -> Unit) {
        val userRef = database.child("users").child(name)
        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                onComplete(false, "Username already taken!")
            } else {
                // Создаем нового юзера
                currentUserName = name
                currentBestWave = 0

                // ИСПРАВЛЕНИЕ: ПРИНУДИТЕЛЬНО ПИШЕМ В БАЗУ
                val newUser = UserRecord(name, 0)
                userRef.setValue(newUser)
                    .addOnSuccessListener { onComplete(true, null) }
                    .addOnFailureListener { onComplete(false, "DB Error") }
            }
        }.addOnFailureListener {
            onComplete(false, "Network error")
        }
    }

    private fun registerGuest(onComplete: (Boolean, String?) -> Unit) {
        val counterRef = database.child("meta").child("guest_counter")

        counterRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                var value = currentData.getValue(Int::class.java) ?: 0
                currentData.value = value + 1
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (committed) {
                    val count = (currentData?.getValue(Int::class.java) ?: 1) - 1
                    val name = "Guest$count"

                    currentUserName = name
                    currentBestWave = 0

                    // ИСПРАВЛЕНИЕ: ПРИНУДИТЕЛЬНО ПИШЕМ В БАЗУ
                    val newUser = UserRecord(name, 0)
                    database.child("users").child(name).setValue(newUser)

                    onComplete(true, null)
                } else {
                    onComplete(false, "Error creating guest")
                }
            }
        })
    }

    // Сохранение рекорда (используется только при Game Over)
    fun saveScore(wave: Int) {
        if (currentUserName == "Unknown") return

        // Если волна больше рекорда -> обновляем
        if (wave > currentBestWave) {
            currentBestWave = wave
            val userRef = database.child("users").child(currentUserName)
            userRef.setValue(UserRecord(currentUserName, currentBestWave))
        }
    }

    fun getLeaderboard(callback: (List<UserRecord>) -> Unit) {
        val query = database.child("users").orderByChild("bestWave").limitToLast(20)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<UserRecord>()
                for (child in snapshot.children) {
                    val record = child.getValue(UserRecord::class.java)
                    if (record != null) list.add(record)
                }
                callback(list.reversed())
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}