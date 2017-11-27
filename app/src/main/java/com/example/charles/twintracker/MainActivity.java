package com.example.charles.twintracker;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.support.v7.app.NotificationCompat;
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
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    //Main class of the app

    //standard UI items
    Button strtBttn1,strtBttn2,stopBttn1,stopBttn2,vitaminBttn1,vitaminBttn2,ironBttn1,ironBttn2;
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
    ArrayList<vitamin> vitamins;
    ArrayList<iron> ironinputs;
    ArrayList<liveFeed> liveEvents;
    liveFeed liveTwin1, liveTwin2;
    iron currentiron1,currentiron2;
    int ironindex1,ironindex2,liveTwin1index,liveTwin2index;

    //Time objects for tracking
    Timer timer1, timer2;
    TwinTimerTask twinTimerTask1, twinTimerTask2;

    //APIResources
    public static final String GET_DATA_URL = "http://japansio.info/api/feedings.json";
    public static final String PUT_DATA_URL = "http://japansio.info/api/putdata.php";
    public static final String PUT_VITAMIN_DATA_URL = "http://japansio.info/api/putvitamindata.php";
    public static final String GET_VITAMIN_DATA_URL = "http://japansio.info/api/vitamin.json";
    public static final String PUT_IRON_DATA_URL = "http://japansio.info/api/putirondata.php";
    public static final String GET_IRON_DATA_URL = "http://japansio.info/api/iron.json";
    public static final String GET_LIVEFEED_DATA_URL = "http://japansio.info/api/livefeed.json";
    public static final String PUT_LIVEFEED_DATA_URL = "http://japansio.info/api/putlivefeed.php";

    //Using preferences to save data locally, namely status of timers to handle App being sent to background
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
    private String[] mmenutTitles;

    //triggers updates in background on a regular basis
    // Setup a recurring alarm every half hour
    public void scheduleAlarm(liveFeed live1, liveFeed live2) {
        // Construct an intent that will execute the AlarmReceiver
        Intent intent = new Intent(getApplicationContext(), twinTrackerAlarmReceiver.class);
        String latest1, latest2;
        latest1 = "";
        latest2 = "";
        if(live1!=null) {
            latest1 = live1.getStartDate();
        }
        if(live2!=null) {
            latest2 = live2.getStartDate();
        }
        intent.putExtra("latest1",latest1);
        intent.putExtra("latest2", latest2);
        // Create a PendingIntent to be triggered when the alarm goes off
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, twinTrackerAlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // Setup periodic alarm every every half hour from this point onwards
        long firstMillis = System.currentTimeMillis(); // alarm is set right away
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        // First parameter is the type: ELAPSED_REALTIME, ELAPSED_REALTIME_WAKEUP, RTC_WAKEUP
        // Interval can be INTERVAL_FIFTEEN_MINUTES, INTERVAL_HALF_HOUR, INTERVAL_HOUR, INTERVAL_DAY
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis, 10000, pIntent);
    }

    public void cancelAlarm() {
        Intent intent = new Intent(getApplicationContext(), twinTrackerAlarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, twinTrackerAlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pIntent);
    }

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

                    //empty datasets
                    feedings.clear();
                    vitamins.clear();
                    ironinputs.clear();
                    liveEvents.clear();
                    ironindex1 = 0;
                    ironindex2 = 0;

                    //asynchronously calls the API

                    //feedings
                    new DownloadWebpageTask(new AsyncResult() {
                        @Override
                        public void onResult(JSONArray object) {
                            processJson(object);
                        }
                    }).execute(GET_DATA_URL);

                    //Vitamins
                    new DownloadWebpageTask(new AsyncResult() {
                        @Override
                        public void onResult(JSONArray object) {
                            processJsonvitamin(object);
                        }
                    }).execute(GET_VITAMIN_DATA_URL);

                    //iron
                    new DownloadWebpageTask(new AsyncResult() {
                        @Override
                        public void onResult(JSONArray object) {
                            processJsonIron(object);
                        }
                    }).execute(GET_IRON_DATA_URL);

                    //get Live Feeds (remote ongoing feedings)
                    new DownloadWebpageTask(new AsyncResult() {
                        @Override
                        public void onResult(JSONArray object) {
                            processJsonLiveFeed(object);
                        }
                    }).execute(GET_LIVEFEED_DATA_URL);

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


    /* The click listener for ListView in the navigation drawer */
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
        setTitle(mmenutTitles[position]);
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
                strtBttn1.setText(R.string.pause);
            }

        }
        if(ongoing2) {
            if(timer2==null) {
                timer2 = new Timer();
                twinTimerTask2 = new TwinTimerTask(txtCurrentCount2, txtCurrentDuration2,starttime2 ,started2);
                timer2.schedule(twinTimerTask2, 1000, 1000);
                strtBttn2.setText(R.string.pause);
            }
        }

        //refresh data
        //test for network
        /* ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            liveEvents.clear();
            //get Live Feeds (remote ongoing feedings)
            new DownloadWebpageTask(new AsyncResult() {
                @Override
                public void onResult(JSONArray object) {
                    processJsonLiveFeed(object);
                }
            }).execute(GET_LIVEFEED_DATA_URL);
        } */


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

    //Callback on success of the HTTP GET request of the API and parses the JSON into an array of custom vitamin object
    private void processJsonIron(JSONArray object) {
        try {

            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat joursemaine = new SimpleDateFormat("EEEE", Locale.FRANCE);
            String aujourdhui = joursemaine.format(calendar.getTime());

            //read from the end of the dataset to display last entry first
            for(int r=0; r< object.length(); ++r) {
                JSONObject row = object.getJSONObject(r);
                String name = row.getString("name");
                String jour = row.getString("day");
                int count = row.getInt("count");
                ironinputs.add(new iron(name, jour, count));
            }


            //Fetch latest iron data for Twin1 (agathe)

            int f = ironinputs.size();
            for(int j =0; j<f; j++)
            {
                if(ironinputs.get(j).getName().equals("agathe")) {

                    currentiron1 = ironinputs.get(j);
                    ironindex1 = j;
                }
            }

            //Display the data on the Iron button : same day but one count is grey, same day and two counts is blue, previous or other day is red
            if(currentiron1!=null) {
                String bouton1 = "  Fer : " + currentiron1.getDay() + " (" + currentiron1.getCount() + ")  ";
                ironBttn1.setText(bouton1);

                if(!currentiron1.getDay().equals(aujourdhui)) {
                    ironBttn1.setBackgroundResource(R.color.colorAccent);
                }
                else {
                    if(currentiron1.getCount() == 2) {
                        ironBttn1.setBackgroundResource(R.color.colorPrimary);
                    }
                    if(currentiron1.getCount() == 1) {
                        ironBttn1.setBackgroundResource(R.color.colorNormal);
                    }
                }
            }

            //Fetch latest iron data for Twin2 (Zoé)

            for(int j =0; j<f; j++)
            {
                if(ironinputs.get(j).getName().equals("zoé")) {

                    currentiron2 = ironinputs.get(j);
                    ironindex2 = j;
                }
            }

            //Display the data on the Iron button : same day but one count is grey, same day and two counts is blue, previous or other day is red
            if(currentiron2!=null) {
                String bouton2 = "  Fer : " + currentiron2.getDay() + " (" + currentiron2.getCount() + ")  ";
                ironBttn2.setText(bouton2);

                if(!currentiron2.getDay().equals(aujourdhui)) {
                    ironBttn2.setBackgroundResource(R.color.colorAccent);
                }
                else {
                    if(currentiron2.getCount() == 2) {
                        ironBttn2.setBackgroundResource(R.color.colorPrimary);
                    }
                    if(currentiron2.getCount() == 1) {
                        ironBttn2.setBackgroundResource(R.color.colorNormal);
                    }
                }
            }
        }
        catch (JSONException e) {
            //In case parsing goes wrong
            Toast.makeText(getApplicationContext(),"Erreur de traitement des données", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    //Callback on success of the HTTP GET request of the API and parses the JSON into an array of custom vitamin object
    private void processJsonvitamin(JSONArray object) {

        try {

            //read from the end of the dataset to display last entry first
            for(int r=0; r< object.length(); ++r) {
                JSONObject row = object.getJSONObject(r);
                String name = row.getString("name");
                String jour = row.getString("day");
                vitamins.add(new vitamin(name,jour));
            }

            //Fetch latest vitamin data for Twin1 (agathe)
            String a1 = "";
            String jourvitamines1 = "";
            int f = vitamins.size();
            for(int j =0; j<f; j++)
            {
                if(vitamins.get(j).getName().equals("agathe")) {
                    jourvitamines1 = vitamins.get(j).getDay();
                    a1 = " Vitamines : "+ jourvitamines1 + " ";

                }
            }
            vitaminBttn1.setText(a1);
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat joursemaine = new SimpleDateFormat("EEEE", Locale.FRANCE);
            String aujourdhui = joursemaine.format(calendar.getTime());
            //Compare the last entry day for vitamins with current day
            if(!aujourdhui.equals(jourvitamines1)) {
                //if they do not match, display the button in red (kids must have their vitamins daily)
                vitaminBttn1.setBackgroundResource(R.color.colorAccent);
            }
            //fine if it's today
            else vitaminBttn1.setBackgroundResource(R.color.colorPrimary);

            //Fetch latest vitamin data for Twin2 (Zoé)
            String z1 = "";
            String jourvitamines2 = "";
            for(int j =0; j<f; j++)
            {
                if(vitamins.get(j).getName().equals("zoé")) {
                    jourvitamines2 = vitamins.get(j).getDay();
                    z1 = " Vitamines : " + jourvitamines2+ " ";
                }
            }
            vitaminBttn2.setText(z1);
            //Compare the last entry day for vitamins with current day
            if(!aujourdhui.equals(jourvitamines2)) {
                //if they do not match, display the button in red (kids must have their vitamins daily)
                vitaminBttn2.setBackgroundResource(R.color.colorAccent);
            }
            else vitaminBttn2.setBackgroundResource(R.color.colorPrimary);



        } catch (JSONException e) {
            //In case parsing goes wrong
            Toast.makeText(getApplicationContext(),"Erreur de traitement des données", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void processJsonLiveFeed(JSONArray object) {

        try {
            //read from the end of the dataset to display last entry first
            for(int r=0; r< object.length(); ++r) {
                JSONObject row = object.getJSONObject(r);
                String name = row.getString("name");
                Boolean isgoing = row.getBoolean("isOngoing");
                long starttime = row.getLong("startTime");
                String startDate = row.getString("startDate");
                liveEvents.add(new liveFeed(name, isgoing, starttime,startDate));
            }

            int f = liveEvents.size();

            //get the latest event for agathe
            for(int j =0; j<f; j++)
            {
                if(liveEvents.get(j).getName().equals("agathe")) {

                    liveTwin1 = liveEvents.get(j);
                    liveTwin1index = j;
                }
            }

            //get the latest event for Zoé
            for(int j =0; j<f; j++)
            {
                if(liveEvents.get(j).getName().equals("zoé")) {

                    liveTwin2 = liveEvents.get(j);
                    liveTwin2index = j;
                }
            }

            //resume timer if there is a live feed for twin 1
            if(liveTwin1!=null && liveTwin1.getOngoing()) {

                if(timer1==null) {
                    timer1 = new Timer();
                    twinTimerTask1 = new TwinTimerTask(txtCurrentCount1, txtCurrentDuration1, liveTwin1.getStartTime() ,liveTwin1.getStartDate());
                    timer1.schedule(twinTimerTask1, 1000, 1000);
                    strtBttn1.setText(R.string.pause);
                }
            }

            //stop any timer if ongoing is false on the server

            if(liveTwin1!=null && !liveTwin1.getOngoing()) {

                if(timer1!=null) {
                    timer1.cancel();
                    txtCurrentCount1.setText("");
                    txtCurrentDuration1.setText("");
                    strtBttn1.setText(R.string.start);
                    final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                    final SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("ongoing1", false);
                    editor.apply();
                }
            }

            if(liveTwin2!=null && !liveTwin2.getOngoing()) {

                if(timer2!=null) {
                    timer2.cancel();
                    txtCurrentCount2.setText("");
                    txtCurrentDuration2.setText("");
                    strtBttn2.setText(R.string.start);
                    final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                    final SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("ongoing2", false);
                    editor.apply();
                }
            }


            //resume timer if there is a live feed for twin 1
            if(liveTwin2!=null && liveTwin2.getOngoing()) {

                if(timer2==null) {
                    timer2 = new Timer();
                    twinTimerTask2 = new TwinTimerTask(txtCurrentCount2, txtCurrentDuration2, liveTwin2.getStartTime() ,liveTwin2.getStartDate());
                    timer2.schedule(twinTimerTask2, 1000, 1000);
                    strtBttn2.setText(R.string.pause);
                }
            }

            scheduleAlarm(liveTwin1,liveTwin2);


        }
        catch (JSONException e) {
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
        vitamins = new ArrayList<>();
        ironinputs = new ArrayList<>();
        ironindex1 = ironindex2 = 0;
        liveEvents = new ArrayList<>();
        liveTwin1index = liveTwin2index =0;

        setContentView(R.layout.activity_main);

        /* Drawer Menu Stuff */

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        ActionBarDrawerToggle mDrawerToggle;

        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();

        mTitle = mDrawerTitle = "Twin Tracker";
        mmenutTitles = new String[]{"Accueil", "Historique"};
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);


        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mmenutTitles));
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

        //notification
        final NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final Intent notificationIntent = new Intent(this,   MainActivity.class);
        notificationIntent.putExtra("extra", "value");
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.setAction("android.intent.action.MAIN");
        notificationIntent.addCategory("android.intent.category.LAUNCHER");

        final PendingIntent contentIntent = PendingIntent
                .getActivity(this, 0, notificationIntent,0);

        //Standard UI components

        strtBttn1 = (Button)findViewById(R.id.start_button_1);
        strtBttn2 = (Button)findViewById(R.id.start_button_2);
        stopBttn1 = (Button)findViewById(R.id.stop_button_1);
        stopBttn2 = (Button)findViewById(R.id.stop_button_2);
        vitaminBttn1 = (Button)findViewById(R.id.vitamin_button_1);
        vitaminBttn2 = (Button)findViewById(R.id.vitamin_button_2);
        ironBttn1 = (Button)findViewById(R.id.iron_button_1);
        ironBttn2 = (Button)findViewById(R.id.iron_button_2);

        txtCurrentCount1 = (TextView)findViewById(R.id.current_timer_1);
        txtCurrentCount2 = (TextView)findViewById(R.id.current_timer_2);
        txtLastDate1 = (TextView)findViewById(R.id.last1_time);
        txtLastDate2 = (TextView)findViewById(R.id.last2_time);
        txtPreLast1 = (TextView)findViewById(R.id.prelast1_time);
        txtPreLast2 = (TextView)findViewById(R.id.prelast2_time);

        txtCurrentDuration1 = (TextView)findViewById(R.id.current_duration_1);
        txtCurrentDuration2 = (TextView)findViewById(R.id.current_duration_2);

        //Check for network and fetch data from API on creation of the page to fill the "last data" fields and vitamin buttons
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();


        if(networkInfo != null && networkInfo.isConnected()) {

            //get Feeding data
            new DownloadWebpageTask(new AsyncResult() {
                @Override
                public void onResult(JSONArray object) {
                    processJson(object);
                }
            }).execute(GET_DATA_URL);

            //get Vitamin data
            new DownloadWebpageTask(new AsyncResult() {
                @Override
                public void onResult(JSONArray object) {
                    processJsonvitamin(object);
                }
            }).execute(GET_VITAMIN_DATA_URL);

            //get Iron data
            new DownloadWebpageTask(new AsyncResult() {
                @Override
                public void onResult(JSONArray object) {
                    processJsonIron(object);
                }
            }).execute(GET_IRON_DATA_URL);

            //get Live Feeds (remote ongoing feedings)
            new DownloadWebpageTask(new AsyncResult() {
                @Override
                public void onResult(JSONArray object) {
                    processJsonLiveFeed(object);
                }
            }).execute(GET_LIVEFEED_DATA_URL);

        }
        if(networkInfo == null || !networkInfo.isConnected()) {
            //inform the user if no network is connected
            Toast.makeText(getApplicationContext(),"Pas de Connection Internet",Toast.LENGTH_LONG).show();
        }

        // Click handler for the Iron Input button for twin 1 Adds 1 count to the current day to reach a maximum of 2. Sends the data to the API
        ironBttn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat joursemaine = new SimpleDateFormat("EEEE", Locale.FRANCE);
                String jourfer = joursemaine.format(calendar.getTime());

                if(currentiron1 != null && currentiron1.getDay().equals(jourfer)) {
                    if(currentiron1.getCount() ==2) {
                        //nothing to do but inform the user if iron was already given twice today
                        Toast.makeText(getApplicationContext(), "Fer déjà donné 2 fois aujourd'hui", Toast.LENGTH_LONG).show();
                    }
                    if(currentiron1.getCount() ==1) {
                        //Iron was given only once today : update today's data with incremented iron counter
                        currentiron1.setCount(2);
                        ironinputs.get(ironindex1).setCount(2);
                        String bouton1 = "  Fer : " + currentiron1.getDay() + " (" + currentiron1.getCount() +")  ";
                        ironBttn1.setText(bouton1);
                        ironBttn1.setBackgroundResource(R.color.colorPrimary);
                        String json = new Gson().toJson(ironinputs);

                        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                        if (networkInfo != null && networkInfo.isConnected()) {
                            //send new day to server if network is available
                            new UploadDataTask().execute(PUT_IRON_DATA_URL, json);
                            Toast.makeText(getApplicationContext(), "Donnée Enregistrée", Toast.LENGTH_SHORT).show();

                            //force update of iron data just after input to handle index rebuilding and avoid duplication in case of double press
                            ironinputs.clear();
                            new DownloadWebpageTask(new AsyncResult() {
                                @Override
                                public void onResult(JSONArray object) {
                                    processJsonIron(object);
                                }
                            }).execute(GET_IRON_DATA_URL);
                        }
                        if (networkInfo == null || !networkInfo.isConnected()) {
                            //inform user if network is not available
                            Toast.makeText(getApplicationContext(), "Pas de Connection Internet", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                else {

                    //first Iron input of the day : create new data entry for today
                    iron ironjour1 = new iron("agathe",jourfer,1);
                    ironinputs.add(ironjour1);
                    String json = new Gson().toJson(ironinputs);

                    String bouton1 = "  Fer : " + jourfer + " (1)  ";

                    ironBttn1.setText(bouton1);
                    ironBttn1.setBackgroundResource(R.color.colorNormal);

                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                    if (networkInfo != null && networkInfo.isConnected()) {
                        //send new day to server if network is available
                        new UploadDataTask().execute(PUT_IRON_DATA_URL, json);
                        Toast.makeText(getApplicationContext(), "Donnée Enregistrée", Toast.LENGTH_SHORT).show();
                        //force update of iron data just after input to handle index rebuilding and avoid duplication in case of double press
                        ironinputs.clear();
                        new DownloadWebpageTask(new AsyncResult() {
                            @Override
                            public void onResult(JSONArray object) {
                                processJsonIron(object);
                            }
                        }).execute(GET_IRON_DATA_URL);
                    }
                    if (networkInfo == null || !networkInfo.isConnected()) {
                        //inform user if network is not available
                        Toast.makeText(getApplicationContext(), "Pas de Connection Internet", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });


        //same as ironBttn1
        ironBttn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat joursemaine = new SimpleDateFormat("EEEE", Locale.FRANCE);
                String jourfer = joursemaine.format(calendar.getTime());

                if(currentiron2!=null && currentiron2.getDay().equals(jourfer)) {
                    if(currentiron2.getCount() ==2) {
                        //nothing to do but inform the user if vitamins were already given today
                        Toast.makeText(getApplicationContext(), "Fer déjà donné 2 fois aujourd'hui", Toast.LENGTH_LONG).show();
                    }
                    if(currentiron2.getCount() ==1) {
                        currentiron2.setCount(2);
                        ironinputs.get(ironindex2).setCount(2);
                        String json = new Gson().toJson(ironinputs);

                        String bouton2 = "  Fer : " + currentiron2.getDay() + " (" + currentiron2.getCount() +")  ";
                        ironBttn2.setText(bouton2);
                        ironBttn2.setBackgroundResource(R.color.colorPrimary);

                        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                        if (networkInfo != null && networkInfo.isConnected()) {
                            //send new day to server if network is available
                            new UploadDataTask().execute(PUT_IRON_DATA_URL, json);
                            Toast.makeText(getApplicationContext(), "Donnée Enregistrée", Toast.LENGTH_SHORT).show();
                            //force update of iron data just after input to handle index rebuilding and avoid duplication in case of double press
                            ironinputs.clear();
                            new DownloadWebpageTask(new AsyncResult() {
                                @Override
                                public void onResult(JSONArray object) {
                                    processJsonIron(object);
                                }
                            }).execute(GET_IRON_DATA_URL);
                        }
                        if (networkInfo == null || !networkInfo.isConnected()) {
                            //inform user if network is not available
                            Toast.makeText(getApplicationContext(), "Pas de Connection Internet", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                else {
                    iron ironjour2 = new iron("zoé",jourfer,1);
                    ironinputs.add(ironjour2);
                    String json = new Gson().toJson(ironinputs);

                    String bouton2 = "  Fer : " + jourfer + " (1)  ";

                    ironBttn2.setText(bouton2);
                    ironBttn2.setBackgroundResource(R.color.colorNormal);

                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                    if (networkInfo != null && networkInfo.isConnected()) {
                        //send new day to server if network is available
                        new UploadDataTask().execute(PUT_IRON_DATA_URL, json);
                        Toast.makeText(getApplicationContext(), "Donnée Enregistrée", Toast.LENGTH_SHORT).show();
                        //force update of iron data just after input to handle index rebuilding and avoid duplication in case of double press
                        ironinputs.clear();
                        new DownloadWebpageTask(new AsyncResult() {
                            @Override
                            public void onResult(JSONArray object) {
                                processJsonIron(object);
                            }
                        }).execute(GET_IRON_DATA_URL);
                    }
                    if (networkInfo == null || !networkInfo.isConnected()) {
                        //inform user if network is not available
                        Toast.makeText(getApplicationContext(), "Pas de Connection Internet", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        //vitamin Button 1 Click sends today's date to the server unless current day is already the latest data
        vitaminBttn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //get latest data entry for twin 1
                String jourvitamines1 = "";
                int f = vitamins.size();
                for(int j =0; j<f; j++)
                {
                    if(vitamins.get(j).getName().equals("agathe")) {
                        jourvitamines1 = vitamins.get(j).getDay();
                    }
                }
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat joursemaine = new SimpleDateFormat("EEEE", Locale.FRANCE);
                String jourvitamines = joursemaine.format(calendar.getTime());

                //compare with current date
                if(!jourvitamines.equals(jourvitamines1)) {

                    //update text if new date
                    String vitaminbttn1 = "  Vitamines : " + jourvitamines + "  ";
                    vitaminBttn1.setText(vitaminbttn1);
                    vitaminBttn1.setBackgroundResource(R.color.colorPrimary);
                    vitamin vitamin1 = new vitamin("agathe", jourvitamines);
                    vitamins.add(vitamin1);
                    String json = new Gson().toJson(vitamins);

                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                    if (networkInfo != null && networkInfo.isConnected()) {
                        //send new day to server if network is available
                        new UploadDataTask().execute(PUT_VITAMIN_DATA_URL, json);
                        Toast.makeText(getApplicationContext(), "Donnée Enregistrée", Toast.LENGTH_SHORT).show();
                    }
                    if (networkInfo == null || !networkInfo.isConnected()) {
                        //inform user if network is not available
                        Toast.makeText(getApplicationContext(), "Pas de Connection Internet", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    //nothing to do but inform the user if vitamins were already given today
                    Toast.makeText(getApplicationContext(), "Vitamines déjà données aujourd'hui", Toast.LENGTH_LONG).show();
                }
            }
        });


        //vitamin Button 2  Click sends today's date to the server unless current day is already the latest data same as Vitamin button 1
        //TODO factor code for buttons that only differ by twin number (function with integer as parameter and button array ?)
        vitaminBttn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int f = vitamins.size();
                String jourvitamines2 = "";
                for (int j = 0; j < f; j++) {
                    if (vitamins.get(j).getName().equals("zoé")) {
                        jourvitamines2 = vitamins.get(j).getDay();
                    }
                }

                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat joursemaine = new SimpleDateFormat("EEEE", Locale.FRANCE);
                String jourvitamines = joursemaine.format(calendar.getTime());
                if (!jourvitamines.equals(jourvitamines2)) {

                    String vitaminbttn2 = "  Vitamines : " + jourvitamines + "  ";
                    vitaminBttn2.setText(vitaminbttn2);
                    vitaminBttn2.setBackgroundResource(R.color.colorPrimary);
                    vitamin vitamin2 = new vitamin("zoé", jourvitamines);
                    vitamins.add(vitamin2);
                    String json = new Gson().toJson(vitamins);

                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                    if (networkInfo != null && networkInfo.isConnected()) {
                        new UploadDataTask().execute(PUT_VITAMIN_DATA_URL, json);
                        Toast.makeText(getApplicationContext(), "Donnée Enregistrée", Toast.LENGTH_SHORT).show();
                    }
                    if (networkInfo == null || !networkInfo.isConnected()) {
                        Toast.makeText(getApplicationContext(), "Pas de Connection Internet", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Toast.makeText(getApplicationContext(), "Vitamines déjà données aujourd'hui", Toast.LENGTH_LONG).show();
                }
            }
        });

        //Start Timer N1
        strtBttn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(timer1 != null) {
                    timer1.cancel();
                    timer1 = null;

                    //cancel the ongoing feeding on the server by setting ongoing to false
                    liveTwin1.setOngoing(false);
                    liveEvents.get(liveTwin1index).setOngoing(false);
                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                    if(networkInfo != null && networkInfo.isConnected()) {

                        String json = new Gson().toJson(liveEvents);
                        new UploadDataTask().execute(PUT_LIVEFEED_DATA_URL, json);
                    }
                    if(networkInfo == null || !networkInfo.isConnected()) {
                        Toast.makeText(getApplicationContext(),"Pas de Connection Internet",Toast.LENGTH_LONG).show();
                    }

                    strtBttn1.setText(R.string.start);
                    mNotificationManager.cancel(1);
                }
                else {
                    timer1 = new Timer();
                    long now = System.currentTimeMillis();
                    twinTimerTask1 = new TwinTimerTask(txtCurrentCount1, txtCurrentDuration1);
                    timer1.schedule(twinTimerTask1, 1000, 1000);
                    strtBttn1.setText(R.string.pause);
                    txtCurrentCount1.setText("");
                    txtCurrentDuration1.setText("");
                    android.support.v4.app.NotificationCompat.Builder mnbuilder = new NotificationCompat.Builder(MainActivity.this)
                            .setSmallIcon(R.mipmap.ic_notif)
                            .setWhen(System.currentTimeMillis())  // the time stamp, you will probably use System.currentTimeMillis() for most scenarios
                            .setUsesChronometer(true)
                            .setContentTitle("Agathe")
                            .setContentText("Têtée en cours")
                            .setContentIntent(contentIntent);
                    mNotificationManager.notify(1,mnbuilder.build());

                    //inform the server that tiwn 1 is being fed
                    liveTwin1.setOngoing(true);
                    liveTwin1.setStartTime(now);
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("HH", Locale.FRANCE);
                    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("mm", Locale.FRANCE);
                    String startDate = simpleDateFormat1.format(calendar.getTime())+ "h"+ simpleDateFormat2.format(calendar.getTime());
                    liveTwin1.setStartDate(startDate);

                    liveEvents.get(liveTwin1index).setOngoing(true);
                    liveEvents.get(liveTwin1index).setStartDate(startDate);
                    liveEvents.get(liveTwin1index).setStartTime(now);

                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                    if(networkInfo != null && networkInfo.isConnected()) {

                        String json = new Gson().toJson(liveEvents);
                        new UploadDataTask().execute(PUT_LIVEFEED_DATA_URL, json);
                    }
                    if(networkInfo == null || !networkInfo.isConnected()) {
                        Toast.makeText(getApplicationContext(),"Pas de Connection Internet",Toast.LENGTH_LONG).show();
                    }


                }
            }
        });

        //Start Timer N2
        strtBttn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(timer2 != null) {
                    timer2.cancel();
                    timer2 = null;
                    strtBttn2.setText(R.string.start);
                    mNotificationManager.cancel(2);

                    //cancel the ongoing feeding on the server by setting ongoing to false
                    liveTwin2.setOngoing(false);
                    liveEvents.get(liveTwin2index).setOngoing(false);
                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                    if(networkInfo != null && networkInfo.isConnected()) {

                        String json = new Gson().toJson(liveEvents);
                        new UploadDataTask().execute(PUT_LIVEFEED_DATA_URL, json);
                    }
                    if(networkInfo == null || !networkInfo.isConnected()) {
                        Toast.makeText(getApplicationContext(),"Pas de Connection Internet",Toast.LENGTH_LONG).show();
                    }

                }
                else {
                    timer2 = new Timer();
                    long now = System.currentTimeMillis();
                    twinTimerTask2 = new TwinTimerTask(txtCurrentCount2, txtCurrentDuration2);
                    timer2.schedule(twinTimerTask2, 1000, 1000);
                    strtBttn2.setText(R.string.pause);
                    txtCurrentCount2.setText("");
                    txtCurrentDuration2.setText("");
                    android.support.v4.app.NotificationCompat.Builder mnbuilder = new NotificationCompat.Builder(MainActivity.this)
                            .setSmallIcon(R.mipmap.ic_notif)
                            .setWhen(System.currentTimeMillis())  // the time stamp, you will probably use System.currentTimeMillis() for most scenarios
                            .setUsesChronometer(true)
                            .setContentTitle("Zoé")
                            .setContentText("Têtée en cours")
                            .setContentIntent(contentIntent);
                    mNotificationManager.notify(2,mnbuilder.build());

                    //inform the server that tiwn 2 is being fed
                    liveTwin2.setOngoing(true);
                    liveEvents.get(liveTwin2index).setOngoing(true);

                    liveTwin2.setStartTime(now);
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("HH", Locale.FRANCE);
                    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("mm", Locale.FRANCE);
                    String startDate = simpleDateFormat1.format(calendar.getTime())+ "h"+ simpleDateFormat2.format(calendar.getTime());
                    liveTwin2.setStartDate(startDate);

                    liveEvents.get(liveTwin2index).setOngoing(true);
                    liveEvents.get(liveTwin2index).setStartDate(startDate);
                    liveEvents.get(liveTwin2index).setStartTime(now);


                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                    if(networkInfo != null && networkInfo.isConnected()) {

                        String json = new Gson().toJson(liveEvents);
                        new UploadDataTask().execute(PUT_LIVEFEED_DATA_URL, json);
                    }
                    if(networkInfo == null || !networkInfo.isConnected()) {
                        Toast.makeText(getApplicationContext(),"Pas de Connection Internet",Toast.LENGTH_LONG).show();
                    }
                }
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
                                        String lastdate1 = txtCurrentCount1.getText().toString() + "  " + txtCurrentDuration1.getText().toString();
                                        txtLastDate1.setText(lastdate1);

                                        feedings.add(new feeding("agathe", txtCurrentCount1.getText().toString(), txtCurrentDuration1.getText().toString()));
                                        String json = new Gson().toJson(feedings);

                                        new UploadDataTask().execute(PUT_DATA_URL, json);

                                        txtCurrentCount1.setText("");
                                        txtCurrentDuration1.setText("");
                                        Toast.makeText(getApplicationContext(),"Donnée Enregistrée",Toast.LENGTH_SHORT).show();
                                        strtBttn1.setText(R.string.start);
                                        mNotificationManager.cancel(1);

                                        //cancel live feeding on the server
                                        liveTwin1.setOngoing(false);
                                        liveEvents.get(liveTwin1index).setOngoing(false);
                                        String jsonl = new Gson().toJson(liveEvents);
                                        new UploadDataTask().execute(PUT_LIVEFEED_DATA_URL, jsonl);


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
                                        String lastdate1 = txtCurrentCount1.getText().toString() + "  " + txtCurrentDuration1.getText().toString();
                                        txtLastDate1.setText(lastdate1);

                                        feedings.add(new feeding("agathe", txtCurrentCount1.getText().toString(), txtCurrentDuration1.getText().toString()));
                                        String json = new Gson().toJson(feedings);

                                        new UploadDataTask().execute(PUT_DATA_URL, json);

                                        txtCurrentCount1.setText("");
                                        txtCurrentDuration1.setText("");
                                        Toast.makeText(getApplicationContext(),"Donnée Enregistrée",Toast.LENGTH_SHORT).show();
                                        strtBttn1.setText(R.string.start);
                                        mNotificationManager.cancel(1);

                                        //cancel live feeding on the server
                                        liveTwin1.setOngoing(false);
                                        liveEvents.get(liveTwin1index).setOngoing(false);
                                        String jsonl = new Gson().toJson(liveEvents);
                                        new UploadDataTask().execute(PUT_LIVEFEED_DATA_URL, jsonl);

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
                                    String lastdate2 = txtCurrentCount2.getText().toString()+"  "+txtCurrentDuration2.getText().toString();
                                    txtLastDate2.setText(lastdate2);

                                    feedings.add(new feeding("zoé",txtCurrentCount2.getText().toString(),txtCurrentDuration2.getText().toString()));
                                    String json = new Gson().toJson(feedings);

                                    new UploadDataTask().execute(PUT_DATA_URL,json);

                                    txtCurrentCount2.setText("");
                                    txtCurrentDuration2.setText("");
                                        Toast.makeText(getApplicationContext(),"Donnée Enregistrée",Toast.LENGTH_SHORT).show();
                                        strtBttn2.setText(R.string.start);
                                        mNotificationManager.cancel(2);

                                        //cancel live feeding on the server
                                        liveTwin2.setOngoing(false);
                                        liveEvents.get(liveTwin2index).setOngoing(false);
                                        String jsonl = new Gson().toJson(liveEvents);
                                        new UploadDataTask().execute(PUT_LIVEFEED_DATA_URL, jsonl);

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
                                    String lastdate2 = txtCurrentCount2.getText().toString()+"  "+txtCurrentDuration2.getText().toString();
                                    txtLastDate2.setText(lastdate2);

                                    feedings.add(new feeding("zoé",txtCurrentCount2.getText().toString(),txtCurrentDuration2.getText().toString()));
                                    String json = new Gson().toJson(feedings);

                                    new UploadDataTask().execute(PUT_DATA_URL,json);

                                    txtCurrentCount2.setText("");
                                    txtCurrentDuration2.setText("");
                                        Toast.makeText(getApplicationContext(),"Donnée Enregistrée",Toast.LENGTH_SHORT).show();
                                        strtBttn2.setText(R.string.start);
                                        mNotificationManager.cancel(2);

                                        //cancel live feeding on the server
                                        liveTwin2.setOngoing(false);
                                        liveEvents.get(liveTwin2index).setOngoing(false);
                                        String jsonl = new Gson().toJson(liveEvents);
                                        new UploadDataTask().execute(PUT_LIVEFEED_DATA_URL, jsonl);
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
    protected void onDestroy() {
        super.onDestroy();

        cancelAlarm();

        if(liveEvents!=null) {liveEvents.clear();}

        if(timer1!=null) {timer1.cancel();}
        if(timer2!=null) {timer2.cancel();}

        //cleanup just like when app is dying because the back button behavior and the delete app from recent activity is different
        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        final SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("ongoing1", false);
        editor.putBoolean("ongoing2", false);
        editor.apply();

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


    //Custom timer task to schedule a counter to track time elapsed from button click and display it in hh:mm:ss format.
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
            SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("HH", Locale.FRANCE);
             SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("mm", Locale.FRANCE);
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
                    String timertext = String.format(Locale.FRANCE,"%02d:%02d",m,s);
                    refTxtView2.setText(timertext);
                    refTxtView1.setText(startDate);
                }
            });
        }
    }
}
