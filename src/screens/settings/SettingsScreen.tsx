import { JSX, useState, useCallback, useEffect } from "react";
import { View, StyleSheet, Text, Alert, Button, Switch, TouchableOpacity, ScrollView } from "react-native";
import { NativeModules } from "react-native";
import { DeviceBlock } from "../../components/DeviceBlock";

const { DatabaseModule, NotificationSettingsModule } = NativeModules;
const DEVICE_ID = "ESP1337";

export default function SettingsScreen(): JSX.Element {
  const [isClearing, setIsClearing] = useState(false);
  const [volumePercentage, setVolumePercentage] = useState(50);
  const [vibrationEnabled, setVibrationEnabled] = useState(true);
  const [enableSound, setEnableSound] = useState(true);
  const [enableVibration, setEnableVibration] = useState(true);

  useEffect(() => {
    loadCurrentSettings();
  }, []);

  const loadCurrentSettings = async () => {
    try {
      if (NotificationSettingsModule?.getCurrentVolume) {
        const volumeInfo = await NotificationSettingsModule.getCurrentVolume();
        setVolumePercentage(volumeInfo.volumePercentage);
      }
      
      if (NotificationSettingsModule?.getVibrationEnabled) {
        const enabled = await NotificationSettingsModule.getVibrationEnabled();
        setVibrationEnabled(enabled);
      }
      
      if (NotificationSettingsModule?.forceUpdateNotificationChannel) {
        await NotificationSettingsModule.forceUpdateNotificationChannel();
      }
    } catch (error) {
      console.error("Ошибка загрузки настроек:", error);
    }
  };

  const clearHistory = useCallback(async () => {
    Alert.alert(
      "Очистить историю",
      "Вы уверены, что хотите удалить всю историю уведомлений? Это действие нельзя отменить.",
      [
        { text: "Отмена", style: "cancel" },
        {
          text: "Очистить",
          style: "destructive",
          onPress: async () => {
            try {
              setIsClearing(true);
              if (DatabaseModule && DatabaseModule.clearAllHistory) {
                const deletedRows = await DatabaseModule.clearAllHistory();
                Alert.alert(
                  "История очищена",
                  `Удалено ${deletedRows} записей из истории уведомлений.`
                );
              } else {
                throw new Error("DatabaseModule недоступен");
              }
            } catch (error) {
              console.error("Ошибка очистки истории:", error);
              Alert.alert(
                "Ошибка",
                `Не удалось очистить историю: ${error}`
              );
            } finally {
              setIsClearing(false);
            }
          },
        },
      ]
    );
  }, []);

  const updateNotificationSettings = async () => {
    try {
      if (NotificationSettingsModule?.setNotificationChannelSettings) {
        await NotificationSettingsModule.setNotificationChannelSettings(
          "esp-channel",
          4, 
          enableSound,
          enableVibration
        );
        
        
        if (NativeModules.ForegroundServiceModule?.updateServiceNotification) {
          await NativeModules.ForegroundServiceModule.updateServiceNotification();
        }
      }
    } catch (error) {
      console.error("Ошибка обновления настроек уведомлений:", error);
    }
  };

  const clamp = (value: number, min: number, max: number) => Math.max(min, Math.min(max, value));

  const changeVolume = async (newValue: number) => {
    const clampedValue = clamp(newValue, 0, 100);
    setVolumePercentage(clampedValue);
    try {
      if (NotificationSettingsModule?.setNotificationVolume) {
        await NotificationSettingsModule.setNotificationVolume(clampedValue);
      }
      
     
      if (NotificationSettingsModule?.forceUpdateNotificationChannel) {
        await NotificationSettingsModule.forceUpdateNotificationChannel();
      }
    } catch (error) {
      console.error("Ошибка изменения громкости:", error);
    }
  };

  const changeVibrationEnabled = async (enabled: boolean) => {
    setVibrationEnabled(enabled);
    try {
      if (NotificationSettingsModule?.setVibrationEnabled) {
        await NotificationSettingsModule.setVibrationEnabled(enabled);
      }
      
      
      if (NotificationSettingsModule?.forceUpdateNotificationChannel) {
        const newChannelId = await NotificationSettingsModule.forceUpdateNotificationChannel();
        console.log("Новый канал уведомлений создан:", newChannelId);
      }
      
      if (NotificationSettingsModule?.testVibration) {
        await NotificationSettingsModule.testVibration({
          duration: 500,
          intensity: enabled ? 100 : 0
        });
      }
    } catch (error) {
      console.error("Ошибка изменения вибрации:", error);
    }
  };

  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>

<View style={styles.section}>
        <Text style={styles.sectionTitle}>Управление данными</Text>
        <View style={styles.buttonContainer}>
          <Button
            title={isClearing ? "Очистка..." : "Очистить историю уведомлений"}
            onPress={clearHistory}
            disabled={isClearing}
            color="#FF4C4C"
          />
        </View>
      </View>
      
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Настройки уведомлений</Text>


        <View style={styles.settingRow}>
          <Text style={styles.settingLabel}>Громкость уведомлений: {volumePercentage}%</Text>
          <View style={styles.inlineControls}>
            <TouchableOpacity
              style={styles.controlButton}
              onPress={() => changeVolume(volumePercentage - 10)}
              activeOpacity={0.8}
            >
              <Text style={styles.controlButtonText}>-</Text>
            </TouchableOpacity>
            <Text style={styles.valueText}>{volumePercentage}%</Text>
            <TouchableOpacity
              style={styles.controlButton}
              onPress={() => changeVolume(volumePercentage + 10)}
              activeOpacity={0.8}
            >
              <Text style={styles.controlButtonText}>+</Text>
            </TouchableOpacity>
          </View>
        </View>

        
        
      </View>
      
      
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    paddingHorizontal: 10,
    paddingVertical: 10,
    gap: 12,
    flex: 1,
    backgroundColor: "#F6F6F6",
  },
  section: {
    backgroundColor: '#FFFFFF',
    borderRadius: 15,
    borderWidth: 2,
    borderColor: '#EBEBEB',
    padding: 20,
    marginBottom: 10,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 15,
  },
  settingRow: {
    marginBottom: 20,
  },
  settingLabel: {
    fontSize: 16,
    color: '#333',
    marginBottom: 12,
    fontWeight: '500',
  },
  inlineControls: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 10,
    gap: 10,
  },
  controlButton: {
    backgroundColor: '#6C757D',
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 8,
    minWidth: 80,
    alignItems: 'center',
  },
  controlButtonActive: {
    backgroundColor: '#007AFF',
  },
  controlButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
  valueText: {
    fontSize: 16,
    color: '#495057',
    fontWeight: '600',
    minWidth: 60,
    textAlign: 'center',
  },
  buttonContainer: {
    marginBottom: 10,
  },
  sectionDescription: {
    fontSize: 14,
    color: '#666',
    fontStyle: 'italic',
  },
  sliderContainer: {
    marginTop: 10,
    alignItems: 'center',
  },
  sliderTrack: {
    width: 250,
    height: 8,
    backgroundColor: '#E9ECEF',
    borderRadius: 4,
    position: 'relative',
    elevation: 5,
  },
  sliderFill: {
    position: 'absolute',
    height: '100%',
    backgroundColor: '#007AFF',
    borderRadius: 4,
    left: 0,
    top: 0,
  },
  sliderThumb: {
    position: 'absolute',
    width: 20,
    height: 20,
    backgroundColor: '#007AFF',
    borderRadius: 10,
    top: -6,
    transform: [{ translateX: -10 }],
    elevation: 5,
  },
});
