import React, { JSX, useEffect, useRef } from 'react';
import { Animated, StyleSheet, View, Text } from 'react-native';
import { HistoryMessage } from '../types/types';
import DoorBellIcon from './icons/message/DoorBellIcon';
import BatteryIcon from './icons/message/BatteryIcon';
import UnknownIcon from './icons/message/UnknownIcon';
import BabyIcon from './icons/message/BabyIcon';
import IntercomIcon from './icons/message/IntercomIcon';
import SmokeIcon from './icons/message/SmokeIcon';
import GasIcon from './icons/message/GasIcon';
import PhoneIcon from './icons/message/PhoneIcon';
import TestIcon from './icons/message/TestIcon';

type HistoryBlockProps = {
  date: Date;
  items: HistoryMessage[];
};

export function HistoryBlock({ date, items }: HistoryBlockProps): JSX.Element {
  const months: string[] = [
    'ЯНВАРЯ', 
    'ФЕВРАЛЯ',
    'МАРТА',
    'АПРЕЛЯ',
    'МАЯ',
    'ИЮНЯ',
    'ИЮЛЯ',
    'АВГУСТА',
    'СЕНТЯБРЯ',
    'ОКТЯБРЯ',
    'НОЯБРЯ',
    'ДЕКАБРЯ'
  ];

  return (
    <View style={styles.box}>
      <Text style={styles.date}>{date.getDate()} {months[date.getMonth()]}</Text>
      <View style={styles.messageList}>
        {items.map((item, index) => (
          <FadeSlideInView key={index}>
            <View style={styles.messageRow}>
                {renderIconByType(item.type)}
                <Text style={styles.messageText}>{item.message}</Text>
                <Text style={styles.messageTime}>{item.time.hours}:{item.time.minutes}</Text>
            </View>
          </FadeSlideInView>
        ))}
      </View>
    </View>
  );
}

function renderIconByType(type?: string) {
    switch (type) {
        case 'doorbell':
          return <DoorBellIcon color='#ffa600ff' style={styles.icon} />;
        case 'batterylow':
          return <BatteryIcon color='#FF4C4C' style={styles.icon} />;
        case 'babycry':
          return <BabyIcon color='#ffdbac' style={styles.icon} />
        case 'intercom': 
          return <IntercomIcon color='#000000' style={styles.icon} />
        case 'smoke':
          return <SmokeIcon color='#aaaaaa96' style={styles.icon} />
        case 'gas':
          return <GasIcon color='#00d9ffff' style={styles.icon} />
        case 'phone':
          return <PhoneIcon color='#000000ff' style={styles.icon} />
        case 'test':
          return <TestIcon color='#008cffff' style={styles.icon} />
          
        default:
          return <UnknownIcon color='#888' style={styles.icon} />;
  }
}


function FadeSlideInView({ children }: { children: React.ReactNode }) {
  const opacity = useRef(new Animated.Value(0)).current;
  const translateX = useRef(new Animated.Value(-30)).current;

  useEffect(() => {
    Animated.parallel([
      Animated.timing(opacity, {
        toValue: 1,
        duration: 400,
        useNativeDriver: true,
      }),
      Animated.timing(translateX, {
        toValue: 0,
        duration: 400,
        useNativeDriver: true,
      }),
    ]).start();
  }, []);

  return (
    <Animated.View style={{ opacity, transform: [{ translateX }] }}>
      {children}
    </Animated.View>
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
    marginBottom: 5,
  },
  line: {
    height: 0.4,
    backgroundColor: "#515151",
    marginTop: 20,
    marginBottom: 8
  },
  date: {
    fontSize: 16,
    fontWeight: '900',
    marginBottom: 12,
  },
  messageList: {
    gap: 4,
  },
  messageRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 15,
    //backgroundColor: "#0000002d"
  },
  messageText: {
    flex: 1,
    fontSize: 16,
    flexWrap: 'wrap',
    color: '#515151',
    fontWeight: '500',
  },
  messageTime: {
    textAlign: 'right',
    fontSize: 16,
    fontWeight: '900'
  },
  icon: {
    verticalAlign: 'middle',
    marginRight: 7
  }
});
