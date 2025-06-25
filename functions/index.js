const {onSchedule} = require("firebase-functions/v2/scheduler");
const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
admin.initializeApp();

const db = admin.firestore();

exports.sendAppointmentReminders = onSchedule({
  schedule: "every 5 minutes",
  timeZone: "Europe/Warsaw" // Your timezone
}, async () => {
  const now = admin.firestore.Timestamp.now();
  
  // Add 2 hours (7200000 ms) to account for UTC+2 timezone
  const nowWithOffset = new Date(now.toMillis() + 2 * 60 * 60 * 1000);
  const fifteenMinutesFromNow = new Date(
    nowWithOffset.getTime() + 15 * 60 * 1000
  );
  
  console.log("Current time (UTC):", now.toDate());
  console.log("Current time (UTC+2):", nowWithOffset);
  console.log("Fifteen minutes from now (UTC+2):", fifteenMinutesFromNow);
  
  try {
    const currentDateString = nowWithOffset.toISOString().split("T")[0];
    const currentTimeStr = nowWithOffset.getHours().toString()
      .padStart(2, "0") + ":" + nowWithOffset.getMinutes().toString()
      .padStart(2, "0");
    
    const fifteenMinTimeStr = fifteenMinutesFromNow.getHours().toString()
      .padStart(2, "0") + ":" + fifteenMinutesFromNow.getMinutes().toString()
      .padStart(2, "0");
    
    console.log("Current time (HH:MM, UTC+2):", currentTimeStr);
    console.log("Fifteen minutes from now (HH:MM, UTC+2):", fifteenMinTimeStr);
    
    const bookingsSnapshot = await db.collection("bookings")
      .where("date", "==", currentDateString)
      .get();
    
    const upcomingBookings = [];
    bookingsSnapshot.forEach(doc => {
      const booking = doc.data();
      if (booking.start_time >= currentTimeStr && 
          booking.start_time <= fifteenMinTimeStr) {
        upcomingBookings.push({id: doc.id, ...booking});
      }
    });
    
    console.log(`Found ${upcomingBookings.length} upcoming appointments.`);
    
    if (upcomingBookings.length === 0) {
      console.log("No appointments found in the next 15 minutes.");
      return null;
    }
    
    const notifications = [];
    for (const booking of upcomingBookings) {
      console.log("Processing booking:", booking.id);
      
      const doctorRef = db.collection("doctors").doc(booking.doctor_id);
      const doctorSnapshot = await doctorRef.get();
      
      if (!doctorSnapshot.exists) {
        console.log(`Doctor ${booking.doctor_id} not found.`);
        continue;
      }
      
      const doctorData = doctorSnapshot.data();
      if (!doctorData.fcmToken) {
        console.log(`No FCM token found for doctor ${booking.doctor_id}`);
        continue;
      }
      
      const message = {
        notification: {
          title: "Appointment Reminder",
          body: `You have an appointment in 15 minutes with ${
            booking.patient_name
          }.`
        },
        token: doctorData.fcmToken
      };
      
      notifications.push(admin.messaging().send(message));
      console.log(`Notification queued for doctor ${booking.doctor_id}`);
    }
    
    await Promise.all(notifications);
    console.log(`Successfully sent ${notifications.length} notifications.`);
    
  } catch (error) {
    console.error("Error in appointment reminder function:", error);
    throw error;
  }
});

exports.sendChatNotification = onDocumentCreated(
  "chats/{chatId}/messages/{messageId}",
  async event => {
    const messageData = event.data.data();
    if (!messageData) {
      console.log("No message data found");
      return null;
    }

    const {chatId} = event.params;
    const {senderId, text} = messageData;

    try {
      const chatDoc = await db.collection("chats").doc(chatId).get();
      if (!chatDoc.exists) {
        console.log(`Chat ${chatId} not found`);
        return null;
      }

      const chatData = chatDoc.data();
      const {participants, doctorId, patientId} = chatData;

      const recipientId = participants.find(id => id !== senderId);
      if (!recipientId) {
        console.log("No valid recipient found");
        return null;
      }

      // Determine recipient type (doctor or patient)
      const recipientType = recipientId === doctorId ? "doctors" : "patients";
      const recipientDoc = await db.collection(recipientType)
        .doc(recipientId).get();
      
      if (!recipientDoc.exists) {
        console.log(`Recipient ${recipientId} not found`);
        return null;
      }

      const recipientToken = recipientDoc.data().fcmToken;
      if (!recipientToken) {
        console.log(`No FCM token found for recipient ${recipientId}`);
        return null;
      }

      // Get sender name
      let senderName = "User";
      if (senderId === doctorId) {
        const doctorDoc = await db.collection("doctors").doc(doctorId).get();
        senderName = doctorDoc.data()?.name || "Doctor";
      } else if (senderId === patientId) {
        const patientDoc = await db.collection("patients")
          .doc(patientId).get();
        senderName = patientDoc.data()?.name || "Patient";
      }

      // Prepare notification payload
      const payload = {
        notification: {
          title: `New message from ${senderName}`,
          body: text.length > 100 ? text.substring(0, 97) + "..." : text,
          sound: "default"
        },
        data: {
          chatId,
          type: "NEW_MESSAGE",
          senderId
        },
        token: recipientToken
      };

      // Send notification
      await admin.messaging().send(payload);
      console.log(`Successfully sent notification to ${recipientId}`);
      
    } catch (error) {
      console.error("Error in chat notification function:", error);
      throw error;
    }
  }
);