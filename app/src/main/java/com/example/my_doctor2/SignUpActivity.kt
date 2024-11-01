package com.example.my_doctor2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val signUpButton = findViewById<Button>(R.id.signUpButton)
        val doctorRadioButton = findViewById<RadioButton>(R.id.doctorRadioButton)
        val patientRadioButton = findViewById<RadioButton>(R.id.patientRadioButton)

        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val name = nameEditText.text.toString()
            val isDoctor = doctorRadioButton.isChecked

            signUp(email, password, name, isDoctor)
        }
    }

    private fun signUp(email: String, password: String, name: String, isDoctor: Boolean) {
        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Email, password, and name must not be empty", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userID = user?.uid

                    val userRef = db.collection("users").document(email)

                    val userData = hashMapOf(
                        "email" to email,
                        "name" to name,
                        "role" to if (isDoctor) "Doctor" else "Patient"
                        // Add other user information fields as needed.
                    )

                    userRef.set(userData)
                        .addOnSuccessListener {
                            if (isDoctor) {
                                val doctorData = hashMapOf(
                                    "email" to email,
                                    "name" to name
                                    // Add other doctor information fields as needed.
                                )
                                db.collection("doctors").document(email).set(doctorData)
                                val intent = Intent(this, DoctorDashboardActivity::class.java)
                                startActivity(intent)
                            } else {
                                val patientData = hashMapOf(
                                    "email" to email,
                                    "name" to name
                                    // Add other patient information fields as needed.
                                )
                                db.collection("patients").document(email).set(patientData)
                                val intent = Intent(this, PatientDashboardActivity::class.java)
                                startActivity(intent)
                            }
                            finish()
                        }
                        .addOnFailureListener { e ->
                            val errorMessage = e.message ?: "Sign-up failed"
                            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                } else {
                    val errorMessage = task.exception?.message ?: "Sign-up failed"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

}