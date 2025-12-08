
// import ReconnectingWebSocket from 'reconnecting-websocket';
// import { useEffect, useRef, useState } from 'react';
// import { Alert } from 'react-native';

// export function useESPWebSocket(socketUrl: string | null) {
//   const socketRef = useRef<ReconnectingWebSocket | null>(null);
//   const [isConnected, setIsConnected] = useState(false);

//   useEffect(() => {
//     if (!socketUrl) return;

//     socketRef.current = new ReconnectingWebSocket(socketUrl);

//     socketRef.current.onopen = () => {
//       Alert.alert('WebSocket соединение открыто'); 
//       setIsConnected(true);
//     };

//     socketRef.current.onclose = () => {
//       Alert.alert('WebSocket закрыт');
//       setIsConnected(false);
//     };

     


//     //   socketRef.current.onmessage = (event) => {
//     //   try {
//     //     const data = JSON.parse(event.data);
//     //     Alert.alert('Получены данные от ESP:', data);
//     //   } catch {
//     //     Alert.alert('Ошибка парсинга данных');
//     //   }
//     // };

//     socketRef.current.onerror = (err) => {
//         Alert.alert('Ошибка WebSocket:', err.message);
//         setIsConnected(false);
//       };

//     return () => {
//       socketRef.current?.close();
//       socketRef.current = null;
//       setIsConnected(false);
//     };
//   }, [socketUrl]);

//   return { socketRef, isConnected };
// }
