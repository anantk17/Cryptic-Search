package com.example.anant.searchcrypt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import crypto.HomomorphicUtils;
import crypto.KeyBunch;
import db.DbHelper;
import db.DbUtility;
import db.SharedPrefHelper;

import static android.app.PendingIntent.getActivity;
import static db.SharedPrefHelper.bytesToHex;

public class MainActivity extends AppCompatActivity {

    private ListView lv;
    private HomomorphicUtils h;
    private DbHelper dbHelper;
    private List<String> filesList;
    private ArrayAdapter<String> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        File dir_name = this.getFilesDir();
        String dir = dir_name.toString();
        Log.d("anant-file",dir);

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),"Uploads");
        if(!file.mkdirs())
            Log.d("anant-sd","Dir not created");

        Log.d("anant-sd",file.toString());

        File file1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"Download-Files");
        if(!file1.mkdirs())
            Log.d("anant-sd","Dir1 not created");

        Log.d("anant-sd", file1.toString());

        dbHelper = new DbHelper(this);
        SharedPreferences sh = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        h = HomomorphicUtils.getInstance(sh,dbHelper);

        for(int i=0;i<1024;i++)
        {
            Log.d("anant-perm-key",Objects.toString(h.randomPerm(i)));
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, FileBrowserActivity.class);
                startActivity(intent);
            }
        });

        lv = (ListView) findViewById(R.id.allFileList);
        filesList = getAllFiles();
        arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,filesList);

        lv.setAdapter(arrayAdapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String filename = (String) parent.getAdapter().getItem(position);

                final String absFileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/Download-Files/" + filename;
                String hostName = getResources().getString(R.string.hostName);
                String url = "http://"+hostName+"/app/file/" + filename;
                final Context c = getApplicationContext();
                RequestQueue queue = Volley.newRequestQueue(c);

                JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String fileData = response.getString("fileData");
                            String decryptedString = h.getDecryptedString(fileData);
                            File f = new File(absFileName);
                            PrintWriter writer = new PrintWriter(f);
                            writer.println(decryptedString);
                            writer.close();
                            Toast.makeText(c, "File Downloaded", Toast.LENGTH_SHORT).show();
                        } catch (JSONException | FileNotFoundException e) {
                            Log.d("anant-net", "JSON Error");
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("anant-net", "Failed");
                    }
                });
                Volley.newRequestQueue(c).add(getRequest);

            }
        });
    }

    protected void onResume()
    {
        super.onResume();
        filesList = getAllFiles();
        if(arrayAdapter!=null)
            arrayAdapter.notifyDataSetChanged();
    }

    private List<String> getAllFiles() {
        DbUtility dbUtil = new DbUtility(dbHelper);
        List<String> fileNames = dbUtil.getAllFiles();
        return fileNames;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void searchOnKeyword(View view) {
        Intent intent = new Intent(this, SearchActivity.class);
        EditText editText =  (EditText) findViewById(R.id.searchText);
        final String searchString = editText.getText().toString();

        //List<String> listOfFiles = h.search(searchString);
        DbUtility dbUtil = new DbUtility(dbHelper);
        int index = dbUtil.getIndexForWord(searchString);
        int permuted_index=-1;
        BigInteger column_key=null;

        if(index > -1) {
            index = index - 1;
            permuted_index = h.randomPerm(index);
            column_key = h.getColumnKey(permuted_index);

            byte[] stringBuf = column_key.toByteArray();
            String hostName = getResources().getString(R.string.hostName);
            //String url = "http://"+hostName+"/app/file/" + filename;
            String url = "http://"+hostName+"/app/query?p=" + Objects.toString(permuted_index) + "&f=" + bytesToHex(stringBuf);
            final Context c = getApplicationContext();
            RequestQueue queue = Volley.newRequestQueue(c);
            final List<String> listOfFiles = new ArrayList<String>();
            JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        JSONArray fileNames = response.getJSONArray("filenames");
                        if (fileNames != null) {
                            int len = fileNames.length();
                            for (int i = 0; i < len; i++) {
                                listOfFiles.add(fileNames.get(i).toString());
                            }
                        }
                        Intent i = new Intent(MainActivity.this, SearchActivity.class);
                        Bundle query = new Bundle();
                        query.putString("keyword", searchString);
                        query.putStringArrayList("fileList", (ArrayList<String>) listOfFiles);
                        i.putExtras(query);
                        startActivity(i);
                    }
                    //String decryptedString = h.getDecryptedString(fileData);
                    //Toast.makeText(c, "File Downloaded", Toast.LENGTH_SHORT).show();
                    catch (JSONException e) {
                        Log.d("anant-net", "JSON Error");
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("anant-net", "Failed");
                }
            });
            Volley.newRequestQueue(c).add(getRequest);

        }
        else
        {
            Toast t = Toast.makeText(this,"Keyword not present in dictionary",Toast.LENGTH_SHORT);
            t.show();
        }

    }

}
