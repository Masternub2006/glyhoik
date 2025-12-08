import { useEffect, useState, useCallback, JSX } from "react";
import { ScrollView, View, StyleSheet, Text } from "react-native";
import { useFocusEffect } from "@react-navigation/native";
import { HistoryBlock } from "../../components/HistoryBlock";
import { useESPConnection } from "../../hooks/useESPConnection";
import { History } from "../../types/types";
import { transformHistory } from "../../utils/historyUtils";

const { DatabaseModule } = require("react-native").NativeModules;
const DEVICE_ID = "ESP1337";

export default function HistoryScreen(): JSX.Element {
  const [history, setHistory] = useState<History[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { lastMessage } = useESPConnection();

  const loadLocalHistory = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      
      if (DatabaseModule && DatabaseModule.getHistory) {
        const local = await DatabaseModule.getHistory(DEVICE_ID);
        const items = local.map(({ type, message, timestamp }: any) => ({
          type,
          eventMessage: message,
          timestamp: new Date(timestamp).toISOString(), 
        }));
        setHistory(transformHistory(items));
      } else {
        throw new Error("DatabaseModule недоступен");
      }
    } catch (error) {
      console.error("Ошибка загрузки истории:", error);
      setError("Не удалось загрузить историю");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const initialize = async () => {
      try {
        if (DatabaseModule && DatabaseModule.initDB) {
          await DatabaseModule.initDB();
        }
        await loadLocalHistory();
      } catch (error) {
        console.error("Ошибка инициализации базы:", error);
        setError("Ошибка инициализации базы данных");
      }
    };
    initialize();
  }, []);

  useFocusEffect(
    useCallback(() => {
      loadLocalHistory();
    }, [loadLocalHistory])
  );

  useEffect(() => {
    if (lastMessage) {
      loadLocalHistory();
    }
  }, [lastMessage, loadLocalHistory]);

  if (loading) {
    return (
      <View style={styles.content}>
        <View style={styles.centerContainer}>
          <Text style={styles.loadingText}>Загрузка истории...</Text>
        </View>
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.content}>
        <View style={styles.centerContainer}>
          <Text style={styles.errorText}>{error}</Text>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.content}>
      <ScrollView contentContainerStyle={styles.container}>
        <View>
          {history.length === 0 ? (
            <View style={styles.centerContainer}>
              <Text style={styles.emptyText}>История пуста</Text>
              <Text style={styles.emptySubtext}>
                Уведомления от ESP устройств появятся здесь
              </Text>
            </View>
          ) : (
            history.map((item, index) => (
              <HistoryBlock key={index} items={item.items} date={item.date} />
            ))
          )}
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    paddingHorizontal: 10,
    paddingBottom: 10,
    gap: 12,
  },
  content: {
    flex: 1,
    backgroundColor: "#F6F6F6",
  },
  centerContainer: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    paddingVertical: 50,
  },
  loadingText: {
    fontSize: 16,
    color: "#666",
    textAlign: "center",
  },
  errorText: {
    fontSize: 16,
    color: "#FF4C4C",
    textAlign: "center",
  },
  emptyText: {
    fontSize: 18,
    color: "#666",
    textAlign: "center",
    marginBottom: 8,
  },
  emptySubtext: {
    fontSize: 14,
    color: "#999",
    textAlign: "center",
  },
});