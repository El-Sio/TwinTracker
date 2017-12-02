package com.example.charles.twintracker;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


//activity that display's current user settings and allow for settings modification and saving via the settings API;
public class SettingsActivity extends AppCompatActivity {

    ProgressDialog loadingdialog;
    Button savesettingsBttn;
    EditText twin1input,twin2input;
    Boolean shouldNotify, autostop;
    Switch notificationSwitch, autostopswitch;
    ArrayList<twinSettings> preferences;
    int myindex;
    public static final String GET_SETTINGS_URL = "http://japansio.info/api/settings.json";
    public static final String PUT_SETTINGS_URL = "http://japansio.info/api/putsettings.php";
    String uuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //Menu Stuff

        Toolbar toolbar = (Toolbar) findViewById(R.id.child_toolbar);

        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //UI init
        savesettingsBttn = (Button)findViewById(R.id.savesettingsbttn);
        twin1input = (EditText)findViewById(R.id.twin1inputtext);
        twin2input = (EditText)findViewById(R.id.twin2inputtext);
        notificationSwitch = (Switch)findViewById(R.id.notifswitch);
        notificationSwitch.setChecked(false);
        autostopswitch = (Switch)findViewById(R.id.autostopswitch);
        autostopswitch.setChecked(false);

        //API ressources
        preferences = new ArrayList<>();
        preferences.clear();
        myindex = -1;

        uuid = Installation.id(this);
        Log.i("settings debug", uuid);

        getsettings();

        //listeners
        savesettingsBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savesettings();
            }
        });

        notificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(notificationSwitch.isChecked()) {
                    shouldNotify = true;
                }
                else shouldNotify = false;
            }
        });

        autostopswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(autostopswitch.isChecked()) {
                    autostop = true;
                }
                else autostop = false;
            }
        });

    }

    private void processJson(JSONArray object) {

        try {

            //read from the end of the dataset to display last feedings first
            for(int r=0; r< object.length(); ++r) {
                JSONObject row = object.getJSONObject(r);
                String user = row.getString("user");
                String twin1name = row.getString("twin1name");
                String twin2name = row.getString("twin2name");
                Boolean notificationchoice = row.getBoolean("shouldnotify");
                Boolean autostopchoice = row.getBoolean("autoStop");
                preferences.add(new twinSettings(user,notificationchoice, autostopchoice, twin1name, twin2name));
            }

            int f = preferences.size();
            for(int j=0; j<f; ++j) {
                if(preferences.get(j).getUser().equals(uuid)) {
                    myindex = j;
                }
            }

            if(myindex !=-1) {
                shouldNotify = preferences.get(myindex).getShouldnotify();
                notificationSwitch.setOnCheckedChangeListener (null);
                notificationSwitch.setChecked(shouldNotify);
                notificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if(notificationSwitch.isChecked()) {
                            shouldNotify = true;
                        }
                        else shouldNotify = false;
                    }
                });

                autostop = preferences.get(myindex).getAutoStop();
                autostopswitch.setOnCheckedChangeListener (null);
                autostopswitch.setChecked(autostop);
                autostopswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if(autostopswitch.isChecked()) {
                            autostop = true;
                        }
                        else autostop = false;
                    }
                });
                twin1input.setText(preferences.get(myindex).getTwin1name());
                twin2input.setText(preferences.get(myindex).getTwin2name());
            }

        } catch (JSONException e) {
            //In case parsing goes wrong
            Toast.makeText(getApplicationContext(),"Erreur de traitement des données", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        //when data is loaded, hide the progres popup.
        loadingdialog.dismiss();
    }

    private void getsettings() {
        //Check for network and fetch data from API on creation of the page to fill the "last data" fields and vitamin buttons
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();


        if(networkInfo != null && networkInfo.isConnected()) {

            //Display progress popup while data is being loaded

            loadingdialog = new ProgressDialog(this);
            loadingdialog.setTitle("Chargement");
            loadingdialog.setMessage("Merci de patienter pendant le chargement des données...");
            loadingdialog.setCancelable(false); // disable dismiss by tapping outside of the dialog
            loadingdialog.show();

            //get Settings data
            new DownloadWebpageTask(new AsyncResult() {
                @Override
                public void onResult(JSONArray object) {
                    processJson(object);
                }
            }).execute(GET_SETTINGS_URL);

        }
        if(networkInfo == null || !networkInfo.isConnected()) {
            //inform the user if no network is connected
            Toast.makeText(getApplicationContext(),"Pas de Connection Internet",Toast.LENGTH_LONG).show();
        }
    }

    private void savesettings() {

        //save settings in new object if new user, or in user's position otherwise
        if(myindex != -1) {
            preferences.get(myindex).setShouldnotify(notificationSwitch.isChecked());
            preferences.get(myindex).setAutoStop(autostopswitch.isChecked());
            preferences.get(myindex).setTwin1name(twin1input.getText().toString());
            preferences.get(myindex).setTwin2name(twin2input.getText().toString());
        }
        else {

            twinSettings mysettings = new twinSettings(uuid,notificationSwitch.isChecked(),autostopswitch.isChecked(),twin1input.getText().toString(),twin2input.getText().toString());
            preferences.add(mysettings);
            myindex = preferences.size();
        }
        //encode this in JSON
        String json = new Gson().toJson(preferences);

        //send settings to the server if network is available

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            //send new day to server if network is available
            new UploadDataTask().execute(PUT_SETTINGS_URL, json);
            Toast.makeText(getApplicationContext(), "Donnée Enregistrée", Toast.LENGTH_SHORT).show();

        }
        if (networkInfo == null || !networkInfo.isConnected()) {
            //inform user if network is not available
            Toast.makeText(getApplicationContext(), "Pas de Connection Internet", Toast.LENGTH_LONG).show();
        }
    }

}
