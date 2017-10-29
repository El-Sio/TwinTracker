package com.example.charles.twintracker;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

//Display History of the feedings for both twins in the form of a list view populated from the JSON data through a HTTP GET request on the API
public class DisplayHistoryActivity extends AppCompatActivity {

    public static final String GET_DATA_URL = "http://japansio.info/api/feedings.json";
    ArrayList<feeding> feedings;
    ListView listview;

    //Menu stuff : a single action menu in action bar to refresh the page
    //TODO implement support of pull to refresh widget on the ListView
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        return super.onPrepareOptionsMenu(menu);
    }

    //Handle menu interaction : Navigate back to Main activity (home) or refresh data (re-send HttP get request if the network is available
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.action_sync: {
                feedings.clear();

                ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                if(networkInfo != null && networkInfo.isConnected()) {
                    new DownloadWebpageTask(new AsyncResult() {
                        @Override
                        public void onResult(JSONArray object) {
                            processJson(object);
                        }
                    }).execute(GET_DATA_URL);
                }
                if(networkInfo == null || !networkInfo.isConnected()) {
                    Toast.makeText(getApplicationContext(),"Pas de Connection Internet",Toast.LENGTH_LONG).show();
                }
                return true;
            }
            case R.id.home: {
                displayHome(findViewById(R.id.home));
                return true;
            }
            default:return super.onOptionsItemSelected(item);
        }
    }



// Method to navigate back to main activity
//TODO figure out if this is useless considering it's just a child of the Main Activity
    public void displayHome(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }


    //Create the Display History page, and downloads data from the API through a GET HTTP request.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        feedings = new ArrayList<>();

        setContentView(R.layout.activity_display_history);

        //Menu Stuff

        Toolbar toolbar = (Toolbar) findViewById(R.id.child_toolbar);

        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        /* Menu Tests */


        listview = (ListView)findViewById(R.id.listview);

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if(networkInfo != null && networkInfo.isConnected()) {
            new DownloadWebpageTask(new AsyncResult() {
                @Override
                public void onResult(JSONArray object) {
                    processJson(object);
                }
            }).execute(GET_DATA_URL);
        }
        if(networkInfo == null || !networkInfo.isConnected()) {
            Toast.makeText(getApplicationContext(),"Pas de Connection Internet",Toast.LENGTH_LONG).show();
        }

    }

    //Handle menu from action bar
    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
//        mDrawerToggle.syncState();
    }

    //Handle menu from action bar
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        //      mDrawerToggle.onConfigurationChanged(newConfig);
    }

    //Process thz JSON data fetched from the API through the GET HTTP request.
    private void processJson(JSONArray object) {

                try {

                    for(int r=object.length(); r>0; --r) {
                        JSONObject row = object.getJSONObject(r-1);
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
