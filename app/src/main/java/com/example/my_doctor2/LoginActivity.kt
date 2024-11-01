package com.example.my_doctor2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signUpRedirectButton = findViewById<Button>(R.id.signUpRedirectButton)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            // Call the login function to authenticate the user.
            login(email, password)
        }

        signUpRedirectButton.setOnClickListener {
            // Navigate to the Sign Up Activity.
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid

                    // Fetch the user's role from Firestore
                    if (userId != null) {
                        db.collection("users").document(email)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val userRole = document.getString("role")
                                    val intent = if (userRole == "Doctor") {
                                        Intent(this, DoctorDashboardActivity::class.java)
                                    } else {
                                        Intent(this, PatientDashboardActivity::class.java)
                                    }
                                    startActivity(intent)
                                    finish() // Optional: Finish the login activity to prevent going back with the back button.
                                } else {
                                    // Handle the case where the user's document doesn't exist in Firestore.
                                    // You can add an error message here.
                                }
                            }
                            .addOnFailureListener { exception ->
                                // Handle the case where role check fails.
                                val errorMessage = exception.message ?: "Login failed"
                                // You can display this error message to the user.
                            }
                    } else {
                        // Handle the case where the user ID is null.
                    }
                } else {
                    // Login failed, handle the error, e.g., display an error message to the user.
                    val errorMessage = task.exception?.message ?: "Login failed"
                    // You can display this error message to the user.
                }
            }
    }
}
