import React from "react";
import { Text, View, StyleSheet, ScrollView, ActivityIndicator, TouchableOpacity, NativeModules, Alert } from "react-native";
import { useESPConnection } from "../../context/ESPConnectionContext";
import { useNavigation } from "@react-navigation/native";

const { ForegroundServiceModule } = NativeModules;

export default function FirstScreen() {
  const { isConnected } = useESPConnection();
  const navigation = useNavigation();

  const handleHistoryPress = () => {
    // @ts-ignore - игнорируем типы для навигации
    navigation.navigate("History");
  };

  return (
    <ScrollView contentContainerStyle={styles.scrollArea}>
      <Text style={styles.welcomeTitle}>Добро пожаловать!</Text>
      <Text style={styles.welcomeText}>
        Перед началом работы необходимо подключить ваше устройство.
      </Text>

      {!isConnected && (
        <View style={styles.retryButton}>
          <ActivityIndicator size="large" color="#000" />
          <Text style={{ marginTop: 10 }}>Идёт попытка подключения...</Text>
        </View>
      )}

      {/* Кнопка для перехода к истории */}
      <TouchableOpacity style={styles.historyButton} onPress={handleHistoryPress}>
        <Text style={styles.historyButtonText}>Просмотреть историю</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  scrollArea: {
    flexGrow: 1,
    justifyContent: "center",
    alignItems: "center",
    paddingBottom: 40,
  },
  welcomeTitle: {
    fontSize: 27,
    fontWeight: "bold",
    textAlign: "center",
  },
  welcomeText: {
    fontSize: 16,
    fontWeight: "bold",
    textAlign: "center",
    marginVertical: 20,
    color: "#585858",
  },
  retryButton: {
    alignItems: "center",
    marginTop: 20,
  },
  testButton: {
    backgroundColor: "#007AFF",
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 8,
    marginTop: 30,
  },
  testButtonText: {
    color: "white",
    fontSize: 16,
    fontWeight: "600",
  },
  permissionButton: {
    backgroundColor: "#4CAF50", 
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 8,
    marginTop: 20,
  },
  permissionButtonText: {
    color: "white",
    fontSize: 16,
    fontWeight: "600",
  },
  historyButton: {
    backgroundColor: "#6C757D",
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 8,
    marginTop: 20,
  },
  historyButtonText: {
    color: "white",
    fontSize: 16,
    fontWeight: "600",
  },
});
