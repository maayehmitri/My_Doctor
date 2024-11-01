package com.example.my_doctor2

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ViewDocumentsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_documents)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val webView: WebView = findViewById(R.id.webView)

        // Retrieve current patient's email
        val patientEmail = auth.currentUser?.email ?: ""

        // Retrieve document URL for the patient
        retrieveDocumentUrl(patientEmail) { documentUrl ->
            // Load the document URL in the WebView
            webView.loadUrl("https://docs.google.com/viewer?url=$documentUrl")
        }
    }

    private fun retrieveDocumentUrl(patientEmail: String, callback: (String) -> Unit) {
        val documentsCollection = firestore.collection("documents")
        val documentRef = documentsCollection.document(patientEmail)

        documentRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val documentUrl = document.getString("documentUrl")
                    if (!documentUrl.isNullOrEmpty()) {
                        callback.invoke(documentUrl)
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Handle failure to retrieve document URL
            }
    }
}
