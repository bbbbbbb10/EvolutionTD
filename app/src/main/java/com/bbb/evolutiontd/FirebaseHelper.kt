package com.bbb.evolutiontd

import com.bbb.evolutiontd.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

object FirebaseHelper {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    var currentUserName: String = "Player"
    var currentBestWave: Int = 0
    val uid: String? get() = auth.currentUser?.uid

    // 1. СОЗДАНИЕ ДАННЫХ (6 ТАБЛИЦ ДЛЯ ПРАКТИКИ)
    fun createInitialUserData(name: String, email: String, pass: String, onComplete: (Boolean) -> Unit) {
        val u = uid ?: return
        currentUserName = name

        val updates = mapOf(
            "users/$u" to UserProfile(name, email, pass),
            "stats/$u" to PlayerStats(0, 0, 0),
            "leaderboard/$u" to LeaderboardEntry(name, 0),
            "friends/$u" to mapOf("init" to true),
            "friend_requests/$u" to mapOf("init" to true),
            "chats/global/init_status" to "ready" // Техническая метка для папки чата
        )
        db.updateChildren(updates).addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    // 2. ЗАГРУЗКА ПРОФИЛЯ ПРИ ВХОДЕ
    fun loadUserData(onComplete: (Boolean) -> Unit) {
        val u = uid ?: return
        db.child("users").child(u).get().addOnSuccessListener { snapshot ->
            try {
                // Проверяем, существует ли узел вообще
                if (snapshot.exists()) {
                    val profile = snapshot.getValue(UserProfile::class.java)
                    currentUserName = profile?.name ?: "Player"

                    db.child("leaderboard").child(u).child("bestWave").get().addOnSuccessListener { waveSnap ->
                        currentBestWave = waveSnap.getValue(Int::class.java) ?: 0
                        onComplete(true)
                    }.addOnFailureListener { onComplete(false) }
                } else {
                    // Если узла нет (удалили в базе вручную)
                    logout { onComplete(false) }
                }
            } catch (e: Exception) {
                // Если данные в базе "кривые" и класс UserProfile не может их прочитать
                android.util.Log.e("TD_ERROR", "Data format error: ${e.message}")
                onComplete(false)
            }
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    // 3. СОХРАНЕНИЕ РЕКОРДА И СТАТИСТИКИ УБИЙСТВ
    fun saveScore(wave: Int, kills: Int) {
        val u = uid ?: return

        // Обновляем лучший результат волны
        if (wave > currentBestWave) {
            currentBestWave = wave
            db.child("leaderboard").child(u).child("bestWave").setValue(wave)
            db.child("stats").child(u).child("bestWave").setValue(wave)
        }

        // Прибавляем убийства к общей статистике
        db.child("stats").child(u).child("totalKills").get().addOnSuccessListener { snapshot ->
            val currentKills = snapshot.getValue(Int::class.java) ?: 0
            db.child("stats").child(u).child("totalKills").setValue(currentKills + kills)
        }

        // +1 к сыгранным играм
        db.child("stats").child(u).child("totalGames").get().addOnSuccessListener { snapshot ->
            val games = snapshot.getValue(Int::class.java) ?: 0
            db.child("stats").child(u).child("totalGames").setValue(games + 1)
        }
    }

    // 4. ЗАГРУЗКА СТАТИСТИКИ ДЛЯ ОКНА ПРОФИЛЯ
    fun loadStats(callback: (PlayerStats) -> Unit) {
        val u = uid ?: return
        db.child("stats").child(u).get().addOnSuccessListener {
            callback(it.getValue(PlayerStats::class.java) ?: PlayerStats())
        }
    }

    // 5. ГЛОБАЛЬНЫЙ ЧАТ (С защитой от вылета)
    fun sendGlobalMessage(text: String) {
        val msg = ChatMessage(currentUserName, text, System.currentTimeMillis())
        db.child("chats").child("global").push().setValue(msg)
    }

    fun observeGlobalChat(onUpdate: (List<ChatMessage>) -> Unit) {
        db.child("chats").child("global").limitToLast(50).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val list = mutableListOf<ChatMessage>()
                for (child in s.children) {
                    // Проверяем, что это сообщение, а не техническая метка init_status
                    if (child.hasChild("sender")) {
                        child.getValue(ChatMessage::class.java)?.let { list.add(it) }
                    }
                }
                onUpdate(list)
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    // 6. ПРИВАТНЫЙ ЧАТ С ДРУГОМ
    fun sendPrivateMessage(friendUid: String, text: String) {
        val myUid = uid ?: return
        val chatId = if (myUid < friendUid) "${myUid}_$friendUid" else "${friendUid}_$myUid"
        val msg = ChatMessage(currentUserName, text, System.currentTimeMillis())
        db.child("chats").child("private").child(chatId).push().setValue(msg)
    }

    fun observePrivateChat(friendUid: String, onUpdate: (List<ChatMessage>) -> Unit) {
        val myUid = uid ?: return
        val chatId = if (myUid < friendUid) "${myUid}_$friendUid" else "${friendUid}_$myUid"
        db.child("chats").child("private").child(chatId).limitToLast(50).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val list = s.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                onUpdate(list)
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    // 7. СИСТЕМА ДРУЗЕЙ
    fun findUserByName(name: String, callback: (UserProfile?) -> Unit) {
        db.child("users").orderByChild("name").equalTo(name).get().addOnSuccessListener { snap ->
            if (snap.exists()) {
                val child = snap.children.first()
                val user = child.getValue(UserProfile::class.java)?.copy(uid = child.key ?: "")
                callback(user)
            } else callback(null)
        }
    }

    fun sendFriendRequest(targetUid: String) {
        val myUid = uid ?: return
        db.child("friend_requests").child(targetUid).child(myUid).setValue(currentUserName)
    }

    fun observeFriendRequests(callback: (List<FriendItem>) -> Unit) {
        val myUid = uid ?: return
        db.child("friend_requests").child(myUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val list = s.children.filter { it.key != "init" }.map {
                    FriendItem(it.key ?: "", it.value.toString())
                }
                callback(list)
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    fun acceptFriend(fUid: String, fName: String) {
        val myUid = uid ?: return
        db.child("friends").child(myUid).child(fUid).setValue(fName)
        db.child("friends").child(fUid).child(myUid).setValue(currentUserName)
        db.child("friend_requests").child(myUid).child(fUid).removeValue()
    }

    fun declineFriendRequest(senderUid: String) {
        val myUid = uid ?: return
        db.child("friend_requests").child(myUid).child(senderUid).removeValue()
    }

    fun observeFriends(callback: (List<FriendItem>) -> Unit) {
        val myUid = uid ?: return
        db.child("friends").child(myUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val list = s.children.filter { it.key != "init" }.map {
                    FriendItem(it.key ?: "", it.value.toString())
                }
                callback(list)
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    fun removeFriend(fUid: String) {
        val myUid = uid ?: return
        db.child("friends").child(myUid).child(fUid).removeValue()
        db.child("friends").child(fUid).child(myUid).removeValue()
    }

    // 8. ЛИДЕРБОРД
    fun getLeaderboard(callback: (List<LeaderboardEntry>) -> Unit) {
        db.child("leaderboard").orderByChild("bestWave").limitToLast(25).get().addOnSuccessListener { snapshot ->
            val list = mutableListOf<LeaderboardEntry>()
            for (child in snapshot.children) {
                child.getValue(LeaderboardEntry::class.java)?.let { list.add(it) }
            }
            callback(list.sortedByDescending { it.bestWave })
        }
    }
    // Управление статусом "В сети"
    fun managePresence() {
        val u = uid ?: return
        // Ссылка на узел статуса конкретного пользователя
        val statusRef = db.child("status").child(u)

        // Специальная ссылка Firebase, которая следит за соединением
        val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")

        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    // Если соединение есть ставим online
                    statusRef.child("online").setValue(true)

                    // Если связь разорвется Firebase сам поставит false на сервере
                    statusRef.child("online").onDisconnect().setValue(false)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Функция для получения статуса конкретного друга
    fun getFriendStatus(friendUid: String, onUpdate: (Boolean) -> Unit) {
        db.child("status").child(friendUid).child("online").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnline = snapshot.getValue(Boolean::class.java) ?: false
                onUpdate(isOnline)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 9. ВЫХОД
    fun logout(onComplete: () -> Unit) {
        val u = uid ?: return

        // 1. Сначала принудительно ставим статус "оффлайн" в базе
        db.child("status").child(u).child("online").setValue(false).addOnCompleteListener {

            // 2. Отменяем действие onDisconnect, чтобы оно не сработало позже случайно
            db.child("status").child(u).child("online").onDisconnect().cancel()

            // 3. Выходим из аккаунта Firebase
            auth.signOut()

            // 4. Сообщаем Activity, что можно переходить на экран логина
            onComplete()
        }
    }
}