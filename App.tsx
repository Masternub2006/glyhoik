import React from "react";
import { NavigationContainer } from "@react-navigation/native";
import { ESPConnectionProvider, useESPConnection } from "./src/context/ESPConnectionContext";
import BottomTabs from "./src/navigation/BottomTabs";
import AppNavigator from "./src/navigation/AppNavigator";
import { configureNotifications } from "./src/utils/notifications";

function Main() {
  const { isConnected } = useESPConnection();

  return isConnected ? (
    <NavigationContainer>
      <BottomTabs />
    </NavigationContainer>
  ) : (
    <NavigationContainer>
      <AppNavigator />
    </NavigationContainer>
  );
}

export default function App() {
  React.useEffect(() => {
    console.log("[App] configureNotifications called");
    configureNotifications();
  }, []);

  return (
    <ESPConnectionProvider>
      <Main />
    </ESPConnectionProvider>
  );
}
