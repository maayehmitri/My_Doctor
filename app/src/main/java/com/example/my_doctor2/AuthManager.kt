package com.example.my_doctor2

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser

class AuthManager {
    private val auth = FirebaseAuth.getInstance()

    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = auth.currentUser
                    callback(true, null)
                } else {
                    val exception = task.exception as? FirebaseAuthException
                    if (exception != null) {
                        val errorCode = exception.errorCode
                        val errorMessage = when (errorCode) {
                            "ERROR_INVALID_EMAIL" -> "Invalid email"
                            "ERROR_WRONG_PASSWORD" -> "Wrong password"
                            "ERROR_USER_NOT_FOUND" -> "User not found"
                            else -> "Login failed"
                        }
                        callback(false, errorMessage)
                    } else {
                        callback(false, "Login failed")
                    }
                }
            }
    }

    fun signUp(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    callback(true, null)
                } else {
                    val exception = task.exception as? FirebaseAuthException
                    if (exception != null) {
                        val errorCode = exception.errorCode
                        val errorMessage = when (errorCode) {
                            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email already in use"
                            "ERROR_WEAK_PASSWORD" -> "Weak password"
                            else -> "Sign-up failed"
                        }
                        callback(false, errorMessage)
                    } else {
                        callback(false, "Sign-up failed")
                    }
                }
            }
    }


    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
}
