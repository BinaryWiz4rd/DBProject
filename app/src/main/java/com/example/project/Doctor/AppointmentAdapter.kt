package com.example.project.doctor

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.chat.ChatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Adapter for displaying a list of appointments in a RecyclerView.
 *
 * @param context The context of the calling activity.
 * @param appointments The list of [AppointmentCalendar] objects to display.
 * @param highlightedTime An optional time string to highlight a specific appointment.
 * @param onItemClick An optional lambda function to be invoked when an item is clicked.
 */
class AppointmentAdapter(
    private val context: Context,
    private val appointments: List<AppointmentCalendar>,
    private val highlightedTime: String? = null,
    private val onItemClick: ((AppointmentCalendar) -> Unit)? = null
) : RecyclerView.Adapter<AppointmentAdapter.ViewHolder>() {

    /**
     * ViewHolder for individual appointment items.
     *
     * @param view The view of the appointment item.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeTextView: TextView = view.findViewById(R.id.timeTextView)
        val patientNameTextView: TextView = view.findViewById(R.id.patientNameTextView)
        val serviceTextView: TextView = view.findViewById(R.id.serviceTextView)
        val notesTextView: TextView = view.findViewById(R.id.notesTextView)
        val chatButton: Button = view.findViewById(R.id.btnChat)
    }

    /**
     * Called when RecyclerView needs a new [ViewHolder] of the given type to represent an item.
     *
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new [ViewHolder] that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_item_appointment, parent, false)
        return ViewHolder(view)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * @param holder The [ViewHolder] which should be updated to represent the contents of the item at the given position.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]

        holder.timeTextView.text = "${appointment.timeSlot} - ${appointment.endTime}"
        holder.patientNameTextView.text = appointment.patientName
        val serviceInfo = "${appointment.serviceName} ($${appointment.servicePrice}, ${appointment.serviceDuration} min)"
        holder.serviceTextView.text = serviceInfo
        holder.notesTextView.text = if (appointment.notes.isNotEmpty()) appointment.notes else "No notes"

        holder.chatButton.setOnClickListener {
            val doctorId = FirebaseAuth.getInstance().currentUser?.uid
            val doctorName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Doctor"
            if (doctorId != null) {
                val intent = Intent(context, ChatActivity::class.java).apply {
                    putExtra("patientId", appointment.patientId)
                    putExtra("doctorId", doctorId)
                    putExtra("patientName", appointment.patientName)
                    putExtra("doctorName", doctorName)
                    putExtra("chatTitle", "Chat with ${appointment.patientName}")
                }
                context.startActivity(intent)
            }
        }

        if (highlightedTime != null && appointment.timeSlot == highlightedTime) {
            holder.itemView.setBackgroundResource(R.color.highlight_background)
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent)
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(appointment)
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount() = appointments.size
}