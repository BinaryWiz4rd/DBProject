const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendHardcodedNotification = functions.https.onRequest(
  async (req, res) => {
    console.log("Function called - sendHardcodedNotification");
    console.log("Request method:", req.method);
    console.log("Request headers:", req.headers);
    
    // Validate that admin is initialized
    console.log("Firebase Admin initialized:", !!admin.app());
    console.log("Messaging available:", !!admin.messaging);

    const fcmToken = "eXGh9kjgQAmSRTikLE2jlE:APA91bF5I8yxm8ml7dwMv-ErpuXfgF"+
      "AHu7q7Z25D1ejLGtuaV10t6BbpQOnT7_iB_o-C2Ei3X-bDtmApTWev7Z9o2bwouDw51v"+
      "amN4GZbBTQKYdU_LH-3-0";
    
    console.log("FCM Token (first 20 chars):", fcmToken.substring(0, 20));
    console.log("FCM Token length:", fcmToken.length);

    const message = {
      notification: {
        title: "Test Notification",
        body: "This is a hardcoded notification for testing purposes."
      },
      token: fcmToken,
      // Add android-specific config for default notifications
      android: {
        notification: {
          channelId: "default",
          defaultSound: true,
          defaultVibrateTimings: true,
          defaultLightSettings: true,
          priority: "high"
        }
      },
      // Add iOS-specific config for default notifications
      apns: {
        payload: {
          aps: {
            alert: {
              title: "Test Notification",
              body: "This is a hardcoded notification for testing purposes."
            },
            sound: "default",
            badge: 1
          }
        }
      }
    };

    console.log("Message payload:", JSON.stringify(message, null, 2));
    console.log("Attempting to send notification...");

    try {
      const response = await admin.messaging().send(message);
      console.log("Successfully sent message:", response);
      console.log("Message ID:", response);
      res.status(200).send("Notification sent successfully!");
    } catch (error) {
      console.error("Error sending message:", error);
      console.error("Error code:", error.code);
      console.error("Error message:", error.message);
      console.error("Error details:", error.details);
      res.status(500).send("Error sending notification: " + error.message);
    }
  }
);