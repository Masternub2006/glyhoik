import { History, HistoryMessage } from "../types/types";

export function transformHistory(items: { eventMessage: string; timestamp: string; type: string }[]): History[] {
  const sorted = items.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
  const grouped: Record<string, HistoryMessage[]> = {};

  for (const item of sorted) {
    const date = new Date(item.timestamp);
    const key = date.toDateString();

    if (!grouped[key]) grouped[key] = [];

    grouped[key].push({
      time: {
        hours: date.getHours().toString().padStart(2, "0"),
        minutes: date.getMinutes().toString().padStart(2, "0"),
      },
      message: item.eventMessage,
      type: item.type,
    });
  }

  return Object.entries(grouped).map(([dayStr, items]) => ({
    date: new Date(dayStr),
    items,
  }));
}

export function getMessageFromRaw(raw: any): { type: string; message: string } {
  if (raw.gpio) {
    const key = Object.keys(raw.gpio)[0];
    
    return { type: "gpio", message: `${key} → ${raw.gpio[key]}` };
  }

  if (raw.alarm) {
    const key = Object.keys(raw.alarm)[0];
    const messages: Record<string, string> = {
      doorbell: "Кто-то позвонил в дверь",
      babycry: "Ребёнок плачет",
      intercom: "Звонит домофон",
      smoke: "Сработал датчик дыма",
      gas: "Сработал датчик утечки бытового газа",
      phone: "Звонит телефон",
      test: "Проверка работы системы",
    };
    return { type: key, message: messages[key] ?? `Событие: ${key}` };
  }

  if (raw.batterylow) {
    const key = Object.keys(raw.batterylow)[0];
    const messages: Record<string, string> = {
      doorbell: "Разряжена батарея датчика дверного звонка",
      babycry: "Разряжена батарея радионяни",
      intercom: "Разряжена батарея домофона",
      smoke: "Разряжена батарея датчика дыма",
      gas: "Разряжена батарея датчика утечки газа",
      phone: "Разряжена батарея датчика телефона",
    };
    return { type: "batterylow", message: messages[key] ?? `Разряжен датчик: ${key}` };
  }

  return { type: "", message: "" };
}
