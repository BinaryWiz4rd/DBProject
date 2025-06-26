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

/**
 * A [Fragment] for doctors to manage their availability slots.
 * It displays a list of existing availability slots and allows adding, editing, and deleting them.
 */
class DoctorAvailabilityFragment : Fragment() {

    private lateinit var availabilityRecyclerView: RecyclerView
    private lateinit var fabAddAvailability: FloatingActionButton
    private val availabilityList = mutableListOf<Availability>()
    private lateinit var adapter: AvailabilityAdapter
    private lateinit var firestoreHelper: FirestoreHelper
    private var currentDoctorId: String = ""
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    /**
     * Inflates the layout, initializes UI components and Firebase,
     * and sets up the RecyclerView and FAB for managing availability.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_doctor_availability, container, false)

        availabilityRecyclerView = view.findViewById(R.id.availabilityRecyclerView)
        fabAddAvailability = view.findViewById(R.id.fabAddAvailability)

        firestoreHelper = FirestoreHelper()

        availabilityRecyclerView.layoutManager = LinearLayoutManager(context)

        adapter = AvailabilityAdapter(availabilityList) { availability ->
            showAddOrEditAvailabilityDialog(availability)
        }
        availabilityRecyclerView.adapter = adapter

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            currentDoctorId = currentUser.uid
            loadAvailability()
        } else {
            Log.w("DoctorAvailability", "User not logged in.")
            Toast.makeText(context, "User not logged in. Cannot load availability.", Toast.LENGTH_LONG).show()
        }

        fabAddAvailability.setOnClickListener {
            showAddOrEditAvailabilityDialog(null)
        }

        return view
    }

    /**
     * Loads the availability slots for the current doctor from Firestore.
     */
    private fun loadAvailability() {
        if (currentDoctorId.isBlank()) {
            Log.w("DoctorAvailability", "Doctor ID not set.")
            updateDisplayList(emptyList(), "Doctor ID not available.")
            return
        }

        firestoreHelper.getAllAvailabilityForDoctor(currentDoctorId)
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

    /**
     * Updates the RecyclerView with a new list of availability slots and shows an empty state message if needed.
     * @param availabilities The list of availability slots to display.
     * @param emptyMessage The message to show when the list is empty.
     */
    private fun updateDisplayList(availabilities: List<Availability>, emptyMessage: String) {
        availabilityList.clear()
        availabilityList.addAll(availabilities)

        if (availabilities.isEmpty()) {
            view?.findViewById<View>(R.id.emptyAvailabilityView)?.visibility = View.VISIBLE
            availabilityRecyclerView.visibility = View.GONE
        } else {
            view?.findViewById<View>(R.id.emptyAvailabilityView)?.visibility = View.GONE
            availabilityRecyclerView.visibility = View.VISIBLE
        }

        adapter.notifyDataSetChanged()
    }

    /**
     * Shows a dialog to add a new or edit an existing availability slot.
     * @param existingAvailability The availability slot to edit, or null to add a new one.
     */
    private fun showAddOrEditAvailabilityDialog(existingAvailability: Availability?) {
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
                    try {
                        val fullStartDateTimeStr = "$date $startTime"
                        val fullEndDateTimeStr = "$date $endTime"
                        val combinedDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                        val startDate = combinedDateTimeFormat.parse(fullStartDateTimeStr)
                        val endDate = combinedDateTimeFormat.parse(fullEndDateTimeStr)

                        if (startDate != null && endDate != null && endDate.after(startDate)) {
                            val availability = Availability(
                                id = existingAvailability?.id ?: "",
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
            .setNegativeButton("Cancel") { dialogInterface, which -> }

        if (existingAvailability != null) {
            builder.setNeutralButton("Delete") { dialogInterface, which ->
                showDeleteConfirmationDialog(existingAvailability)
            }
        }
        builder.show()
    }

    /**
     * Saves a new or updated availability slot to Firestore.
     * @param availability The availability slot to save.
     * @param isNew `true` if it's a new slot, `false` if it's an existing one being updated.
     */
    private fun saveAvailability(availability: Availability, isNew: Boolean) {
        val task = if (isNew) {
            firestoreHelper.addAvailability(availability.copy(id = ""))
        } else {
            firestoreHelper.updateAvailability(availability.id, mapOf(
                "date" to availability.date,
                "start_time" to availability.start_time,
                "end_time" to availability.end_time
            ))
        }

        task.addOnSuccessListener {
            Toast.makeText(context, "Availability ${if (isNew) "added" else "updated"}", Toast.LENGTH_SHORT).show()
            loadAvailability()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Failed to save availability: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shows a confirmation dialog before deleting an availability slot.
     * @param availability The availability slot to delete.
     */
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
            .setNegativeButton("Cancel") { dialogInterface, which -> }
            .show()
    }

    /**
     * An adapter for displaying availability slots in the RecyclerView.
     * @property items The list of availability slots.
     * @property onItemClick A lambda to be invoked when an item is clicked.
     */
    inner class AvailabilityAdapter(
        private val items: List<Availability>,
        private val onItemClick: (Availability) -> Unit
    ) : RecyclerView.Adapter<AvailabilityAdapter.ViewHolder>() {

        /**
         * ViewHolder for an availability slot item.
         */
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val dateTextView: TextView = view.findViewById(R.id.availabilityDateTextView)
            private val timeRangeTextView: TextView = view.findViewById(R.id.availabilityTimeRangeTextView)

            /**
             * Binds an availability slot's data to the views.
             * @param availability The availability slot to bind.
             */
            fun bind(availability: Availability) {
                dateTextView.text = availability.date
                timeRangeTextView.text = "${availability.start_time} - ${availability.end_time}"
                itemView.setOnClickListener { onItemClick(availability) }
            }
        }

        /**
         * Creates a new [ViewHolder] for an availability slot item.
         */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_availability_day, parent, false)
            return ViewHolder(view)
        }

        /**
         * Binds the data of an availability slot at a given position to the [ViewHolder].
         */
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        /**
         * Returns the total number of availability slots.
         */
        override fun getItemCount() = items.size
    }
}