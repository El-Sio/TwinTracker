package com.example.charles.twintracker;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class DisplayHistoryActivity extends AppCompatActivity {

    ArrayList<feeding> feedings;
    ImageButton btnDownload,btnUpload;
    ListView listview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        feedings = new ArrayList<feeding>();

        setContentView(R.layout.activity_display_history);

        btnDownload = (ImageButton) findViewById(R.id.downloadBttn);

        listview = (ListView)findViewById(R.id.listview);

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            btnDownload.setEnabled(true);
        } else {
            btnDownload.setEnabled(false);
        }

        new DownloadWebpageTask(new AsyncResult() {
            @Override
            public void onResult(JSONArray object) {
                processJson(object);
            }
        }).execute("http://japansio.info/api/feedings.json");

    }

/*    public  void uploadClickHandler (View view) {

        String json = new Gson().toJson(feedings);

        System.out.println(json);
        new UploadDataTask().execute("http://japansio.info/api/putdata.php",json);
    }
*/
        public void buttonClickHandler(View view) {

            feedings.clear();

            new DownloadWebpageTask(new AsyncResult() {
                @Override
                public void onResult(JSONArray object) {
                    processJson(object);
                }
            }).execute("http://japansio.info/api/feedings.json");
        }

        private void processJson(JSONArray object) {

                try {

                    for(int r=0; r< object.length(); ++r) {
                        JSONObject row = object.getJSONObject(r);
                        String name = row.getString("name");
                        String duration = row.getString("duration");
                        String start = row.getString("start");
                        feedings.add(new feeding(name,start,duration));
                    }

                final FeedingsAdapter adapter = new FeedingsAdapter(this, R.layout.feeding, feedings);
                listview.setAdapter(adapter);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
}
