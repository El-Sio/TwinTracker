package com.example.charles.twintracker;

import android.os.AsyncTask;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Created by charl on 25/10/2017.
 */

public class UploadDataTask extends AsyncTask <String, String, String> {

    @Override
    protected String doInBackground(String... params) {
        // TODO Auto-generated method stub
        try {
            postData(params[0],params[1]);
            System.out.println("post successfull");
        } catch (IOException e) {
            System.out.println("post request failed");
            e.printStackTrace();
        }
        return "done";
    }

    public void postData(String urlString, String data) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        byte[] postData = data.getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", "utf-8");
        conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        conn.setUseCaches(false);

        try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
            wr.write( postData );
        }

        System.out.println(conn.getResponseCode());

        return;
    }
}