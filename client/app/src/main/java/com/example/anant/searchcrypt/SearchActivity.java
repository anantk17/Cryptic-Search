package com.example.anant.searchcrypt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import crypto.HomomorphicUtils;
import db.DbHelper;
import db.DbUtility;

import static db.SharedPrefHelper.bytesToHex;

public class SearchActivity extends AppCompatActivity {

    private ListView lv;
    private HomomorphicUtils h;
    private DbHelper dbHelper;
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Intent intent = getIntent();
        Bundle query = intent.getExtras();
        List<String> fileList = query.getStringArrayList("fileList");
        String searchString = query.getString("keyword");
        //Log.d("anant", fileList.get(0));
        EditText et = (EditText) findViewById(R.id.editText2);
        et.setText(searchString, TextView.BufferType.EDITABLE);
        tv = (TextView) findViewById(R.id.textView2);
        tv.setText("Files containing " + searchString);

        dbHelper = new DbHelper(this);
        SharedPreferences sh = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        h = HomomorphicUtils.getInstance(sh,dbHelper);

        lv = (ListView) findViewById(R.id.list1);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,fileList);

        lv.setAdapter(arrayAdapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String filename = (String) parent.getAdapter().getItem(position);

                final String absFileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString() + "/Download-Files/" + filename;
                String hostName = getResources().getString(R.string.hostName);
                //String url = "http://"+hostName+"/app/file/" + filename;
                String url = "http://"+hostName+"/app/file/" + filename;
                Context c = getApplicationContext();
                RequestQueue queue = Volley.newRequestQueue(c);

                JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String fileData = response.getString("fileData");
                            String decryptedString = h.getDecryptedString(fileData);
                            PrintWriter writer = new PrintWriter(absFileName, "UTF-8");
                            writer.close();
                        } catch (JSONException | FileNotFoundException | UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
                Volley.newRequestQueue(c).add(getRequest);

            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void searchOnKeyword(View view) {
        EditText editText =  (EditText) findViewById(R.id.editText2);
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
                        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(SearchActivity.this,android.R.layout.simple_list_item_1,listOfFiles);
                        lv.setAdapter(arrayAdapter);

                        //TextView tv = (TextView) findViewById(R.id.textView2);
                        tv.setText("Files containing " + searchString);

                        //Intent i = new Intent(MainActivity.this, SearchActivity.class);
                        //Bundle query = new Bundle();
                        //query.putString("keyword", searchString);
                        //query.putStringArrayList("fileList", (ArrayList<String>) listOfFiles);
                        //i.putExtras(query);
                        //startActivity(i);

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
