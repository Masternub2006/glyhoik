import { JSX } from "react";
import { Alert, TouchableOpacity, StyleSheet, View, Text } from 'react-native';
import * as Haptics from 'expo-haptics';
import { deleteHistory } from "../utils/database";

export function DeviceBlock(): JSX.Element {
    
    const clearHistory = () => {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Rigid);
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
