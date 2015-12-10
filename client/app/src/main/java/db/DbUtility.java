package db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import crypto.KeyBunch;

/**
 * Created by anant on 11/14/15.
 */
public class DbUtility {

    private DbHelper dbHelper;
    public DbUtility(DbHelper d) {
        dbHelper = d;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public List<SecretKey> getColumnKeys(){
        List<SecretKey> columnKeys = new ArrayList<SecretKey>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] Projection = {
                ColumnKeyContract.ColumnKeyEntry.COLUMN_NAME_COLUMNKEY
        };

        Cursor c = db.query(ColumnKeyContract.ColumnKeyEntry.TABLE_NAME,Projection,null,null,null,null,null,null);

        if(c.moveToFirst()) {
            do {
                byte[] key = c.getBlob(c.getColumnIndexOrThrow(ColumnKeyContract.ColumnKeyEntry.COLUMN_NAME_COLUMNKEY));
                SecretKey k = new SecretKeySpec(key, 0, key.length, "HmacSHA1");
                columnKeys.add(k);
            } while (c.moveToNext());
        }
        return columnKeys;
    }

    public void storeColumnKeys(List<SecretKey> keys){
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues c = new ContentValues();
        for(SecretKey key: keys) {
            byte[] keyBytes = key.getEncoded();
            c.put(ColumnKeyContract.ColumnKeyEntry.COLUMN_NAME_COLUMNKEY, keyBytes);

            long rowId = db.insert(ColumnKeyContract.ColumnKeyEntry.TABLE_NAME, null, c);
            Log.d("anant-db", Objects.toString(rowId));
        }
    }

    public int getIndexForWord(String keyword){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int index = -1;
        String[] Projection = {
                DictionaryContract.DictionaryEntry._ID,
                DictionaryContract.DictionaryEntry.COLUMN_NAME_WORD
        };

        Cursor c = db.query(DictionaryContract.DictionaryEntry.TABLE_NAME,Projection,"word=?",new String[]{keyword},null,null,null);

        if(c.moveToFirst()){
            index = c.getInt(c.getColumnIndexOrThrow(DictionaryContract.DictionaryEntry._ID));
            Log.d("anant-dict",Objects.toString(index));
        }
        c.close();
        return index;
    }

    public int addWord(String keyword){
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues c = new ContentValues();
        c.put(DictionaryContract.DictionaryEntry.COLUMN_NAME_WORD,keyword);

        int rowId = (int)db.insert(DictionaryContract.DictionaryEntry.TABLE_NAME,null,c);
        Log.d("anant-dict-add", Objects.toString(rowId));

        return rowId;
    }

    public int addFile(String filename){
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues c = new ContentValues();
        c.put(FileContract.FileEntry.COLUMN_NAME_FILE_NAME,filename);

        byte[] filenameBytes = filename.getBytes();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] theDigest = md.digest(filenameBytes);
            String hashedFileName = bytesToHex(theDigest) + ".enc";
            c.put(FileContract.FileEntry.COLUMN_NAME_HASHED_FILE_NAME,hashedFileName);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        int rowId = (int)db.insert(FileContract.FileEntry.TABLE_NAME,null,c);
        Log.d("anant-file-add", Objects.toString(rowId));
        return rowId;
    }

    public int getFileIndex(String filename){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int index = -1;
        String[] Projection = {
                FileContract.FileEntry._ID,
        };

        Cursor c = db.query(FileContract.FileEntry.TABLE_NAME,Projection,"file_name=?",new String[]{filename},null,null,null);

        if(c.moveToFirst()){
            index = c.getInt(c.getColumnIndexOrThrow(FileContract.FileEntry._ID));
            Log.d("anant-file", Objects.toString(index));
        }
        c.close();
        return index;
    }

    public String getFilename(String hashedFileName){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String filename = "";
        String[] Projection = {
                FileContract.FileEntry.COLUMN_NAME_FILE_NAME,
        };

        Cursor c = db.query(FileContract.FileEntry.TABLE_NAME,Projection,"hashed_file_name=?",new String[]{hashedFileName},null,null,null);

        if(c.moveToFirst()){
            filename = c.getString(c.getColumnIndexOrThrow(FileContract.FileEntry.COLUMN_NAME_FILE_NAME));
            Log.d("anant-file", filename);
        }
        c.close();
        return filename;
    }

    public String getHashedFilename(String fileName){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String filename = "";
        String[] Projection = {
                FileContract.FileEntry.COLUMN_NAME_HASHED_FILE_NAME,
        };

        Cursor c = db.query(FileContract.FileEntry.TABLE_NAME,Projection,"file_name=?",new String[]{fileName},null,null,null);

        if(c.moveToFirst()){
            filename = c.getString(c.getColumnIndexOrThrow(FileContract.FileEntry.COLUMN_NAME_HASHED_FILE_NAME));
            Log.d("anant-file", filename);
        }
        c.close();
        return filename;
    }

    public List<String> getAllFiles(){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> files = new ArrayList<String>();

        String[] Projection = {
                FileContract.FileEntry.COLUMN_NAME_FILE_NAME,
        };

        Cursor c = db.query(FileContract.FileEntry.TABLE_NAME,Projection,null,null,null,null,null);

        String file;
        if(c.moveToFirst()){
            do {
                file = c.getString(c.getColumnIndexOrThrow(FileContract.FileEntry.COLUMN_NAME_FILE_NAME));
                Log.d("anant-file", file);
                files.add(file);
            }while(c.moveToNext());
        }
        c.close();
        return files;
    }

    public int getFileCount(){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return (int)DatabaseUtils.queryNumEntries(db,FileContract.FileEntry.TABLE_NAME);
    }

    public void removeColumnKeys(){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(ColumnKeyContract.ColumnKeyEntry.TABLE_NAME,null,null);

        return;
    }

    public int getColumnKeyCount(){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return (int)DatabaseUtils.queryNumEntries(db, ColumnKeyContract.ColumnKeyEntry.TABLE_NAME);
    }

}
