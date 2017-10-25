package com.example.charles.twintracker;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.example.charles.twintracker.MainActivity.PREFS_NAME;

public class DisplayHistoryActivity extends AppCompatActivity {

   // TextView txthisto11,txthisto12,txthisto13,txthisto14,txthisto15,txthisto16;
   // TextView txthisto21,txthisto22,txthisto23,txthisto24,txthisto25,txthisto26;
    ArrayList<feeding> feedings;
    ImageButton btnDownload;
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

/*        txthisto11 = (TextView)findViewById(R.id.historylast11);
        txthisto12 = (TextView)findViewById(R.id.historylast12);
        txthisto13 = (TextView)findViewById(R.id.historylast13);
        txthisto14 = (TextView)findViewById(R.id.historylast14);
        txthisto15 = (TextView)findViewById(R.id.historylast15);
        txthisto16 = (TextView)findViewById(R.id.historylast16);

        txthisto21 = (TextView)findViewById(R.id.historylast21);
        txthisto22 = (TextView)findViewById(R.id.historylast22);
        txthisto23 = (TextView)findViewById(R.id.historylast23);
        txthisto24 = (TextView)findViewById(R.id.historylast24);
        txthisto25 = (TextView)findViewById(R.id.historylast25);
        txthisto26 = (TextView)findViewById(R.id.historylast26);

        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        txthisto11.setText(settings.getString("last1",""));
        txthisto12.setText(settings.getString("prelast1",""));
        txthisto13.setText(settings.getString("last13",""));
        txthisto14.setText(settings.getString("last14",""));
        txthisto15.setText(settings.getString("last15",""));
        txthisto16.setText(settings.getString("last16",""));

        txthisto21.setText(settings.getString("last2",""));
        txthisto22.setText(settings.getString("prelast2",""));
        txthisto23.setText(settings.getString("last23",""));
        txthisto24.setText(settings.getString("last24",""));
        txthisto25.setText(settings.getString("last25",""));
        txthisto26.setText(settings.getString("last26",""));
*/
    }

        public void buttonClickHandler(View view) {

            feedings.clear();

            new DownloadWebpageTask(new AsyncResult() {
                @Override
                public void onResult(JSONObject object) {
                    processJson(object);
                }
        //    }).execute("https://spreadsheets.google.com/tq?key=1zccFXSWEoHmroT8jbOqNuIvWzSj4F9yIFKJ2thvXc2Y");
            }).execute("http://japansio.info/api/data2");
        }

        private void processJson(JSONObject object) {

            try {
                JSONArray rows = object.getJSONArray("rows");

                for (int r = 0; r < rows.length(); ++r) {
                    JSONObject row = rows.getJSONObject(r);
                    JSONArray columns = row.getJSONArray("c");

                    String name = columns.getJSONObject(0).getString("v");
                    String start = columns.getJSONObject(1).getString("v");
                    String duration = columns.getJSONObject(2).getString("v");
                    feedings.add(new feeding(name,start,duration));
                }

                final FeedingsAdapter adapter = new FeedingsAdapter(this, R.layout.feeding, feedings);
                listview.setAdapter(adapter);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
}
