/**
 * Helper class for Firestore database operations, including CRUD for services, availability, bookings, doctors, patients, and chats.
 * Provides methods to interact with Firestore collections and documents.
 */

package com.example.project.util

import com.example.project.Admin.Doctor
import com.example.project.Admin.Patient
import com.example.project.Availability
import com.example.project.Booking
import com.example.project.Service
import com.example.project.Patient.PatientAppointmentDetails
import com.example.project.chat.Chat
import com.example.project.chat.Message
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class FirestoreHelper {

    private val db = FirebaseFirestore.getInstance()
    private val servicesCollection = db.collection("services")
    private val availabilityCollection = db.collection("availability")
    private val bookingsCollection = db.collection("bookings")
    private val doctorsCollection = db.collection("doctors")
    private val patientsCollection = db.collection("patients")
    private val doctorSettingsCollection = db.collection("doctorSettings")
    private val chatsCollection = db.collection("chats")

    /**
     * Retrieves the Firestore database instance.
     *
     * @return FirebaseFirestore instance.
     */
    // Getter for db if direct access is preferred for flexibility, though specific methods are safer.
    fun getDbInstance(): FirebaseFirestore {
        return db
    }

    // Service-related functions
    /**
     * Adds a service to the Firestore database.
     *
     * @param service The service object to be added.
     * @return Task<Void> representing the asynchronous operation.
     */
    fun addService(service: Service): Task<Void> {
        return servicesCollection.document().set(service)
    }

    /**
     * Retrieves a service from the Firestore database by its ID.
     *
     * @param serviceId The ID of the service to retrieve.
     * @return Task<DocumentSnapshot> representing the asynchronous operation.
     */
    fun getService(serviceId: String): Task<DocumentSnapshot> {
        return servicesCollection.document(serviceId).get()
    }

    /**
     * Retrieves a service from the Firestore database by its ID.
     *
     * @param serviceId The ID of the service to retrieve.
     * @return Task<DocumentSnapshot> representing the asynchronous operation.
     */
    fun getServiceById(serviceId: String): Task<DocumentSnapshot> {
        return servicesCollection.document(serviceId).get()
    }

    /**
     * Retrieves all services from the Firestore database.
     *
     * @return Task<QuerySnapshot> representing the asynchronous operation.
     */
    fun getAllServices(): Task<QuerySnapshot> {
        return servicesCollection.get()
    }

    /**
     * Updates a service in the Firestore database.
     *
     * @param serviceId The ID of the service to update.
     * @param serviceData A map containing the updated service data.
     * @return Task<Void> representing the asynchronous operation.
     */
    fun updateService(serviceId: String, serviceData: Map<String, Any>): Task<Void> {
        return servicesCollection.document(serviceId).update(serviceData)
    }

    /**
     * Deletes a service from the Firestore database by its ID.
     *
     * @param serviceId The ID of the service to delete.
     * @return Task<Void> representing the asynchronous operation.
     */
    fun deleteService(serviceId: String): Task<Void> {
        return servicesCollection.document(serviceId).delete()
    }

    /**
     * Retrieves all services for a specific doctor.
     *
     * @param doctorId The ID of the doctor.
     * @return Task<QuerySnapshot> representing the asynchronous operation.
     */
    fun getServicesForDoctor(doctorId: String): Task<QuerySnapshot> {
        return servicesCollection.whereEqualTo("doctor_id", doctorId).get()
    }

    /**
     * Creates a sample service for a doctor if no services exist.
     *
     * @param doctorId The ID of the doctor.
     * @return Task<Void> representing the asynchronous operation.
     */
    fun createSampleServiceIfNeeded(doctorId: String): Task<Void> {
        return getServicesForDoctor(doctorId)
            .continueWithTask { task ->
                if (task.isSuccessful && task.result.isEmpty) {
                    // No services found, create a sample one
                    val sampleService = Service(
                        doctor_id = doctorId,
                        name = "General Consultation",
                        price = 100,
                        duration_minutes = 30
                    )
                    addService(sampleService)
                } else {
                    // Services already exist or task failed
                    Tasks.forResult<Void>(null)
                }
            }
    }

    // Availability-related functions
    /**
     * Adds availability for a doctor to the Firestore database.
     *
     * @param availability The availability object to be added.
     * @return Task<Void> representing the asynchronous operation.
     */
    fun addAvailability(availability: Availability): Task<Void> {
        return availabilityCollection.document().set(availability)
    }

    /**
     * Retrieves availability for a doctor on a specific date.
     *
     * @param doctorId The ID of the doctor.
     * @param date The date for which availability is to be retrieved.
     * @return Task<QuerySnapshot> representing the asynchronous operation.
     */
    fun getAvailabilityForDoctorByDate(doctorId: String, date: String): Task<QuerySnapshot> {
        return availabilityCollection
            .whereEqualTo("doctor_id", doctorId)
            .whereEqualTo("date", date)
            .get()
    }

    /**
     * Retrieves all availability for a doctor.
     *
     * @param doctorId The ID of the doctor.
     * @return Query representing the availability query.
     */
    fun getAllAvailabilityForDoctor(doctorId: String): Query {
        return availabilityCollection
            .whereEqualTo("doctor_id", doctorId)
            .orderBy("date")
            .orderBy("start_time")
    }

    /**
     * Retrieves availability for a doctor within a specific date range.
     *
     * @param doctorId The ID of the doctor.
     * @param startDate The start date of the range.
     * @param endDate The end date of the range.
     * @return Task<QuerySnapshot> representing the asynchronous operation.
     */
    fun getAvailabilityForDateRange(doctorId: String, startDate: String, endDate: String): Task<QuerySnapshot> {
        return availabilityCollection
            .whereEqualTo("doctor_id", doctorId)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date")
            .orderBy("start_time")
            .get()
    }

    /**
     * Updates availability in the Firestore database.
     *
     * @param availabilityId The ID of the availability to update.
     * @param availabilityData A map containing the updated availability data.
     * @return Task<Void> representing the asynchronous operation.
     */
    fun updateAvailability(availabilityId: String, availabilityData: Map<String, Any>): Task<Void> {
        return availabilityCollection.document(availabilityId).update(availabilityData)
    }

    /**
     * Deletes availability from the Firestore database.
     *
     * @param availabilityId The ID of the availability to delete.
     * @return Task<Void> representing the asynchronous operation.
     */
    fun deleteAvailability(availabilityId: String): Task<Void> {
        return availabilityCollection.document(availabilityId).delete()
    }

    // Booking-related functions
    /**
     * Adds a booking to the Firestore database.
     *
     * @param booking The booking object to add.
     * @return Task<Void> representing the asynchronous operation.
     */
    fun addBooking(booking: Booking): Task<Void> {
        return bookingsCollection.document().set(booking)
    }

    /**
     * Retrieves a booking from the Firestore database by its ID.
     *
     * @param bookingId The ID of the booking to retrieve.
     * @return Task<DocumentSnapshot> representing the asynchronous operation.
     */
    fun getBooking(bookingId: String): Task<DocumentSnapshot> {
        return bookingsCollection.document(bookingId).get()
    }

    /**
     * Retrieves bookings for a doctor on a specific date.
     *
     * @param doctorId The ID of the doctor.
     * @param date The date for the bookings.
     * @return Task<QuerySnapshot> representing the asynchronous operation.
     */
    fun getBookingsForDoctor(doctorId: String, date: String): Task<QuerySnapshot> {
        return bookingsCollection
            .whereEqualTo("doctor_id", doctorId)
            .whereEqualTo("date", date)
            .get()
    }

    /**
     * Retrieves bookings for a doctor within a date range.
     *
     * @param doctorId The ID of the doctor.
     * @param startDate The start date of the range.
     * @param endDate The end date of the range.
     * @return Task<QuerySnapshot> representing the asynchronous operation.
     */
    fun getBookingsForDoctorDateRange(doctorId: String, startDate: String, endDate: String): Task<QuerySnapshot> {
        return bookingsCollection
            .whereEqualTo("doctor_id", doctorId)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date")
            .orderBy("start_time")
            .get()
    }

    /**
     * Retrieves bookings with their service details for a doctor on a specific date.
     *
     * @param doctorId The ID of the doctor.
     * @param date The date for the bookings.
     * @return Task<List<Pair<Booking, Service?>>> representing the asynchronous operation.
     */
    fun getBookingsWithServiceDetails(doctorId: String, date: String): Task<List<Pair<Booking, Service?>>> {
        return getBookingsForDoctor(doctorId, date).continueWithTask { bookingsTask ->
            if (!bookingsTask.isSuccessful) {
                throw bookingsTask.exception ?: Exception("Failed to fetch bookings")
            }

            val bookings = bookingsTask.result.documents.mapNotNull { doc ->
                doc.toObject(Booking::class.java)?.copy(id = doc.id)
            }

            if (bookings.isEmpty()) {
                return@continueWithTask Tasks.forResult(emptyList<Pair<Booking, Service?>>())
            }

            // Extract all service IDs from bookings
            val serviceIds = bookings.mapNotNull { it.service_id }.toSet()

            // Fetch all services in a single query
            getAllServices().continueWith { servicesTask ->
                if (!servicesTask.isSuccessful) {
                    throw servicesTask.exception ?: Exception("Failed to fetch services")
                }

                val services = servicesTask.result.documents.mapNotNull { doc ->
                    doc.toObject(Service::class.java)?.copy(id = doc.id)
                }

                val serviceMap = services.associateBy { it.id }

                // Pair each booking with its service
                bookings.map { booking ->
                    Pair(booking, serviceMap[booking.service_id])
                }
            }
        }
    }

    /**
     * Retrieves bookings for a patient.
     *
     * @param patientId The ID of the patient.
     * @return Task<QuerySnapshot> representing the asynchronous operation.
     */
    fun getBookingsForPatient(patientId: String): Task<QuerySnapshot> {
        return bookingsCollection
            .whereEqualTo("patient_id", patientId)
            .orderBy("date", Query.Direction.ASCENDING)
            .orderBy("start_time", Query.Direction.ASCENDING)
            .get()
    }

    /**
     * Retrieves upcoming bookings for a patient starting from today.
     *
     * @param patientId The ID of the patient.
     * @return Task<QuerySnapshot> representing the asynchronous operation.
     */
    fun getUpcomingBookingsForPatient(patientId: String): Task<QuerySnapshot> {
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        return bookingsCollection
            .whereEqualTo("patient_id", patientId)
            .whereGreaterThanOrEqualTo("date", currentDate)
            .orderBy("date")
            .orderBy("start_time")
            .get()
    }

    /**
     * Updates the status of a booking in the Firestore database.
     *
     * @param bookingId The ID of the booking to update.
     * @param status The new status value.
     * @return Task<Void> representing the asynchronous operation.
     */
    fun updateBookingStatus(bookingId: String, status: String): Task<Void> {
        return bookingsCollection.document(bookingId).update("status", status)
    }

    /**
     * Cancels a booking by setting its status to 'cancelled'.
     *
     * @param bookingId The ID of the booking to cancel.
     * @return Task<Void> representing the asynchronous operation.
     */
    fun cancelBooking(bookingId: String): Task<Void> {
        return bookingsCollection.document(bookingId).update("status", "cancelled")
    }

    // Patient-specific methods
    /**
     * Retrieves a patient document by ID.
     *
     * @param patientId The ID of the patient.
     * @return Task<DocumentSnapshot> representing the asynchronous operation.
     */
    fun getPatientById(patientId: String): Task<DocumentSnapshot> {
        return patientsCollection.document(patientId).get()
    }

    /**
     * Updates patient data in the Firestore database.
     *
     * @param patientId The ID of the patient.
     * @param updates Map of fields and values to update.
     * @return Task<Void> representing the asynchronous operation.
     */
    fun updatePatient(patientId: String, updates: Map<String, Any>): Task<Void> {
        return patientsCollection.document(patientId).update(updates)
    }

    // Doctor discovery methods
    /**
     * Retrieves all doctors from the Firestore database.
     *
     * @return Task<QuerySnapshot> representing the asynchronous operation.
     */
    fun getAllDoctors(): Task<QuerySnapshot> {
        return doctorsCollection.get()
    }

    /**
     * Searches doctors by first name prefix.
     *
     * @param searchTerm The search term prefix.
     * @return Task<QuerySnapshot> representing the asynchronous operation.
     */
    fun searchDoctors(searchTerm: String): Task<QuerySnapshot> {
        return doctorsCollection
            .orderBy("firstName")
            .startAt(searchTerm)
            .endAt(searchTerm + '\uf8ff')
            .get()
    }

    /**
     * Retrieves doctors by specialization.
     *
     * @param specialization The specialization to filter by.
     * @return Task<QuerySnapshot> representing the asynchronous operation.
     */
    fun getDoctorsBySpecialization(specialization: String): Task<QuerySnapshot> {
        return doctorsCollection.whereEqualTo("specialization", specialization).get()
    }

    /**
     * Retrieves a doctor's document by ID.
     *
     * @param doctorId The ID of the doctor.
     * @return Task<DocumentSnapshot> representing the asynchronous operation.
     */
    fun getDoctorById(doctorId: String): Task<DocumentSnapshot> {
        return doctorsCollection.document(doctorId).get()
    }

    /**
     * Retrieves doctor settings document by ID.
     *
     * @param doctorId The ID of the doctor.
     * @return Task<DocumentSnapshot> representing the asynchronous operation.
     */
    fun getDoctorSettings(doctorId: String): Task<DocumentSnapshot> {
        return doctorSettingsCollection.document(doctorId).get()
    }

    /**
     * Validates a booking slot by checking for time conflicts.
     *
     * @param booking The booking to validate.
     * @return Task<Boolean> indicating whether the slot is valid.
     */
    fun validateBookingSlot(booking: Booking): Task<Boolean> {
        return bookingsCollection
            .whereEqualTo("doctor_id", booking.doctor_id)
            .whereEqualTo("date", booking.date)
            .get()
            .continueWith { task ->
                if (task.isSuccessful) {
                    val existingBookings = task.result.documents
                    !hasTimeConflict(booking, existingBookings)
                } else {
                    false
                }
            }
    }

    /**
     * Checks for time conflicts between a new booking and existing bookings.
     *
     * @param newBooking The new booking to check.
     * @param existingBookings List of existing booking snapshots.
     * @return True if there is a conflict, false otherwise.
     */
    private fun hasTimeConflict(newBooking: Booking, existingBookings: List<DocumentSnapshot>): Boolean {
        val newStart = parseTime(newBooking.start_time)
        val newEnd = parseTime(newBooking.end_time)
        
        for (doc in existingBookings) {
            val booking = doc.toObject(Booking::class.java)
            if (booking != null && booking.status != "cancelled") {
                val existingStart = parseTime(booking.start_time)
                val existingEnd = parseTime(booking.end_time)
                
                // Check for overlap
                if (newStart < existingEnd && newEnd > existingStart) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Parses a time string (HH:mm) into minutes since midnight.
     *
     * @param timeString Time string in HH:mm format.
     * @return Minutes since midnight.
     */
    private fun parseTime(timeString: String): Int {
        val parts = timeString.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    // Callback-based methods for easier fragment integration
    /**
     * Retrieves a patient by ID using a callback.
     *
     * @param patientId The ID of the patient.
     * @param callback Callback to receive the Patient object or null.
     */
    fun getPatientById(patientId: String, callback: (com.example.project.Admin.Patient?) -> Unit) {
        patientsCollection.document(patientId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val patient = document.toObject(com.example.project.Admin.Patient::class.java)
                    callback(patient)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    /**
     * Retrieves upcoming patient bookings with details using a callback.
     *
     * @param patientId The ID of the patient.
     * @param callback Callback to receive a list of appointment details.
     */
    fun getUpcomingBookingsForPatient(patientId: String, callback: (List<com.example.project.Patient.PatientAppointmentDetails>) -> Unit) {
        getUpcomingBookingsForPatient(patientId)
            .addOnSuccessListener { querySnapshot ->
                val bookings = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(id = doc.id)
                }
                
                if (bookings.isEmpty()) {
                    callback(emptyList())
                    return@addOnSuccessListener
                }
                
                val appointmentDetails = mutableListOf<com.example.project.Patient.PatientAppointmentDetails>()
                var processedCount = 0
                
                bookings.forEach { booking ->
                    loadAppointmentDetails(booking) { details ->
                        appointmentDetails.add(details)
                        processedCount++
                        
                        if (processedCount == bookings.size) {
                            // Sort by date and time
                            appointmentDetails.sortWith(compareBy({ it.date }, { it.startTime }))
                            callback(appointmentDetails)
                        }
                    }
                }
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }
    
    /**
     * Loads appointment details for a booking including doctor and service info.
     *
     * @param booking The booking object.
     * @param callback Callback to receive PatientAppointmentDetails.
     */
    private fun loadAppointmentDetails(
        booking: Booking,
        callback: (com.example.project.Patient.PatientAppointmentDetails) -> Unit
    ) {
        var doctorName = "Loading..."
        var serviceName = "Loading..."
        var servicePrice = 0
        var serviceDuration = 0
        var completedCalls = 0
        
        fun checkCompletion() {
            completedCalls++
            if (completedCalls >= 2) {
                callback(
                    com.example.project.Patient.PatientAppointmentDetails(
                        booking = booking,
                        doctorName = doctorName,
                        serviceName = serviceName,
                        servicePrice = servicePrice,
                        serviceDuration = serviceDuration
                    )
                )
            }
        }
        
        // Load doctor details
        getDoctorById(booking.doctor_id)
            .addOnSuccessListener { doctorDoc ->
                if (doctorDoc.exists()) {
                    val firstName = doctorDoc.getString("firstName") ?: ""
                    val lastName = doctorDoc.getString("lastName") ?: ""
                    doctorName = "$firstName $lastName".trim().takeIf { it.isNotBlank() } 
                        ?: "Dr. ${booking.doctor_id}"
                } else {
                    doctorName = "Dr. ${booking.doctor_id}"
                }
                checkCompletion()
            }
            .addOnFailureListener {
                doctorName = "Dr. ${booking.doctor_id}"
                checkCompletion()
            }
        
        // Load service details
        getServiceById(booking.service_id)
            .addOnSuccessListener { serviceDoc ->
                if (serviceDoc.exists()) {
                    serviceName = serviceDoc.getString("name") ?: booking.service_id
                    servicePrice = serviceDoc.getLong("price")?.toInt() ?: 0
                    serviceDuration = serviceDoc.getLong("duration_minutes")?.toInt() ?: 0
                } else {
                    serviceName = booking.service_id
                }
                checkCompletion()
            }
            .addOnFailureListener {
                serviceName = booking.service_id
                checkCompletion()
            }
    }

    /**
     * Retrieves all doctors with callback.
     *
     * @param callback Callback to receive a list of Doctor objects.
     */
    fun getAllDoctors(callback: (List<com.example.project.Admin.Doctor>) -> Unit) {
        getAllDoctors()
            .addOnSuccessListener { querySnapshot ->
                val doctors = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(com.example.project.Admin.Doctor::class.java)?.copy(uid = doc.id)
                }
                callback(doctors)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    /**
     * Searches doctors by name with callback.
     *
     * @param searchTerm The search term.
     * @param callback Callback to receive a list of Doctor objects.
     */
    fun searchDoctors(searchTerm: String, callback: (List<com.example.project.Admin.Doctor>) -> Unit) {
        searchDoctors(searchTerm)
            .addOnSuccessListener { querySnapshot ->
                val doctors = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(com.example.project.Admin.Doctor::class.java)?.copy(uid = doc.id)
                }
                callback(doctors)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    /**
     * Retrieves completed bookings for a patient using a callback.
     *
     * @param patientName The name of the patient.
     * @param callback Callback to receive a list of appointment details.
     */
    fun getCompletedBookingsForPatient(patientName: String, callback: (List<PatientAppointmentDetails>) -> Unit) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        bookingsCollection
            .whereEqualTo("patient_name", patientName)
            .whereLessThan("date", today)
            .get()
            .addOnSuccessListener { bookingSnapshot ->
                if (bookingSnapshot.isEmpty) {
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                val appointmentDetails = mutableListOf<PatientAppointmentDetails>()
                var processedCount = 0
                val totalBookings = bookingSnapshot.documents.size

                for (bookingDoc in bookingSnapshot.documents) {
                    val booking = bookingDoc.toObject(Booking::class.java)?.copy(id = bookingDoc.id)
                    if (booking != null) {
                        // Get doctor name
                        doctorsCollection.document(booking.doctor_id).get()
                            .addOnSuccessListener { doctorDoc ->
                                val doctor = doctorDoc.toObject(com.example.project.Admin.Doctor::class.java)
                                val doctorName = if (doctor != null) {
                                    "${doctor.firstName} ${doctor.lastName}"
                                } else {
                                    "Unknown Doctor"
                                }

                                // Get service details
                                servicesCollection.document(booking.service_id).get()
                                    .addOnSuccessListener { serviceDoc ->
                                        val service = serviceDoc.toObject(Service::class.java)
                                        val serviceName = service?.name ?: "Unknown Service"

                                        appointmentDetails.add(
                                            PatientAppointmentDetails(
                                                booking = booking,
                                                doctorName = doctorName,
                                                serviceName = serviceName,
                                                servicePrice = service?.price ?: 0,
                                                serviceDuration = service?.duration_minutes ?: 0
                                            )
                                        )

                                        processedCount++
                                        if (processedCount == totalBookings) {
                                            // Sort by date (newest first)
                                            appointmentDetails.sortByDescending { it.date }
                                            callback(appointmentDetails)
                                        }
                                    }
                                    .addOnFailureListener {
                                        processedCount++
                                        if (processedCount == totalBookings) {
                                            appointmentDetails.sortByDescending { it.date }
                                            callback(appointmentDetails)
                                        }
                                    }
                            }
                            .addOnFailureListener {
                                processedCount++
                                if (processedCount == totalBookings) {
                                    appointmentDetails.sortByDescending { it.date }
                                    callback(appointmentDetails)
                                }
                            }
                    } else {
                        processedCount++
                        if (processedCount == totalBookings) {
                            appointmentDetails.sortByDescending { it.date }
                            callback(appointmentDetails)
                        }
                    }
                }
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    /**
     * Retrieves all bookings for a patient using a callback.
     *
     * @param patientName The name of the patient.
     * @param callback Callback to receive a list of appointment details.
     */
    fun getAllBookingsForPatient(patientName: String, callback: (List<PatientAppointmentDetails>) -> Unit) {
        bookingsCollection
            .whereEqualTo("patient_name", patientName)
            .get()
            .addOnSuccessListener { bookingSnapshot ->
                if (bookingSnapshot.isEmpty) {
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                val appointmentDetails = mutableListOf<PatientAppointmentDetails>()
                var processedCount = 0
                val totalBookings = bookingSnapshot.documents.size

                for (bookingDoc in bookingSnapshot.documents) {
                    val booking = bookingDoc.toObject(Booking::class.java)?.copy(id = bookingDoc.id)
                    if (booking != null) {
                        // Get doctor name
                        doctorsCollection.document(booking.doctor_id).get()
                            .addOnSuccessListener { doctorDoc ->
                                val doctor = doctorDoc.toObject(com.example.project.Admin.Doctor::class.java)
                                val doctorName = if (doctor != null) {
                                    "${doctor.firstName} ${doctor.lastName}"
                                } else {
                                    "Unknown Doctor"
                                }

                                // Get service details
                                servicesCollection.document(booking.service_id).get()
                                    .addOnSuccessListener { serviceDoc ->
                                        val service = serviceDoc.toObject(Service::class.java)
                                        val serviceName = service?.name ?: "Unknown Service"

                                        appointmentDetails.add(
                                            PatientAppointmentDetails(
                                                booking = booking,
                                                doctorName = doctorName,
                                                serviceName = serviceName,
                                                servicePrice = service?.price ?: 0,
                                                serviceDuration = service?.duration_minutes ?: 0
                                            )
                                        )

                                        processedCount++
                                        if (processedCount == totalBookings) {
                                            // Sort by date (newest first)
                                            appointmentDetails.sortByDescending { it.date }
                                            callback(appointmentDetails)
                                        }
                                    }
                                    .addOnFailureListener {
                                        processedCount++
                                        if (processedCount == totalBookings) {
                                            appointmentDetails.sortByDescending { it.date }
                                            callback(appointmentDetails)
                                        }
                                    }
                            }
                            .addOnFailureListener {
                                processedCount++
                                if (processedCount == totalBookings) {
                                    appointmentDetails.sortByDescending { it.date }
                                    callback(appointmentDetails)
                                }
                            }
                    } else {
                        processedCount++
                        if (processedCount == totalBookings) {
                            appointmentDetails.sortByDescending { it.date }
                            callback(appointmentDetails)
                        }
                    }
                }
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    // Chat-related functions

    /**
     * Gets or creates a chat for a patient and doctor.
     *
     * @param patientId The ID of the patient.
     * @param doctorId The ID of the doctor.
     * @param patientName Name of the patient.
     * @param doctorName Name of the doctor.
     * @return Task<String> representing the chat ID.
     */
    fun getOrCreateChat(patientId: String, doctorId: String, patientName: String, doctorName: String): Task<String> {
        val participants = listOf(patientId, doctorId).sorted()
        val chatId = "${participants[0]}_${participants[1]}"
        val chatRef = chatsCollection.document(chatId)

        return chatRef.get().continueWithTask { task ->
            if (task.isSuccessful && task.result.exists()) {
                // Chat exists, return its ID
                Tasks.forResult(chatId)
            } else {
                // Chat doesn't exist, create it
                val newChat = Chat(
                    id = chatId,
                    patientId = patientId,
                    doctorId = doctorId,
                    patientName = patientName,
                    doctorName = doctorName,
                    participants = participants
                )
                chatRef.set(newChat).continueWith { chatId }
            }
        }
    }

    /**
     * Retrieves chat messages query for a given chat ID.
     *
     * @param chatId The ID of the chat.
     * @return Query ordered by message timestamp.
     */
    fun getChatMessages(chatId: String): Query {
        return chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("timestamp")
    }

    /**
     * Sends a message by batching the message document and updating chat metadata.
     *
     * @param chatId The ID of the chat.
     * @param message The message object to send.
     * @return Task<Void> representing the asynchronous operation.
     */
    fun sendMessage(chatId: String, message: Message): Task<Void> {
        val chatRef = chatsCollection.document(chatId)
        val messageRef = chatRef.collection("messages").document()
        message.id = messageRef.id

        return db.runBatch { batch ->
            batch.set(messageRef, message)
            batch.update(chatRef, "lastMessage", message.text)
            batch.update(chatRef, "lastMessageTimestamp", message.timestamp)
        }
    }

    /**
     * Retrieves chats for a user ordered by last message timestamp.
     *
     * @param userId The ID of the user.
     * @return Query representing the chats query.
     */
    fun getChatsForUser(userId: String): Query {
        return chatsCollection.whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
    }
}
