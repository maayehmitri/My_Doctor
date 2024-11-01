package com.example.my_doctor2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


   class DoctorDashboardActivity : AppCompatActivity(), PatientListAdapter.PatientClickListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var patientRecyclerView: RecyclerView
    private lateinit var btnAddPatient: Button
    private lateinit var btnSignOut: Button
    private lateinit var etPatientEmail: EditText
    private lateinit var doctorId: String
    private lateinit var adapter: PatientListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_dashboard)

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Get the doctor's unique ID (you can get it from Firebase Auth)
        doctorId = auth.currentUser?.email ?: ""

        // Initialize views
        patientRecyclerView = findViewById(R.id.patientRecyclerView)
        btnAddPatient = findViewById(R.id.btnAddPatient)
        btnSignOut = findViewById(R.id.btnSignOut)
        etPatientEmail = findViewById(R.id.etPatientEmail)

        // Set up RecyclerView
        patientRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PatientListAdapter(this)
        patientRecyclerView.adapter = adapter

        // Fetch and display list of patients
        fetchPatients()

        // Set click listener for adding a patient
        btnAddPatient.setOnClickListener {
            addPatient()
        }

        // Set click listener for signing out
        btnSignOut.setOnClickListener {
            // Sign out the current user
            auth.signOut()
            // Navigate to the login screen or any desired activity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun fetchPatients() {
        // Fetch list of patients associated with the doctor from Firestore
        firestore.collection("doctors").document(doctorId).collection("patients")
            .get()
            .addOnSuccessListener { result ->
                val patientList = mutableListOf<Patient>()
                for (document in result) {
                    val patientName = document.getString("name")
                    val patientEmail = document.getString("email")
                    if (patientName != null && patientEmail != null) {
                        val patient = Patient(patientName, patientEmail)
                        patientList.add(patient)
                    }
                }
                // Update the patient list in the adapter
                adapter.updateList(patientList)
            }
            .addOnFailureListener { exception ->
                // Handle any errors
                Log.d(TAG, "Error getting documents: ", exception)
                Toast.makeText(this, "Error fetching patients", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addPatient() {
        val patientEmail = etPatientEmail.text.toString().trim() // Get patient email from EditText

        if (patientEmail.isNotEmpty()) {
            // Check if the patient exists in the patients collection
            firestore.collection("patients").document(patientEmail)
                .get()
                .addOnSuccessListener { result ->
                    if (result.exists()) {
                        // Patient exists, check if the patient is already added to the doctor's list
                        firestore.collection("doctors").document(doctorId)
                            .collection("patients").document(patientEmail)
                            .get()
                            .addOnSuccessListener { doctorResult ->
                                if (doctorResult.exists()) {
                                    // Patient already exists in the doctor's list
                                    Toast.makeText(this, "Patient already exists in your list", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Patient not already added to the doctor's list, add them
                                    val patientData = hashMapOf<String, Any>()
                                    patientData["name"] = result.getString("name") ?: ""
                                    patientData["email"] = result.getString("email")?:""

                                    // Add the patient to the doctor's list
                                    firestore.collection("doctors").document(doctorId)
                                        .collection("patients").document(patientEmail)
                                        .set(patientData)
                                        .addOnSuccessListener {
                                            // Patient added successfully to the doctor's list
                                            Toast.makeText(this, "Patient added successfully", Toast.LENGTH_SHORT).show()
                                            fetchPatients() // Update the patient list
                                            etPatientEmail.text.clear() // Clear the EditText
                                        }
                                        .addOnFailureListener { e ->
                                            // Error adding patient to the doctor's list
                                            Log.w(TAG, "Error adding patient", e)
                                            Toast.makeText(this, "Error adding patient", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                    } else {
                        // Patient with the provided email does not exist
                        Toast.makeText(this, "Patient with provided email does not exist", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle any errors
                    Log.d(TAG, "Error getting documents: ", exception)
                    Toast.makeText(this, "Error adding patient", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Patient email field is empty
            Toast.makeText(this, "Please enter patient's email", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onRemovePatient(patient: Patient) {
        // Remove the patient from the doctor's list
        firestore.collection("doctors").document(doctorId)
            .collection("patients").document(patient.email)
            .delete()
            .addOnSuccessListener {
                // Patient removed successfully
                Toast.makeText(this, "Patient removed successfully", Toast.LENGTH_SHORT).show()
                fetchPatients() // Update the patient list
            }
            .addOnFailureListener { e ->
                // Error removing patient
                Log.w(TAG, "Error removing patient", e)
                Toast.makeText(this, "Error removing patient", Toast.LENGTH_SHORT).show()
            }
    }



       override fun onViewInfo(patient: Patient) {
           // Start the com.example.my_doctor2.com.example.my_doctor2.com.example.my_doctor2.com.example.my_doctor2.com.example.my_doctor2.com.example.my_doctor2.PatientDetailsActivity and pass the patient's email
           val intent = Intent(this, PatientDetailsActivity::class.java)
           intent.putExtra("email", patient.email)
           startActivity(intent)
       }



       // Your existing code...

           override fun onRemove(patient: DoctorDashboardActivity.Patient) {
               // Remove the patient from the doctor's list
               firestore.collection("doctors").document(doctorId)
                   .collection("patients").document(patient.email)
                   .delete()
                   .addOnSuccessListener {
                       // Patient removed successfully
                       Toast.makeText(this, "Patient removed successfully", Toast.LENGTH_SHORT).show()
                       fetchPatients() // Update the patient list
                   }
                   .addOnFailureListener { e ->
                       // Error removing patient
                       Log.w(TAG, "Error removing patient", e)
                       Toast.makeText(this, "Error removing patient", Toast.LENGTH_SHORT).show()
                   }
           }

           // Your existing code...




       data class Patient(val name: String, val email: String)

    companion object {
        private const val TAG = "com.example.my_doctor2.DoctorDashboardActivity"
    }
}
