package crypto;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Path;
import android.util.Base64;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.net.ssl.HttpsURLConnection;

import db.ColumnKeyContract;
import db.DbHelper;
import db.DbUtility;

/**
 * Created by anant on 11/14/15.
 */
public class HomomorphicUtils {

    private static HomomorphicUtils hInstance = null;

    private KeyBunch key;
    private DbUtility dbUtil;

    private HomomorphicUtils(SharedPreferences sh,DbHelper dbHelper){
        KeyBunch kb = KeyBunch.loadSavedKeyBunch(sh,dbHelper);
        if(!kb.isValid())
        {
            DbUtility dbUtil = new DbUtility(dbHelper);
            dbUtil.removeColumnKeys();
            kb = KeyBunch.generateKeyBunch();
            kb.saveKeyBunch(sh, dbHelper);
        }

        key = kb;
        dbUtil = new DbUtility(dbHelper);
    }

    public static HomomorphicUtils getInstance(SharedPreferences sh, DbHelper dbHelp){
        if(hInstance == null){
            hInstance = new HomomorphicUtils(sh,dbHelp);
        }
        return hInstance;
    }

    public static BigInteger randomFunction(SecretKey key, int input) {
        Mac mac = null;
        byte[] digest = new byte[20];

        ByteBuffer dataBuf = ByteBuffer.allocate(20);
        dataBuf.putInt(input);
        byte[] msgBytes = dataBuf.array();

        BigInteger b = null;
        try {
            mac = Mac.getInstance("HmacSHA1");
            mac.init(key);
            digest = mac.doFinal(msgBytes);
            b = new BigInteger(1,digest);

        }catch (InvalidKeyException | NoSuchAlgorithmException e) {
                e.printStackTrace();
        }
        //ByteBuffer wrapped = ByteBuffer.wrap(digest); // big-endian by default
        //int num = wrapped.getInt();
        return b;
    }

    public static int randomBit(SecretKey key, int input){
        BigInteger  b = randomFunction(key,input);
        BigInteger b2 = new BigInteger("2");
        return (b.mod(b2)).intValue();
    }

    public int randomPerm(int input){
        BigInteger MOD = new BigInteger("32");
        byte[] digest = null;

        int l = input % 32;
        int r = input / 32;

        for(SecretKey k : key.getPermKeys()) {
                int temp_r = r;
                int temp_l = l ^ (randomFunction(k, r).mod(MOD)).intValue();

                int temp = r;
                r = l;
                l = temp;
        }

        return (l*32 + r);
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

    public List<Integer> createIndexList(String filename){
        final int size = 1024;
        List<Integer> arrlist = new ArrayList<Integer>(size);
        int permuted_index = 0;
        int index;
        for(int i=0;i<size;i++)
        {
            arrlist.add(0);
        }
        Log.d("anant-index",filename);
        Scanner s = null;
        try {
            s = new Scanner(new File(filename)).useDelimiter(",|:|\\.| ");
        } catch (FileNotFoundException e) {
            Log.d("anant-index","File Not Found");
            e.printStackTrace();
        }
        List<String> myList = new ArrayList<String>();

        while (s.hasNext())
        {
            String s1 = s.next();
            if(s1.charAt(0) != '\n')
                myList.add(s1.toLowerCase());
        }

        for(int i =0; i<myList.size();i++)
        {
            index = dbUtil.getIndexForWord(myList.get(i)) - 1;
            if(index > -1) {
                permuted_index = randomPerm(index);
                arrlist.set(permuted_index, 1);
            }
            else{
                index = dbUtil.addWord(myList.get(i)) - 1;
                permuted_index = randomPerm(index);
                arrlist.set(permuted_index,1);
            }
        }
        File f = new File(filename);
        dbUtil.addFile(f.getName());
        return arrlist;
    };

    public String createMaskedIndexList(List<Integer> indexList){

        List<Integer> maskedIndexList = new ArrayList<Integer>();
        int FileCount = dbUtil.getFileCount() - 1;

        for(int i=0;i<indexList.size();i++)
        {
            SecretKey r = key.getColumnKeys().get(i);
            int bitRandom = randomBit(r, FileCount);
            int maskedBit = indexList.get(i) ^ bitRandom;
            maskedIndexList.add(maskedBit);
        }

        StringBuilder strbul  = new StringBuilder();
        Iterator<Integer> iter = maskedIndexList.iterator();
        while(iter.hasNext())
        {
            strbul.append(iter.next());
        }
        return strbul.toString();
    }

    public int sendFile(String filename){

        return 0;
    };

    public String getEncryptedString(String fileName){

        try {
            String content = new Scanner(new File(fileName)).useDelimiter("\\Z").next();
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key.getEncKey());
            byte[] contentBytes = content.getBytes();
            byte[] encryptedBytes = cipher.doFinal(content.getBytes());
            return Base64.encodeToString(cipher.doFinal(content.getBytes()), Base64.DEFAULT);

        } catch (FileNotFoundException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException | NoSuchPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }


        return null;
    }

    public String getDecryptedString(String fileData){
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key.getEncKey());
            byte[] contentBytes = Base64.decode(fileData, Base64.DEFAULT);
            byte[] decryptedBytes = cipher.doFinal(contentBytes);
            return new String(cipher.doFinal(Base64.decode(fileData,Base64.DEFAULT)));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> search(String keyword){
        ArrayList<String> list = new ArrayList<String>();
        list.add("A");
        list.add("B");
        list.add("C");
        return list;
    }

    private List<SecretKey> generateColumnKeys() {
        //SQLiteDatabase db = dbHelper.getWritableDatabase();

        List<SecretKey> columnKeys = new ArrayList<SecretKey>();

        //long numberOfRows = DatabaseUtils.queryNumEntries(db, ColumnKeyContract.ColumnKeyEntry.TABLE_NAME);
        int numKeys = 0;
        while (numKeys < 1024) {
            try {
                KeyGenerator k = KeyGenerator.getInstance("HmacSHA1");
                SecretKey column_key = k.generateKey();
                //byte[] keyBytes = column_key.getEncoded();
                Log.d("anant", Objects.toString(numKeys));
                columnKeys.add(column_key);

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            numKeys++;
        }
        return columnKeys;
    }

    private List<SecretKey> generatePermKey(){
        List<SecretKey> roundKeys = new ArrayList<SecretKey>();

        int numKeys = 0;
        while(numKeys < 4){
            try{
                KeyGenerator k = KeyGenerator.getInstance("HmacSHA1");
                SecretKey roundKey = k.generateKey();
                //byte[] roundKeyBytes = column_key.getEncoded();
                roundKeys.add(roundKey);

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            numKeys++;
        }
        return roundKeys;
    }

    private SecretKey generateFuncKey(){
        SecretKey funcKey = null;
        try{
            KeyGenerator k = KeyGenerator.getInstance("HmacSHA1");
            funcKey = k.generateKey();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return funcKey;
    }

    private SecretKey generateEncKey(){
        SecretKey encKey = null;
        try{
            KeyGenerator k = KeyGenerator.getInstance("AES");
            encKey = k.generateKey();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return encKey;
    }

    public BigInteger getColumnKey(int index){
        SecretKey k = key.getColumnKeys().get(index);
        byte[] columnKeyBytes = k.getEncoded();
        BigInteger b = new BigInteger(1,columnKeyBytes);
        return b;
    }
}
