package db;

import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by anant on 11/14/15.
 */

public class SharedPrefHelper {
    private SharedPreferences sh;

    public SharedPrefHelper(SharedPreferences s){
        sh = s;
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

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public List<SecretKey> getPermKeys(){
        String keyString = sh.getString("permKeys", "");
        List<SecretKey> keys = new ArrayList<SecretKey>();
        if(keyString.length() == 160) {
            for (int i = 0; i < 4; i++) {
                String key = keyString.substring(40*i, 40 * (i + 1));
                byte[] keyBytes = hexStringToByteArray(key);
                SecretKey k = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA1");
                keys.add(k);
            }
        }
        return keys;
    }

    public SecretKey getSecretKey(String alias,String algo){
        String encString = sh.getString(alias,"");
        if(encString.length() > 0) {
            byte[] keyBytes = hexStringToByteArray(encString);
            SecretKey k = new SecretKeySpec(keyBytes, 0, keyBytes.length, algo);
            return k;
        }
        return null;
    }

    public void storePermKeys(List<SecretKey> keys){
        String hexKeyString = "";
        for (SecretKey key: keys){
            byte[] keyBytes = key.getEncoded();
            String keyString = bytesToHex(keyBytes);
            hexKeyString = hexKeyString + keyString;
        }

        SharedPreferences.Editor e = sh.edit();
        e.putString("permKeys",hexKeyString);
        e.commit();
    }

    public void storeSecretKey(String alias,SecretKey key){
        byte[] keyBytes = key.getEncoded();
        String keyString = bytesToHex(keyBytes);

        SharedPreferences.Editor e = sh.edit();
        e.putString(alias,keyString);
        e.commit();
    }

}
