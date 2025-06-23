package com.example.project.Patient

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R

class TimeSlotAdapter(
    private val timeSlots: List<String>,
    private val onTimeSlotClick: (String) -> Unit
) : RecyclerView.Adapter<TimeSlotAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_time_slot, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val timeSlot = timeSlots[position]
        holder.timeSlotTextView.text = timeSlot
        holder.itemView.setOnClickListener {
            onTimeSlotClick(timeSlot)
        }
    }

    override fun getItemCount(): Int {
        return timeSlots.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeSlotTextView: TextView = view.findViewById(R.id.timeSlotTextView)
    }
} 