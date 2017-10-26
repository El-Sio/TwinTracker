package com.example.charles.twintracker;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimerTask;

import java.util.Timer;

public class MainActivity extends AppCompatActivity {

    Button strtBttn1,strtBttn2,stopBttn1,stopBttn2,bathBttn1,bathBttn2;
    ImageButton historyBttn;
    TextView txtLastDate1,txtLastDate2,txtCurrentCount1,txtCurrentCount2,txtPreLast1,txtPreLast2,txtCurrentDuration1,txtCurrentDuration2;

    ArrayList<feeding> feedings;
    Timer timer1, timer2;

    TwinTimerTask twinTimerTask1, twinTimerTask2;

    public static final String PREFS_NAME = "lastdata";

    public static boolean ongoing1,ongoing2;
    public static String started1,started2;
    public static long starttime1, starttime2;

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
                    a1 = feedings.get(j).getStart() + feedings.get(j).getDuration();

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
                    z1 = feedings.get(j).getStart() + feedings.get(j).getDuration();

                }
            }

            txtLastDate2.setText(z1);
            txtPreLast2.setText(z2);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        feedings = new ArrayList<>();

        setContentView(R.layout.activity_main);
        historyBttn = (ImageButton)findViewById(R.id.historybttn);
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

        new DownloadWebpageTask(new AsyncResult() {
            @Override
            public void onResult(JSONArray object) {
                processJson(object);

            }
        }).execute("http://japansio.info/api/feedings.json");


        historyBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                displayHistory(v);
            }
        });

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

                                    timer1.cancel();
                                    timer1 = null;
                                    txtPreLast1.setText(txtLastDate1.getText());
                                    txtLastDate1.setText(txtCurrentCount1.getText().toString()+"  "+txtCurrentDuration1.getText().toString());

                                    feedings.add(new feeding("agathe",txtCurrentCount1.getText().toString(),txtCurrentDuration1.getText().toString()));
                                    String json = new Gson().toJson(feedings);

                                    System.out.println(json);
                                    new UploadDataTask().execute("http://japansio.info/api/putdata.php",json);

                                    txtCurrentCount1.setText("");
                                    txtCurrentDuration1.setText("");

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
                else {

                    AlertDialog.Builder builder;
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Enregister")
                            .setMessage("La têtée est terminée ?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                    txtPreLast1.setText(txtLastDate1.getText());
                                    txtLastDate1.setText(txtCurrentCount1.getText().toString()+"  "+txtCurrentDuration1.getText().toString());

                                    feedings.add(new feeding("agathe",txtCurrentCount1.getText().toString(),txtCurrentDuration1.getText().toString()));
                                    String json = new Gson().toJson(feedings);

                                    System.out.println(json);
                                    new UploadDataTask().execute("http://japansio.info/api/putdata.php",json);

                                    txtCurrentCount1.setText("");
                                    txtCurrentDuration1.setText("");

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

                    AlertDialog.Builder builder;
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Enregister")
                            .setMessage("La têtée est terminée ?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

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
                else {
                    AlertDialog.Builder builder;
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Enregister")
                            .setMessage("La têtée est terminée ?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                    txtPreLast2.setText(txtLastDate2.getText());
                                    txtLastDate2.setText(txtCurrentCount2.getText().toString()+"  "+txtCurrentDuration2.getText().toString());

                                    feedings.add(new feeding("zoé",txtCurrentCount2.getText().toString(),txtCurrentDuration2.getText().toString()));
                                    String json = new Gson().toJson(feedings);

                                    System.out.println(json);
                                    new UploadDataTask().execute("http://japansio.info/api/putdata.php",json);

                                    txtCurrentCount2.setText("");
                                    txtCurrentDuration2.setText("");
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
