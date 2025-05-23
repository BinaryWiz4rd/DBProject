package com.example.project.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.doctor.model.Doctor
import com.example.project.R
import java.util.*

class AdminDoctorAdapter(
    private val onEditClick: (Doctor) -> Unit,
    private val onDeleteClick: (Doctor) -> Unit,
    private val onViewAppointmentsClick: (Doctor) -> Unit,
    private val onViewServicesClick: (Doctor) -> Unit
) : RecyclerView.Adapter<AdminDoctorAdapter.DoctorViewHolder>() {

    private var doctors = listOf<Doctor>()
    private var filteredDoctors = listOf<Doctor>()

    inner class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.doctorNameTextView)
        private val specializationTextView: TextView = itemView.findViewById(R.id.doctorSpecializationTextView)
        private val contactTextView: TextView = itemView.findViewById(R.id.doctorContactTextView)
        private val editButton: Button = itemView.findViewById(R.id.editDoctorButton)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteDoctorButton)
        private val viewAppointmentsButton: Button = itemView.findViewById(R.id.viewAppointmentsButton)
        private val viewServicesButton: Button = itemView.findViewById(R.id.viewServicesButton)

        fun bind(doctor: Doctor) {
            nameTextView.text = "Dr. ${doctor.firstName} ${doctor.lastName}"
            specializationTextView.text = doctor.specialization
            contactTextView.text = "${doctor.email}\n${doctor.phoneNumber}"

            editButton.setOnClickListener { onEditClick(doctor) }
            deleteButton.setOnClickListener { onDeleteClick(doctor) }
            viewAppointmentsButton.setOnClickListener { onViewAppointmentsClick(doctor) }
            viewServicesButton.setOnClickListener { onViewServicesClick(doctor) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_doctor, parent, false)
        return DoctorViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        holder.bind(filteredDoctors[position])
    }

    override fun getItemCount(): Int = filteredDoctors.size

    fun updateDoctors(newDoctors: List<Doctor>) {
        doctors = newDoctors
        filteredDoctors = newDoctors
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredDoctors = if (query.isEmpty()) {
            doctors
        } else {
            val lowercaseQuery = query.lowercase(Locale.getDefault())
            doctors.filter { doctor ->
                doctor.firstName.lowercase(Locale.getDefault()).contains(lowercaseQuery) ||
                doctor.lastName.lowercase(Locale.getDefault()).contains(lowercaseQuery) ||
                doctor.specialization.lowercase(Locale.getDefault()).contains(lowercaseQuery) ||
                doctor.email.lowercase(Locale.getDefault()).contains(lowercaseQuery)
            }
        }
        notifyDataSetChanged()
    }
}