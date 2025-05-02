package com.example.project.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R

class PatientAdapter(private val patientList: List<Patient>) :
    RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {

    class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emailTextView: TextView = itemView.findViewById(R.id.tvPatientEmail)
        val nameTextView: TextView = itemView.findViewById(R.id.tvPatientName)
        val dobTextView: TextView = itemView.findViewById(R.id.tvPatientDOB)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient, parent, false)
        return PatientViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val currentPatient = patientList[position]
        holder.emailTextView.text = currentPatient.email
        holder.nameTextView.text = "${currentPatient.firstName} ${currentPatient.lastName}"
        holder.dobTextView.text = currentPatient.dateOfBirth
    }

    override fun getItemCount() = patientList.size
}