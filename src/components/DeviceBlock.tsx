import { JSX } from "react";
import { Alert, TouchableOpacity, StyleSheet, View, Text, Platform, Vibration } from 'react-native';
//import * as Haptics from 'expo-haptics';
import ReactNativeHapticFeedback, {HapticFeedbackTypes,} from "react-native-haptic-feedback";
import { deleteHistory } from "../utils/database";

type ExpoImpact =
  | "Light"
  | "Medium"
  | "Heavy"
  | "Soft"
  | "Rigid";

const hapticOptions = {
  enableVibrateFallback: true,
  ignoreAndroidSystemSettings: false,
};

export function triggerImpact(style: ExpoImpact)
{
  const hapticMap: Record<ExpoImpact, string> =
  {
    Light: "impactLight",
    Medium: "impactMedium",
    Heavy: "impactHeavy",
    Soft: "impactMedium",
    Rigid: "impactHeavy",
  };

  const hapticType = hapticMap[style];// || "selection";

  try
  {
    /*if (Platform.OS === "android")
    {
        // android
        ReactNativeHapticFeedback.trigger(hapticType,
        {
            ...hapticOptions,
            enableVibrateFallback: true,
        });
    }
    else
    {
      // iOS*/
    ReactNativeHapticFeedback.trigger(hapticType as HapticFeedbackTypes, hapticOptions);
    //}
  }
  catch (err)
  {
    console.warn("Haptic error, falling back to Vibration:", err);
    Vibration.vibrate(40);
  }
}


export function DeviceBlock(): JSX.Element {
    
    const clearHistory = () => {
        //Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Rigid);
        /*ReactNativeHapticFeedback.trigger("impactRigid",
        {
            enableVibrateFallback: false,
            ignoreAndroidSystemSettings: false,
        }
        );//*/
        triggerImpact("Rigid");
        Alert.alert(
            "Подтверждение",
            "Вы действительно хотите очистить историю?",
            [
              { text: "Отмена", style: "cancel" },
              { text: "Да", onPress: async () => { await deleteHistory(); }, style: "destructive" },
            ],
            { cancelable: true }
        );
    };

    return (
        <View style={styles.box}>
            <TouchableOpacity style={styles.button} onPress={clearHistory} activeOpacity={0.7}>
                <Text style={styles.buttontext}>ОЧИСТИТЬ ИСТОРИЮ</Text>
            </TouchableOpacity>
        </View>
    );
}

const styles = StyleSheet.create({
    box: {
        backgroundColor: '#FFFFFF',
        borderRadius: 15,
        borderWidth: 2,
        borderColor: '#EBEBEB',
        paddingVertical: 15,
        paddingHorizontal: 20,
        width: '100%',
        marginBottom: 5
    },
    button: {
        backgroundColor: '#F94242',
        borderRadius: 9,
        height: 35,
        justifyContent: 'center',
        marginTop: 0
    },
    buttontext: {
        color: '#FFF',
        fontSize: 14,
        fontWeight: 'bold',
        textAlign: 'center'
    }
});
