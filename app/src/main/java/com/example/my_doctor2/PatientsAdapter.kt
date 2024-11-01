package com.example.my_doctor2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PatientsAdapter : RecyclerView.Adapter<PatientsAdapter.PatientViewHolder>() {

    private var patientsList: List<String> = listOf()

    // Function to update the patient list in the adapter
    fun submitPatients(patients: List<String>) {
        this.patientsList = patients
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.patient_item, parent, false)
        return PatientViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val patientId = patientsList[position]
        holder.bind(patientId)
    }

    override fun getItemCount(): Int {
        return patientsList.size
    }

    // ViewHolder class
    class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val patientTextView: TextView = itemView.findViewById(R.id.patientNameTextView)

        fun bind(patientId: String) {
            patientTextView.text = patientId
        }
    }
}
