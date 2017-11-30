package com.example.charles.twintracker;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.service.notification.StatusBarNotification;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by clesoil on 26/11/2017.
 */

public class twinTrackerService extends IntentService {

    public static final String GET_LIVEFEED_DATA_URL = "http://japansio.info/api/livefeed.json";
    liveFeed livetwin1,livetwin2;
    ArrayList<liveFeed> liveEvents;
    String lastdata1, lastdata2;

    private void processJsonLiveFeed(JSONArray object) {

        //notification items
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

                    livetwin1 = liveEvents.get(j);
                }
            }

            //get the latest event for Zoé
            for(int j =0; j<f; j++)
            {
                if(liveEvents.get(j).getName().equals("zoé")) {

                    livetwin2 = liveEvents.get(j);

                }
            }

            //show notification for each twin that has ongoing feeding if it is not the current one
            //should avoid notification for feedings started on the local device, and allow for only 1 notification per feeding

            if(livetwin1!=null && livetwin1.getOngoing()) {

                Log.i("Debug", "il y a une têtée en cours");
                if(!livetwin1.getStartDate().equals(lastdata1)) {

                    Log.i("Debug", "mais c'est une nouvelle donnée");
                    //show this notification only once.
                    StatusBarNotification[] notifications = mNotificationManager.getActiveNotifications();
                    Boolean wasshown = false;
                    for (StatusBarNotification notification : notifications) {
                        if (notification.getId() == 11) {
                            wasshown = true;
                            Log.i("Debug", "mais j'ai déjà été notifié");
                        }
                    }

                    if (!wasshown) {
                        Log.i("Debug", "Je notifie !");
                        android.support.v4.app.NotificationCompat.Builder mnbuilder = new NotificationCompat.Builder(twinTrackerService.this)
                                .setSmallIcon(R.mipmap.ic_notif)
                                .setWhen(livetwin1.getStartTime())  // the time stamp, you will probably use System.currentTimeMillis() for most scenarios
                                .setUsesChronometer(false)
                                .setContentTitle("Agathe")
                                .setVibrate(new long[]{0, 1000})
                                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                                .setContentText("Têtée en cours")
                                .setContentIntent(contentIntent)
                                .setAutoCancel(true);
                        mNotificationManager.notify(11, mnbuilder.build());
                    }
                }
            }

            if(livetwin2!=null && livetwin2.getOngoing()) {

                Log.i("Debug", "il y a une têtée en cours");
                if(!livetwin2.getStartDate().equals(lastdata2)) {

                    Log.i("Debug", "mais c'est une nouvelle donnée");
                    //show this notification only once.
                    StatusBarNotification[] notifications = mNotificationManager.getActiveNotifications();
                    Boolean wasshown = false;
                    for (StatusBarNotification notification : notifications) {
                        if (notification.getId() == 21) {
                            wasshown = true;
                            Log.i("Debug", "mais j'ai déjà été notifié");
                        }
                    }

                    if (!wasshown) {

                        Log.i("Debug", "Je notifie !");
                        android.support.v4.app.NotificationCompat.Builder mnbuilder = new NotificationCompat.Builder(twinTrackerService.this)
                                .setSmallIcon(R.mipmap.ic_notif)
                                .setWhen(livetwin2.getStartTime())  // the time stamp, you will probably use System.currentTimeMillis() for most scenarios
                                .setUsesChronometer(false)
                                .setContentTitle("Zoé")
                                .setVibrate(new long[]{0, 1000})
                                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                                .setContentText("Têtée en cours")
                                .setContentIntent(contentIntent)
                                .setAutoCancel(true);
                        mNotificationManager.notify(21, mnbuilder.build());
                    }
                }
            }

            //Show notification for each twin if it has been more than 4h since the last feeding

            if(livetwin1!=null && !livetwin1.getOngoing()) {
                //pas de têtée en cours
                Log.i("Debug", "pas de têtée en cours");
                if(!livetwin1.getStartDate().equals(lastdata1)) {
                    //c'est une nouvelle têtée qui doit donc être terminée, la notification peut disparaître.
                    Log.i("Debug","mais nouvelle donnée donc têtée terminée. Je fais disparaitre la notification");
                    mNotificationManager.cancel(11);
                }
            }

            if(livetwin2!=null && !livetwin2.getOngoing()) {
                //pas de têtée en cours
                Log.i("Debug", "pas de têtée en cours");
                if(!livetwin2.getStartDate().equals(lastdata2)) {
                    //c'est une nouvelle têtée qui doit donc être terminée, la notification peut disparaître.
                    Log.i("debug","mais nouvelle donnée donc têtée terminée. Je fais disparaitre la notification");
                    mNotificationManager.cancel(21);
                }
            }

        }
        catch (JSONException e) {
            //In case parsing goes wrong
            Toast.makeText(getApplicationContext(),"Erreur de traitement des données", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public twinTrackerService() {
        super("twinTrackerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Do the task here

        if(intent !=null && intent.getExtras() !=null) {
            lastdata1 = intent.getExtras().getString("lastdata1");
            lastdata2 = intent.getExtras().getString("lastdata2");
        }

        liveEvents = new ArrayList<>();
        liveEvents.clear();

        //Check for network and fetch data from API on creation of the page to fill the "last data" fields and vitamin buttons
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();


        if(networkInfo != null && networkInfo.isConnected()) {

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

        Log.i("twinTrackerService", "Service running");
        Log.i("twinTrackerService", "received : " + lastdata1 + " and "+ lastdata2);
    }

}