package com.example.charles.twintracker;

import android.app.Application;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import static com.example.charles.twintracker.MainActivity.PREFS_NAME;

public class DisplayHistoryActivity extends AppCompatActivity {

    TextView txthisto11,txthisto12,txthisto13,txthisto14,txthisto15,txthisto16;
    TextView txthisto21,txthisto22,txthisto23,txthisto24,txthisto25,txthisto26;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_history);

        txthisto11 = (TextView)findViewById(R.id.historylast11);
        txthisto12 = (TextView)findViewById(R.id.historylast12);
        txthisto13 = (TextView)findViewById(R.id.historylast13);
        txthisto14 = (TextView)findViewById(R.id.historylast14);
        txthisto15 = (TextView)findViewById(R.id.historylast15);
        txthisto16 = (TextView)findViewById(R.id.historylast16);

        txthisto21 = (TextView)findViewById(R.id.historylast21);
        txthisto22 = (TextView)findViewById(R.id.historylast22);
        txthisto23 = (TextView)findViewById(R.id.historylast23);
        txthisto24 = (TextView)findViewById(R.id.historylast24);
        txthisto25 = (TextView)findViewById(R.id.historylast25);
        txthisto26 = (TextView)findViewById(R.id.historylast26);

        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        txthisto11.setText(settings.getString("last1",""));
        txthisto12.setText(settings.getString("prelast1",""));
        txthisto13.setText(settings.getString("last13",""));
        txthisto14.setText(settings.getString("last14",""));
        txthisto15.setText(settings.getString("last15",""));
        txthisto16.setText(settings.getString("last16",""));

        txthisto21.setText(settings.getString("last2",""));
        txthisto22.setText(settings.getString("prelast2",""));
        txthisto23.setText(settings.getString("last23",""));
        txthisto24.setText(settings.getString("last24",""));
        txthisto25.setText(settings.getString("last25",""));
        txthisto26.setText(settings.getString("last26",""));
    }
}
