package com.example.project.util

import com.example.project.Admin.Doctor
import com.example.project.Admin.Patient
import com.example.project.Availability
import com.example.project.Booking
import com.example.project.Service
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Query // Added import

class FirestoreHelper {

    private val db = FirebaseFirestore.getInstance()
    private val servicesCollection = db.collection("services")
    private val availabilityCollection = db.collection("availability")
    private val bookingsCollection = db.collection("bookings")
    private val doctorsCollection = db.collection("doctors")
    private val patientsCollection = db.collection("patients")

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
        return bookingsCollection.whereEqualTo("patient_name", patientId).get()
    }

    fun getUpcomingBookingsForPatient(patientId: String, currentDate: String): Task<QuerySnapshot> {
        return bookingsCollection
            .whereEqualTo("patient_name", patientId)
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

    // Doctor-related functions
    fun getDoctorById(doctorId: String): Task<DocumentSnapshot> {
        return doctorsCollection.document(doctorId).get()
    }

    fun getAllDoctors(): Task<QuerySnapshot> {
        return doctorsCollection.get()
    }

    // Patient-related functions
    fun getPatientById(patientId: String): Task<DocumentSnapshot> {
        return patientsCollection.document(patientId).get()
    }

    fun searchPatients(query: String): Task<QuerySnapshot> {
        return patientsCollection
            .orderBy("lastName")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limit(10)
            .get()
    }

    // Calendar-related functions
    fun getWorkingHours(doctorId: String): Task<DocumentSnapshot> {
        return db.collection("doctorSettings")
            .document(doctorId)
            .get()
    }

    fun setWorkingHours(doctorId: String, startHour: Int, endHour: Int): Task<Void> {
        val data = hashMapOf(
            "startHour" to startHour,
            "endHour" to endHour
        )

        return db.collection("doctorSettings")
            .document(doctorId)
            .set(data)
    }

    fun getAppointmentsForDate(doctorId: String, date: String): Task<QuerySnapshot> {
        return db.collection("doctorCalendars")
            .document(doctorId)
            .collection("dates")
            .document(date)
            .collection("appointments")
            .get()
    }

    fun addAppointment(doctorId: String, date: String, appointment: Any): Task<Void> {
        val doc = db.collection("doctorCalendars")
            .document(doctorId)
            .collection("dates")
            .document(date)
            .collection("appointments")
            .document()

        return doc.set(appointment)
    }

    fun updateAppointment(doctorId: String, date: String, appointmentId: String, appointment: Any): Task<Void> {
        return db.collection("doctorCalendars")
            .document(doctorId)
            .collection("dates")
            .document(date)
            .collection("appointments")
            .document(appointmentId)
            .set(appointment)
    }

    fun deleteAppointment(doctorId: String, date: String, appointmentId: String): Task<Void> {
        return db.collection("doctorCalendars")
            .document(doctorId)
            .collection("dates")
            .document(date)
            .collection("appointments")
            .document(appointmentId)
            .delete()
    }

    fun addAppointmentListener(doctorId: String, date: String,
                               listener: (QuerySnapshot?, Exception?) -> Unit): ListenerRegistration {
        return db.collection("doctorCalendars")
            .document(doctorId)
            .collection("dates")
            .document(date)
            .collection("appointments")
            .addSnapshotListener { snapshot, e ->
                listener(snapshot, e)
            }
    }
}
