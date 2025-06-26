package com.example.project.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R

/**
 * RecyclerView adapter for displaying a list of [Patient] objects.
 *
 * This adapter manages the display of patient data in a RecyclerView, including their name and email.
 * It also provides functionality for approving or rejecting patient additions, controlled by the
 * [onApproveReject] lambda.
 *
 * @param patientList The initial list of [Patient] objects to display.
 * @param onApproveReject A lambda function invoked when an approve or reject button is clicked.
 * It takes the patient's UID (String), the type of action (String, e.g., "add"), and a boolean
 * indicating approval (true) or rejection (false).
 */
class PatientAdapter(
    private var patientList: List<Patient>,
    private val onApproveReject: (String, String, Boolean) -> Unit
) : RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {

    /**
     * ViewHolder for individual patient items in the RecyclerView.
     *
     * @param itemView The View for a single patient list item.
     */
    class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewName: TextView = itemView.findViewById(R.id.textViewName)
        val textViewEmail: TextView = itemView.findViewById(R.id.textViewEmail)
        val approveButton: Button? = itemView.findViewById(R.id.buttonApprove)
        val rejectButton: Button? = itemView.findViewById(R.id.buttonReject)

        /**
         * Binds a [Patient] object to the ViewHolder's views.
         *
         * Sets the patient's name and email, and conditionally shows/hides approve/reject buttons
         * based on the [Patient.add] property. Sets click listeners for the approve and reject buttons.
         *
         * @param patient The [Patient] object to bind.
         * @param onApproveReject The lambda function to call when approve or reject is clicked.
         */
        fun bind(patient: Patient, onApproveReject: (String, String, Boolean) -> Unit) {
            textViewName.text = "${patient.firstName} ${patient.lastName}"
            textViewEmail.text = patient.email

            if (patient.add == true) {
                approveButton?.visibility = View.VISIBLE
                rejectButton?.visibility = View.VISIBLE
            } else {
                approveButton?.visibility = View.GONE
                rejectButton?.visibility = View.GONE
            }

            approveButton?.setOnClickListener {
                onApproveReject(patient.uid, "add", true)
            }
            rejectButton?.setOnClickListener {
                onApproveReject(patient.uid, "add", false)
            }
        }
    }

    /**
     * Called when RecyclerView needs a new [PatientViewHolder] of the given type to represent
     * an item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     * @return A new [PatientViewHolder] that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient, parent, false)
        return PatientViewHolder(itemView)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * This method updates the contents of the [itemView] to reflect the item at the given
     * position.
     *
     * @param holder The [PatientViewHolder] which should be updated to represent the contents
     * of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(patientList[position], onApproveReject)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int {
        return patientList.size
    }

    /**
     * Updates the list of patients displayed by the adapter and notifies the RecyclerView
     * to refresh its views.
     *
     * @param newPatientList The new list of [Patient] objects to display.
     */
    fun updateList(newPatientList: List<Patient>) {
        patientList = newPatientList
        notifyDataSetChanged()
    }
}