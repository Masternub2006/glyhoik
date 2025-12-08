import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import FirstScreen from '../screens/first/FirstScreen';
import HistoryScreen from '../screens/history/HistoryScreen';

const Stack = createNativeStackNavigator();

export default function AppNavigator() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}
    >
      <Stack.Screen name="Connection" component={FirstScreen} />
      <Stack.Screen 
        name="History" 
        component={HistoryScreen}
        options={{
          headerShown: true,
          title: 'История',
          headerBackTitle: 'Назад',
        }}
      />
    </Stack.Navigator>
  );
}
