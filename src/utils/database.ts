//import * as SQLite from 'expo-sqlite';
import SQLite, {SQLiteDatabase} from "react-native-sqlite-storage";

let db: SQLiteDatabase | null = null;
/*type HistoryRow =
{
  type: string;
  message: string;
  timestamp: string;
};//*/
SQLite.enablePromise(true);

export async function initDB(): Promise<void>
{
  //db = await SQLite.openDatabaseAsync('esp-history.db');
  if (db) return;

  db = await SQLite.openDatabase({
    name: "esp-history.db",
    location: "default",
  });
  await db.executeSql(`
    CREATE TABLE IF NOT EXISTS history (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      type TEXT NOT NULL,
      message TEXT NOT NULL,
      timestamp TEXT NOT NULL
    );
  `);
}

async function ensureDB()
{
  if (!db)
  {
    //db = await SQLite.openDatabaseAsync('esp-history.db');
    /*const db = await SQLite.openDatabase({
      name: "esp-history.db",
      location: "default",
    });
    await db.executeSql(`
      CREATE TABLE IF NOT EXISTS history (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        type TEXT NOT NULL,
        message TEXT NOT NULL,
        timestamp TEXT NOT NULL
      );
    `);//*/
    await initDB();
  }
  return db!;
}

export async function insertHistory(type: string, message: string, timestamp: string): Promise<void>
{
  const db = await ensureDB();
  await db.executeSql(
    `INSERT INTO history (type, message, timestamp) VALUES (?, ?, ?);`,
    [type, message, timestamp]
  );
}

async function selectAll<T>(db: SQLiteDatabase, sql: string): Promise<T[]> {
  const [result] = await db.executeSql(sql);
  const items: T[] = [];

  for (let i = 0; i < result.rows.length; i++) {
    items.push(result.rows.item(i) as T);
  }

  return items;
}

export async function getHistory(): Promise<{ type: string, message: string; timestamp: string }[]>
{
  const db = await ensureDB();
  //const rows = await db.getAllAsync<{ type: string, message: string; timestamp: string }>(
  //  `SELECT type, message, timestamp FROM history ORDER BY timestamp DESC;`
  //);
  const rows = await selectAll<{ type: string, message: string; timestamp: string }>
  (
    db, 'SELECT type, message, timestamp FROM history ORDER BY timestamp DESC;'
  )
  return rows;
}

export async function deleteHistory()
{
  //await ensureDB();
  try
  {
    const db = await ensureDB();
    await db.executeSql(`DELETE FROM history;`);
    return true;
  } catch (e) {
    console.error('Ошибка удаления истории:', e);
    throw new Error('Ошибка удаления');
  }
}
