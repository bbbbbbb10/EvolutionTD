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

    // Проверка сохраненного пользователя (Авто-вход)
    fun verifySavedUser(name: String, onResult: (Boolean) -> Unit) {
        val userRef = database.child("users").child(name)
        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Юзер есть в базе, загружаем
                val record = snapshot.getValue(UserRecord::class.java)
                currentUserName = record?.name ?: name
                currentBestWave = record?.bestWave ?: 0
                onResult(true)
            } else {
                // Юзера нет в базе (возможно, базу очистили)
                onResult(false)
            }
        }.addOnFailureListener {
            onResult(false) // Ошибка сети
        }
    }

    // Попытка входа / Регистрации
    fun login(username: String?, onComplete: (Boolean, String?) -> Unit) {
        if (!username.isNullOrEmpty()) {
            // Игрок ввел ник. Проверяем, свободен ли он.
            checkAndSignIn(username, onComplete)
        } else {
            // Гость. Генерируем уникального.
            registerGuest(onComplete)
        }
    }

    private fun checkAndSignIn(name: String, onComplete: (Boolean, String?) -> Unit) {
        val userRef = database.child("users").child(name)
        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // ТАКОЙ НИК УЖЕ ЕСТЬ! Запрещаем вход.
                onComplete(false, "Username already taken!")
            } else {
                // Ник свободен, создаем
                currentUserName = name
                currentBestWave = 0
                saveScore(0)
                onComplete(true, null)
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
                // Увеличиваем счетчик
                currentData.value = value + 1
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (committed) {
                    val count = (currentData?.getValue(Int::class.java) ?: 1) - 1
                    val name = "Guest$count"
                    // Для гостя проверку делать не надо, мы гарантировали уникальность счетчиком
                    currentUserName = name
                    currentBestWave = 0
                    saveScore(0)
                    onComplete(true, null)
                } else {
                    onComplete(false, "Error creating guest")
                }
            }
        })
    }

    fun saveScore(wave: Int) {
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