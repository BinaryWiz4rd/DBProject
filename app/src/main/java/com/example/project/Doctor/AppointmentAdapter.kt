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

class AppointmentAdapter(
    private val context: Context,
    private val appointments: List<AppointmentCalendar>,
    private val highlightedTime: String? = null,
    private val onItemClick: ((AppointmentCalendar) -> Unit)? = null
) : RecyclerView.Adapter<AppointmentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeTextView: TextView = view.findViewById(R.id.timeTextView)
        val patientNameTextView: TextView = view.findViewById(R.id.patientNameTextView)
        val serviceTextView: TextView = view.findViewById(R.id.serviceTextView)
        val notesTextView: TextView = view.findViewById(R.id.notesTextView)
        val chatButton: Button = view.findViewById(R.id.btnChat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_item_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]

        holder.timeTextView.text = "${appointment.timeSlot} - ${appointment.endTime}"
        holder.patientNameTextView.text = appointment.patientName
        
        // Display service information
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

        // Highlight the appointment if it matches the highlighted time
        if (highlightedTime != null && appointment.timeSlot == highlightedTime) {
            // Set background color to highlight this appointment
            holder.itemView.setBackgroundResource(R.color.highlight_background)
        } else {
            // Reset background for other items
            holder.itemView.setBackgroundResource(android.R.color.transparent)
        }

        // Set click listener for the entire item
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(appointment)
        }
    }

    override fun getItemCount() = appointments.size
}