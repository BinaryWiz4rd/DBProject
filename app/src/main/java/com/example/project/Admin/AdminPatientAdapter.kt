package com.example.project.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.Admin.Patient
import com.example.project.R

class AdminPatientAdapter(
    private val onEditClick: (Patient) -> Unit,
    private val onDeleteClick: (Patient) -> Unit,
    private val onViewAppointmentsClick: (Patient) -> Unit
) : RecyclerView.Adapter<AdminPatientAdapter.PatientViewHolder>() {

    private val patientList = mutableListOf<Patient>()
    private var filteredList = mutableListOf<Patient>()

    init {
        filteredList = patientList.toMutableList()
    }

    fun updatePatients(patients: List<Patient>) {
        patientList.clear()
        patientList.addAll(patients)
        filter("")
    }

    fun filter(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(patientList)
        } else {
            val lowercaseQuery = query.lowercase()
            patientList.forEach { patient ->
                if (patient.firstName.lowercase().contains(lowercaseQuery) ||
                    patient.lastName.lowercase().contains(lowercaseQuery) ||
                    patient.email.lowercase().contains(lowercaseQuery)
                ) {
                    filteredList.add(patient)
                }
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_patient, parent, false)
        return PatientViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    override fun getItemCount(): Int = filteredList.size

    inner class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.patientNameTextView)
        private val emailTextView: TextView = itemView.findViewById(R.id.patientEmailTextView)
        private val dobTextView: TextView = itemView.findViewById(R.id.patientDobTextView)
        private val idTextView: TextView = itemView.findViewById(R.id.patientIdTextView)
        private val editButton: Button = itemView.findViewById(R.id.editPatientButton)
        private val deleteButton: Button = itemView.findViewById(R.id.deletePatientButton)
        private val viewAppointmentsButton: Button = itemView.findViewById(R.id.viewAppointmentsButton)

        fun bind(patient: Patient) {
            val fullName = "${patient.firstName} ${patient.lastName}"
            nameTextView.text = fullName
            emailTextView.text = patient.email
            dobTextView.text = patient.dateOfBirth
            idTextView.text = "ID: ${patient.uid}"

            editButton.setOnClickListener { onEditClick(patient) }
            deleteButton.setOnClickListener { onDeleteClick(patient) }
            viewAppointmentsButton.setOnClickListener { onViewAppointmentsClick(patient) }
        }
    }
}