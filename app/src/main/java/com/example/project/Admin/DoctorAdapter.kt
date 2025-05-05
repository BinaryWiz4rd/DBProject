package com.example.project.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R

class DoctorAdapter(
    private var doctors: List<Doctor>,
    private val onApproveReject: (String, String, Boolean) -> Unit
) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

    class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewName: TextView = itemView.findViewById(R.id.textViewName)
        val textViewEmail: TextView = itemView.findViewById(R.id.textViewEmail)
        val approveButton: Button = itemView.findViewById(R.id.buttonApprove)
        val rejectButton: Button = itemView.findViewById(R.id.buttonReject)

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor, parent, false)
        return DoctorViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        holder.bind(doctors[position], onApproveReject)
    }

    override fun getItemCount(): Int {
        return doctors.size
    }

    fun updateList(newDoctors: List<Doctor>) {
        doctors = newDoctors
        notifyDataSetChanged()
    }
}