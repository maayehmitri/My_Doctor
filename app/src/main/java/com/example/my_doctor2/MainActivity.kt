package com.example.my_doctor2

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Navigate to the login page (replace com.example.my_doctor2.com.example.my_doctor2.com.example.my_doctor2.LoginActivity::class.java with your login page).
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Optional: Finish the main activity.
    }
}
