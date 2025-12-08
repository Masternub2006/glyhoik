import React, { createContext, useContext, useEffect, useState } from "react";
import axios from "axios";
import { NativeModules, NativeEventEmitter, Alert, AppState } from "react-native";
import { configureNotifications, forceUpdateNotificationChannel } from "../utils/notifications";

const { ForegroundServiceModule } = NativeModules;

const eventEmitter = new NativeEventEmitter(ForegroundServiceModule);

type ConnectionMode = "router" | "ap" | "unknown";

type ESPConnectionContextType = {
  mode: ConnectionMode;
  localIP: string | null;
  socketUrl: string | null;
  isConnected: boolean;
  lastMessage: string | null;
};

export const ESPConnectionContext = createContext<ESPConnectionContextType | null>(null);

export const ESPConnectionProvider = ({ children }: { children: React.ReactNode }) => {
  const [mode, setMode] = useState<ConnectionMode>("unknown");
  const [localIP, setLocalIP] = useState<string | null>(null);
  const [socketUrl, setSocketUrl] = useState<string | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [lastMessage, setLastMessage] = useState<string | null>(null);

  useEffect(() => {
    configureNotifications();
    forceUpdateNotificationChannel();

    async function detectConnectionAndStartService() {
      if (!ForegroundServiceModule || !ForegroundServiceModule.startService) {
        return;
      }

      try {
        const res = await axios.get("https://www.kbkontur.ru/iot/search.php");
        if (res.data?.local_ip) {
          const ip = res.data.local_ip;
          setMode("router");
          setLocalIP(ip);
          const url = `ws://${ip}/ws`;
          setSocketUrl(url);
          await ForegroundServiceModule.startService("ESP Уведомления", "Подключение к ESP", url);
          return;
        }
        throw new Error("No local_ip in response");
      } catch (error) {
        const apIp = "192.168.4.1";
        setMode("ap");
        setLocalIP(apIp);
        const url = `ws://${apIp}/ws`;
        setSocketUrl(url);
        await ForegroundServiceModule.startService("ESP Уведомления", "Подключение к ESP (AP)", url);
      }
    }

    detectConnectionAndStartService();

    const handleAppStateChange = (nextAppState: string) => {
      if (nextAppState === 'active') {
        ForegroundServiceModule.startService("APP_FOREGROUND", "", "");
      } else if (nextAppState === 'background' || nextAppState === 'inactive') {
        ForegroundServiceModule.startService("APP_BACKGROUND", "", "");
      }
    };

    const subscription = AppState.addEventListener('change', handleAppStateChange);

    const statusSub = eventEmitter.addListener("WebSocketConnectionStatus", (status: boolean) => {
      setIsConnected(status);
    });
  
    const messageSub = eventEmitter.addListener("WebSocketMessage", (message: string) => {
      setLastMessage(message);
    });

    const errorSub = eventEmitter.addListener("WebSocketError", (errorMsg: string) => {
      Alert.alert("Ошибка подключения", errorMsg, [{ text: "OK" }]);
    });

    const alertSub = eventEmitter.addListener("ShowAlert", (alertData: string) => {
      const [title, message] = alertData.split("|");
      Alert.alert(title || "Данные устройства", message || alertData, [{ text: "OK" }]);
    });

    return () => {
      subscription.remove();
      statusSub.remove();
      messageSub.remove();
      errorSub.remove();
      alertSub.remove();
    };
  }, []);

  return (
    <ESPConnectionContext.Provider value={{ mode, localIP, socketUrl, isConnected, lastMessage }}>
      {children}
    </ESPConnectionContext.Provider>
  );
};

export const useESPConnection = () => {
  const context = useContext(ESPConnectionContext);
  if (!context) {
    throw new Error("useESPConnection must be used within ESPConnectionProvider");
  }
  return context;
};

