package com.bbb.evolutiontd

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        prefs = getSharedPreferences("EvolutionTD_Prefs", Context.MODE_PRIVATE)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGuest = findViewById<Button>(R.id.btnGuest)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // --- ЛОГИКА АВТО-ВХОДА ---
        val savedUser = prefs.getString("username", null)

        if (savedUser != null) {
            // Показываем загрузку
            progressBar.visibility = View.VISIBLE
            btnLogin.visibility = View.GONE
            btnGuest.visibility = View.GONE
            etUsername.visibility = View.GONE

            FirebaseHelper.verifySavedUser(savedUser) { result ->
                when (result) {
                    1, 0 -> {
                        // 1 = Успех, 0 = Нет интернета (но юзер был сохранен)
                        // В обоих случаях пускаем в игру!
                        FirebaseHelper.currentUserName = savedUser // На всякий случай
                        goToMain()
                    }
                    2 -> {
                        // 2 = База ответила, что юзера нет (удалили)
                        // Только тогда стираем сохранение
                        prefs.edit().remove("username").apply()
                        Toast.makeText(this, "User deleted from DB. Please login again.", Toast.LENGTH_LONG).show()

                        // Возвращаем интерфейс
                        progressBar.visibility = View.GONE
                        btnLogin.visibility = View.VISIBLE
                        btnGuest.visibility = View.VISIBLE
                        etUsername.visibility = View.VISIBLE
                    }
                }
            }
        }

        fun startLoading() {
            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false
            btnGuest.isEnabled = false
        }

        fun stopLoading() {
            progressBar.visibility = View.GONE
            btnLogin.isEnabled = true
            btnGuest.isEnabled = true
        }

        btnLogin.setOnClickListener {
            val name = etUsername.text.toString().trim()

            if (name.length > 32) {
                Toast.makeText(this, "Nickname too long!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (name.contains(".") || name.contains("#") || name.contains("$") || name.contains("[") || name.contains("]")) {
                Toast.makeText(this, "Invalid characters!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.isNotEmpty()) {
                startLoading()
                FirebaseHelper.login(name) { success, message ->
                    if (success) {
                        prefs.edit().putString("username", FirebaseHelper.currentUserName).apply()
                        goToMain()
                    } else {
                        stopLoading()
                        Toast.makeText(this, message ?: "Error", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Enter name!", Toast.LENGTH_SHORT).show()
            }
        }

        btnGuest.setOnClickListener {
            startLoading()
            FirebaseHelper.login(null) { success, message ->
                if (success) {
                    prefs.edit().putString("username", FirebaseHelper.currentUserName).apply()
                    goToMain()
                } else {
                    stopLoading()
                    Toast.makeText(this, message ?: "Error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}