package com.example.project.view

import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var addAppointmentButton: Button
    private lateinit var appointmentsListView: ListView
    private val appointmentsMap = mutableMapOf<String, MutableList<String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar_doctor)

        calendarView = findViewById(R.id.calendarView)
        addAppointmentButton = findViewById(R.id.addAppointmentButton)
        appointmentsListView = findViewById(R.id.appointmentsListView)

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "$year-${month + 1}-$dayOfMonth"
            loadAppointments(selectedDate)
        }

        addAppointmentButton.setOnClickListener {
            val selectedDate = dateFormatter.format(Date(calendarView.date))
            addAppointment(selectedDate, "New Patient Appointment at 10:00 AM")
        }
    }

    private fun loadAppointments(date: String) {
        val appointments = appointmentsMap[date] ?: mutableListOf()
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, appointments)
        appointmentsListView.adapter = adapter
    }

    private fun addAppointment(date: String, appointment: String) {
        if (!appointmentsMap.containsKey(date)) {
            appointmentsMap[date] = mutableListOf()
        }
        appointmentsMap[date]?.add(appointment)
        Toast.makeText(this, "Appointment added for $date", Toast.LENGTH_SHORT).show()
        loadAppointments(date)
    }
}
