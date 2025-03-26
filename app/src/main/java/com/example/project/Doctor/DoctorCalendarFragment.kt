/** package com.example.project.Doctor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.project.R
import java.text.SimpleDateFormat
import java.util.*

class DoctorCalendarFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var addAppointmentButton: Button
    private lateinit var appointmentsListView: ListView
    private val appointmentsMap = mutableMapOf<String, MutableList<String>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_doctor_calendar, container, false)

        calendarView = view.findViewById(R.id.calendarView)
        addAppointmentButton = view.findViewById(R.id.addAppointmentButton)
        appointmentsListView = view.findViewById(R.id.appointmentsListView)

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "$year-${month + 1}-$dayOfMonth"
            loadAppointments(selectedDate)
        }

        addAppointmentButton.setOnClickListener {
            val selectedDate = dateFormatter.format(Date(calendarView.date))
            addAppointment(selectedDate, "New Patient Appointment at 10:00 AM")
        }

        return view
    }

    private fun loadAppointments(date: String) {
        val appointments = appointmentsMap[date] ?: mutableListOf()
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, appointments)
        appointmentsListView.adapter = adapter
    }

    private fun addAppointment(date: String, appointment: String) {
        if (!appointmentsMap.containsKey(date)) {
            appointmentsMap[date] = mutableListOf()
        }
        appointmentsMap[date]?.add(appointment)
        Toast.makeText(requireContext(), "Appointment added for $date", Toast.LENGTH_SHORT).show()
        loadAppointments(date)
    }
}
*/