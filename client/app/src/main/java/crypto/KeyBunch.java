package crypto;

import android.content.SharedPreferences;
import android.util.Log;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import db.DbHelper;
import db.DbUtility;
import db.SharedPrefHelper;

import static crypto.HomomorphicUtils.bytesToHex;
import static crypto.HomomorphicUtils.randomFunction;

/**
 * Created by anant on 11/14/15.
 */
public class KeyBunch {

    private List<SecretKey> columnKeys;
    private List<SecretKey> permKeys;
    private SecretKey funcKey;
    private SecretKey encKey;

    public KeyBunch(){

    }

    public List<SecretKey> getColumnKeys(){
        return columnKeys;
    }

    public List<SecretKey> getPermKeys(){
        return permKeys;
    }

    public SecretKey getFuncKey(){
        return funcKey;
    }

    public SecretKey getEncKey(){
        return encKey;
    }

    public boolean isValid(){
        return (columnKeys.size() == 1024 && permKeys.size() == 4 && funcKey!= null && encKey != null);
    }

    public static KeyBunch generateKeyBunch(){

        KeyBunch k = new KeyBunch();

        k.permKeys = generatePermKey();
        k.funcKey = generateFuncKey();
        k.encKey = generateEncKey();
        k.columnKeys = generateColumnKeys(k.funcKey);

        return k;
    }

    private static List<SecretKey> generateColumnKeys(SecretKey key) {
        List<SecretKey> columnKeys = new ArrayList<SecretKey>();

        byte[] funcKeyBytes = key.getEncoded();
        BigInteger funcKey = new BigInteger(1,funcKeyBytes);
        Log.d("anant-func-key",funcKey.toString(16));


        for(int i=0;i<1024;i++){

            BigInteger columnKeyInt = randomFunction(key,i);
            //Log.d("anant-col-key", columnKeyInt.toString(16));
            byte[] stringBuf = columnKeyInt.toByteArray();
            Log.d("anant-col-key",bytesToHex(stringBuf));


            //ByteBuffer dataBuf = ByteBuffer.allocate(20);
            //dataBuf.putInt(columnKeyInt);
            byte[] columnKeyBytes = columnKeyInt.toByteArray();

            SecretKey columnKey = new SecretKeySpec(columnKeyBytes,0,columnKeyBytes.length,"HmacSHA1");
            columnKeys.add(columnKey);
        }
        return columnKeys;
    }

    private static SecretKey generateEncKey() {
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (keyGen != null) {
            keyGen.init(256); // for example
        }
        SecretKey secretKey = keyGen.generateKey();
        return secretKey;
    }

    private static SecretKey generateFuncKey() {
        KeyGenerator k = null;
        try {
            k = KeyGenerator.getInstance("HmacSHA1");
            k.init(160);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        SecretKey key = k.generateKey();
        return key;
    }

    private static List<SecretKey> generatePermKey() {
        List<SecretKey> keys = new ArrayList<SecretKey>();
        for(int i=0;i<4;i++) {

            KeyGenerator k = null;
            try {
                k = KeyGenerator.getInstance("HmacSHA1");
                k.init(160);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            SecretKey round_key = k.generateKey();
            keys.add(round_key);
        }
        return keys;
    }

    public static KeyBunch loadSavedKeyBunch(SharedPreferences sh, DbHelper dbHelper){
        DbUtility dbUtil = new DbUtility(dbHelper);
        SharedPrefHelper shPrefHelp = new SharedPrefHelper(sh);

        KeyBunch kb = new KeyBunch();
        kb.columnKeys = dbUtil.getColumnKeys();
        kb.permKeys = shPrefHelp.getPermKeys();
        kb.funcKey = shPrefHelp.getSecretKey("funcKey","HmacSHA1");
        kb.encKey = shPrefHelp.getSecretKey("encKey","AES");

        return kb;
    }

    public void saveKeyBunch(SharedPreferences sh, DbHelper dbHelper){

        DbUtility dbUtil = new DbUtility(dbHelper);
        SharedPrefHelper shPrefHelp = new SharedPrefHelper(sh);

        dbUtil.storeColumnKeys(columnKeys);
        shPrefHelp.storePermKeys(permKeys);
        shPrefHelp.storeSecretKey("funcKey",funcKey);
        shPrefHelp.storeSecretKey("encKey",encKey);

        return ;
    }
}
