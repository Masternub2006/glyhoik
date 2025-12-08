import React from 'react';
import Svg, { Path } from 'react-native-svg';

type Props = {
    style: any,
    color?: string;
    size?: number;
};

export default function PhoneIcon({ color = 'black', size = 20, style }: Props) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" style={style}>
      <Path
        d="M16.18,13.94l-2.12,2.12L8.4,10.4l2.12-2.12a2,2,0,0,0,0-2.83L7,1.92a1,1,0,0,0-1.41,0L2.73,4.76a2,2,0,0,0-.55,1.79A19.36,19.36,0,0,0,7.7,16.76a19.36,19.36,0,0,0,10.22,5.52,2,2,0,0,0,1.79-.55l2.84-2.84a1,1,0,0,0,0-1.41L19,13.94A2,2,0,0,0,16.18,13.94Z"
        fill={color}
      />
    </Svg>
  );
}