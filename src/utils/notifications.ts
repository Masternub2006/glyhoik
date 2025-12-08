import { Platform } from "react-native";
import PushNotification, { Importance } from "react-native-push-notification";


export const configureNotifications = () => {
  if (Platform.OS === "android") {
    PushNotification.createChannel(
      {
        channelId: "esp-channel", 
        channelName: "ESP Notifications",
        channelDescription: "Канал для уведомлений ESP Foreground Service",
        importance: Importance.HIGH, 
        vibrate: true, 
        soundName: "sound", 
        playSound: true, 
      },
      (created) => {
        console.log("Notification channel created:", created);
      }
    );
  }
};

export const showLocalNotification = (title: string, message: string) => {
  PushNotification.localNotification({
    channelId: "esp-channel", 
    title,
    message,
    playSound: true, 
    vibrate: true, 
    priority: "high",
    importance: "high",
  });
};

export const forceUpdateNotificationChannel = () => {
  if (Platform.OS === "android") {
    PushNotification.deleteChannel("esp-channel");
    PushNotification.createChannel(
      {
        channelId: "esp-channel",
        channelName: "ESP Notifications",
        channelDescription: "Канал для уведомлений ESP Foreground Service",
        importance: Importance.HIGH,
        vibrate: true,
        soundName: "sound", 
        playSound: true,
      },
      (created) => {
        console.log("Notification channel force updated:", created);
      }
    );
  }
};


