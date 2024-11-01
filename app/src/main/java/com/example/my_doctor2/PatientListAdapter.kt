package com.example.my_doctor2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PatientListAdapter(private val listener: PatientClickListener) : RecyclerView.Adapter<PatientListAdapter.PatientViewHolder>() {

    private val patientList = mutableListOf<DoctorDashboardActivity.Patient>()

    fun updateList(newList: List<DoctorDashboardActivity.Patient>) {
        patientList.clear()
        patientList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_patient, parent, false)
        return PatientViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val patient = patientList[position]
        holder.bind(patient)
    }

    override fun getItemCount(): Int {
        return patientList.size
    }

    inner class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.patientNameTextView)
        private val emailTextView: TextView = itemView.findViewById(R.id.patientEmailTextView)
        private val removeButton: Button = itemView.findViewById(R.id.removeButton)
        private val viewInfoButton: Button = itemView.findViewById(R.id.viewInfoButton)


        fun bind(patient: DoctorDashboardActivity.Patient) {
            nameTextView.text = patient.name
            emailTextView.text = patient.email

            removeButton.setOnClickListener {
                listener.onRemovePatient(patient)
            }

            viewInfoButton.setOnClickListener {
                listener.onViewInfo(patient)
            }
        }
    }

    interface PatientClickListener {
        fun onRemovePatient(patient: DoctorDashboardActivity.Patient)
        fun onViewInfo(patient: DoctorDashboardActivity.Patient)
        fun onRemove(patient: DoctorDashboardActivity.Patient)
    }
}
