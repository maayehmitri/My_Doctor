package com.example.my_doctor2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException

class PatientDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var recyclerView: RecyclerView
    private lateinit var filesAdapter: UploadedFilesAdapter

    private val uploadedFilesList = mutableListOf<UploadedFile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_dashboard)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize views
        val btnSignOut: Button = findViewById(R.id.btnSignOut)
        val btnUploadDocument: Button = findViewById(R.id.btnUploadDocument)
        val btnUploadImage: Button = findViewById(R.id.btnUploadImage)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        filesAdapter = UploadedFilesAdapter(uploadedFilesList,
            onItemClick = { file -> openFile(file) },
            onRemoveClick = { file -> removeFile(file) }
        )
        recyclerView.adapter = filesAdapter

        // Button click listener to sign out
        btnSignOut.setOnClickListener {
            signOut()
        }

        // Button click listener to upload a document
        btnUploadDocument.setOnClickListener {
            openDocumentPicker.launch("application/pdf")
        }

        // Button click listener to upload an image
        btnUploadImage.setOnClickListener {
            openImagePicker.launch("image/*")
        }

        // Retrieve and display uploaded files
        retrieveUploadedFiles()
    }

    private val openDocumentPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uploadFile(it)
        }
    }

    private val openImagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uploadFile(it)
        }
    }

    private fun uploadFile(uri: Uri) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val storageRef = storage.reference
            val fileName = generateFileName(uri)
            val fileRef = storageRef.child("${currentUser.email}/$fileName")

            fileRef.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    // File uploaded successfully
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                        val uploadedFile = UploadedFile(fileName, downloadUri.toString())
                        currentUser.email?.let {
                            firestore.collection("patients").document(currentUser.email!!)
                                .collection("files").add(uploadedFile)
                                .addOnSuccessListener {
                                    uploadedFilesList.add(uploadedFile)
                                    filesAdapter.notifyDataSetChanged()
                                }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle error
                    Log.e(TAG, "Error uploading file", exception)
                }
        }
    }

    private fun generateFileName(uri: Uri): String {
        val fileName = uri.lastPathSegment ?: "File"
        val mimeType = contentResolver.getType(uri)
        val fileType = when {
            mimeType?.startsWith("image") == true -> "image"
            mimeType?.startsWith("application/pdf") == true -> "document"
            else -> "file"
        }
        val randomNumber = (1000..9999).random() // Generate random 4-digit number
        return "$fileType$randomNumber"
    }

    private fun retrieveUploadedFiles() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val filesCollection = firestore.collection("patients").document(currentUser.email!!)
                .collection("files")

            filesCollection.get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val fileName = document.getString("fileName") ?: "File"
                        val fileUrl = document.getString("fileUrl") ?: ""
                        val uploadedFile = UploadedFile(fileName, fileUrl)
                        uploadedFilesList.add(uploadedFile)
                    }
                    filesAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    // Handle failure
                    Log.e(TAG, "Error retrieving uploaded files", exception)
                }
        }
    }

    private fun openFile(file: UploadedFile) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(file.fileUrl)
        startActivity(intent)
    }

    private fun removeFile(file: UploadedFile) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val storageRef = storage.reference
            val fileRef = storageRef.child("${currentUser.email}/${file.fileName}")

            // Check if the file exists before attempting to delete it
            fileRef.metadata.addOnSuccessListener { metadata ->
                if (metadata != null && metadata.sizeBytes > 0) {
                    // File exists, proceed with deletion
                    fileRef.delete()
                        .addOnSuccessListener {
                            // File deleted successfully from Firebase Storage
                            Log.d(TAG, "File deleted from Firebase Storage")

                            val filesCollection = firestore.collection("patients").document(currentUser.email!!)
                                .collection("files")

                            filesCollection.whereEqualTo("fileName", file.fileName)
                                .get()
                                .addOnSuccessListener { documents ->
                                    for (document in documents) {
                                        document.reference.delete()
                                            .addOnSuccessListener {
                                                // Document deleted successfully from Firestore
                                                Log.d(TAG, "Document deleted from Firestore")
                                                uploadedFilesList.remove(file)
                                                filesAdapter.notifyDataSetChanged()
                                            }
                                            .addOnFailureListener { exception ->
                                                Log.e(TAG, "Error deleting document from Firestore", exception)
                                            }
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.e(TAG, "Error getting documents", exception)
                                }
                        }
                        .addOnFailureListener { exception ->
                            // Handle Firebase Storage delete failure
                            Log.e(TAG, "Error deleting file from Firebase Storage", exception)
                        }
                } else {
                    // File does not exist
                    Log.e(TAG, "File does not exist at the specified location")
                    // You can handle this scenario here, such as displaying a message to the user
                }
            }
        } else {
            Log.e(TAG, "Current user is null")
        }
    }




    private fun signOut() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    companion object {
        const val TAG = "PatientDashboardActivity"
    }
}


class UploadedFilesAdapter(
    private val files: List<UploadedFile>,
    private val onItemClick: (UploadedFile) -> Unit,
    private val onRemoveClick: (UploadedFile) -> Unit
) : RecyclerView.Adapter<UploadedFilesAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileNameTextView: TextView = itemView.findViewById(R.id.fileNameTextView)
        val removeButton: Button = itemView.findViewById(R.id.removeButton)
        val openButton: Button = itemView.findViewById(R.id.openButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_uploaded_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        holder.fileNameTextView.text = file.fileName

        holder.removeButton.setOnClickListener {
            onRemoveClick(file)
        }

        holder.openButton.setOnClickListener {
            onItemClick(file)
        }
    }

    override fun getItemCount(): Int = files.size
}

data class UploadedFile(
    val fileName: String,
    val fileUrl: String
)
