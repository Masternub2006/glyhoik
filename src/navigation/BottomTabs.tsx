import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import HistoryScreen from '../screens/history/HistoryScreen';
import SettingsScreen from '../screens/settings/SettingsScreen';
import BellIcon from '../components/icons/BellIcon';
//import * as Haptics from 'expo-haptics';
import ReactNativeHapticFeedback from "react-native-haptic-feedback"
import SettingsIcon from '../components/icons/SettingsIcon';
import { Platform } from 'react-native';

const Tab = createBottomTabNavigator();

export default function BottomTabs() {
  return (
    <Tab.Navigator
        screenOptions={({ route }) => ({
        headerTitleAlign: 'center',
        tabBarIcon: ({ focused, color }) => {
            //Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light)
            ReactNativeHapticFeedback.trigger("impactLight",
            {
              enableVibrateFallback: false,
              ignoreAndroidSystemSettings: false,
            });
            if (route.name === 'Уведомления') {
              return <BellIcon active={focused} color={color} />;
            } else if (route.name === 'Настройки') {
              return <SettingsIcon active={focused} color={color} />;
            }
        },
        tabBarActiveTintColor: '#000000',
        tabBarInactiveTintColor: '#8C8C8C',
        tabBarLabelStyle: { fontSize: 15, fontWeight: '600', marginTop: 5,  },
        tabBarStyle: {
            height: Platform.OS === 'ios' ? 105 : 115,
            paddingBottom: 8,
            paddingTop: 10,
            paddingHorizontal: 10
        },
        })}
    >
    <Tab.Screen
      name="Уведомления"
      component={HistoryScreen}
      options={{ title: 'Уведомления' }}
    />
    <Tab.Screen
      name="Настройки"
      component={SettingsScreen}
      options={{ title: 'Настройки' }}
    />
  </Tab.Navigator>
  );
}