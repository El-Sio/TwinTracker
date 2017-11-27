package com.example.charles.twintracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by clesoil on 27/11/2017.
 */

public class twinTrackerAlarmReceiver extends BroadcastReceiver {
    public static final int REQUEST_CODE = 12345;
    public static final String ACTION = "com.example.charles.twintracker.alarm";
    String lastdata1, lastdata2;

    // Triggered by the Alarm periodically (starts the service to run task)
    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent !=null & intent.getExtras()!=null) {
            lastdata1 = intent.getExtras().getString("latest1");
            lastdata2 = intent.getExtras().getString("latest2");

            System.out.println("Alarme received : " + lastdata1 +" and " + lastdata2);
        }
        Intent i = new Intent(context, twinTrackerService.class);
        i.putExtra("lastdata1",lastdata1);
        i.putExtra("lastdata2", lastdata2);
        context.startService(i);
    }


}
