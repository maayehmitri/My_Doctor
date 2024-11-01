package com.example.my_doctor2

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class PatientDetailsActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var interpreter: Interpreter

    private lateinit var patientNameTextView: TextView
    private lateinit var patientEmailTextView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var filesAdapter: UploadedFilesAdapterPatientDetails
    private lateinit var uploadedFilesList: MutableList<Pair<String, Uri>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_details)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize TensorFlow Lite Interpreter
        interpreter = Interpreter(loadModelFileFromAssets("QuantizedModel.tflite"))

        // Initialize views
        patientNameTextView = findViewById(R.id.patientNameTextView)
        patientEmailTextView = findViewById(R.id.patientEmailTextView)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        uploadedFilesList = mutableListOf() // Initialize uploadedFilesList

        // Initialize files adapter
        filesAdapter = UploadedFilesAdapterPatientDetails(uploadedFilesList) { uri ->
            // Handle opening the file here
            openFile(uri)
        }
        recyclerView.adapter = filesAdapter

        // Get patient email from intent extras
        val patientEmail = intent.getStringExtra("email") ?: ""

        // Fetch patient info and files if patient email is not null
        if (patientEmail.isNotEmpty()) {
            fetchPatientDetails(patientEmail)
        } else {
            Toast.makeText(this, "Patient email not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun fetchPatientDetails(patientEmail: String) {
        // Query Firestore to get the patient document based on the email
        firestore.collection("patients")
            .document(patientEmail) // Use the email as the document ID
            .get()
            .addOnSuccessListener { patientSnapshot ->
                val patientName = patientSnapshot.getString("name")
                val patientEmail = patientSnapshot.getString("email")

                if (patientName != null && patientEmail != null) {
                    // Update UI with patient's name and email
                    patientNameTextView.text = "Name: $patientName"
                    patientEmailTextView.text = "Email: $patientEmail"

                    // Retrieve files for the patient from the 'files' subcollection
                    firestore.collection("patients")
                        .document(patientEmail)
                        .collection("files")
                        .get()
                        .addOnSuccessListener { fileDocuments ->
                            uploadedFilesList.clear() // Clear the existing list of uploaded files

                            // Iterate through each file document and process the image
                            fileDocuments.forEach { fileDocument ->
                                val fileUrl = fileDocument.getString("fileUrl")
                                val fileName = fileDocument.getString("fileName")
                                val fileUri = Uri.parse(fileUrl)

                                Pair(fileName, fileUri)?.let { uploadedFilesList.add(it as Pair<String, Uri>) }

                                if (fileUrl != null && fileName != null && fileName.startsWith("image")) {
                                    val fileUri = Uri.parse(fileUrl)
                                    // Add the file to the uploadedFilesList

                                    // Update prediction result before processing the image
                                    updatePredictionResult(fileName, "Calculating...")
                                    // Download and process the image
                                    downloadAndProcessImage(patientEmail, fileName, fileUri)
                                }
                            }
                            // Notify the adapter of the dataset change
                            filesAdapter.notifyDataSetChanged()
                            Log.d(
                                "com.example.my_doctor2.PatientDetailsActivity",
                                "Patient files loaded: ${uploadedFilesList.size} files"
                            )
                        }
                        .addOnFailureListener { exception ->
                            // Handle failure to fetch files
                            Toast.makeText(
                                this,
                                "Error fetching patient files: ${exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e(
                                "com.example.my_doctor2.PatientDetailsActivity",
                                "Error fetching patient files:",
                                exception
                            )
                            finish()
                        }
                } else {
                    Toast.makeText(this, "Error: Patient name or email is null", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("com.example.my_doctor2.PatientDetailsActivity", "Error: Patient name or email is null")
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                // Handle failure
                Toast.makeText(
                    this,
                    "Error fetching patient info: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("com.example.my_doctor2.PatientDetailsActivity", "Error fetching patient info:", exception)
                finish()
            }
    }


    private fun downloadAndProcessImage(patientEmail: String, fileName: String, fileUri: Uri) {
        // Use Picasso to download the image
        Picasso.get().load(fileUri).into(object : com.squareup.picasso.Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                bitmap?.let { processImageFile(patientEmail, fileName, it) }
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                Log.e("com.example.my_doctor2.PatientDetailsActivity", "Failed to download image: $e")
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
        })
    }

    private fun processImageFile(patientEmail: String, fileName: String, bitmap: Bitmap) {
        try {
            // Make a new prediction
            val inputImage = preprocessImage(bitmap)
            GlobalScope.launch(Dispatchers.IO) {
                val output = runInference(inputImage)
                val result = interpretOutput(output)
                Log.d("com.example.my_doctor2.PatientDetailsActivity", "File: $fileName, Prediction: $result")
                // Save the prediction to Firestore
                savePredictionToFirestore(patientEmail, fileName, result)
                // Update the predictionTextView with the result
                updatePredictionResult(fileName, result)
            }
        } catch (e: Exception) {
            Log.e("com.example.my_doctor2.PatientDetailsActivity", "Error processing image: $e")
        }
    }


    private suspend fun getSavedPredictionFromFirestore(fileName: String): String? {
        val patientEmail = intent.getStringExtra("email") ?: return null
        val filePath = "patients/$patientEmail/files/$fileName"

        // Query Firestore to get the prediction for this file
        val fileDocRef = firestore.document(filePath)
        return try {
            val snapshot = fileDocRef.get().await()
            snapshot.getString("prediction")
        } catch (e: Exception) {
            Log.e("com.example.my_doctor2.PatientDetailsActivity", "Error getting prediction from Firestore: $e")
            null
        }
    }

    private fun updatePredictionResult(fileName: String, prediction: String) {
        for (i in 0 until recyclerView.childCount) {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(i)
            if (viewHolder is UploadedFilesAdapterPatientDetails.ViewHolder) {
                val filePair = uploadedFilesList[i]
                if (filePair.first == fileName) {
                    viewHolder.predictionTextView.text = "Prediction: $prediction"
                    break
                }
            }
        }
    }

    private fun savePredictionToFirestore(patientEmail: String, fileName: String, prediction: String) {
        val predictionDocRef = firestore.collection("predictions")
            .document(patientEmail)
            .collection("files")
            .document(fileName)

        val data = hashMapOf("prediction" to prediction)

        predictionDocRef.set(data)
            .addOnSuccessListener {
                Log.d("com.example.my_doctor2.PatientDetailsActivity", "Prediction saved to Firestore: $prediction")
            }
            .addOnFailureListener { e ->
                Log.e("com.example.my_doctor2.PatientDetailsActivity", "Error saving prediction to Firestore", e)
            }
    }

    private fun openFile(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = uri
        startActivity(intent)
    }

    private fun loadModelFileFromAssets(modelPath: String): MappedByteBuffer {
        val assetFileDescriptor = assets.openFd(modelPath)
        val fileDescriptor = assetFileDescriptor.fileDescriptor
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val fileChannel = FileInputStream(fileDescriptor).channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun preprocessImage(bitmap: Bitmap): TensorImage {
        val inputShape = interpreter.getInputTensor(0).shape()
        val inputImageBuffer = TensorImage(DataType.FLOAT32)

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputShape[2], inputShape[1], true)

        inputImageBuffer.load(resizedBitmap)

        return inputImageBuffer
    }

    private fun runInference(inputImage: TensorImage): FloatArray {
        val outputShape = interpreter.getOutputTensor(0).shape()
        val outputBuffer = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32)
        interpreter.run(inputImage.buffer, outputBuffer.buffer)
        return outputBuffer.floatArray
    }

    private fun interpretOutput(output: FloatArray): String {

        val labels = listOf("COVID", "Lung Opacity", "Normal", "Pneumonia", "Tuberculosis")
        val maxIndex = output.indices.maxByOrNull { output[it] } ?: -1
        return if (maxIndex != -1) labels[maxIndex] else "Unknown"
    }

    class UploadedFilesAdapterPatientDetails(
        private val files: List<Pair<String, Uri>>,
        private val onOpenClickListener: (Uri) -> Unit
    ) :
        RecyclerView.Adapter<UploadedFilesAdapterPatientDetails.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val fileNameTextView: TextView = itemView.findViewById(R.id.fileNameTextView)
            val openButton: Button = itemView.findViewById(R.id.openButton)
            val predictionTextView: TextView = itemView.findViewById(R.id.predictionTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_uploaded_filee, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (fileName, uri) = files[position]
            holder.fileNameTextView.text = fileName

            // Hide predictionTextView if file name starts with "document"
            if (fileName.startsWith("document")) {
                holder.predictionTextView.visibility = View.GONE
            } else {
                holder.predictionTextView.visibility = View.VISIBLE
            }

            // Set click listener for the "Open" button
            holder.openButton.setOnClickListener {
                // Call the onOpenClickListener with the URI when the button is clicked
                onOpenClickListener.invoke(uri)
            }
        }


        override fun getItemCount(): Int = files.size
    }
}
