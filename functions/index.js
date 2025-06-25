const {onSchedule} = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");
admin.initializeApp();
exports.sendAppointmentReminders = onSchedule({
  schedule: "every 5 minutes",
  timeZone: "Europe/Warsaw"
}, async () => {
  const db = admin.firestore();
  const now = admin.firestore.Timestamp.now();
  const fifteenMinutesFromNow = new Date(now.toMillis() + 15 * 60 * 1000);
  console.log("Current time:", now.toDate());
  console.log("Fifteen minutes from now:", fifteenMinutesFromNow);
  try {
    const currentDateString = now.toDate().toISOString().split("T")[0];
    const currentTime = now.toDate();
    const currentTimeHM = currentTime.getHours().toString().padStart(2, "0") + 
      ":" + currentTime.getMinutes().toString().padStart(2, "0");
    const fifteenMinTime = new Date(currentTime.getTime() + 15 * 60 * 1000);
    const fifteenMinTimeHM = fifteenMinTime.getHours().toString()
      .padStart(2, "0") + ":" + fifteenMinTime.getMinutes().toString()
      .padStart(2, "0");
    
    console.log("Current time (HH:MM):", currentTimeHM);
    console.log("Fifteen minutes from now (HH:MM):", fifteenMinTimeHM);
    
    // Query only by date first, then filter by time in memory
    const bookingsSnapshot = await db.collection("bookings")
      .where("date", "==", currentDateString)
      .get();
    
    // Filter bookings within the time range
    const upcomingBookings = [];
    bookingsSnapshot.forEach(doc => {
      const booking = doc.data();
      const startTime = booking.start_time; // This is in "HH:MM" format
      if (startTime >= currentTimeHM && startTime <= fifteenMinTimeHM) {
        upcomingBookings.push({id: doc.id, ...booking});
      }
    });
    
    console.log("Found " + upcomingBookings.length + " upcoming appointments.");
    if (upcomingBookings.length === 0) {
      console.log("No upcoming appointments found.");
      return null;
    }
    const notifications = [];
    for (const booking of upcomingBookings) {
      console.log("Processing booking:", booking);
      const doctorId = booking.doctor_id;
      const doctorSnapshot = await db.collection("doctors").doc(doctorId)
        .get();
      if (!doctorSnapshot.exists) {
        console.log("Doctor with ID " + doctorId + " not found.");
        continue;
      }
      const doctorData = doctorSnapshot.data();
      const fcmToken = doctorData.fcmToken;
      console.log("Found FCM token for doctor " + doctorId + ": " + fcmToken);
      const message = {
        notification: {
          title: "Appointment Reminder",
          body: "You have an appointment scheduled in 15 minutes with " +
            booking.patient_name + "."
        },
        token: fcmToken
      };
      notifications.push(admin.messaging().send(message));
    }
    const responses = await Promise.all(notifications);
    console.log("Notifications sent successfully:", responses);
  } catch (error) {
    console.error("Error sending notifications:", error);
  }
});