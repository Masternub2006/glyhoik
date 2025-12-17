declare module "react-native-sqlite-storage"
{
  export interface SQLiteDatabase
  {
    executeSql
    (
      sqlStatement: string,
      params?: any[]
    ): 
    Promise<[{
      rows:
      {
        length: number;
        item: (index: number) => any;
        raw: () => any[];
      };
      rowsAffected: number;
      insertId?: number;
    }]>;

    transaction
    (
      fn: (tx: any) => void,
      error?: (err: any) => void,
      success?: () => void
    ): void;

    close(): Promise<void>;
  }

  interface SQLiteStatic
  {
    openDatabase(config:
    {
      name: string;
      location?: string;
    }): Promise<SQLiteDatabase>;

    enablePromise(value: boolean): void;
  }

  const SQLite: SQLiteStatic;
  export default SQLite;
}