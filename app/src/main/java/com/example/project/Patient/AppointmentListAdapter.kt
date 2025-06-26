package com.example.project.Patient

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.chat.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView adapter for displaying a list of patient appointments.
 *
 * This adapter handles the presentation of appointment details, including date, time, doctor's name,
 * service, and status. It also manages UI interactions like displaying action buttons based on appointment
 * status and navigating to a chat activity.
 *
 * @param appointments The initial list of [PatientAppointmentDetails] to be displayed.
 * @param onAppointmentClick A lambda function invoked when an appointment item is clicked,
 * passing the [PatientAppointmentDetails] of the clicked appointment.
 */
class AppointmentListAdapter(
    private var appointments: List<PatientAppointmentDetails>,
    private val onAppointmentClick: (PatientAppointmentDetails) -> Unit
) : RecyclerView.Adapter<AppointmentListAdapter.AppointmentViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    /**
     * Called when RecyclerView needs a new [AppointmentViewHolder] of the given type to represent
     * an item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     * @return A new [AppointmentViewHolder] that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment_list, parent, false)
        return AppointmentViewHolder(view)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * This method updates the contents of the [itemView] to reflect the item at the given
     * position.
     *
     * @param holder The [AppointmentViewHolder] which should be updated to represent the contents
     * of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(appointments[position])
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int = appointments.size

    /**
     * Updates the list of appointments and notifies the RecyclerView to refresh its views.
     *
     * @param newAppointments The new list of [PatientAppointmentDetails] to display.
     */
    fun updateAppointments(newAppointments: List<PatientAppointmentDetails>) {
        this.appointments = newAppointments
        notifyDataSetChanged()
    }

    /**
     * ViewHolder for individual appointment items in the RecyclerView.
     *
     * @param itemView The View for a single appointment list item.
     */
    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.tvAppointmentDate)
        private val timeTextView: TextView = itemView.findViewById(R.id.tvAppointmentTime)
        private val doctorTextView: TextView = itemView.findViewById(R.id.tvDoctorName)
        private val serviceTextView: TextView = itemView.findViewById(R.id.tvServiceName)
        private val statusTextView: TextView = itemView.findViewById(R.id.tvAppointmentStatus)
        private val actionButtonsLayout: LinearLayout = itemView.findViewById(R.id.actionButtonsLayout)
        private val chatButton: Button = itemView.findViewById(R.id.btnChat)

        /**
         * Binds a [PatientAppointmentDetails] object to the ViewHolder's views.
         *
         * Populates the TextViews with appointment data, formats the date, checks if the appointment
         * is in the past, sets the status text and color, and manages the visibility and click
         * listeners for action buttons like the chat button.
         *
         * @param appointment The [PatientAppointmentDetails] object to bind.
         */
        fun bind(appointment: PatientAppointmentDetails) {
            var isPast = false
            try {
                val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = inputDateFormat.parse(appointment.date)
                dateTextView.text = if (date != null) dateFormat.format(date) else appointment.date

                if (date != null) {
                    val today = Calendar.getInstance()
                    today.set(Calendar.HOUR_OF_DAY, 0)
                    today.set(Calendar.MINUTE, 0)
                    today.set(Calendar.SECOND, 0)
                    today.set(Calendar.MILLISECOND, 0)
                    isPast = date.before(today.time)
                }

            } catch (e: Exception) {
                dateTextView.text = appointment.date
            }

            timeTextView.text = "${appointment.startTime} - ${appointment.endTime}"
            doctorTextView.text = "Dr. ${appointment.doctorName}"
            serviceTextView.text = appointment.serviceName

            statusTextView.text = appointment.status.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }

            when (appointment.status.lowercase()) {
                "confirmed" -> {
                    statusTextView.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                    actionButtonsLayout.visibility = View.VISIBLE
                    chatButton.visibility = View.VISIBLE
                }
                "pending" -> {
                    statusTextView.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                    actionButtonsLayout.visibility = View.GONE
                }
                "cancelled" -> {
                    statusTextView.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                    actionButtonsLayout.visibility = View.GONE
                }
                else -> {
                    statusTextView.setTextColor(itemView.context.getColor(R.color.grey))
                    actionButtonsLayout.visibility = View.GONE
                }
            }

            chatButton.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, ChatActivity::class.java).apply {
                    putExtra("patientId", FirebaseAuth.getInstance().currentUser?.uid)
                    putExtra("doctorId", appointment.doctorId)
                    putExtra("patientName", FirebaseAuth.getInstance().currentUser?.displayName ?: "Patient")
                    putExtra("doctorName", appointment.doctorName)
                    putExtra("chatTitle", "Chat with Dr. ${appointment.doctorName}")
                }
                context.startActivity(intent)
            }

            itemView.alpha = if (isPast) 0.5f else 1.0f

            itemView.setOnClickListener {
                onAppointmentClick(appointment)
            }
        }
    }
}