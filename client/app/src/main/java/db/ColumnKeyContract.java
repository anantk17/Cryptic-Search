package db;

import android.provider.BaseColumns;

/**
 * Created by anant on 11/14/15.
 */

public class ColumnKeyContract {
    public static final String TEXT_TYPE = " TEXT";
    public static final String INTEGER_TYPE = " INTEGER";
    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + ColumnKeyEntry.TABLE_NAME;
    private static final String COMMA_SEP = ",";
    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + ColumnKeyEntry.TABLE_NAME + " (" +
                    ColumnKeyEntry._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT NOT NULL" + COMMA_SEP +
                    ColumnKeyEntry.COLUMN_NAME_COLUMNKEY + TEXT_TYPE +
                    " )";
    public ColumnKeyContract() {
    }

    public static abstract class ColumnKeyEntry implements BaseColumns {
        public static final String TABLE_NAME = "columnKey";
        public static final String COLUMN_NAME_COLUMNKEY = "column_key";
    }
}
