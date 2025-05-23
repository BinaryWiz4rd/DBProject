# Firebase Integration Plan

This document provides information about the Firebase integration in the project, focusing on completed features and important Firebase structures.

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

## 3. Completed Features

### 3.1 Firebase Helper

- `FirestoreHelper.kt` encapsulates Firestore CRUD operations.

### 3.2 Doctor's View

- **Schedule Management**: `DoctorScheduleFragment.kt` displays bookings for the current doctor.
- **Service Management**: `DoctorServicesFragment.kt` allows doctors to manage their services.
- **Availability Management**: `DoctorAvailabilityFragment.kt` allows doctors to manage their availability.
- **Calendar**: `DoctorCalendarFragment.kt` provides a calendar view of appointments.

### 3.3 Patient's View

- **Service Browsing**: `PatientBrowseServicesFragment.kt` allows patients to view available services.
- **Availability Viewing**: `PatientServiceAvailabilityFragment.kt` allows patients to view doctor's availability.
- **Booking Confirmation**: `ConfirmBookingFragment.kt` allows patients to confirm bookings.

## 4. Calendar Functionality

The calendar functionality uses the following Firestore structure:

- `doctorSettings/{doctorId}`: Contains doctor-specific settings like working hours
  ```
  {
    "startHour": 8,
    "endHour": 16
  }
  ```

- `doctorCalendars/{doctorId}/dates/{date}/appointments/{appointmentId}`: Hierarchical structure for appointments
  ```
  {
    "patientName": "John Doe",
    "patientId": "patient_uid",
    "timeSlot": "10:00",
    "endTime": "10:30",
    "notes": "Follow-up visit",
    "serviceId": "service_id",
    "serviceName": "Cardiac Checkup",
    "servicePrice": 100,
    "serviceDuration": 30
  }
  ```

- Integration with the `bookings` collection to maintain a flat structure for querying patient appointments

## 5. Firestore Security Rules

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow users to read and update their own patient profiles
    match /patients/{patientId} {
      allow read, write: if request.auth != null && request.auth.uid == patientId;
    }
    
    // Allow doctors to read and update their own doctor profiles
    match /doctors/{doctorId} {
      allow read, write: if request.auth != null && request.auth.uid == doctorId;
    }
    
    // Allow doctors to manage their own settings (including working hours)
    match /doctorSettings/{doctorId} {
      allow read, write: if request.auth != null && request.auth.uid == doctorId;
    }
    
    // Allow doctors to manage their own calendar
    match /doctorCalendars/{doctorId} {
      allow read, write: if request.auth != null && request.auth.uid == doctorId;
    }
    
    // Allow access to specific date collections within doctorCalendars
    match /doctorCalendars/{doctorId}/dates/{dateId} {
      allow read, write: if request.auth != null && request.auth.uid == doctorId;
    }
    
    // Allow access to appointments within specific dates
    match /doctorCalendars/{doctorId}/dates/{dateId}/appointments/{appointmentId} {
      allow read, write: if request.auth != null && request.auth.uid == doctorId;
    }
    
    // Allow access to categories
    match /categories/{categoryId} {
      allow read: if request.auth != null;
    }
    
    // Allow admins to read and write all data
    match /{document=**} {
      allow read, write: if request.auth != null && 
        exists(/databases/$(database)/documents/admins/$(request.auth.uid));
    }
    // Allow users to read and update their own patient profiles
    match /patients/{patientId} {
      allow read, write: if request.auth != null && request.auth.uid == patientId;
    }
    // Allow doctors to read patient data for scheduling purposes
    match /patients/{patientId} {
      allow read: if request.auth != null && 
        exists(/databases/$(database)/documents/doctors/$(request.auth.uid));
    }
    // Allow everyone to read services
    match /services/{serviceId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
        exists(/databases/$(database)/documents/doctors/$(request.auth.uid));
    }
    
    // Allow availability to be read by patients and managed by the relevant doctor
    match /availability/{availId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
        request.resource.data.doctor_id == request.auth.uid;
    }
    
    // Allow bookings to be read by the relevant doctor and patient
    // and created/updated by patients
    match /bookings/{bookingId} {
      allow read: if request.auth != null && 
        (resource.data.doctor_id == request.auth.uid || 
         resource.data.patient_name == request.auth.uid);
      allow create: if request.auth != null;
      allow update: if request.auth != null && 
        (resource.data.doctor_id == request.auth.uid || 
         resource.data.patient_name == request.auth.uid);
    }
  }
}
```

## 6. Patient Data Model

The `Patient` data model is defined as follows:

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

## 7. Admin Panel Overhaul (Completed)

This section outlines the admin panel functionality that provides administrators with enhanced management capabilities.

### 7.1 View and Manage All Appointments (Completed)
- Created a comprehensive appointment management interface with filtering and sorting options
- Implemented appointment editing and deletion with proper Firebase integration
- Added cross-reference display of doctor, patient, and service details
- Ensured synchronization between bookings and doctorCalendars collections

### 7.2 Create and Register New Doctors (Completed)
- Implemented a doctor registration form with validation for all required fields
- Created two-step registration process (Authentication + Firestore)
- Added automatic setup of default doctor settings and working hours
- Ensured data consistency across Firebase services

### 7.3 View and Manage All Users (Patients) (Completed)
- Developed patient management interface with search and sorting capabilities
- Created UI components for displaying and interacting with patient data
- Implemented CRUD operations for patient management
- Integrated Firestore for real-time data synchronization

### 7.4 View and Manage All Doctors (Completed)
- Built doctor management interface with search and sorting functionality
- Implemented comprehensive doctor detail display with action buttons
- Added editing and deletion capabilities with proper validation
- Created navigation to doctor-specific views (appointments, services)
- Ensured cascade deletion of related data when removing doctors

### 7.5 Admin Panel Navigation and Structure (Implemented)
- Implemented BottomNavigationView in MainAdminActivity for intuitive navigation
- Created modular fragments for each admin functionality
- Added fragment transaction system for seamless navigation

## 8. Doctor UI/UX Modernization Plan

This section outlines the strategy for modernizing the doctor interface to create a more engaging, professional, and user-friendly experience.

### 8.1 Visual Design Improvements

#### 8.1.1 Color Scheme and Typography
- Implement a consistent, medical-friendly color palette (blues, greens, neutral tones)
- Replace current colors with a professional color scheme:
  - Primary: #1976D2 (medical blue)
  - Secondary: #00BFA5 (mint green)
  - Accent: #FF5722 (attention color for important actions)
  - Background gradient: Light blue to white
- Update typography to improve readability:
  - Use sans-serif fonts (Roboto or Google Sans)
  - Implement proper text hierarchy with distinct sizes for headers, subheaders, and body text
  - Add consistent letter spacing and line heights

#### 8.1.2 Component Styling
- Apply consistent corner radius (8dp) to all cards, buttons, and input fields
- Add subtle shadows and elevation to create depth
- Implement transition animations between screens and for interactive elements
- Add skeleton loading screens instead of traditional progress indicators

### 8.2 Layout and Navigation Improvements

#### 8.2.1 Doctor Home Fragment
- Redesign the dashboard with appointment cards and key statistics
- Add visual graphs for weekly appointment summary
- Implement quick action buttons for common tasks
- Replace text-based search with a modern search bar with voice input option

#### 8.2.2 Doctor Calendar Fragment
- Replace standard CalendarView with a custom, more visually appealing calendar
- Implement day, week, and month view options
- Add color-coding for different appointment types
- Improve appointment creation flow with a bottom sheet dialog
- Add drag-and-drop functionality for rescheduling

#### 8.2.3 Doctor Schedule Fragment
- Implement a timeline view for today's appointments
- Add patient photos/avatars to appointment items
- Create collapsible appointment cards with patient details
- Add status indicators (confirmed, pending, completed)

#### 8.2.4 Doctor Services Fragment
- Redesign service cards with visuals and clearer pricing
- Implement a grid layout for services instead of list view
- Add animations for service creation and editing
- Implement swipe actions for quick editing/deletion

### 8.3 Interaction and Usability Improvements

#### 8.3.1 Gesture Support
- Add pull-to-refresh on all list views
- Implement swipe actions for common tasks
- Add pinch-to-zoom on the calendar view
- Support long-press contextual menus

#### 8.3.2 Feedback and Notifications
- Add haptic feedback for interactions
- Implement in-app notifications for appointment changes
- Create toast messages with custom styling
- Add animated success/error states

#### 8.3.3 Accessibility Improvements
- Ensure proper content descriptions for all UI elements
- Implement high contrast mode
- Support text scaling
- Add voice commands for common actions

### 8.4 Implementation Plan

#### 8.4.1 Phase 1: Foundation
- Create a comprehensive styles.xml with the new color scheme and text styles
- Update existing layouts to use the new styles
- Implement consistent card and button styling

#### 8.4.2 Phase 2: Component Updates
- Redesign the doctor home dashboard
- Update the calendar view with modern styling
- Improve service cards and list presentation

#### 8.4.3 Phase 3: Interaction Enhancements
- Add animations and transitions
- Implement gesture controls
- Create custom dialogs and bottom sheets
- Integrate loading states and feedback mechanisms

#### 8.4.4 Phase 4: Final Polish
- Conduct usability testing
- Optimize performance
- Add final design touches and animations
- Ensure consistency across all doctor screens
