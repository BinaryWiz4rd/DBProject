package com.example.project.doctor

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.Availability
import com.example.project.R
import com.example.project.util.FirestoreHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DoctorAvailabilityFragment : Fragment() {

    private lateinit var availabilityRecyclerView: RecyclerView
    private lateinit var fabAddAvailability: FloatingActionButton
    private val availabilityList = mutableListOf<Availability>()
    private lateinit var adapter: AvailabilityAdapter
    private lateinit var firestoreHelper: FirestoreHelper
    private var currentDoctorId: String = "" // Will be fetched from Firebase Auth
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_doctor_availability, container, false)

        availabilityRecyclerView = view.findViewById(R.id.availabilityRecyclerView)
        fabAddAvailability = view.findViewById(R.id.fabAddAvailability)

        firestoreHelper = FirestoreHelper()

        // Initialize the RecyclerView with layout manager
        availabilityRecyclerView.layoutManager = LinearLayoutManager(context)

        // Create adapter for the RecyclerView
        adapter = AvailabilityAdapter(availabilityList) { availability ->
            // Item click listener
            showAddOrEditAvailabilityDialog(availability)
        }
        availabilityRecyclerView.adapter = adapter

        // Fetch current doctor's ID from Firebase Auth
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            currentDoctorId = currentUser.uid
            loadAvailability()
        } else {
            // Handle the case where the user is not logged in or ID is not available
            Log.w("DoctorAvailability", "User not logged in.")
            Toast.makeText(context, "User not logged in. Cannot load availability.", Toast.LENGTH_LONG).show()
            // Optionally, disable UI elements or navigate away
        }

        fabAddAvailability.setOnClickListener {
            showAddOrEditAvailabilityDialog(null)
        }

        return view
    }

    private fun loadAvailability() {
        if (currentDoctorId.isBlank()) {
            Log.w("DoctorAvailability", "Doctor ID not set.")
            updateDisplayList(emptyList(), "Doctor ID not available.")
            return
        }

        // For simplicity, loading all availability for a doctor.
        // In a real app, you might want to filter by date range or show upcoming availability.
        firestoreHelper.getAllAvailabilityForDoctor(currentDoctorId) // Changed to use the new method
            .get()
            .addOnSuccessListener { result ->
                val newAvailabilityList = mutableListOf<Availability>()
                for (document in result) {
                    val availability = document.toObject(Availability::class.java).copy(id = document.id)
                    newAvailabilityList.add(availability)
                }
                availabilityList.clear()
                availabilityList.addAll(newAvailabilityList)
                updateDisplayList(newAvailabilityList, "No availability slots found. Add one!")
            }
            .addOnFailureListener { e ->
                Log.e("DoctorAvailability", "Error loading availability", e)
                updateDisplayList(emptyList(), "Error loading availability slots.")
                Toast.makeText(context, "Failed to load availability.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDisplayList(availabilities: List<Availability>, emptyMessage: String) {
        availabilityList.clear()
        availabilityList.addAll(availabilities)

        // Check if the list is empty to show empty state view
        if (availabilities.isEmpty()) {
            view?.findViewById<View>(R.id.emptyAvailabilityView)?.visibility = View.VISIBLE
            availabilityRecyclerView.visibility = View.GONE
        } else {
            view?.findViewById<View>(R.id.emptyAvailabilityView)?.visibility = View.GONE
            availabilityRecyclerView.visibility = View.VISIBLE
        }

        adapter.notifyDataSetChanged()
    }

    private fun showAddOrEditAvailabilityDialog(existingAvailability: Availability?) {
        // TODO: Inflate R.layout.dialog_add_availability instead of a generic one
        // For now, creating EditTexts programmatically for demonstration as layout is not available.
        // This is NOT a good practice for real apps. Use XML layouts.
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_availability_placeholder, null)
        val dateEditText = dialogView.findViewById<EditText>(R.id.editTextAvailabilityDate)
        val startTimeEditText = dialogView.findViewById<EditText>(R.id.editTextAvailabilityStartTime)
        val endTimeEditText = dialogView.findViewById<EditText>(R.id.editTextAvailabilityEndTime)

        val calendar = Calendar.getInstance()

        dateEditText.isFocusable = false
        dateEditText.setOnClickListener {
            DatePickerDialog(requireContext(), {
                    _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                dateEditText.setText(dateFormat.format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        startTimeEditText.isFocusable = false
        startTimeEditText.setOnClickListener {
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                startTimeEditText.setText(timeFormat.format(calendar.time))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        endTimeEditText.isFocusable = false
        endTimeEditText.setOnClickListener {
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                endTimeEditText.setText(timeFormat.format(calendar.time))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        if (existingAvailability != null) {
            dateEditText.setText(existingAvailability.date)
            startTimeEditText.setText(existingAvailability.start_time)
            endTimeEditText.setText(existingAvailability.end_time)
        }

        val builder = AlertDialog.Builder(requireContext())
            .setTitle(if (existingAvailability == null) "Add Availability" else "Edit Availability")
            .setView(dialogView)
            .setPositiveButton(if (existingAvailability == null) "Add" else "Save") { dialogInterface, which ->
                val date = dateEditText.text.toString()
                val startTime = startTimeEditText.text.toString()
                val endTime = endTimeEditText.text.toString()

                if (date.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()) {
                    // Basic validation: end time should be after start time
                    try {
                        val fullStartDateTimeStr = "$date $startTime"
                        val fullEndDateTimeStr = "$date $endTime"
                        // Explicitly use a combined format for parsing date and time together
                        val combinedDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                        val startDate = combinedDateTimeFormat.parse(fullStartDateTimeStr)
                        val endDate = combinedDateTimeFormat.parse(fullEndDateTimeStr)

                        if (startDate != null && endDate != null && endDate.after(startDate)) {
                            val availability = Availability(
                                id = existingAvailability?.id ?: "", // Keep existing ID if editing
                                doctor_id = currentDoctorId,
                                date = date,
                                start_time = startTime,
                                end_time = endTime
                            )
                            saveAvailability(availability, existingAvailability == null)
                        } else {
                            Toast.makeText(context, "End time must be after start time.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Invalid date or time format.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Please fill all fields.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialogInterface, which -> /* Do nothing or dismiss */ }

        if (existingAvailability != null) {
            builder.setNeutralButton("Delete") { dialogInterface, which ->
                showDeleteConfirmationDialog(existingAvailability)
            }
        }
        builder.show()
    }

    private fun saveAvailability(availability: Availability, isNew: Boolean) {
        val task = if (isNew) {
            firestoreHelper.addAvailability(availability.copy(id = "")) // Ensure ID is not set for new, Firestore will generate
        } else {
            firestoreHelper.updateAvailability(availability.id, mapOf(
                "date" to availability.date,
                "start_time" to availability.start_time,
                "end_time" to availability.end_time
                // doctor_id should not change here
            ))
        }

        task.addOnSuccessListener {
            Toast.makeText(context, "Availability ${if (isNew) "added" else "updated"}", Toast.LENGTH_SHORT).show()
            loadAvailability()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Failed to save availability: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDeleteConfirmationDialog(availability: Availability) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Availability")
            .setMessage("Are you sure you want to delete this slot: ${availability.date} (${availability.start_time} - ${availability.end_time})?")
            .setPositiveButton("Delete") { dialogInterface, which ->
                firestoreHelper.deleteAvailability(availability.id)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Availability slot deleted", Toast.LENGTH_SHORT).show()
                        loadAvailability()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to delete slot: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancel") { dialogInterface, which -> /* Do nothing or dismiss */ }
            .show()
    }

    inner class AvailabilityAdapter(
        private val items: List<Availability>,
        private val onItemClick: (Availability) -> Unit
    ) : RecyclerView.Adapter<AvailabilityAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            // TODO: Find and bind views from item_availability_day layout
            private val dateTextView: TextView = view.findViewById(R.id.availabilityDateTextView)
            private val timeRangeTextView: TextView = view.findViewById(R.id.availabilityTimeRangeTextView)

            fun bind(availability: Availability) {
                // TODO: Bind data to views
                dateTextView.text = availability.date
                timeRangeTextView.text = "${availability.start_time} - ${availability.end_time}"
                itemView.setOnClickListener { onItemClick(availability) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_availability_day, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size
    }
}