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

    // Getter for db if direct access is preferred for flexibility, though specific methods are safer.
    fun getDbInstance(): FirebaseFirestore {
        return db
    }

    // Service-related functions
    fun addService(service: Service): Task<Void> {
        return servicesCollection.document().set(service)
    }

    fun getService(serviceId: String): Task<DocumentSnapshot> {
        return servicesCollection.document(serviceId).get()
    }

    fun getServiceById(serviceId: String): Task<DocumentSnapshot> {
        return servicesCollection.document(serviceId).get()
    }

    fun getServicesForDoctor(doctorId: String): Task<QuerySnapshot> {
        return servicesCollection.whereEqualTo("doctor_id", doctorId).get()
    }

    fun getAllServices(): Task<QuerySnapshot> {
        return servicesCollection.get()
    }

    fun updateService(serviceId: String, serviceData: Map<String, Any>): Task<Void> {
        return servicesCollection.document(serviceId).update(serviceData)
    }

    fun deleteService(serviceId: String): Task<Void> {
        return servicesCollection.document(serviceId).delete()
    }

    // Utility method to create a sample service for a doctor
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
    fun addAvailability(availability: Availability): Task<Void> {
        return availabilityCollection.document().set(availability)
    }

    fun getAvailabilityForDoctorByDate(doctorId: String, date: String): Task<QuerySnapshot> {
        return availabilityCollection
            .whereEqualTo("doctor_id", doctorId)
            .whereEqualTo("date", date)
            .get()
    }

    fun getAllAvailabilityForDoctor(doctorId: String): Query {
        return availabilityCollection
            .whereEqualTo("doctor_id", doctorId)
            .orderBy("date")
            .orderBy("start_time")
    }

    fun getAvailabilityForDateRange(doctorId: String, startDate: String, endDate: String): Task<QuerySnapshot> {
        return availabilityCollection
            .whereEqualTo("doctor_id", doctorId)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date")
            .orderBy("start_time")
            .get()
    }

    fun updateAvailability(availabilityId: String, availabilityData: Map<String, Any>): Task<Void> {
        return availabilityCollection.document(availabilityId).update(availabilityData)
    }

    fun deleteAvailability(availabilityId: String): Task<Void> {
        return availabilityCollection.document(availabilityId).delete()
    }

    // Booking-related functions
    fun addBooking(booking: Booking): Task<Void> {
        return bookingsCollection.document().set(booking)
    }

    fun getBooking(bookingId: String): Task<DocumentSnapshot> {
        return bookingsCollection.document(bookingId).get()
    }

    fun getBookingsForDoctor(doctorId: String, date: String): Task<QuerySnapshot> {
        return bookingsCollection
            .whereEqualTo("doctor_id", doctorId)
            .whereEqualTo("date", date)
            .get()
    }

    fun getBookingsForDoctorDateRange(doctorId: String, startDate: String, endDate: String): Task<QuerySnapshot> {
        return bookingsCollection
            .whereEqualTo("doctor_id", doctorId)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date")
            .orderBy("start_time")
            .get()
    }

    // Enhanced booking functions
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

    fun getBookingsForPatient(patientId: String): Task<QuerySnapshot> {
        return bookingsCollection
            .whereEqualTo("patient_id", patientId)
            .orderBy("date", Query.Direction.ASCENDING)
            .orderBy("start_time", Query.Direction.ASCENDING)
            .get()
    }

    fun getUpcomingBookingsForPatient(patientId: String): Task<QuerySnapshot> {
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        return bookingsCollection
            .whereEqualTo("patient_id", patientId)
            .whereGreaterThanOrEqualTo("date", currentDate)
            .orderBy("date")
            .orderBy("start_time")
            .get()
    }

    fun updateBookingStatus(bookingId: String, status: String): Task<Void> {
        return bookingsCollection.document(bookingId).update("status", status)
    }

    fun cancelBooking(bookingId: String): Task<Void> {
        return bookingsCollection.document(bookingId).update("status", "cancelled")
    }

    // Patient-specific methods
    fun getPatientById(patientId: String): Task<DocumentSnapshot> {
        return patientsCollection.document(patientId).get()
    }

    fun updatePatient(patientId: String, updates: Map<String, Any>): Task<Void> {
        return patientsCollection.document(patientId).update(updates)
    }

    // Doctor discovery methods
    fun getAllDoctors(): Task<QuerySnapshot> {
        return doctorsCollection.get()
    }

    fun searchDoctors(searchTerm: String): Task<QuerySnapshot> {
        return doctorsCollection
            .orderBy("firstName")
            .startAt(searchTerm)
            .endAt(searchTerm + '\uf8ff')
            .get()
    }

    fun getDoctorsBySpecialization(specialization: String): Task<QuerySnapshot> {
        return doctorsCollection.whereEqualTo("specialization", specialization).get()
    }

    fun getDoctorById(doctorId: String): Task<DocumentSnapshot> {
        return doctorsCollection.document(doctorId).get()
    }
    
    fun getDoctorSettings(doctorId: String): Task<DocumentSnapshot> {
        return doctorSettingsCollection.document(doctorId).get()
    }

    // Enhanced booking methods
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

    private fun parseTime(timeString: String): Int {
        val parts = timeString.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    // Callback-based methods for easier fragment integration
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

    fun getChatMessages(chatId: String): Query {
        return chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("timestamp")
    }

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

    fun getChatsForUser(userId: String): Query {
        return chatsCollection.whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
    }
}
