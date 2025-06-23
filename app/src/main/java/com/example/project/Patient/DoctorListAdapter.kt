package com.example.project.Patient

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.project.R
import com.example.project.Admin.Doctor

class DoctorListAdapter(
    private val doctors: List<Doctor>,
    private val onDoctorClick: (Doctor) -> Unit
) : RecyclerView.Adapter<DoctorListAdapter.DoctorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor_list, parent, false)
        return DoctorViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        holder.bind(doctors[position])
    }

    override fun getItemCount(): Int = doctors.size

    inner class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val doctorImage: ImageView = itemView.findViewById(R.id.doctorImageView)
        private val doctorName: TextView = itemView.findViewById(R.id.doctorNameTextView)
        private val doctorSpecialization: TextView = itemView.findViewById(R.id.doctorSpecializationTextView)
//        private val doctorRating: TextView = itemView.findViewById(R.id.doctorRatingTextView)
        private val doctorExperience: TextView = itemView.findViewById(R.id.doctorExperienceTextView)

        fun bind(doctor: Doctor) {
            doctorName.text = "Dr. ${doctor.firstName} ${doctor.lastName}"
            doctorSpecialization.text = doctor.specialization
//            doctorRating.text = "★ 4.5" // Default rating for now
            doctorExperience.text = "5+ years" // Default experience for now

            // Set a placeholder doctor image
            Glide.with(itemView.context)
                .load(R.drawable.doctor)
                .circleCrop()
                .into(doctorImage)

            itemView.setOnClickListener {
                onDoctorClick(doctor)
            }
        }
    }
}
