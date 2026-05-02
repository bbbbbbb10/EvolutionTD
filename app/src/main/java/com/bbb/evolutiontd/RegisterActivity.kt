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

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName = findViewById<EditText>(R.id.etRegName)
        val etEmail = findViewById<EditText>(R.id.etRegEmail)
        val etPass = findViewById<EditText>(R.id.etRegPass)
        val btnRegister = findViewById<Button>(R.id.btnDoRegister)
        val btnBack = findViewById<Button>(R.id.btnBackToLogin)
        val progressBar = findViewById<ProgressBar>(R.id.progressBarReg)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || pass.length < 6) {
                Toast.makeText(this, "Fill all (Pass min 6)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnRegister.isEnabled = false

            // 1. Создаем аккаунт в системе Auth
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    // 2. Если Auth успех -> создаем 6 таблиц в базе, включая пароль
                    FirebaseHelper.createInitialUserData(name, email, pass) { success ->
                        if (success) {
                            Toast.makeText(this, "Registration Success!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            // Ошибка записи в саму базу данных
                            progressBar.visibility = View.GONE
                            btnRegister.isEnabled = true
                            Toast.makeText(this, "Database Error! Check internet.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    // Ошибка регистрации (например, такая почта уже занята или нет интернета)
                    progressBar.visibility = View.GONE
                    btnRegister.isEnabled = true
                    Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnBack.setOnClickListener {
            finish() // Возвращает на экран логина
        }
    }
}