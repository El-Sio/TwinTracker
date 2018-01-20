package com.example.charles.twintracker;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    //Main class of the app

    //standard UI items
    Button strtBttn1,strtBttn2,stopBttn1,stopBttn2,vitaminBttn1,vitaminBttn2,ironBttn1,ironBttn2,bathBttn1,bathBttn2,inexiumBttn1,inexiumBttn2;
    TextView txtLastDate1,txtLastDate2,txtCurrentCount1,txtCurrentCount2,txtCurrentDuration1,txtCurrentDuration2,twin1label, twin2label;
    String photopath1,photopath2;
    ImageView photo1,photo2;

    ProgressDialog loadingdialog;
    int progress;
    Boolean datacomplete;
    Boolean startTwin1Intent,startTwin2Intent,stopTwin1Intent,stopTwin2Intent;

    //Popup Items
    String selected_name;
    String input_started;
    String input_duration;
    EditText start_input, duration_input;
    Spinner select_name;
    AlertDialog.Builder feeding_input;

    //Structured data Filled from the API
    ArrayList<twinSettings> preferences;
    ArrayList<feeding> feedings;
    ArrayList<vitamin> vitamins;
    ArrayList<bath> baths;
    ArrayList<iron> ironinputs;
    ArrayList<liveFeed> liveEvents;
    ArrayList<inexium> inexiums;
    liveFeed liveTwin1, liveTwin2;
    iron currentiron1,currentiron2;
    int ironindex1,ironindex2,liveTwin1index,liveTwin2index, myindex;
    Boolean shouldNotify,autostop,unsaveddata1,unsaveddata2;

    //Time objects for tracking
    Timer timer1, timer2;
    TwinTimerTask twinTimerTask1, twinTimerTask2;

    //notifications
    public NotificationManager mNotificationManager;

    //APIResources
    public static final String GET_DATA_URL = "http://japansio.info/api/feedings.json";
    public static final String PUT_DATA_URL = "http://japansio.info/api/putdata.php";
    public static final String PUT_VITAMIN_DATA_URL = "http://japansio.info/api/putvitamindata.php";
    public static final String GET_VITAMIN_DATA_URL = "http://japansio.info/api/vitamin.json";
    public static final String PUT_INEXIUM_DATA_URL = "http://japansio.info/api/putinexiumdata.php";
    public static final String GET_INEXIUM_DATA_URL = "http://japansio.info/api/inexium.json";
    public static final String PUT_BATH_DATA_URL = "http://japansio.info/api/putbathdata.php";
    public static final String GET_BATH_DATA_URL = "http://japansio.info/api/bath.json";
    public static final String PUT_IRON_DATA_URL = "http://japansio.info/api/putirondata.php";
    public static final String GET_IRON_DATA_URL = "http://japansio.info/api/iron.json";
    public static final String GET_LIVEFEED_DATA_URL = "http://japansio.info/api/livefeed.json";
    public static final String PUT_LIVEFEED_DATA_URL = "http://japansio.info/api/putlivefeed.php";
    public static final String GET_SETTINGS_URL = "http://japansio.info/api/settings.json";

    //Auto Stop Limit (30 min)
    public static  final long AUTO_STOP_DELAY = 30*60*1000;

    //Auto Stop testing (30s)
    //public static  final long AUTO_STOP_DELAY = 30*1000;

    //unique user ID for settings per client
    String uuid;

    //is the data displayed up-to-date ?
    public static Boolean needsrefresh;

    // Drawer menu items

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private android.support.v4.app.ActionBarDrawerToggle mDrawerToggle;

    private String[] mmenutTitles;

    //triggers updates in background on a regular basis
    // Setup a recurring alarm every minute
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
        // Setup periodic alarm every every minute from this point onwards
        long firstMillis = System.currentTimeMillis(); // alarm is set right away
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        if(alarm !=null) {
            alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis, 10000, pIntent);
        }
    }

    //Cancels background cheks if app is killed (only called @ OnDestroy)
    public void cancelAlarm() {
        Intent intent = new Intent(getApplicationContext(), twinTrackerAlarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, twinTrackerAlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        if(alarm !=null) {
            alarm.cancel(pIntent);
        }
    }

    public void enableUI() {
        strtBttn1.setEnabled(true);
        strtBttn2.setEnabled(true);
        stopBttn1.setEnabled(true);
        stopBttn2.setEnabled(true);
        vitaminBttn1.setEnabled(true);
        vitaminBttn2.setEnabled(true);
        ironBttn1.setEnabled(true);
        ironBttn2.setEnabled(true);
        bathBttn1.setEnabled(true);
        bathBttn2.setEnabled(true);
        inexiumBttn1.setEnabled(true);
        inexiumBttn2.setEnabled(true);

        if(startTwin1Intent) {

            strtBttn1.callOnClick();
        }

        if (startTwin2Intent) {

            strtBttn2.callOnClick();
        }

        if(stopTwin1Intent) {
            stopBttn1.callOnClick();
        }

        if(stopTwin2Intent) {
            stopBttn2.callOnClick();
        }

    }

    public void disaleUI() {
        strtBttn1.setEnabled(false);
        strtBttn2.setEnabled(false);
        stopBttn1.setEnabled(false);
        stopBttn2.setEnabled(false);
        vitaminBttn1.setEnabled(false);
        vitaminBttn2.setEnabled(false);
        ironBttn1.setEnabled(false);
        ironBttn2.setEnabled(false);
        bathBttn1.setEnabled(false);
        bathBttn2.setEnabled(false);
        inexiumBttn2.setEnabled(false);
        inexiumBttn1.setEnabled(false);
    }

    //Method to fetch all data from the API.
    public void refreshdata() {


        //test for network
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {

            //if connected start by showing a loading dialog to avoid user interaction during loading (can take a few seconds on slow networks)

            progress = 0;
            datacomplete = true;
            loadingdialog = new ProgressDialog(this);
            loadingdialog.setTitle("Chargement");
            loadingdialog.setMessage("Merci de patienter pendant le chargement des données...");
            loadingdialog.setMax(100);
            loadingdialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            loadingdialog.setProgressNumberFormat(null);
            loadingdialog.setCancelable(false); // disable dismiss by tapping outside of the dialog
            loadingdialog.show();

            //empty datasets
            feedings.clear();
            vitamins.clear();
            ironinputs.clear();
            liveEvents.clear();
            baths.clear();
            inexiums.clear();
            ironindex1 = 0;
            ironindex2 = 0;

            //asynchronously calls the API

            //get settings
            new DownloadWebpageTask(new AsyncResult() {
                @Override
                public void onResult(JSONArray object) {
                    processJsonSettings(object);
                }
            }).execute(GET_SETTINGS_URL);

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

            //get inexium
            new DownloadWebpageTask(new AsyncResult() {
                @Override
                public void onResult(JSONArray object) {
                    processJsoninexium(object);
                }
            }).execute(GET_INEXIUM_DATA_URL);

            //Get Bath Data
            new DownloadWebpageTask(new AsyncResult() {
                @Override
                public void onResult(JSONArray object) {
                    processJsonbath(object);
                }
            }).execute(GET_BATH_DATA_URL);

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



            //data is now up-to-date
            needsrefresh = false;
        }
        //if no network, warn user with toast
        if (networkInfo == null || !networkInfo.isConnected()) {
            Toast.makeText(getApplicationContext(), "Pas de Connection Internet", Toast.LENGTH_LONG).show();
        }

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

                //force refresh data when the sync button is selected.
                refreshdata();
                return true;
            }
            case R.id.action_edit: {

                //display a popup with 3 inputs to manually enter feeding data

                //prepare the popup content
                //the container
                LayoutInflater factory = LayoutInflater.from(this);
                final View feeding_input_form = factory.inflate(R.layout.feeding_form,null);

                //the twin selector (dropdown is spinner in android)
                select_name = (Spinner) feeding_input_form.findViewById(R.id.select_name);
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                        R.array.twins, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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

                //text inputs
                start_input = (EditText) feeding_input_form.findViewById(R.id.start_input);
                duration_input = (EditText)feeding_input_form.findViewById(R.id.duration_input);

                //the popup itself
                feeding_input = new AlertDialog.Builder(this);
                feeding_input.setIcon(R.drawable.ic_edit_white).setTitle("Enregistrer une têtée").setView(feeding_input_form).setPositiveButton("Enregistrer",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                /* User clicked OK so record the data */
                                input_duration = duration_input.getText().toString();
                                input_started = start_input.getText().toString();

                                //send this data if network is present.
                                ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                                if(networkInfo != null && networkInfo.isConnected()) {
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
     * User clicked cancel so do nothing
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
            //Menu Item #1 navigates to history page
            if(position==1) {
                displayHistory(view);
            }
            //Menu Item #2 navigates to settings page
            if(position==2) {
                displaySettings(view);
            }
        }
    }

    private void selectItem(int position) {


        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mmenutTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    //Calls History page by creating an instance of the DisplayHistoryActivity class
    public void displayHistory(View view) {
        Intent intent = new Intent(this, DisplayHistoryActivity.class);
        startActivity(intent);
    }

    //Calls Settings Page by creating an instance of the SettingsActivity class
    public void displaySettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    //This is called when the app is being sent to background but not killed and after onpause. not much to do
    @Override
    protected void onStop() {
        super.onStop();
        Log.i("bugrelou", "debut de onstop");
        Log.i("bugrelou", needsrefresh.toString());
    }

    //This is called when the app goes into the background. Invalidate any data to force refresh when app returns to foreground
    @Override
    protected void onPause() {
        super.onPause();
        needsrefresh = true;

        Log.i("bugrelou", "debut de onpause");
        Log.i("bugrelou", needsrefresh.toString());
    }

    //This is called when app retunrs in foreground, timers are restarted according to their status on leaving read from the API (through a refresh)
    @Override
    protected void onResume() {
        super.onResume();

        Log.i("bugrelou","début de on resume");
        Log.i("bugrelou", needsrefresh.toString());

        //only refresh in onResume when it's called before onCreate, otherwise data gets duplicated
        if(needsrefresh) {
            refreshdata();
            startTwin2Intent = false;
            startTwin1Intent = false;
            stopTwin1Intent = false;
            stopTwin2Intent = false;
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

            progress +=30;
            String progresstext = progress+"%";
            loadingdialog.setProgress(progress);
            Log.i("progress", "feedings done : "+ progresstext);

        } catch (JSONException e) {
            //In case parsing goes wrong
            Toast.makeText(getApplicationContext(),"Erreur de traitement des données", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            progress +=30;
            String progresstext = progress+"%";
            loadingdialog.setProgress(progress);
            Log.i("progress", "feedings failed : "+ progresstext);
            datacomplete = false;
        }

        if(progress == 100) {
            loadingdialog.dismiss();
            if(!datacomplete) {
                disaleUI();
            } else enableUI();
        }
    }

    //Callback on success of the HTTP GET request of the API and parses the JSON into an array of custom iron object
    private void processJsonIron(JSONArray object) {
        try {

            //iron is needed twice a day so compare last input and count to current day.

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

            progress +=20;
            String progresstext = progress+"%";
            loadingdialog.setProgress(progress);
            Log.i("progress", "iron done : "+ progresstext);


        }
        catch (JSONException e) {
            //In case parsing goes wrong
            Toast.makeText(getApplicationContext(),"Erreur de traitement des données", Toast.LENGTH_SHORT).show();
            e.printStackTrace();

            progress +=20;
            String progresstext = progress+"%";
            loadingdialog.setProgress(progress);
            Log.i("progress", "feedings error : "+ progresstext);
            datacomplete = false;

        }

        if(progress == 100) {
            loadingdialog.dismiss();
            if(!datacomplete) {
                disaleUI();
            } else enableUI();
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
                    a1 = " Vit. : "+ jourvitamines1 + " ";

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
                    z1 = " Vit. : " + jourvitamines2+ " ";
                }
            }
            vitaminBttn2.setText(z1);
            //Compare the last entry day for vitamins with current day
            if(!aujourdhui.equals(jourvitamines2)) {
                //if they do not match, display the button in red (kids must have their vitamins daily)
                vitaminBttn2.setBackgroundResource(R.color.colorAccent);
            }
            else vitaminBttn2.setBackgroundResource(R.color.colorPrimary);


            progress +=10;
            String progresstext = progress+"%";
            loadingdialog.setProgress(progress);
            Log.i("progress", "vitamin done : "+ progresstext);


        } catch (JSONException e) {
            //In case parsing goes wrong
            Toast.makeText(getApplicationContext(),"Erreur de traitement des données", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            progress +=10;
            String progresstext = progress+"%";
            loadingdialog.setProgress(progress);
            Log.i("progress", "vitamin failed : "+ progresstext);
            datacomplete = false;
        }

        if(progress == 100) {
            loadingdialog.dismiss();
            if(!datacomplete) {
                disaleUI();
            } else enableUI();
        }
    }

    //Callback on success of the HTTP GET request of the API and parses the JSON into an array of custom vitamin object
    private void processJsoninexium(JSONArray object) {

        try {

            //read from the end of the dataset to display last entry first
            for(int r=0; r< object.length(); ++r) {
                JSONObject row = object.getJSONObject(r);
                String name = row.getString("name");
                String jour = row.getString("day");
                inexiums.add(new inexium(name,jour));
            }

            //Fetch latest inexium data for Twin1 (agathe)
            String a1 = "";
            String jourinexium1 = "";
            int f = inexiums.size();
            for(int j =0; j<f; j++)
            {
                if(inexiums.get(j).getName().equals("agathe")) {
                    jourinexium1 = inexiums.get(j).getDay();
                    a1 = " Inex. : "+ jourinexium1 + " ";

                }
            }
            inexiumBttn1.setText(a1);
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat joursemaine = new SimpleDateFormat("EEEE", Locale.FRANCE);
            String aujourdhui = joursemaine.format(calendar.getTime());
            //Compare the last entry day for vitamins with current day
            if(!aujourdhui.equals(jourinexium1)) {
                //if they do not match, display the button in red (kids must have their vitamins daily)
                inexiumBttn1.setBackgroundResource(R.color.colorAccent);
            }
            //fine if it's today
            else inexiumBttn1.setBackgroundResource(R.color.colorPrimary);

            //Fetch latest vitamin data for Twin2 (Zoé)
            String z1 = "";
            String jourinexium2 = "";
            for(int j =0; j<f; j++)
            {
                if(inexiums.get(j).getName().equals("zoé")) {
                    jourinexium2 = inexiums.get(j).getDay();
                    z1 = " Inex. : " + jourinexium2+ " ";
                }
            }
            inexiumBttn2.setText(z1);
            //Compare the last entry day for vitamins with current day
            if(!aujourdhui.equals(jourinexium2)) {
                //if they do not match, display the button in red (kids must have their vitamins daily)
                inexiumBttn2.setBackgroundResource(R.color.colorAccent);
            }
            else inexiumBttn2.setBackgroundResource(R.color.colorPrimary);


            progress +=10;
            String progresstext = progress+"%";
            loadingdialog.setProgress(progress);
            Log.i("progress", "inexium done : "+ progresstext);


        } catch (JSONException e) {
            //In case parsing goes wrong
            Toast.makeText(getApplicationContext(),"Erreur de traitement des données", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            progress +=10;
            String progresstext = progress+"%";
            loadingdialog.setProgress(progress);
            Log.i("progress", "inexium failed : "+ progresstext);
            datacomplete = false;
        }

        if(progress == 100) {
            loadingdialog.dismiss();
            if(!datacomplete) {
                disaleUI();
            } else enableUI();
        }
    }

    //Callback on success of the HTTP GET request of the API and parses the JSON into an array of custom bath object
    private void processJsonbath(JSONArray object) {

        try {

            //read from the end of the dataset to display last entry first
            for(int r=0; r< object.length(); ++r) {
                JSONObject row = object.getJSONObject(r);
                String name = row.getString("name");
                String jour = row.getString("day");
                baths.add(new bath(name,jour));
            }

            //Fetch latest vitamin data for Twin1 (agathe)
            String a1 = "";
            String jourbain1 = "";
            int f = baths.size();
            for(int j =0; j<f; j++)
            {
                if(baths.get(j).getName().equals("agathe")) {
                    jourbain1 = baths.get(j).getDay();
                    a1 = " Bain : "+ jourbain1 + " ";

                }
            }
            bathBttn1.setText(a1);
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat joursemaine = new SimpleDateFormat("EEEE", Locale.FRANCE);
            String aujourdhui = joursemaine.format(calendar.getTime());
            //Compare the last entry day for vitamins with current day
            if(!aujourdhui.equals(jourbain1)) {
                //if they do not match, display the button in red (kids must have their vitamins daily)
                bathBttn1.setBackgroundResource(R.color.colorAccent);
            }
            //fine if it's today
            else bathBttn1.setBackgroundResource(R.color.colorPrimary);

            //Fetch latest vitamin data for Twin2 (Zoé)
            String z1 = "";
            String jourbain2 = "";
            for(int j =0; j<f; j++)
            {
                if(baths.get(j).getName().equals("zoé")) {
                    jourbain2 = baths.get(j).getDay();
                    z1 = " Bain : " + jourbain2+ " ";
                }
            }
            bathBttn2.setText(z1);
            //Compare the last entry day for vitamins with current day
            if(!aujourdhui.equals(jourbain2)) {
                //if they do not match, display the button in red (kids must have their vitamins daily)
                bathBttn2.setBackgroundResource(R.color.colorAccent);
            }
            else bathBttn2.setBackgroundResource(R.color.colorPrimary);

            progress +=10;
            String progresstext = progress+"%";
            loadingdialog.setProgress(progress);
            Log.i("progress", "bath done : "+ progresstext);


        } catch (JSONException e) {
            //In case parsing goes wrong
            Toast.makeText(getApplicationContext(),"Erreur de traitement des données", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            progress +=10;
            String progresstext = progress+"%";
            loadingdialog.setProgress(progress);
            Log.i("progress", "bath failed : "+ progresstext);
            datacomplete = false;
        }

        if(progress == 100) {
            loadingdialog.dismiss();
            if(!datacomplete) {
                disaleUI();
            } else enableUI();
        }
    }

    //Callback on success of the HTTP GET request of the API and parses the JSON into an array of custom livefeed object
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

                    //change shortcut labels

                    ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

                    Intent twin1Intent = new Intent();
                    twin1Intent.putExtra("stopTwin1", true);
                    twin1Intent.putExtra("stopTwin2", false);
                    twin1Intent.setClass(getApplicationContext(), MainActivity.class);
                    twin1Intent.setAction(Intent.ACTION_VIEW);
                    twin1Intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    twin1Intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    ShortcutInfo shortcut = new ShortcutInfo.Builder(getApplicationContext(), "twin1")
                            .setShortLabel(twin1label.getText())
                            .setLongLabel("Stopper "+twin1label.getText())
                            .setIntent(twin1Intent)
                            .build();

                    shortcutManager.updateShortcuts(Arrays.asList(shortcut));

                }
            }

            //stop any timer if ongoing is false on the server

            if(liveTwin1!=null && !liveTwin1.getOngoing()) {

                if(timer1!=null) {
                    timer1.cancel();
                    txtCurrentCount1.setText("");
                    txtCurrentDuration1.setText("");
                    strtBttn1.setText(R.string.start);

                    //change shortcut labels

                    ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

                    Intent twin1Intent = new Intent();
                    twin1Intent.putExtra("startTwin1", true);
                    twin1Intent.putExtra("startTwin2", false);
                    twin1Intent.setClass(getApplicationContext(), MainActivity.class);
                    twin1Intent.setAction(Intent.ACTION_VIEW);
                    twin1Intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    twin1Intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    ShortcutInfo shortcut = new ShortcutInfo.Builder(getApplicationContext(), "twin1")
                            .setShortLabel(twin1label.getText())
                            .setLongLabel("Nourrir "+twin1label.getText())
                            .setIntent(twin1Intent)
                            .build();

                    shortcutManager.updateShortcuts(Arrays.asList(shortcut));
                }
            }

            if(liveTwin2!=null && !liveTwin2.getOngoing()) {

                if(timer2!=null) {
                    timer2.cancel();
                    txtCurrentCount2.setText("");
                    txtCurrentDuration2.setText("");
                    strtBttn2.setText(R.string.start);

                    //change shortcut labels

                    ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

                    Intent twin2Intent = new Intent();
                    twin2Intent.putExtra("startTwin1", false);
                    twin2Intent.putExtra("startTwin2", true);
                    twin2Intent.setClass(getApplicationContext(), MainActivity.class);
                    twin2Intent.setAction(Intent.ACTION_VIEW);
                    twin2Intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    twin2Intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    ShortcutInfo shortcut = new ShortcutInfo.Builder(getApplicationContext(), "twin2")
                            .setShortLabel(twin2label.getText())
                            .setLongLabel("Nourrir "+twin2label.getText())
                            .setIntent(twin2Intent)
                            .build();

                    shortcutManager.updateShortcuts(Arrays.asList(shortcut));
                }
            }


            //resume timer if there is a live feed for twin 1
            if(liveTwin2!=null && liveTwin2.getOngoing()) {

                if(timer2==null) {
                    timer2 = new Timer();
                    twinTimerTask2 = new TwinTimerTask(txtCurrentCount2, txtCurrentDuration2, liveTwin2.getStartTime() ,liveTwin2.getStartDate());
                    timer2.schedule(twinTimerTask2, 1000, 1000);
                    strtBttn2.setText(R.string.pause);

                    //change shortcut labels

                    ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

                    Intent twin2Intent = new Intent();
                    twin2Intent.putExtra("stopTwin1", false);
                    twin2Intent.putExtra("stopTwin2", true);
                    twin2Intent.setClass(getApplicationContext(), MainActivity.class);
                    twin2Intent.setAction(Intent.ACTION_VIEW);
                    twin2Intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    twin2Intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    ShortcutInfo shortcut = new ShortcutInfo.Builder(getApplicationContext(), "twin2")
                            .setShortLabel(twin2label.getText())
                            .setLongLabel("Stopper "+twin2label.getText())
                            .setIntent(twin2Intent)
                            .build();

                    shortcutManager.updateShortcuts(Arrays.asList(shortcut));
                }
            }

            /*The background service looks for new data originated from a remote client.
            The latest data from the server that we just downloaded is the reference of the background service
             to determine if there is any "new" data */

            scheduleAlarm(liveTwin1,liveTwin2);


            //after any refresh, check if a feeding has been ongoing for more than AUTOSTOP_DELAY (=30mins) and stop & record it if that's the case
            if(autostop) {

                if(timer1 != null && ((System.currentTimeMillis()-twinTimerTask1.getStartTime()) > AUTO_STOP_DELAY)) {

                    Toast.makeText(getApplicationContext(),"Auto Stopping Twin 1", Toast.LENGTH_LONG).show();
                    //pause
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
                    unsaveddata1 = true;
                    txtCurrentCount1.setTextColor(Color.RED);
                    txtCurrentDuration1.setTextColor(Color.RED);
                }
                if(timer2 != null && ((System.currentTimeMillis()- twinTimerTask2.getStartTime()) > AUTO_STOP_DELAY)) {

                    Toast.makeText(getApplicationContext(),"Auto Stopping Twin 2", Toast.LENGTH_LONG).show();
                    //pause
                    timer2.cancel();
                    timer2 = null;

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

                    strtBttn2.setText(R.string.start);
                    mNotificationManager.cancel(2);
                    unsaveddata2 = true;
                    txtCurrentCount2.setTextColor(Color.RED);
                    txtCurrentDuration2.setTextColor(Color.RED);
                }

            }

            progress +=10;
            loadingdialog.setProgress(progress);
            String progresstext = progress+"%";
            Log.i("progress", "livefeed done : "+ progresstext);

        }
        catch (JSONException e) {
            //In case parsing goes wrong
            Toast.makeText(getApplicationContext(),"Erreur de traitement des données", Toast.LENGTH_SHORT).show();
            e.printStackTrace();

            progress +=10;
            String progresstext = progress+"%";
            loadingdialog.setProgress(progress);
            Log.i("progress", "livefeed done : "+ progresstext);
            datacomplete = false;
        }

        if(progress == 100) {
            loadingdialog.dismiss();
            if(!datacomplete) {
                disaleUI();
            } else enableUI();
        }
    }


    private void setPic1() {
        // Get the dimensions of the View
        int targetW = photo1.getWidth();
        int targetH = photo1.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photopath1, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(photopath1, bmOptions);
        photo1.setImageBitmap(bitmap);
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

        Intent twin1Intent = new Intent();
        twin1Intent.putExtra("startTwin1", true);
        twin1Intent.putExtra("startTwin2", false);
        twin1Intent.setClass(this, MainActivity.class);
        twin1Intent.setAction(Intent.ACTION_VIEW);
        twin1Intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        twin1Intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "twin1")
                .setShortLabel(twin1label.getText())
                .setLongLabel("Nourrir "+twin1label.getText())
                .setIcon(Icon.createWithBitmap(bitmap))
                .setIntent(twin1Intent)
                .build();

        shortcutManager.addDynamicShortcuts(Arrays.asList(shortcut));


    }


    private void setPic2() {
        // Get the dimensions of the View
        int targetW = photo2.getWidth();
        int targetH = photo2.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photopath2, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(photopath2, bmOptions);
        photo2.setImageBitmap(bitmap);

        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

        Intent twin2Intent = new Intent();
        twin2Intent.putExtra("startTwin2", true);
        twin2Intent.putExtra("startTwin1", false);
        twin2Intent.setClass(this, MainActivity.class);
        twin2Intent.setAction(Intent.ACTION_VIEW);
        twin2Intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        twin2Intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "twin2")
                .setShortLabel(twin2label.getText())
                .setLongLabel("Nourrir "+twin2label.getText())
                .setIcon(Icon.createWithBitmap(bitmap))
                .setIntent(twin2Intent)
                .build();

        shortcutManager.addDynamicShortcuts(Arrays.asList(shortcut));

    }


    //Callback on success of the HTTP GET request of the API and parses the JSON into an array of custom twinSettings object
    private void processJsonSettings(JSONArray object) {

        try {

            //read from the end of the dataset to display last feedings first
            for(int r=0; r< object.length(); ++r) {
                JSONObject row = object.getJSONObject(r);
                String user = row.getString("user");
                String twin1name = row.getString("twin1name");
                String twin2name = row.getString("twin2name");
                Boolean notificationchoice = row.getBoolean("shouldnotify");
                Boolean autostopchoice = row.getBoolean("autoStop");
                String photo1path = row.getString("photopath1");
                String photo2path = row.getString("photopath2");
                preferences.add(new twinSettings(user,notificationchoice, autostopchoice, twin1name, twin2name, photo1path, photo2path));
            }

            //look for the current user's unique id in the list of settings
            int f = preferences.size();
            for(int j=0; j<f; ++j) {
                if(preferences.get(j).getUser().equals(uuid)) {
                    myindex = j;
                }
            }

            //if you found your ID, read your preferences from file and set the correct variables accordingly
            if(myindex !=-1) {

                twin1label.setText(preferences.get(myindex).getTwin1name());
                twin2label.setText(preferences.get(myindex).getTwin2name());
                shouldNotify = preferences.get(myindex).getShouldnotify();
                autostop = preferences.get(myindex).getAutoStop();
                photopath1 = preferences.get(myindex).getPhotopath1();
                photopath2 = preferences.get(myindex).getPhotopath2();

                if(photopath1 != "") {
                    setPic1();
                }
                if(photopath2 != "") {
                    setPic2();
                }

            }

            progress +=10;
            String progresstext = progress+"%";
            loadingdialog.setProgress(progress);
            Log.i("progress", "settings done : "+ progresstext);

        } catch (JSONException e) {
            //In case parsing goes wrong
            Toast.makeText(getApplicationContext(),"Erreur de traitement des données", Toast.LENGTH_SHORT).show();
            e.printStackTrace();

            progress +=10;
            String progresstext = progress+"%";
            loadingdialog.setProgress(progress);
            Log.i("progress", "settings failed : "+ progresstext);
            datacomplete = false;
        }

        if(progress == 100) {
            loadingdialog.dismiss();
            if(!datacomplete) {
                disaleUI();
            } else enableUI();
        }
    }

    //Create the main activity page
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //initialisations

        startTwin1Intent = false;
        startTwin2Intent = false;
        stopTwin2Intent = false;
        stopTwin1Intent = false;

        //is there any data in the intent ?
        Intent called = this.getIntent();
        if(called.getExtras() !=null) {
            try {
                startTwin1Intent = called.getExtras().getBoolean("startTwin1");
                startTwin2Intent = called.getExtras().getBoolean("startTwin2");
                stopTwin1Intent = called.getExtras().getBoolean("stopTwin1");
                stopTwin2Intent = called.getExtras().getBoolean("stopTwin2");
                Log.i("shortcuts", called.getExtras().get("startTwin1").toString());
                Log.i("shortcuts", called.getExtras().get("startTwin2").toString());
            }
            catch (NullPointerException e) {
                e.printStackTrace();
            }
        }


        progress = 0;
        needsrefresh = true;
        datacomplete = true;
        shouldNotify = true;
        autostop = false;
        photopath1 = "";
        photopath2 = "";

        Log.i("bugrelou","début de on create");
        Log.i("bugrelou", needsrefresh.toString());

        feedings = new ArrayList<>();
        vitamins = new ArrayList<>();
        inexiums = new ArrayList<>();
        ironinputs = new ArrayList<>();
        ironindex1 = ironindex2 = 0;
        liveEvents = new ArrayList<>();
        liveTwin1index = liveTwin2index =0;
        preferences = new ArrayList<>();
        myindex = -1;
        baths = new ArrayList<>();

        //this reads uuid from a file on internal storage or creates one if it doesn't exist (first install or re-install)
        uuid = Installation.id(this);

        //main view
        setContentView(R.layout.activity_main);

        /* Drawer Menu Stuff */

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        ActionBarDrawerToggle mDrawerToggle;

        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();

        mmenutTitles = new String[]{"Twin Tracker", "Historique","Réglages"};
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
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final Intent notificationIntent = new Intent(this,   MainActivity.class);
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
        bathBttn1 = (Button)findViewById(R.id.bathbutton1);
        bathBttn2 = (Button)findViewById(R.id.bathbutton2);
        inexiumBttn1 = (Button)findViewById(R.id.inexiumbutton1);
        inexiumBttn2 = (Button)findViewById(R.id.inexiumbutton2);

        twin1label = (TextView)findViewById(R.id.twin1);
        twin2label = (TextView)findViewById(R.id.twin2);

        txtCurrentCount1 = (TextView)findViewById(R.id.current_timer_1);
        txtCurrentCount2 = (TextView)findViewById(R.id.current_timer_2);
        txtLastDate1 = (TextView)findViewById(R.id.last1_time);
        txtLastDate2 = (TextView)findViewById(R.id.last2_time);

        txtCurrentDuration1 = (TextView)findViewById(R.id.current_duration_1);
        txtCurrentDuration2 = (TextView)findViewById(R.id.current_duration_2);

        photo1 = (ImageView)findViewById(R.id.twin1photo);
        photo2 = (ImageView)findViewById(R.id.twin2photo);

        disaleUI();

        //Check for network and fetch data from API on creation of the page to fill the "last data" fields, iron and vitamin buttons as well as check if any feedings could be ongoing and fecth settings values
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();


        if(networkInfo != null && networkInfo.isConnected()) {


            needsrefresh = false;

            loadingdialog = new ProgressDialog(this);
            loadingdialog.setTitle("Chargement");
            loadingdialog.setMessage("Merci de patienter pendant le chargement des données...");
            loadingdialog.setMax(100);
            loadingdialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            loadingdialog.setProgressNumberFormat(null);
            loadingdialog.setCancelable(false); // disable dismiss by tapping outside of the dialog
            loadingdialog.show();

            //get settings (remote ongoing feedings)
            new DownloadWebpageTask(new AsyncResult() {
                @Override
                public void onResult(JSONArray object) {
                    processJsonSettings(object);
                }
            }).execute(GET_SETTINGS_URL);

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

            //get Inexium data
            new DownloadWebpageTask(new AsyncResult() {
                @Override
                public void onResult(JSONArray object) {
                    processJsoninexium(object);
                }
            }).execute(GET_INEXIUM_DATA_URL);

            //Get Bath Data
            new DownloadWebpageTask(new AsyncResult() {
                @Override
                public void onResult(JSONArray object) {
                    processJsonbath(object);
                }
            }).execute(GET_BATH_DATA_URL);

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

        //Bath Button 1 Click sends today's date to the server unless current day is already the latest data
        bathBttn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //get latest data entry for twin 1
                String jourbain1 = "";
                int f = baths.size();
                for(int j =0; j<f; j++)
                {
                    if(baths.get(j).getName().equals("agathe")) {
                        jourbain1 = baths.get(j).getDay();
                    }
                }
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat joursemaine = new SimpleDateFormat("EEEE", Locale.FRANCE);
                String jourbain = joursemaine.format(calendar.getTime());

                //compare with current date
                if(!jourbain.equals(jourbain1)) {

                    //update text if new date
                    String bainbttn1 = "  Bain : " + jourbain + "  ";
                    bathBttn1.setText(bainbttn1);
                    bathBttn1.setBackgroundResource(R.color.colorPrimary);
                    bath bath1 = new bath("agathe", jourbain);
                    baths.add(bath1);
                    String json = new Gson().toJson(baths);

                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                    if (networkInfo != null && networkInfo.isConnected()) {
                        //send new day to server if network is available
                        new UploadDataTask().execute(PUT_BATH_DATA_URL, json);
                        Toast.makeText(getApplicationContext(), "Donnée Enregistrée", Toast.LENGTH_SHORT).show();
                    }
                    if (networkInfo == null || !networkInfo.isConnected()) {
                        //inform user if network is not available
                        Toast.makeText(getApplicationContext(), "Pas de Connection Internet", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    //nothing to do but inform the user if vitamins were already given today
                    Toast.makeText(getApplicationContext(), "Bain déjà donné aujourd'hui", Toast.LENGTH_LONG).show();
                }
            }
        });

        //Bath Button 1 Click sends today's date to the server unless current day is already the latest data
        bathBttn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //get latest data entry for twin 2
                String jourbain2 = "";
                int f = baths.size();
                for(int j =0; j<f; j++)
                {
                    if(baths.get(j).getName().equals("zoé")) {
                        jourbain2 = baths.get(j).getDay();
                    }
                }
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat joursemaine = new SimpleDateFormat("EEEE", Locale.FRANCE);
                String jourbain = joursemaine.format(calendar.getTime());

                //compare with current date
                if(!jourbain.equals(jourbain2)) {

                    //update text if new date
                    String bainbttn2 = "  Bain : " + jourbain + "  ";
                    bathBttn2.setText(bainbttn2);
                    bathBttn2.setBackgroundResource(R.color.colorPrimary);
                    bath bath2 = new bath("zoé", jourbain);
                    baths.add(bath2);
                    String json = new Gson().toJson(baths);

                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                    if (networkInfo != null && networkInfo.isConnected()) {
                        //send new day to server if network is available
                        new UploadDataTask().execute(PUT_BATH_DATA_URL, json);
                        Toast.makeText(getApplicationContext(), "Donnée Enregistrée", Toast.LENGTH_SHORT).show();
                    }
                    if (networkInfo == null || !networkInfo.isConnected()) {
                        //inform user if network is not available
                        Toast.makeText(getApplicationContext(), "Pas de Connection Internet", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    //nothing to do but inform the user if vitamins were already given today
                    Toast.makeText(getApplicationContext(), "Bain déjà donné aujourd'hui", Toast.LENGTH_LONG).show();
                }
            }
        });


        //Vitamin Button 1 Click sends today's date to the server unless current day is already the latest data
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
                    String vitaminbttn1 = "  Vit. : " + jourvitamines + "  ";
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

                    String vitaminbttn2 = "  Vit. : " + jourvitamines + "  ";
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


        //Inexium Button 1 Click sends today's date to the server unless current day is already the latest data
        inexiumBttn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //get latest data entry for twin 1
                String jourinexium1 = "";
                int f = inexiums.size();
                for(int j =0; j<f; j++)
                {
                    if(inexiums.get(j).getName().equals("agathe")) {
                        jourinexium1 = inexiums.get(j).getDay();
                    }
                }
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat joursemaine = new SimpleDateFormat("EEEE", Locale.FRANCE);
                String jourinexium = joursemaine.format(calendar.getTime());

                //compare with current date
                if(!jourinexium.equals(jourinexium1)) {

                    //update text if new date
                    String inexiumbttn1 = "  Inex. : " + jourinexium + "  ";
                    inexiumBttn1.setText(inexiumbttn1);
                    inexiumBttn1.setBackgroundResource(R.color.colorPrimary);
                    inexium inexium1 = new inexium("agathe", jourinexium);
                    inexiums.add(inexium1);
                    String json = new Gson().toJson(inexiums);

                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                    if (networkInfo != null && networkInfo.isConnected()) {
                        //send new day to server if network is available
                        new UploadDataTask().execute(PUT_INEXIUM_DATA_URL, json);
                        Toast.makeText(getApplicationContext(), "Donnée Enregistrée", Toast.LENGTH_SHORT).show();
                    }
                    if (networkInfo == null || !networkInfo.isConnected()) {
                        //inform user if network is not available
                        Toast.makeText(getApplicationContext(), "Pas de Connection Internet", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    //nothing to do but inform the user if inexium were already given today
                    Toast.makeText(getApplicationContext(), "Inexium déjà donné aujourd'hui", Toast.LENGTH_LONG).show();
                }
            }
        });


        //Inexium Button 2  Click sends today's date to the server unless current day is already the latest data same as Vitamin button 1
        inexiumBttn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int f = inexiums.size();
                String jourinexium2 = "";
                for (int j = 0; j < f; j++) {
                    if (inexiums.get(j).getName().equals("zoé")) {
                        jourinexium2 = inexiums.get(j).getDay();
                    }
                }

                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat joursemaine = new SimpleDateFormat("EEEE", Locale.FRANCE);
                String jourinexium = joursemaine.format(calendar.getTime());
                if (!jourinexium.equals(jourinexium2)) {

                    String inexiumbttn2 = "  Inex. : " + jourinexium + "  ";
                    inexiumBttn2.setText(inexiumbttn2);
                    inexiumBttn2.setBackgroundResource(R.color.colorPrimary);
                    inexium inexium2 = new inexium("zoé", jourinexium);
                    inexiums.add(inexium2);
                    String json = new Gson().toJson(inexiums);

                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                    if (networkInfo != null && networkInfo.isConnected()) {
                        new UploadDataTask().execute(PUT_INEXIUM_DATA_URL, json);
                        Toast.makeText(getApplicationContext(), "Donnée Enregistrée", Toast.LENGTH_SHORT).show();
                    }
                    if (networkInfo == null || !networkInfo.isConnected()) {
                        Toast.makeText(getApplicationContext(), "Pas de Connection Internet", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Toast.makeText(getApplicationContext(), "Inexium déjà donné aujourd'hui", Toast.LENGTH_LONG).show();
                }
            }
        });





        //Start Timer N1 toggles start/pause status locally and on server
        strtBttn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(timer1 != null) {

                    //pause
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
                    //start
                    timer1 = new Timer();

                    //change shortcut labels

                    ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

                    Intent twin1Intent = new Intent();
                    twin1Intent.putExtra("stopTwin1", true);
                    twin1Intent.putExtra("stopTwin2", false);
                    twin1Intent.setClass(getApplicationContext(), MainActivity.class);
                    twin1Intent.setAction(Intent.ACTION_VIEW);
                    twin1Intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    twin1Intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    ShortcutInfo shortcut = new ShortcutInfo.Builder(getApplicationContext(), "twin1")
                            .setShortLabel(twin1label.getText())
                            .setLongLabel("Stopper "+twin1label.getText())
                            .setIntent(twin1Intent)
                            .build();

                    shortcutManager.updateShortcuts(Arrays.asList(shortcut));


                    long now = System.currentTimeMillis();
                    twinTimerTask1 = new TwinTimerTask(txtCurrentCount1, txtCurrentDuration1);
                    timer1.schedule(twinTimerTask1, 1000, 1000);
                    strtBttn1.setText(R.string.pause);
                    txtCurrentCount1.setText("");
                    txtCurrentDuration1.setText("");
                    if(shouldNotify) {
                        android.support.v4.app.NotificationCompat.Builder mnbuilder = new NotificationCompat.Builder(MainActivity.this)
                                .setSmallIcon(R.mipmap.ic_notif)
                                .setWhen(System.currentTimeMillis())  // the time stamp, you will probably use System.currentTimeMillis() for most scenarios
                                .setUsesChronometer(true)
                                .setContentTitle("Agathe")
                                .setContentText("Têtée en cours")
                                .setContentIntent(contentIntent);
                        mNotificationManager.notify(1, mnbuilder.build());
                    }

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

        //Start Timer N2 toggles start/pause status locally and on server
        strtBttn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(timer2 != null) {

                    //pause
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

                    //start
                    timer2 = new Timer();


                    //change shortcut labels

                    ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

                    Intent twin2Intent = new Intent();
                    twin2Intent.putExtra("stopTwin1", false);
                    twin2Intent.putExtra("stopTwin1", true);
                    twin2Intent.setClass(getApplicationContext(), MainActivity.class);
                    twin2Intent.setAction(Intent.ACTION_VIEW);
                    twin2Intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    twin2Intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    ShortcutInfo shortcut = new ShortcutInfo.Builder(getApplicationContext(), "twin2")
                            .setShortLabel(twin2label.getText())
                            .setLongLabel("Stopper "+twin2label.getText())
                            .setIntent(twin2Intent)
                            .build();

                    shortcutManager.updateShortcuts(Arrays.asList(shortcut));


                    long now = System.currentTimeMillis();
                    twinTimerTask2 = new TwinTimerTask(txtCurrentCount2, txtCurrentDuration2);
                    timer2.schedule(twinTimerTask2, 1000, 1000);
                    strtBttn2.setText(R.string.pause);
                    txtCurrentCount2.setText("");
                    txtCurrentDuration2.setText("");
                    if(shouldNotify) {
                        android.support.v4.app.NotificationCompat.Builder mnbuilder = new NotificationCompat.Builder(MainActivity.this)
                                .setSmallIcon(R.mipmap.ic_notif)
                                .setWhen(System.currentTimeMillis())  // the time stamp, you will probably use System.currentTimeMillis() for most scenarios
                                .setUsesChronometer(true)
                                .setContentTitle("Zoé")
                                .setContentText("Têtée en cours")
                                .setContentIntent(contentIntent);
                        mNotificationManager.notify(2, mnbuilder.build());
                    }

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

        Log.i("bugrelou","fin de on create");
        Log.i("bugrelou", needsrefresh.toString());
    }

    /*this is called on killing the app.
    Destroy all timers and live events to make sure we get a clean restart
    with fresh data from the server on next onCreate */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        needsrefresh = true;
        Log.i("bugrelou", "debut de ondestroy");
        Log.i("bugrelou", needsrefresh.toString());

        //we don't want notifications when app is killed
        cancelAlarm();

        if(liveEvents!=null) {liveEvents.clear();}

        if(timer1!=null) {timer1.cancel();}
        if(timer2!=null) {timer2.cancel();}
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
