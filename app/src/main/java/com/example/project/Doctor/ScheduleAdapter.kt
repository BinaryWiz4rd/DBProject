package com.example.project.Doctor // Zmień na swoją nazwę pakietu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.Doctor.Appointment
import com.example.project.R
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ofPattern

class ScheduleAdapter(
    private var appointments: List<Appointment>,
    private val onItemClick: (Appointment) -> Unit // Funkcja lambda do obsługi kliknięcia
) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    // Formatter dla czasu (np. 09:30)
    private val timeFormatter: DateTimeFormatter = ofPattern("HH:mm")

    // Tworzy ViewHolder (łączy layout list_item_appointment z kodem)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_doctor_list_item_appointment, parent, false)
        return ViewHolder(view)
    }

    // Wiąże dane z widokiem dla konkretnego elementu listy
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.bind(appointment)
    }

    // Zwraca liczbę elementów w liście
    override fun getItemCount(): Int = appointments.size

    // Funkcja do aktualizacji danych w adapterze
    fun updateData(newAppointments: List<Appointment>) {
        appointments = newAppointments.sortedBy { it.time } // Sortuj wg czasu
        notifyDataSetChanged() // Informuje RecyclerView, że dane się zmieniły (są lepsze metody np. DiffUtil)
    }

    // ViewHolder - przechowuje referencje do widoków w elemencie listy
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewTime: TextView = itemView.findViewById(R.id.textViewTime)
        private val textViewPatientName: TextView = itemView.findViewById(R.id.textViewPatientName)

        init {
            // Ustawienie listenera kliknięcia na cały element
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(appointments[position])
                }
            }
        }

        fun bind(appointment: Appointment) {
            textViewTime.text = appointment.time.format(timeFormatter)
            textViewPatientName.text = appointment.patientName
            // Możesz tu dodać więcej logiki, np. ustawienie ikony w zależności od statusu wizyty
        }
    }
}