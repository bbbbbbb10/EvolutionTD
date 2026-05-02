package com.bbb.evolutiontd

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. АВТО-ВХОД (если сессия жива)
        if (FirebaseAuth.getInstance().currentUser != null) {
            FirebaseHelper.loadUserData { success ->
                if (success) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
            // Показываем пустой экран с фоном, пока идет загрузка
            setContentView(View(this).apply { setBackgroundColor(0xFF222222.toInt()) })
            return
        }

        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etLoginEmail)
        val etPass = findViewById<EditText>(R.id.etLoginPass)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoReg = findViewById<Button>(R.id.btnGoRegister)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Enter Email and Password!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    // Загружаем данные из наших 6 таблиц
                    FirebaseHelper.loadUserData { success ->
                        if (success) {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            progressBar.visibility = View.GONE
                            btnLogin.isEnabled = true
                            Toast.makeText(this, "Profile Load Error!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    Toast.makeText(this, "Wrong Email or Password!", Toast.LENGTH_SHORT).show()
                }
        }

        // Переход на экран регистрации
        btnGoReg.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}