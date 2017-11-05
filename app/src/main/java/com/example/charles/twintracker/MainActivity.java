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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
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

    //Main class of the app

    //standard UI items
    Button strtBttn1,strtBttn2,stopBttn1,stopBttn2,bathBttn1,bathBttn2;
    TextView txtLastDate1,txtLastDate2,txtCurrentCount1,txtCurrentCount2,txtPreLast1,txtPreLast2,txtCurrentDuration1,txtCurrentDuration2;

    //Popup Items
    String selected_name;
    String input_started;
    String input_duration;
    EditText start_input, duration_input;
    Spinner select_name;
    AlertDialog.Builder feeding_input;

    //Structured data Filled from the API
    ArrayList<feeding> feedings;

    //Time objects for tracking
    Timer timer1, timer2;
    TwinTimerTask twinTimerTask1, twinTimerTask2;

    //APIResources
    public static final String GET_DATA_URL = "http://japansio.info/api/feedings.json";
    public static final String PUT_DATA_URL = "http://japansio.info/api/putdata.php";

    //<using preferences to save data locally, namely status of t imers to handle App being sent to background
    public static final String PREFS_NAME = "lastdata";

    public static boolean ongoing1,ongoing2;
    public static String started1,started2;
    public static long starttime1, starttime2;

    // Drawer menu items

    private Toolbar toolbar;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private android.support.v4.app.ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mPlanetTitles;

    //Populate the drawer menu
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

    //Handles the toolbar menu with two items : sync and edit
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_sync: {

                //test for network
                ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                if (networkInfo != null && networkInfo.isConnected()) {

                    //empty dataset
                    feedings.clear();

                    //asynchronously calls the API
                    new DownloadWebpageTask(new AsyncResult() {
                        @Override
                        public void onResult(JSONArray object) {
                            processJson(object);
                        }
                    }).execute(GET_DATA_URL);
                }
                //if no network, warn user with toast
                if (networkInfo == null || !networkInfo.isConnected()) {
                    Toast.makeText(getApplicationContext(), "Pas de Connection Internet", Toast.LENGTH_LONG).show();
                }
                return true;
            }
            case R.id.action_edit: {
                LayoutInflater factory = LayoutInflater.from(this);
                final View feeding_input_form = factory.inflate(R.layout.feeding_form,null);

                select_name = (Spinner) feeding_input_form.findViewById(R.id.select_name);

                // Create an ArrayAdapter using the string array and a default spinner layout
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                        R.array.twins, android.R.layout.simple_spinner_item);
                // Specify the layout to use when the list of choices appears
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                // Apply the adapter to the spinner
                select_name.setAdapter(adapter);

                select_name.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        selected_name = parent.getItemAtPosition(position).toString();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                start_input = (EditText) feeding_input_form.findViewById(R.id.start_input);
                duration_input = (EditText)feeding_input_form.findViewById(R.id.duration_input);
                feeding_input = new AlertDialog.Builder(this);
                feeding_input.setIcon(R.mipmap.ic_edit).setTitle("Enregistrer une têtée").setView(feeding_input_form).setPositiveButton("Enregistrer",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                /* User clicked OK so do some stuff */
                                input_duration = duration_input.getText().toString();
                                input_started = start_input.getText().toString();

                                ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                                if(networkInfo != null && networkInfo.isConnected()) {
                                //Toast.makeText(getApplicationContext(), selected_name + " / " + input_started + " / " + input_duration, Toast.LENGTH_SHORT).show();
                                feedings.add(new feeding(selected_name,input_started,input_duration));
                                String json = new Gson().toJson(feedings);

                                System.out.println(json);
                                new UploadDataTask().execute(PUT_DATA_URL, json);
                                Toast.makeText(getApplicationContext(),"Donnée Enregistrée",Toast.LENGTH_SHORT).show();
                            }
                                    if(networkInfo == null || !networkInfo.isConnected()) {
                                Toast.makeText(getApplicationContext(),"Pas de Connection Internet",Toast.LENGTH_LONG).show();
                            }


                            }
                        }).setNegativeButton("Annuler",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
     /*
     * User clicked cancel so do some stuff
     */
                            }
                        });
                feeding_input.show();
            }
            default:
                return true;
        }
    }


    /* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
            //Menu Item navigates to history page
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


    //Calls History page by creating an instance of the DisplayHistoryActivity class
    public void displayHistory(View view) {
        Intent intent = new Intent(this, DisplayHistoryActivity.class);
        startActivity(intent);
    }


    //This is called when the app is being killed. Timer status are deleted from preferences
    @Override
    protected void onStop() {
        super.onStop();

        if(this.isFinishing()) {
            //The app is really terminating, reset all remaining timers
            final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            final SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("ongoing1", false);
            editor.putBoolean("ongoing2", false);
            editor.apply();
        }
        else {
            // App Not Dying, do noting because on pause was called first and on resume will be called next
        }

    }

    //This is called when the app goes into the background. Timer status are stored in preferences
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

    //This is called when app retunrs in foreground, timers are restarted according to their status on leaving read from preferences
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


        //This is called on success of the HTTP GET request to the API and parses the json response into an array of structured data (custom object feeding)
    private void processJson(JSONArray object) {

        try {

            //read from the end of the dataset to display last feedings first
            for(int r=0; r< object.length(); ++r) {
                JSONObject row = object.getJSONObject(r);
                String name = row.getString("name");
                String duration = row.getString("duration");
                String start = row.getString("start");
                feedings.add(new feeding(name,start,duration));
            }

            //Fetch latest data for Twin1 (agathe) and the one before that
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

            //Fetch latest data for Twin2 (Zoé) and the one before that
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
            //In case parsing goes wrong
            Toast.makeText(getApplicationContext(),"Erreur de traitement des données", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }


    //Create the main activity page
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        feedings = new ArrayList<>();

        setContentView(R.layout.activity_main);

        /* Drawer Menu Stuff */

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

        //Standard UI components

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

        //Check for network and fetch data from API on creation of the page to fill the "last data" fields
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

        //Stop timer N1
        //TODO refactor this to change "bath" to "stop"
        bathBttn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(timer1 != null) {
                    timer1.cancel();
                    timer1 = null;
                }

            }
        });

        //Stop timer N2
        //TODO refactor this to change "bath" to "stop"
        bathBttn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(timer2 != null) {
                    timer2.cancel();
                    timer2 = null;
                }
            }
        });

        //Start Timer N1
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

        //Start Timer N2
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

        //Record data of timer N1
        //TODO refactor this to change Stop into Record or save
        //If timer is running or data not empty (stopped by user), display a popup to confirm saving and send the data to the API in JSON through a POST request.
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
                                        new UploadDataTask().execute(PUT_DATA_URL, json);

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
                                        new UploadDataTask().execute(PUT_DATA_URL, json);

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

        //Record data of timer N1
        //TODO refctor this to change Stop into Record or save
        //If timer is running or data not empty (stopped by user), display a popup to confirm saving and send the data to the API in JSON through a POST request.
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
                                    new UploadDataTask().execute(PUT_DATA_URL,json);

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
                                    new UploadDataTask().execute(PUT_DATA_URL,json);

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


    //Handle drawer call from action bar
    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
//        mDrawerToggle.syncState();
    }

    //Handle drawer call from action bar
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
  //      mDrawerToggle.onConfigurationChanged(newConfig);
    }


    //Custom timer task to schedeule a counter to track time elapsed from button click and display it in hh:mm:ss format.
    private class TwinTimerTask extends TimerTask {

        TextView refTxtView1, refTxtView2;
         long startTime;
         String startDate;


        //Constructor on resume case, the start point is passed and the elapse time is calculated from this start
         TwinTimerTask(TextView refTxtView1, TextView refTxtView2, long startTime, String startDate) {
            this.refTxtView1 = refTxtView1;
            this.refTxtView2 = refTxtView2;
            this.startTime = startTime;
            this.startDate = startDate;
        }

        //Basic constructor, takes current time as startpoint
         TwinTimerTask(TextView refTxtView1, TextView refTxtView2) {

            this.refTxtView1 = refTxtView1;
            this.refTxtView2 = refTxtView2;
            startTime = System.currentTimeMillis();
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("HH");
             SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("HH");
            startDate = simpleDateFormat1.format(calendar.getTime())+ "h"+ simpleDateFormat2.format(calendar.getTime());

        }

        //Getters and Setters
        long getStartTime() {
            return startTime;
        }

        String getStartDate() {return startDate;}

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
