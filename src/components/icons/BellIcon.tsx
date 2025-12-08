import React from 'react';
import Svg, { Path } from 'react-native-svg';

type Props = {
  active: boolean;
  color?: string;
  size?: number;
};

export default function BellIcon({ color = 'black', size = 27, active }: Props) {
  return (
    <Svg width={size} height={size} viewBox="0 0 23 27" fill="none">
      <Path
        d="M11.5 5.19444C15.1625 5.19444 18.1316 8.05485 18.1316 11.5833V14.445C18.1316 15.0709 18.3701 15.675 18.8017 16.1428L20.4936 17.9767C21.6341 19.2127 20.7233 21.1667 19.0066 21.1667H3.99337C2.27676 21.1667 1.36596 19.2127 2.50641 17.9767L4.19836 16.1428C4.62997 15.675 4.86839 15.0709 4.86839 14.445L4.86842 11.5833C4.86842 8.05485 7.83749 5.19444 11.5 5.19444ZM11.5 5.19444V2M10.1736 25H12.8262"
        stroke={color}
        strokeWidth={2.7}
        strokeLinecap="round"
        strokeLinejoin="round"
        fill={active ? 'black' : 'none'}
      />
    </Svg>
  );
}