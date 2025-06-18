const {onSchedule} = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");
admin.initializeApp();
const logger = require("firebase-functions/logger");

// Appointment Reminder Function
exports.appointmentReminder = onSchedule("every 5 minutes", async () => {
  const db = admin.firestore();
  const now = Date.now();
  const reminderTime = now + 15 * 60 * 1000; // 15 minutes from now

  const snapshot = await db
    .collection("appointments")
    .where("dateTime", ">=", new Date(now).toISOString())
    .where("dateTime", "<=", new Date(reminderTime).toISOString())
    .get();

  const sendPromises = [];

  snapshot.forEach(doc => {
    const appointment = doc.data();

    if (!appointment.doctorId) return; // skip if no doctor reference
    const doctorRef = db.collection("doctors").doc(appointment.doctorId);

    sendPromises.push(
      doctorRef.get().then(doctorDoc => {
        if (!doctorDoc.exists) return;

        const token = doctorDoc.data().fcmToken;
        if (!token) return;

        const payload = {
          notification: {
            title: "Upcoming Appointment",
            body:
              "You have an appointment with " +
              appointment.patientName +
              " at " +
              new Date(appointment.dateTime).toLocaleTimeString() +
              "."
          }
        };

        return admin.messaging().sendToDevice(token, payload);
      })
    );
  });

  await Promise.all(sendPromises);

  logger.info(
    "Appointment reminders sent for: " +
      new Date(now).toLocaleString() +
      " to " +
      new Date(reminderTime).toLocaleString()
  );

  return null;
});