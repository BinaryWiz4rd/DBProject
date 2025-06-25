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

/**
 * An adapter for displaying a list of doctors in the admin panel.
 * Handles displaying doctor information and provides callbacks for actions
 * such as editing, deleting, and viewing appointments or services.
 *
 * @property onEditClick Callback for when the edit button is clicked.
 * @property onDeleteClick Callback for when the delete button is clicked.
 * @property onViewAppointmentsClick Callback for when the "View Appointments" button is clicked.
 * @property onViewServicesClick Callback for when the "View Services" button is clicked.
 */
class AdminDoctorAdapter(
    private val onEditClick: (Doctor) -> Unit,
    private val onDeleteClick: (Doctor) -> Unit,
    private val onViewAppointmentsClick: (Doctor) -> Unit,
    private val onViewServicesClick: (Doctor) -> Unit
) : RecyclerView.Adapter<AdminDoctorAdapter.DoctorViewHolder>() {

    private var doctors = listOf<Doctor>()
    private var filteredDoctors = listOf<Doctor>()

    /**
     * ViewHolder for a single doctor item in the RecyclerView.
     * @param itemView The view for the list item.
     */
    inner class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.doctorNameTextView)
        private val specializationTextView: TextView = itemView.findViewById(R.id.doctorSpecializationTextView)
        private val contactTextView: TextView = itemView.findViewById(R.id.doctorContactTextView)
        private val editButton: Button = itemView.findViewById(R.id.editDoctorButton)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteDoctorButton)
        private val viewAppointmentsButton: Button = itemView.findViewById(R.id.viewAppointmentsButton)
        private val viewServicesButton: Button = itemView.findViewById(R.id.viewServicesButton)

        /**
         * Binds a doctor's data to the views in the ViewHolder.
         * @param doctor The doctor to bind.
         */
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

    /**
     * Creates a new [DoctorViewHolder] for a list item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_doctor, parent, false)
        return DoctorViewHolder(view)
    }

    /**
     * Binds the data at a given position to the [DoctorViewHolder].
     */
    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        holder.bind(filteredDoctors[position])
    }

    /**
     * Returns the number of items in the filtered list.
     */
    override fun getItemCount(): Int = filteredDoctors.size

    /**
     * Updates the list of doctors with a new list.
     * @param newDoctors The new list of doctors.
     */
    fun updateDoctors(newDoctors: List<Doctor>) {
        doctors = newDoctors
        filteredDoctors = newDoctors
        notifyDataSetChanged()
    }

    /**
     * Filters the list of doctors based on a query string.
     * The filter is case-insensitive and checks the first name, last name, specialization, and email.
     * @param query The string to filter by.
     */
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