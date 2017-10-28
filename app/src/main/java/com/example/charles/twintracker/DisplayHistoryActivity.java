package com.example.charles.twintracker;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class DisplayHistoryActivity extends AppCompatActivity {

    ArrayList<feeding> feedings;
    ListView listview;

        /* menu tests */

    private Toolbar toolbar;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private android.support.v4.app.ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mPlanetTitles;

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
                    }).execute("http://japansio.info/api/feedings.json");
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


    /* Menu Tests */


    public void displayHome(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        feedings = new ArrayList<>();

        setContentView(R.layout.activity_display_history);

        /* Menu Tests */

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
            }).execute("http://japansio.info/api/feedings.json");
        }
        if(networkInfo == null || !networkInfo.isConnected()) {
            Toast.makeText(getApplicationContext(),"Pas de Connection Internet",Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
//        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        //      mDrawerToggle.onConfigurationChanged(newConfig);
    }

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
