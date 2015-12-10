package com.example.anant.searchcrypt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import crypto.HomomorphicUtils;
import db.DbHelper;

public class FileBrowserActivity extends AppCompatActivity {

    private HomomorphicUtils h;
    private ListView lv;
    private DbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new DbHelper(this);
        SharedPreferences sh = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        h = HomomorphicUtils.getInstance(sh,dbHelper);

        File uploadDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),"Uploads");
        lv = (ListView) findViewById(R.id.fileListView);

        File[] filesList = uploadDir.listFiles();
        List<String> fileNames = new ArrayList<String>();
        for(int i=0;i<filesList.length;i++)
            fileNames.add(filesList[i].getName());

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,fileNames);

        lv.setAdapter(arrayAdapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Context c = getApplicationContext();
                String filename = (String) parent.getAdapter().getItem(position);
                uploadFile(filename);
                Toast toast = Toast.makeText(c,filename+" uploaded",Toast.LENGTH_SHORT);
                toast.show();

            }
        });
    }

    public void uploadFile(String filename){
        String absFileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString() + "/Uploads/"+filename;
        List<Integer> indexList = h.createIndexList(absFileName);
        Log.d("anant-index-list", Objects.toString(indexList));
        final String maskedIndexString = h.createMaskedIndexList(indexList);
        Log.d("anant-masked-index",maskedIndexString);
        final String encryptedFileString = h.getEncryptedString(absFileName);
        String hostName = getResources().getString(R.string.hostName);
        //String url = "http://"+hostName+"/app/file/" + filename;
        String url = "http://"+hostName+"/app/index/"+filename;

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest postRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

            }
        },new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }){
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<>();
                // the POST parameters:
                params.put("indexString", maskedIndexString);
                params.put("fileData", encryptedFileString);
                return params;
            }
        };
        Volley.newRequestQueue(this).add(postRequest);
        //int statusCode1 = h.sendFile(filename);
        //int statusCode2 = h.sendMaskedIndex(filename, maskedIndexString);

        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
    }
}
