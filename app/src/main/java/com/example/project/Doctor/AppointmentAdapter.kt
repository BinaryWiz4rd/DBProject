package com.example.project.Doctor

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.project.R

// Changed to use AppointmentCalendar
class AppointmentAdapter(
    context: Context,
    private val appointments: List<AppointmentCalendar> // <-- Change here
) : ArrayAdapter<AppointmentCalendar>(context, 0, appointments) { // <-- Change here

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var itemView = convertView
        if (itemView == null) {
            itemView = LayoutInflater.from(context).inflate(R.layout.activity_item_appointment, parent, false)
        }

        // appointment is now of type AppointmentCalendar
        val appointment = appointments[position]

        val timeTextView = itemView!!.findViewById<TextView>(R.id.timeTextView)
        val patientNameTextView = itemView.findViewById<TextView>(R.id.patientNameTextView)
        val notesTextView = itemView.findViewById<TextView>(R.id.notesTextView)

        // These properties exist in AppointmentCalendar
        timeTextView.text = appointment.timeSlot
        patientNameTextView.text = appointment.patientName
        notesTextView.text = if (appointment.notes.isNotEmpty()) appointment.notes else "No notes"

        return itemView
    }
}