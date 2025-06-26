package com.example.project.doctor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import java.text.NumberFormat
import java.util.Locale

/**
 * `ScheduleAdapter` is a `ListAdapter` for displaying a list of `Appointment` objects
 * in a `RecyclerView`. It provides a click listener for individual appointments.
 *
 * @param onAppointmentClick A lambda function invoked when an appointment item is clicked.
 */
class ScheduleAdapter(
    private val onAppointmentClick: (Appointment) -> Unit
) : ListAdapter<Appointment, ScheduleAdapter.AppointmentViewHolder>(AppointmentDiffCallback()) {

    /**
     * Called when `RecyclerView` needs a new `ViewHolder` of the given type to represent an item.
     * @param parent The `ViewGroup` into which the new `View` will be added.
     * @param viewType The view type of the new `View`.
     * @return A new `AppointmentViewHolder` that holds a `View` of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment_schedule, parent, false)
        return AppointmentViewHolder(view)
    }

    /**
     * Called by `RecyclerView` to display the data at the specified position.
     * @param holder The `ViewHolder` which should be updated to represent the contents of the item at the given position.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * `AppointmentViewHolder` represents a single item view in the `RecyclerView`.
     * It binds `Appointment` data to the corresponding `TextView` elements.
     * @param itemView The root view of a single appointment item.
     */
    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeTextView: TextView = itemView.findViewById(R.id.textViewTime)
        private val patientNameTextView: TextView = itemView.findViewById(R.id.textViewPatientName)
        private val serviceTextView: TextView = itemView.findViewById(R.id.textViewService)
        private val priceTextView: TextView = itemView.findViewById(R.id.textViewPrice)

        /**
         * Binds the `Appointment` data to the `TextView` elements within the ViewHolder.
         * Sets up the click listener for the item view.
         * @param appointment The `Appointment` object to bind.
         */
        fun bind(appointment: Appointment) {
            timeTextView.text = appointment.time
            patientNameTextView.text = "${appointment.patient.firstName} ${appointment.patient.lastName}"
            serviceTextView.text = appointment.serviceName

            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
            priceTextView.text = currencyFormat.format(appointment.servicePrice)

            itemView.setOnClickListener {
                onAppointmentClick(appointment)
            }
        }
    }

    /**
     * `AppointmentDiffCallback` is used by `ListAdapter` to calculate the differences
     * between two lists of appointments. This improves RecyclerView performance.
     */
    class AppointmentDiffCallback : DiffUtil.ItemCallback<Appointment>() {
        /**
         * Checks if two appointment items represent the same item (e.g., have the same unique ID).
         * @param oldItem The old appointment item.
         * @param newItem The new appointment item.
         * @return `true` if the two items are the same, `false` otherwise.
         */
        override fun areItemsTheSame(oldItem: Appointment, newItem: Appointment): Boolean {
            return oldItem.time == newItem.time &&
                    oldItem.patient.email == newItem.patient.email
        }

        /**
         * Checks if the content of two appointment items is the same.
         * This is called only if `areItemsTheSame` returns `true`.
         * @param oldItem The old appointment item.
         * @param newItem The new appointment item.
         * @return `true` if the contents of the items are the same, `false` otherwise.
         */
        override fun areContentsTheSame(oldItem: Appointment, newItem: Appointment): Boolean {
            return oldItem == newItem
        }
    }
}