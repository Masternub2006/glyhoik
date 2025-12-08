import React from 'react';
import Svg, { Path } from 'react-native-svg';

type Props = {
    style: any,
    color?: string;
    size?: number;
};

export default function BatteryIcon({ color = 'black', size = 20, style }: Props) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" style={style}>
      <Path
        d="M9 4V3a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v1h3a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1h3zm4 8V7l-5 7h3v5l5-7h-3z"
        fill={color}
      />
    </Svg>
  );
}