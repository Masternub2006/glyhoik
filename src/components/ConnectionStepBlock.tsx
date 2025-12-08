import { JSX } from 'react';
import { StyleSheet, View, Text } from 'react-native';

type ConnectionStepBlockProps = {
  stepNumber: number;
  title: string;
  items?: string[];
};

function ConnectionStepBlock({stepNumber, title, items}: ConnectionStepBlockProps): JSX.Element {

  return (
    <View style={styles.box}>
      <View style={styles.leftRightBox}>
        <View style={styles.left}>
          <Text style={styles.text}>{stepNumber}. </Text>
        </View>
        <View style={styles.right}>
          <Text style={styles.text}>{title}</Text>
        </View>
      </View>
      {items && (
        <View style={styles.itemsContainer}>
          {items.map((item, index) => (
            <View key={index} style={styles.itemContainer}>
              <Text style={styles.itemText}>{item}</Text>
            </View>
          ))}
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  box: {
    backgroundColor: '#FFFFFF',
    borderRadius: 15,
    borderWidth: 2,
    borderColor: '#EBEBEB',
    paddingVertical: 15,
    paddingHorizontal: 20,
    width: '100%',
  },
  leftRightBox: {
    flexDirection: 'row',
    width: '100%',
  },
  left: {
    textAlign: 'left',
    alignItems: 'center',
    justifyContent: 'center',
  },
  right: {
    flex: 1,
    textAlign: 'left',
    paddingLeft: 10,
  },
  text: {
    fontSize: 17,
    fontFamily: 'Inter',
    fontWeight: 'bold',
  },
  itemsContainer: {
    marginTop: 10,
  },
  itemContainer: {
    backgroundColor: '#EBEBEB',
    width: '100%',
    paddingVertical: 10,
    borderRadius: 15,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 7,
  },
  itemText: {
    fontSize: 17,
    fontWeight: 'bold',
  },
});

export default ConnectionStepBlock;
