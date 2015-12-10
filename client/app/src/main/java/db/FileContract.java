package db;

import android.provider.BaseColumns;

/**
 * Created by anant on 11/14/15.
 */
public class FileContract {
    public static final String TEXT_TYPE = " TEXT";
    public static final String INTEGER_TYPE = " INTEGER";
    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + FileEntry.TABLE_NAME;
    private static final String COMMA_SEP = ",";
    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + FileEntry.TABLE_NAME + " (" +
                    FileEntry._ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT NOT NULL" + COMMA_SEP +
                    FileEntry.COLUMN_NAME_FILE_NAME + TEXT_TYPE + COMMA_SEP +
                    FileEntry.COLUMN_NAME_HASHED_FILE_NAME + TEXT_TYPE +
                    " )";
    public FileContract() {
    }

    public static abstract class FileEntry implements BaseColumns {
        public static final String TABLE_NAME = "file";
        public static final String COLUMN_NAME_FILE_NAME = "file_name";
        public static final String COLUMN_NAME_HASHED_FILE_NAME = "hashed_file_name";

    }

}
