# Firebase Integration Summary

This document provides a summary of the current Firebase integration in the project, focusing on completed features and important Firebase structures. The next phase of development will focus on enhancing the patient-side integration with Firebase.

## 1. Data Models

The following data classes are used to interact with Firestore:

- `Service`: Represents a service offered by a doctor.
  ```kotlin
  data class Service(
      var id: String = "",
      val doctor_id: String = "",
      val name: String = "",
      val price: Int = 0,
      val duration_minutes: Int = 0
  )
  ```
- `Availability`: Represents a doctor's availability slot.
  ```kotlin
  data class Availability(
      var id: String = "",
      val doctor_id: String = "",
      val date: String = "", 
      val start_time: String = "",
      val end_time: String = ""
  )
  ```
- `Booking`: Represents a booking made by a patient for a service.
  ```kotlin
  data class Booking(
      var id: String = "",
      val doctor_id: String = "",
      val service_id: String = "",
      val date: String = "",
      val start_time: String = "",
      val end_time: String = "",
      val patient_name: String = "",
      val status: String = "" // e.g., "confirmed", "cancelled", "pending"
  )
  ```
- `Patient`: Represents a patient user.
  ```kotlin
  data class Patient(
      var uid: String = "", // Stores the Firestore document ID
      var email: String = "", // Stores the patient's email address
      val firstName: String = "",
      val lastName: String = "",
      val role: String = "patient",
      val add: Boolean? = false,
      val delete: Boolean? = false,
      val edit: Boolean? = false,
      val dateOfBirth: String = ""
  )
  ```

## 2. Firebase Firestore Structure

The following collections are used in Firestore:

- `services`: Stores documents of type `Service`.
- `availability`: Stores documents of type `Availability`.
- `bookings`: Stores documents of type `Booking`.
- `doctors`: Stores doctor profiles.
- `patients`: Stores patient profiles.
- `admins`: Stores admin profiles.
- `doctorSettings`: Stores doctor-specific settings like working hours.
- `doctorCalendars`: Hierarchical structure for appointments.

## 3. Implemented Features

### 3.1 Core
- `FirestoreHelper.kt` encapsulates Firestore CRUD operations.

### 3.2 Admin Panel
- **User Management**: View and manage all patients and doctors, including registration of new doctors.
- **Appointment Oversight**: View and manage all appointments in the system.

### 3.3 Doctor's View
- **Schedule Management**: `DoctorScheduleFragment.kt` displays bookings for the current doctor.
- **Service Management**: `DoctorServicesFragment.kt` allows doctors to manage their services.
- **Availability Management**: `DoctorAvailabilityFragment.kt` allows doctors to manage their availability.
- **Calendar**: `DoctorCalendarFragment.kt` provides a calendar view of appointments.

### 3.4 Patient's View (Current State)
- **Service Browsing**: `PatientBrowseServicesFragment.kt` allows patients to view available services.
- **Availability Viewing**: `PatientServiceAvailabilityFragment.kt` allows patients to view doctor's availability.
- **Booking Confirmation**: `ConfirmBookingFragment.kt` allows patients to confirm bookings.

## 4. Calendar Functionality

The calendar functionality uses the following Firestore structure:

- `doctorSettings/{doctorId}`: Contains doctor-specific settings like working hours.
- `doctorCalendars/{doctorId}/dates/{date}/appointments/{appointmentId}`: Hierarchical structure for appointments, integrated with the `bookings` collection.

## 5. Firestore Security Rules

Security rules are implemented to ensure data privacy and integrity for patients, doctors, and admins. Key rules include:
- Users can only access and modify their own profiles.
- Doctors can manage their own settings, calendar, services, and availability.
- Patients can read service and availability data and manage their own bookings.
- Admins have read and write access to all data for administrative purposes.

---

# Patient-Side Firebase Integration Plan

## Overview

The patient-side of the application currently has basic navigation and Firebase integration for booking appointments. However, it needs comprehensive enhancement to provide a complete patient experience including:

1. **Patient Dashboard/Home Screen**
2. **Doctor Profiles and Discovery**
3. **Appointment Management**
4. **Enhanced Service Browsing**
5. **Patient Profile Management**

## Current Patient Views Analysis

### Existing Files:
1. `MainPatientActivity.kt` - Main patient activity with doctor browsing and profile management
2. `PatientBrowseServicesFragment.kt` - Basic service browsing (currently not connected to main activity)
3. `PatientServiceAvailabilityFragment.kt` - View doctor availability
4. `ConfirmBookingFragment.kt` - Booking confirmation
5. `MainViewModel.kt` - Loads categories and doctors from Firebase (using old models)
6. `DoctorsModel.kt` - Old doctor data model (different from Firebase `Doctor` model)
7. `CategoryModel.kt` - Category data model for service categories

### Current Issues:
1. **Missing Patient Home Fragment**: No dedicated patient dashboard
2. **Limited Doctor Integration**: Uses old `DoctorsModel` instead of Firebase `Doctor` model
3. **No Appointment History**: Patients can't view their bookings
4. **Basic UI**: Simple ListView instead of modern RecyclerView with cards
5. **Limited Patient Profile**: No proper patient profile management
6. **No Search/Filter**: No way to search doctors or services
7. **Complex Profile Management**: Uses edit requests and admin approval system instead of direct updates
8. **Missing Navigation**: No connection between MainPatientActivity and existing booking fragments
9. **Outdated Categories**: Uses `CategoryModel` with Firebase categories collection that may not align with services

## Required Firebase Integration Enhancements

### 1. Patient Home/Dashboard Fragment

**Create:** `PatientHomeFragment.kt`

**Features:**
- Welcome message with patient name
- Upcoming appointments widget
- Quick actions (Book appointment, View history)
- Recommended doctors based on location/specialization
- Recent appointment history

**Firebase Integration:**
```kotlin
// Load patient profile
private fun loadPatientProfile() {
    val userId = auth.currentUser?.uid
    firestoreHelper.getPatientById(userId)
        .addOnSuccessListener { document ->
            if (document.exists()) {
                currentPatient = document.toObject(Patient::class.java)
                setupGreeting()
            }
        }
}

// Load upcoming appointments
private fun loadUpcomingAppointments() {
    val today = LocalDate.now().format(dateFormatter)
    firestoreHelper.getUpcomingBookingsForPatient(currentPatient.uid, today)
        .addOnSuccessListener { bookings ->
            // Display in RecyclerView
        }
}
```

### 2. Enhanced Doctor Discovery

**Update:** `MainPatientActivity.kt` and create `PatientDoctorsFragment.kt`

**Features:**
- Modern RecyclerView with doctor cards
- Search functionality by name, specialization
- Filter by location, rating, availability
- Integration with real Firebase doctor data
- View doctor details and services

**Firebase Integration:**
```kotlin
// Replace DoctorsModel with proper Doctor model
private fun loadDoctors() {
    firestoreHelper.getAllDoctors()
        .addOnSuccessListener { result ->
            val doctors = mutableListOf<Doctor>()
            for (document in result) {
                val doctor = document.toObject(Doctor::class.java).copy(uid = document.id)
                doctors.add(doctor)
            }
            updateDoctorsList(doctors)
        }
}

// Load doctor's services
private fun loadDoctorServices(doctorId: String) {
    firestoreHelper.getServicesForDoctor(doctorId)
        .addOnSuccessListener { services ->
            // Update UI with services
        }
}
```

### 3. Enhanced Service Browsing

**Update:** `PatientBrowseServicesFragment.kt`

**Features:**
- Modern card-based UI with service details
- Display doctor information alongside services
- Filter by price range, duration, specialization
- Integration with doctor profiles
- Better service details (description, price, duration)

**Firebase Integration:**
```kotlin
// Enhanced service loading with doctor details
private fun loadServicesWithDoctorInfo() {
    firestoreHelper.getAllServices()
        .addOnSuccessListener { result ->
            val services = mutableListOf<ServiceWithDoctor>()
            for (document in result) {
                val service = document.toObject(Service::class.java).copy(id = document.id)
                // Load doctor info for each service
                loadDoctorForService(service) { serviceWithDoctor ->
                    services.add(serviceWithDoctor)
                    updateServicesRecyclerView(services)
                }
            }
        }
}

data class ServiceWithDoctor(
    val service: Service,
    val doctor: Doctor
)
```

### 4. Patient Appointment Management

**Create:** `PatientAppointmentsFragment.kt`

**Features:**
- View all appointments (past, upcoming, cancelled)
- Cancel upcoming appointments
- Reschedule appointments
- View appointment details
- Rate and review completed appointments

**Firebase Integration:**
```kotlin
// Load patient's appointments
private fun loadPatientAppointments() {
    val patientId = auth.currentUser?.uid
    firestoreHelper.getBookingsForPatient(patientId)
        .addOnSuccessListener { result ->
            val appointments = mutableListOf<AppointmentDetails>()
            for (document in result) {
                val booking = document.toObject(Booking::class.java).copy(id = document.id)
                // Load service and doctor details
                loadAppointmentDetails(booking) { appointmentDetails ->
                    appointments.add(appointmentDetails)
                }
            }
            updateAppointmentsList(appointments)
        }
}

data class AppointmentDetails(
    val booking: Booking,
    val service: Service,
    val doctor: Doctor
)
```

### 5. Enhanced Booking Flow

**Update:** `PatientServiceAvailabilityFragment.kt` and `ConfirmBookingFragment.kt`

**Features:**
- Better availability display with time slots
- Calendar view for date selection
- Service duration consideration
- Conflict checking
- Payment integration placeholder
- Email/SMS confirmation

**Firebase Integration:**
```kotlin
// Enhanced availability loading with conflict checking
private fun loadAvailabilityWithConflicts(doctorId: String, serviceId: String) {
    // Load doctor availability
    firestoreHelper.getAllAvailabilityForDoctor(doctorId)
        .addOnSuccessListener { availabilityResult ->
            // Load existing bookings to check conflicts
            firestoreHelper.getBookingsForDoctor(doctorId)
                .addOnSuccessListener { bookingsResult ->
                    val availableSlots = calculateAvailableSlots(availabilityResult, bookingsResult, serviceId)
                    updateAvailabilityUI(availableSlots)
                }
        }
}

// Enhanced booking confirmation with validation
private fun confirmBooking(booking: Booking) {
    // Check for conflicts before confirming
    validateBookingSlot(booking) { isValid ->
        if (isValid) {
            firestoreHelper.addBooking(booking)
                .addOnSuccessListener {
                    sendBookingConfirmation(booking)
                    navigateToSuccess()
                }
        } else {
            showConflictError()
        }
    }
}
```

### 6. Patient Profile Management

**Create:** `PatientProfileFragment.kt`

**Features:**
- View and edit patient profile
- Update contact information
- Manage notification preferences
- View appointment history
- Account settings

**Firebase Integration:**
```kotlin
// Update patient profile
private fun updatePatientProfile(updatedPatient: Patient) {
    firestoreHelper.updatePatient(updatedPatient.uid, updatedPatient.toMap())
        .addOnSuccessListener {
            Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}
```

## Required FirestoreHelper Extensions

Add the following methods to `FirestoreHelper.kt`:

```kotlin
// Patient-specific methods
fun getPatientById(patientId: String): Task<DocumentSnapshot> {
    return patientsCollection.document(patientId).get()
}

fun updatePatient(patientId: String, updates: Map<String, Any>): Task<Void> {
    return patientsCollection.document(patientId).update(updates)
}

fun getBookingsForPatient(patientId: String): Task<QuerySnapshot> {
    return bookingsCollection.whereEqualTo("patient_name", patientId).get()
}

fun getUpcomingBookingsForPatient(patientId: String, fromDate: String): Task<QuerySnapshot> {
    return bookingsCollection
        .whereEqualTo("patient_name", patientId)
        .whereGreaterThanOrEqualTo("date", fromDate)
        .orderBy("date")
        .orderBy("start_time")
        .get()
}

// Doctor discovery methods
fun getAllDoctors(): Task<QuerySnapshot> {
    return doctorsCollection.get()
}

fun searchDoctors(searchTerm: String): Task<QuerySnapshot> {
    // Implement search functionality
    return doctorsCollection
        .orderBy("firstName")
        .startAt(searchTerm)
        .endAt(searchTerm + '\uf8ff')
        .get()
}

fun getDoctorsBySpecialization(specialization: String): Task<QuerySnapshot> {
    return doctorsCollection.whereEqualTo("specialization", specialization).get()
}

// Enhanced booking methods
fun validateBookingSlot(booking: Booking): Task<Boolean> {
    // Check for conflicts with existing bookings
    return bookingsCollection
        .whereEqualTo("doctor_id", booking.doctor_id)
        .whereEqualTo("date", booking.date)
        .get()
        .continueWith { task ->
            if (task.isSuccessful) {
                val existingBookings = task.result.documents
                // Logic to check for time conflicts
                !hasTimeConflict(booking, existingBookings)
            } else {
                false
            }
        }
}
```

## UI Improvements Required

### 1. Navigation Structure
- Add bottom navigation to patient activity
- Include: Home, Doctors, Appointments, Profile
- Update navigation graph

### 2. Modern UI Components
- Replace ListViews with RecyclerViews
- Add Material Design cards
- Implement search bars with Material TextInputLayout
- Add floating action buttons for quick actions

### 3. Data Classes
- Create `AppointmentDetails` for enhanced appointment view
- Create `ServiceWithDoctor` for enhanced service browsing
- Create `DoctorWithServices` for doctor profiles

## Implementation Priority

### Phase 1: Core Navigation and Model Updates
1. **Connect existing flows**: Add navigation from `MainPatientActivity` to `PatientBrowseServicesFragment`
2. **Update data models**: Replace `DoctorsModel` with Firebase `Doctor` model in `MainViewModel`
3. **Simplify profile management**: Replace edit request system with direct patient profile updates
4. **Add bottom navigation**: Convert to fragment-based architecture with Home, Doctors, Services, Appointments, Profile

### Phase 2: Enhanced Discovery and UI
1. Create `PatientHomeFragment` with basic dashboard  
2. Update `PatientBrowseServicesFragment` with enhanced UI
3. Add search and filter functionality for doctors and services
4. Create `PatientDoctorsFragment` for enhanced doctor discovery

### Phase 3: Appointment Management
1. Create `PatientAppointmentsFragment` for viewing patient's bookings
2. Enhanced booking flow with conflict checking in existing fragments
3. Add appointment cancellation/rescheduling functionality

### Phase 4: Advanced Features
1. Enhanced patient profile management (replace edit request system)
2. Notification system integration
3. Rating and review system for completed appointments
4. Payment integration placeholder

### Critical Integration Points:
- **MainPatientActivity → PatientBrowseServicesFragment**: Add navigation when user selects a category or doctor
- **Doctor cards → Service selection**: Navigate from doctor selection to their available services
- **Profile management**: Simplify the current edit request workflow to direct updates
- **Category system**: Align category-based navigation with actual service offerings

This comprehensive integration plan will transform the patient-side from basic navigation screens into a fully functional patient portal with modern UI and complete Firebase integration.
