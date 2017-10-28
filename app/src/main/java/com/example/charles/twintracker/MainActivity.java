package com.example.charles.twintracker;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    Button strtBttn1,strtBttn2,stopBttn1,stopBttn2,bathBttn1,bathBttn2;
    TextView txtLastDate1,txtLastDate2,txtCurrentCount1,txtCurrentCount2,txtPreLast1,txtPreLast2,txtCurrentDuration1,txtCurrentDuration2;

    ArrayList<feeding> feedings;
    Timer timer1, timer2;

    TwinTimerTask twinTimerTask1, twinTimerTask2;

    public static final String PREFS_NAME = "lastdata";

    public static boolean ongoing1,ongoing2;
    public static String started1,started2;
    public static long starttime1, starttime2;

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
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
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

    /* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
            if(position==1) {
                displayHistory(view);
            }
        }
    }

    private void selectItem(int position) {


        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mPlanetTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
    }

    /* Menu Tests */

    public void displayHistory(View view) {
        Intent intent = new Intent(this, DisplayHistoryActivity.class);
        startActivity(intent);
    }


    @Override
    protected void onStop() {
        super.onStop();

        if(this.isFinishing()) {
            //Dying System.out.println("Dying");
            final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            final SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("ongoing1", false);
            editor.putBoolean("ongoing2", false);
            editor.apply();
        }
        else {
            // Not Dying System.out.println("Not Dying");
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        final SharedPreferences.Editor editor = settings.edit();
        if(timer1!=null) {
            started1 = twinTimerTask1.getStartDate();
            starttime1 = twinTimerTask1.getStartTime();
            ongoing1 = true;
            editor.putBoolean("ongoing1",true);
            editor.apply();
        }
        if(timer1 == null) {
            ongoing1 = false;
            editor.putBoolean("ongoing1", false);
            editor.apply();
        }
        if(timer2!=null) {
            ongoing2 = true;
            editor.putBoolean("ongoing2",true);
            editor.apply();
            started2 = twinTimerTask2.getStartDate();
            starttime2 = twinTimerTask2.getStartTime();
        }
        if(timer2 == null) {
            ongoing2 = false;
            editor.putBoolean("ongoing2", false);
            editor.apply();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        ongoing1 = settings.getBoolean("ongoing1",false);
        ongoing2 = settings.getBoolean("ongoing2", false);
        if(ongoing1) {
            if(timer1==null) {
                timer1 = new Timer();
                twinTimerTask1 = new TwinTimerTask(txtCurrentCount1, txtCurrentDuration1, starttime1 ,started1);
                timer1.schedule(twinTimerTask1, 1000, 1000);
            }

        }
        if(ongoing2) {
            if(timer2==null) {
                timer2 = new Timer();
                twinTimerTask2 = new TwinTimerTask(txtCurrentCount2, txtCurrentDuration2,starttime2 ,started2);
                timer2.schedule(twinTimerTask2, 1000, 1000);
            }
        }

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

            String a1 = "";
            String a2 = "";
            int f = feedings.size();
            for(int j =0; j<f; j++)
            {
                if(feedings.get(j).getName().equals("agathe")) {
                    a2 = a1;
                    a1 = feedings.get(j).getStart() + " " + feedings.get(j).getDuration();

                }
            }
            txtLastDate1.setText(a1);
            txtPreLast1.setText(a2);

            String z1 = "";
            String z2 = "";
            for(int j =0; j<f; j++)
            {
                if(feedings.get(j).getName().equals("zoé")) {
                    z2 = z1;
                    z1 = feedings.get(j).getStart() + " " + feedings.get(j).getDuration();

                }
            }

            txtLastDate2.setText(z1);
            txtPreLast2.setText(z2);

        } catch (JSONException e) {
            Toast.makeText(getApplicationContext(),"Erreur de traitement des données", Toast.LENGTH_SHORT);
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        feedings = new ArrayList<>();

        setContentView(R.layout.activity_main);

        /* Menu Tests */

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        ActionBarDrawerToggle mDrawerToggle;

        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();

        mTitle = mDrawerTitle = "Twin Tracker";
        mPlanetTitles = new String[]{"Accueil", "Historique"};
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);


        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mPlanetTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close)
            {

                public void onDrawerClosed(View view)
                {
                    supportInvalidateOptionsMenu();
                    //drawerOpened = false;
                }

                public void onDrawerOpened(View drawerView)
                {
                    supportInvalidateOptionsMenu();
                    //drawerOpened = true;
                }
            };
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            mDrawerLayout.setDrawerListener(mDrawerToggle);
            mDrawerToggle.syncState();
        }

        if (savedInstanceState == null) {
            selectItem(0);
        }
        /* Menu Tests */

        strtBttn1 = (Button)findViewById(R.id.start_button_1);
        strtBttn2 = (Button)findViewById(R.id.start_button_2);
        stopBttn1 = (Button)findViewById(R.id.stop_button_1);
        stopBttn2 = (Button)findViewById(R.id.stop_button_2);
        bathBttn1 = (Button)findViewById(R.id.bath_button_1);
        bathBttn2 = (Button)findViewById(R.id.bath_button_2);

        txtCurrentCount1 = (TextView)findViewById(R.id.current_timer_1);
        txtCurrentCount2 = (TextView)findViewById(R.id.current_timer_2);
        txtLastDate1 = (TextView)findViewById(R.id.last1_time);
        txtLastDate2 = (TextView)findViewById(R.id.last2_time);
        txtPreLast1 = (TextView)findViewById(R.id.prelast1_time);
        txtPreLast2 = (TextView)findViewById(R.id.prelast2_time);

        txtCurrentDuration1 = (TextView)findViewById(R.id.current_duration_1);
        txtCurrentDuration2 = (TextView)findViewById(R.id.current_duration_2);

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

        bathBttn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(timer1 != null) {
                    timer1.cancel();
                    timer1 = null;
                }

            }
        });

        bathBttn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(timer2 != null) {
                    timer2.cancel();
                    timer2 = null;
                }
            }
        });

        strtBttn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(timer1 != null)
                    timer1.cancel();
                timer1 = new Timer();
                twinTimerTask1 = new TwinTimerTask(txtCurrentCount1,txtCurrentDuration1);
                timer1.schedule(twinTimerTask1,1000,1000);
                txtCurrentCount1.setText("");
                txtCurrentDuration1.setText("");
            }
        });

        strtBttn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(timer2 != null)
                    timer2.cancel();
                timer2 = new Timer();
                twinTimerTask2 = new TwinTimerTask(txtCurrentCount2,txtCurrentDuration2);
                timer2.schedule(twinTimerTask2,1000,1000);
                txtCurrentCount2.setText("");
                txtCurrentDuration2.setText("");
            }
        });

        stopBttn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timer1!=null) {

                    AlertDialog.Builder builder;
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Enregister")
                            .setMessage("La têtée est terminée ?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                                    if(networkInfo != null && networkInfo.isConnected()) {
                                        timer1.cancel();
                                        timer1 = null;
                                        txtPreLast1.setText(txtLastDate1.getText());
                                        txtLastDate1.setText(txtCurrentCount1.getText().toString() + "  " + txtCurrentDuration1.getText().toString());

                                        feedings.add(new feeding("agathe", txtCurrentCount1.getText().toString(), txtCurrentDuration1.getText().toString()));
                                        String json = new Gson().toJson(feedings);

                                        System.out.println(json);
                                        new UploadDataTask().execute("http://japansio.info/api/putdata.php", json);

                                        txtCurrentCount1.setText("");
                                        txtCurrentDuration1.setText("");
                                        Toast.makeText(getApplicationContext(),"Donnée Enregistrée",Toast.LENGTH_SHORT).show();
                                    }
                                    if(networkInfo == null || !networkInfo.isConnected()) {
                                        Toast.makeText(getApplicationContext(),"Pas de Connection Internet",Toast.LENGTH_LONG).show();
                                    }


                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .show();
                }
                else {

                    AlertDialog.Builder builder;
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Enregister")
                            .setMessage("La têtée est terminée ?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                                    if(networkInfo != null && networkInfo.isConnected()) {
                                        txtPreLast1.setText(txtLastDate1.getText());
                                        txtLastDate1.setText(txtCurrentCount1.getText().toString() + "  " + txtCurrentDuration1.getText().toString());

                                        feedings.add(new feeding("agathe", txtCurrentCount1.getText().toString(), txtCurrentDuration1.getText().toString()));
                                        String json = new Gson().toJson(feedings);

                                        System.out.println(json);
                                        new UploadDataTask().execute("http://japansio.info/api/putdata.php", json);

                                        txtCurrentCount1.setText("");
                                        txtCurrentDuration1.setText("");
                                        Toast.makeText(getApplicationContext(),"Donnée Enregistrée",Toast.LENGTH_SHORT).show();
                                    }
                                    if(networkInfo == null || !networkInfo.isConnected()) {
                                        Toast.makeText(getApplicationContext(),"Pas de Connection Internet",Toast.LENGTH_LONG).show();
                                    }


                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .show();
                }
            }
        });

        stopBttn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timer2!=null) {

                    AlertDialog.Builder builder;
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Enregister")
                            .setMessage("La têtée est terminée ?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                                    if(networkInfo != null && networkInfo.isConnected()) {
                                    timer2.cancel();
                                    timer2 = null;

                                    txtPreLast2.setText(txtLastDate2.getText());
                                    txtLastDate2.setText(txtCurrentCount2.getText().toString()+"  "+txtCurrentDuration2.getText().toString());

                                    feedings.add(new feeding("zoé",txtCurrentCount2.getText().toString(),txtCurrentDuration2.getText().toString()));
                                    String json = new Gson().toJson(feedings);

                                    System.out.println(json);
                                    new UploadDataTask().execute("http://japansio.info/api/putdata.php",json);

                                    txtCurrentCount2.setText("");
                                    txtCurrentDuration2.setText("");
                                        Toast.makeText(getApplicationContext(),"Donnée Enregistrée",Toast.LENGTH_SHORT).show();
                                    }
                                    if(networkInfo == null || !networkInfo.isConnected()) {
                                        Toast.makeText(getApplicationContext(),"Pas de Connection Internet",Toast.LENGTH_LONG).show();
                                    }
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .show();
                }
                else {
                    AlertDialog.Builder builder;
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Enregister")
                            .setMessage("La têtée est terminée ?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                                    if(networkInfo != null && networkInfo.isConnected()) {
                                    txtPreLast2.setText(txtLastDate2.getText());
                                    txtLastDate2.setText(txtCurrentCount2.getText().toString()+"  "+txtCurrentDuration2.getText().toString());

                                    feedings.add(new feeding("zoé",txtCurrentCount2.getText().toString(),txtCurrentDuration2.getText().toString()));
                                    String json = new Gson().toJson(feedings);

                                    System.out.println(json);
                                    new UploadDataTask().execute("http://japansio.info/api/putdata.php",json);

                                    txtCurrentCount2.setText("");
                                    txtCurrentDuration2.setText("");
                                        Toast.makeText(getApplicationContext(),"Donnée Enregistrée",Toast.LENGTH_SHORT).show();
                                    }
                                    if(networkInfo == null || !networkInfo.isConnected()) {
                                        Toast.makeText(getApplicationContext(),"Pas de Connection Internet",Toast.LENGTH_LONG).show();
                                    }
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .show();
                }
            }
        });
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


    private class TwinTimerTask extends TimerTask {

        TextView refTxtView1, refTxtView2;
        public long startTime;
        public String startDate;


        public TwinTimerTask(TextView refTxtView1, TextView refTxtView2, long startTime, String startDate) {
            this.refTxtView1 = refTxtView1;
            this.refTxtView2 = refTxtView2;
            this.startTime = startTime;
            this.startDate = startDate;
        }

        public TwinTimerTask(TextView refTxtView1, TextView refTxtView2) {

            this.refTxtView1 = refTxtView1;
            this.refTxtView2 = refTxtView2;
            startTime = System.currentTimeMillis();
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
            startDate = simpleDateFormat.format(calendar.getTime());

        }

        public long getStartTime() {
            return startTime;
        }

        public String getStartDate() {return startDate;}

        @Override
        public void run() {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {


                    long elapsed = System.currentTimeMillis()- startTime;
                    long seconds = elapsed/1000;
                    long s = seconds % 60;
                    long m = (seconds / 60) % 60;
                    long h = (seconds / (60 * 60)) % 24;
                    String timertext = String.format("%d:%02d:%02d", h,m,s);
                    refTxtView2.setText("("+timertext+")");
                    refTxtView1.setText(startDate);
                }
            });
        }
    }
}
