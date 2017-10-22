package com.example.charles.twintracker;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimerTask;

import java.util.Timer;

public class MainActivity extends AppCompatActivity {

    Button strtBttn1,strtBttn2,stopBttn1,stopBttn2;
    TextView txtLastDate1,txtLastDate2,txtCurrentCount1,txtCurrentCount2,txtPreLast1,txtPreLast2,txtCurrentDuration1,txtCurrentDuration2;

    Timer timer1, timer2;

    TwinTimerTask twinTimerTask1, twinTimerTask2;

    public static final String PREFS_NAME = "lastdata";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        strtBttn1 = (Button)findViewById(R.id.start_button_1);
        strtBttn2 = (Button)findViewById(R.id.start_button_2);
        stopBttn1 = (Button)findViewById(R.id.stop_button_1);
        stopBttn2 = (Button)findViewById(R.id.stop_button_2);
        txtCurrentCount1 = (TextView)findViewById(R.id.current_timer_1);
        txtCurrentCount2 = (TextView)findViewById(R.id.current_timer_2);
        txtLastDate1 = (TextView)findViewById(R.id.last1_time);
        txtLastDate2 = (TextView)findViewById(R.id.last2_time);
        txtPreLast1 = (TextView)findViewById(R.id.prelast1_time);
        txtPreLast2 = (TextView)findViewById(R.id.prelast2_time);

        txtCurrentDuration1 = (TextView)findViewById(R.id.current_duration_1);
        txtCurrentDuration2 = (TextView)findViewById(R.id.current_duration_2);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        txtLastDate1.setText(settings.getString("last1",""));
        txtLastDate2.setText(settings.getString("last2",""));

        txtPreLast1.setText(settings.getString("prelast1",""));
        txtPreLast2.setText(settings.getString("prelast2",""));

        final SharedPreferences.Editor editor = settings.edit();

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
                    timer1.cancel();
                    timer1 = null;

                    AlertDialog.Builder builder;
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Enregister")
                            .setMessage("La têtée est terminée ?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

                                    editor.putString("prelast1", settings.getString("last1",""));
                                    editor.apply();
                                    editor.putString("last1",txtCurrentCount1.getText().toString()+"  "+txtCurrentDuration1.getText().toString());
                                    editor.apply();

                                    txtLastDate1.setText(settings.getString("last1",""));
                                    txtPreLast1.setText(settings.getString("prelast1",""));
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
            }
        });

        stopBttn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timer2!=null) {
                    timer2.cancel();
                    timer2 = null;

                    AlertDialog.Builder builder;
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Enregister")
                            .setMessage("La têtée est terminée ?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

                                    editor.putString("prelast2", settings.getString("last2",""));
                                    editor.apply();
                                    editor.putString("last2",txtCurrentCount2.getText().toString()+"  "+txtCurrentDuration2.getText().toString());
                                    editor.apply();

                                    txtLastDate2.setText(settings.getString("last2",""));
                                    txtPreLast2.setText(settings.getString("prelast2",""));
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
            }
        });
    }


    class TwinTimerTask extends TimerTask {

        TextView refTxtView1, refTxtView2;
        int secondes = 0;
        int minutes =0;

        public TwinTimerTask(TextView refTxtView1, TextView refTxtView2) {

            this.refTxtView1 = refTxtView1;
            this.refTxtView2 = refTxtView2;
        }

        @Override
        public void run() {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
            final String strdate = simpleDateFormat.format(calendar.getTime());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    secondes +=1;
                    if(secondes == 60) {
                        minutes +=1;
                        secondes = 0;
                    }

                    String timertext = "("+minutes + " : " + secondes+")";

                    refTxtView2.setText(timertext);
                    refTxtView1.setText(strdate);
                }
            });
        }
    }
}
