import * as SQLite from 'expo-sqlite';

let db: SQLite.SQLiteDatabase;

export async function initDB(): Promise<void> {
  db = await SQLite.openDatabaseAsync('esp-history.db');

  await db.execAsync(`
    CREATE TABLE IF NOT EXISTS history (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      type TEXT NOT NULL,
      message TEXT NOT NULL,
      timestamp TEXT NOT NULL
    );
  `);
}

async function ensureDB() {
  if (!db) {
    db = await SQLite.openDatabaseAsync('esp-history.db');
    await db.execAsync(`
      CREATE TABLE IF NOT EXISTS history (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        type TEXT NOT NULL,
        message TEXT NOT NULL,
        timestamp TEXT NOT NULL
      );
    `);
  }
}

export async function insertHistory(type: string, message: string, timestamp: string): Promise<void> {
  await ensureDB();

  await db.runAsync(
    `INSERT INTO history (type, message, timestamp) VALUES (?, ?, ?);`,
    [type, message, timestamp]
  );
}

export async function getHistory(): Promise<{ type: string, message: string; timestamp: string }[]> {
  await ensureDB();

  const rows = await db.getAllAsync<{ type: string, message: string; timestamp: string }>(
    `SELECT type, message, timestamp FROM history ORDER BY timestamp DESC;`
  );

  return rows;
}

export async function deleteHistory() {
  await ensureDB();

  try {
    await db.runAsync(`DELETE FROM history;`);
    return true;
  } catch (e) {
    console.error('Ошибка удаления истории:', e);
    throw new Error('Ошибка удаления');
  }
}
