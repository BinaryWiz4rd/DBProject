package com.example.project.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R

/**
 * RecyclerView adapter for displaying a list of [Doctor] objects.
 * Handles the binding of doctor data to individual list items and provides
 * callbacks for approval and rejection actions.
 *
 * @param doctors The initial list of [Doctor] objects to display.
 * @param onApproveReject A lambda function invoked when the approve or reject button is clicked.
 * It takes the doctor's UID, the permission field name (e.g., "add"), and a boolean indicating
 * approval (true) or rejection (false).
 */
class DoctorAdapter(
    private var doctors: List<Doctor>,
    private val onApproveReject: (String, String, Boolean) -> Unit
) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

    /**
     * ViewHolder for individual doctor list items.
     *
     * @param itemView The view for a single list item.
     */
    class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewName: TextView = itemView.findViewById(R.id.textViewName)
        val textViewEmail: TextView = itemView.findViewById(R.id.textViewEmail)
        val approveButton: Button = itemView.findViewById(R.id.buttonApprove)
        val rejectButton: Button = itemView.findViewById(R.id.buttonReject)

        /**
         * Binds a [Doctor] object to the ViewHolder's views.
         * Sets the doctor's name and email, and conditionally shows/hides
         * the approve/reject buttons based on the doctor's 'add' permission status.
         * Sets click listeners for the approve and reject buttons.
         *
         * @param doctor The [Doctor] object to bind.
         * @param onApproveReject The lambda function to invoke on approve/reject button clicks.
         */
        fun bind(doctor: Doctor, onApproveReject: (String, String, Boolean) -> Unit) {
            textViewName.text = "${doctor.firstName} ${doctor.lastName}"
            textViewEmail.text = doctor.email

            if (doctor.add == true) {
                approveButton.visibility = View.VISIBLE
                rejectButton.visibility = View.VISIBLE
            } else {
                approveButton.visibility = View.GONE
                rejectButton.visibility = View.GONE
            }

            approveButton.setOnClickListener {
                onApproveReject(doctor.uid, "add", true)
            }
            rejectButton.setOnClickListener {
                onApproveReject(doctor.uid, "add", false)
            }
        }
    }

    /**
     * Called when RecyclerView needs a new [DoctorViewHolder] of the given type to represent an item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     * @return A new [DoctorViewHolder] that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor, parent, false)
        return DoctorViewHolder(itemView)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method updates the contents of the [holder] to reflect the item at the given [position].
     *
     * @param holder The [DoctorViewHolder] which should be updated to represent the contents of the
     * item at the given [position] in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        holder.bind(doctors[position], onApproveReject)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int {
        return doctors.size
    }

    /**
     * Updates the list of doctors and notifies the RecyclerView to refresh its views.
     *
     * @param newDoctors The new list of [Doctor] objects to display.
     */
    fun updateList(newDoctors: List<Doctor>) {
        doctors = newDoctors
        notifyDataSetChanged()
    }
}