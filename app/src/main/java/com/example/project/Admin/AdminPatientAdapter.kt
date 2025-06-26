package com.example.project.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R

/**
 * An adapter for the RecyclerView that displays a list of patients in the admin panel.
 * It provides functionality for displaying patient details, filtering the list, and handling
 * interactions like editing, deleting, or viewing a patient's appointments.
 *
 * @param onEditClick A lambda function to be invoked when the edit button for a patient is clicked.
 * @param onDeleteClick A lambda function to be invoked when the delete button for a patient is clicked.
 * @param onViewAppointmentsClick A lambda function to be invoked when the view appointments button for a patient is clicked.
 */
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

    /**
     * Updates the adapter's data with a new list of patients.
     * Clears the existing list and adds all patients from the provided list.
     * Automatically triggers a filter to refresh the displayed list.
     *
     * @param patients The new list of [Patient] objects to display.
     */
    fun updatePatients(patients: List<Patient>) {
        patientList.clear()
        patientList.addAll(patients)
        filter("")
    }

    /**
     * Filters the displayed list of patients based on a search query.
     * The filter is case-insensitive and checks against the patient's first name, last name, and email.
     *
     * @param query The text to filter the patient list by. If empty, the full list is shown.
     */
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

    /**
     * Called when RecyclerView needs a new [PatientViewHolder] of the given type to represent an item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new PatientViewHolder that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_patient, parent, false)
        return PatientViewHolder(view)
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method updates the
     * contents of the [PatientViewHolder.itemView] to reflect the item at the given position.
     *
     * @param holder The PatientViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter, which is the size of the filtered list.
     */
    override fun getItemCount(): Int = filteredList.size

    /**
     * A ViewHolder describes an item view and metadata about its place within the RecyclerView.
     * It holds the UI components for a single patient item.
     */
    inner class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.patientNameTextView)
        private val emailTextView: TextView = itemView.findViewById(R.id.patientEmailTextView)
        private val dobTextView: TextView = itemView.findViewById(R.id.patientDobTextView)
        private val idTextView: TextView = itemView.findViewById(R.id.patientIdTextView)
        private val editButton: Button = itemView.findViewById(R.id.editPatientButton)
        private val deleteButton: Button = itemView.findViewById(R.id.deletePatientButton)
        private val viewAppointmentsButton: Button = itemView.findViewById(R.id.viewAppointmentsButton)

        /**
         * Binds the patient data to the views in the ViewHolder and sets up click listeners.
         *
         * @param patient The [Patient] object containing the data to display.
         */
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