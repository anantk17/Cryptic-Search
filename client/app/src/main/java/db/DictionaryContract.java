package db;

import android.provider.BaseColumns;

/**
 * Created by anant on 11/14/15.
 */
public class DictionaryContract {
    public static final String TEXT_TYPE = " TEXT";
    public static final String INTEGER_TYPE = " INTEGER";
    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + DictionaryEntry.TABLE_NAME;
    private static final String COMMA_SEP = ",";
    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + DictionaryEntry.TABLE_NAME + " (" +
                    DictionaryEntry._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT NOT NULL" + COMMA_SEP +
                    DictionaryEntry.COLUMN_NAME_WORD + TEXT_TYPE +
                    " )";
    public DictionaryContract() {
    }

    public static abstract class DictionaryEntry implements BaseColumns {
        public static final String TABLE_NAME = "dictionary";
        public static final String COLUMN_NAME_WORD = "word";
    }
}
