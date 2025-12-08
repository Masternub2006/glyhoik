import { useContext } from 'react';
import { ESPConnectionContext } from '../context/ESPConnectionContext';

export const useESPConnection = () => {
  const context = useContext(ESPConnectionContext);
  if (!context) {
    throw new Error('useESPConnection must be used within ESPConnectionProvider');
  }
  return context;
};