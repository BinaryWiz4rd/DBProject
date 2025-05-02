package com.example.project.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R

class DoctorAdapter(private val doctorList: List<Doctor>) :
    RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

    class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emailTextView: TextView = itemView.findViewById(R.id.tvDoctorEmail)
        val nameTextView: TextView = itemView.findViewById(R.id.tvDoctorName)
        val specializationTextView: TextView = itemView.findViewById(R.id.tvDoctorSpecialization)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor, parent, false)
        return DoctorViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        val currentDoctor = doctorList[position]
        holder.emailTextView.text = currentDoctor.email
        holder.nameTextView.text = "${currentDoctor.firstName} ${currentDoctor.lastName}"
        holder.specializationTextView.text = currentDoctor.specialization
    }

    override fun getItemCount() = doctorList.size
}