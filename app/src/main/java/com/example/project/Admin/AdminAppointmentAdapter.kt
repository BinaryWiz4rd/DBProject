package com.example.project.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import java.util.Locale

// Comprehensive data class for admin appointments management
/**
 * Represents a single appointment item in the admin view.
 *
 * @property id The unique ID of the appointment.
 * @property doctorId The ID of the doctor.
 * @property doctorName The name of the doctor.
 * @property patientId The ID of the patient.
 * @property patientName The name of the patient.
 * @property serviceId The ID of the service.
 * @property serviceName The name of the service.
 * @property servicePrice The price of the service.
 * @property serviceDuration The duration of the service in minutes.
 * @property date The date of the appointment.
 * @property startTime The start time of the appointment.
 * @property endTime The end time of the appointment.
 * @property notes Any notes for the appointment.
 * @property status The status of the appointment (e.g., "confirmed", "cancelled").
 */
data class AdminAppointmentItem(
    var id: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    val servicePrice: Int = 0,
    val serviceDuration: Int = 0,
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val notes: String = "",
    val status: String = "confirmed"
)

/**
 * Adapter for displaying a list of appointments in the admin section.
 *
 * @property onEditClick A lambda function to be invoked when the edit button is clicked.
 * @property onDeleteClick A lambda function to be invoked when the delete button is clicked.
 */
class AdminAppointmentAdapter(
    private val onEditClick: (AdminAppointmentItem) -> Unit,
    private val onDeleteClick: (AdminAppointmentItem) -> Unit
) : RecyclerView.Adapter<AdminAppointmentAdapter.AppointmentViewHolder>() {

    private val appointmentList = mutableListOf<AdminAppointmentItem>()

    /**
     * Updates the list of appointments and notifies the adapter of the data change.
     *
     * @param appointments The new list of appointments.
     */
    fun updateAppointments(appointments: List<AdminAppointmentItem>) {
        appointmentList.clear()
        appointmentList.addAll(appointments)
        notifyDataSetChanged()
    }

    /**
     * Called when RecyclerView needs a new [AppointmentViewHolder] of the given type to represent an item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new AppointmentViewHolder that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(appointmentList[position])
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int = appointmentList.size

    /**
     * ViewHolder for an appointment item.
     *
     * @param itemView The view for the appointment item.
     */
    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.appointmentDateTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.appointmentTimeTextView)
        private val patientTextView: TextView = itemView.findViewById(R.id.patientNameTextView)
        private val doctorTextView: TextView = itemView.findViewById(R.id.doctorNameTextView)
        private val serviceTextView: TextView = itemView.findViewById(R.id.serviceNameTextView)
        private val priceTextView: TextView = itemView.findViewById(R.id.servicePriceTextView)
        private val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        private val editButton: Button = itemView.findViewById(R.id.editAppointmentButton)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteAppointmentButton)

        /**
         * Binds the appointment data to the views in the ViewHolder.
         *
         * @param appointment The appointment item to bind.
         */
        fun bind(appointment: AdminAppointmentItem) {
            dateTextView.text = appointment.date
            timeTextView.text = "${appointment.startTime} - ${appointment.endTime}"
            patientTextView.text = appointment.patientName
            doctorTextView.text = appointment.doctorName
            serviceTextView.text = appointment.serviceName
            priceTextView.text = "$${appointment.servicePrice}"
            statusTextView.text = appointment.status.capitalize(Locale.ROOT)

            // Set status text color based on status
            statusTextView.setTextColor(
                when (appointment.status.lowercase(Locale.ROOT)) {
                    "confirmed" -> itemView.context.getColor(android.R.color.holo_green_dark)
                    "cancelled" -> itemView.context.getColor(android.R.color.holo_red_dark)
                    "pending" -> itemView.context.getColor(android.R.color.holo_orange_dark)
                    else -> itemView.context.getColor(android.R.color.darker_gray)
                }
            )

            editButton.setOnClickListener { onEditClick(appointment) }
            deleteButton.setOnClickListener { onDeleteClick(appointment) }
        }
    }

    /**
     * Extension function to capitalize the first letter of a string.
     *
     * @param locale The locale to use for capitalization.
     * @return The capitalized string.
     */
    private fun String.capitalize(locale: Locale): String {
        return if (this.isNotEmpty()) {
            this[0].uppercaseChar() + this.substring(1).lowercase(locale)
        } else {
            this
        }
    }
}