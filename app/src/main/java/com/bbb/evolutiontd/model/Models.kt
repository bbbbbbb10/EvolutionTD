package com.bbb.evolutiontd.model

data class UserProfile(val name: String = "", val email: String = "", val pass: String = "", val uid: String = "")
data class PlayerStats(val totalGames: Int = 0, val totalKills: Int = 0, val bestWave: Int = 0)
data class LeaderboardEntry(val name: String = "", val bestWave: Int = 0)
data class ChatMessage(val sender: String = "", val text: String = "", val timestamp: Long = 0)
data class FriendItem(val uid: String = "", val name: String = "")



