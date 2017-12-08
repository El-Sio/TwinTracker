package com.example.charles.twintracker;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static android.os.Environment.DIRECTORY_PICTURES;
import static android.os.Environment.getExternalStoragePublicDirectory;


//activity that display's current user settings and allow for settings modification and saving via the settings API;
public class SettingsActivity extends AppCompatActivity {

    ProgressDialog loadingdialog;
    Button savesettingsBttn,changePhoto1,changePhoto2;
    ImageView photo1,photo2;
    EditText twin1input,twin2input;
    Boolean shouldNotify, autostop;
    String photopath1,photopath2;
    Switch notificationSwitch, autostopswitch;
    ArrayList<twinSettings> preferences;
    int myindex;
    public static final String GET_SETTINGS_URL = "http://japansio.info/api/settings.json";
    public static final String PUT_SETTINGS_URL = "http://japansio.info/api/putsettings.php";
    String uuid;


    //photo handling
    static final int REQUEST_IMAGE_CAPTURE_1 = 1;
    static final int REQUEST_IMAGE_CAPTURE_2 = 2;

    private void dispatchTakePictureIntent1() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile1();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE_1);
            }
        }
    }

    private void dispatchTakePictureIntent2() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Log.i("photo", "intent créé");
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            Log.i("photo", "autorisation OK");
            File photoFile = null;
            try {
                Log.i("photo", "creation lancee");
                photoFile = createImageFile2();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.i("photo","erreur fichier");
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Log.i("photo", "creation réussie");
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE_2);
                Log.i("photo", "lancement de l'activite");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        photopath1 = "";
        photopath2 = "";
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

        photo1 = (ImageView)findViewById(R.id.twin1photo);
        photo2 = (ImageView)findViewById(R.id.twin2photo);

        changePhoto1 = (Button)findViewById(R.id.changephoto1);
        changePhoto2 = (Button)findViewById(R.id.changephoto2);

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

        changePhoto1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                dispatchTakePictureIntent1();

            }
        });

        changePhoto2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.i("photo", "clicked");
                dispatchTakePictureIntent2();

            }
        });

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
    }

    private File createImageFile1() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        photopath1 = image.getAbsolutePath();
        return image;
    }

    private File createImageFile2() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        Log.i("photo", "creation de fichier OK");
        // Save a file: path for use with ACTION_VIEW intents
        photopath2 = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic(String path) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(path);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE_1 && resultCode == RESULT_OK) {
            galleryAddPic(photopath1);
            setPic1();
        }
        if (requestCode == REQUEST_IMAGE_CAPTURE_2 && resultCode == RESULT_OK) {
            galleryAddPic(photopath2);
            setPic2();
        }
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
                String photo1path = row.getString("photopath1");
                String photo2path = row.getString("photopath2");
                preferences.add(new twinSettings(user,notificationchoice, autostopchoice, twin1name, twin2name, photo1path, photo2path));
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
                photopath1 = preferences.get(myindex).getPhotopath1();
                photopath2 = preferences.get(myindex).getPhotopath2();
                if(photopath1 != "") {
                    setPic1();
                }
                if(photopath2 != "") {
                    setPic2();
                }
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
            preferences.get(myindex).setPhotopath1(photopath1);
            preferences.get(myindex).setPhotopath2(photopath2);
        }
        else {

            twinSettings mysettings = new twinSettings(uuid,notificationSwitch.isChecked(),autostopswitch.isChecked(),twin1input.getText().toString(),twin2input.getText().toString(),photopath1, photopath2);
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
