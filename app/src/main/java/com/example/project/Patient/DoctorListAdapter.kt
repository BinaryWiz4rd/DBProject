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

/**
 * RecyclerView adapter for displaying a list of doctors.
 *
 * This adapter is responsible for binding [Doctor] objects to the views in a RecyclerView,
 * showing the doctor's image, name, specialization, and experience. It also handles
 * click events on individual doctor items.
 *
 * @param doctors The list of [Doctor] objects to be displayed.
 * @param onDoctorClick A lambda function invoked when a doctor item is clicked,
 * passing the [Doctor] object of the clicked item.
 */
class DoctorListAdapter(
    private val doctors: List<Doctor>,
    private val onDoctorClick: (Doctor) -> Unit
) : RecyclerView.Adapter<DoctorListAdapter.DoctorViewHolder>() {

    /**
     * Called when RecyclerView needs a new [DoctorViewHolder] of the given type to represent
     * an item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     * @return A new [DoctorViewHolder] that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor_list, parent, false)
        return DoctorViewHolder(view)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * This method updates the contents of the [itemView] to reflect the item at the given
     * position.
     *
     * @param holder The [DoctorViewHolder] which should be updated to represent the contents
     * of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        holder.bind(doctors[position])
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int = doctors.size

    /**
     * ViewHolder for individual doctor items in the RecyclerView.
     *
     * @param itemView The View for a single doctor list item.
     */
    inner class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val doctorImage: ImageView = itemView.findViewById(R.id.doctorImageView)
        private val doctorName: TextView = itemView.findViewById(R.id.doctorNameTextView)
        private val doctorSpecialization: TextView = itemView.findViewById(R.id.doctorSpecializationTextView)
        private val doctorExperience: TextView = itemView.findViewById(R.id.doctorExperienceTextView)

        /**
         * Binds a [Doctor] object to the ViewHolder's views.
         *
         * Sets the doctor's name, specialization, and a default experience.
         * Loads a placeholder doctor image using Glide and sets a click listener
         * for the entire item view.
         *
         * @param doctor The [Doctor] object to bind.
         */
        fun bind(doctor: Doctor) {
            doctorName.text = "Dr. ${doctor.firstName} ${doctor.lastName}"
            doctorSpecialization.text = doctor.specialization
            doctorExperience.text = "5+ years"

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