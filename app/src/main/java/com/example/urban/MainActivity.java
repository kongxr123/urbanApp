package com.example.urban;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Double.NaN;

public class MainActivity extends AppCompatActivity  {

    TextView tv_time, tv_wx, tv_wy, tv_wz, tv_fx, tv_fy, tv_fz, tv_mx, tv_my, tv_mz, tv_pr;     // Sensor Data Display Controls
    TextView tv_gnss_tim, tv_gnss_lat, tv_gnss_lng, tv_gnss_alt;      // GNSS Data Display Controls
    TextView tv_nmea_tim, tv_nmea_lat, tv_nmea_lng, tv_nmea_alt;     // NMEA Data Display Controls
    Button   btn_save;      // 数据保存按钮
    Button   button ;      // 数据保存按钮
    public float[] wib = new float[30], fsf = new float[30], mag = new float[30];     // motion sensor data
    public float pre;      // 气压传感器数据
    public int[] CNT = new int[]{0, 0, 0, 0};      // count value
    public float[] sensordata = new float[10]; // motion sensor data array
    public double[] gnssData = new double[8]; // GNSS data array
    public double[] nmeaData = new double[10]; // NMEA data array
    public static long t_stp, t0_stp; // system time flag
    public static float t; // system time
    public static float PI = 3.141592653f; // PI constant
    public static float DEG = PI / 180.0f; // degree conversion constant
    public static final int SENSORS = 201; // sensor data display message label
    public static final int GNSS = 202; // GNSS data display label
    public static final int NMEA = 203; // NMEA data display label
    public static final int OPEN_SET_REQUEST = 600; // permission request flag
    public DataStore dataSource; // data storage class
    public MessageEvent messageEvent;
    public static boolean isSave = false; // data save flag
    public final String TAG="position";
    @RequiresApi(api = Build.VERSION_CODES.S)
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_time = findViewById(R.id.tv_time); // Control initialization
        tv_wx = findViewById(R.id.tv_Grox);
        tv_wy = findViewById(R.id.tv_Groy);
        tv_wz = findViewById(R.id.tv_Groz);
        tv_fx = findViewById(R.id.tv_Accx);
        tv_fy = findViewById(R.id.tv_Accy);
        tv_fz = findViewById(R.id.tv_Accz);
        tv_mx = findViewById(R.id.tv_Magx);
        tv_my = findViewById(R.id.tv_Magy);
        tv_mz = findViewById(R.id.tv_Magz);
        tv_pr = findViewById(R.id.tv_Pre);
        tv_gnss_tim = findViewById(R.id.tv_GNSStime);
        tv_gnss_lat = findViewById(R.id.tv_GNSSLat);
        tv_gnss_lng = findViewById(R.id.tv_GNSSLng);
        tv_gnss_alt = findViewById(R.id.tv_GNSSAlt);
        tv_nmea_tim = findViewById(R.id.tv_NMEAtime);
        tv_nmea_lat = findViewById(R.id.tv_NMEALat);
        tv_nmea_lng = findViewById(R.id.tv_NMEALng);
        tv_nmea_alt = findViewById(R.id.tv_NMEAAlt);
        btn_save = findViewById(R.id.btn_save);
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //监听按钮，如果点击，就跳转
                Intent intent = new Intent();
                Bundle bundle=new Bundle();
                if(String.valueOf(gnssData[1])!=null&&String.valueOf(gnssData[2])!=null) {
                    bundle.putString("lat", String.valueOf(gnssData[1]));
                    bundle.putString("lng", String.valueOf(gnssData[2]));
                    Log.d(TAG, String.valueOf(gnssData[1]));
                    Log.d(TAG, String.valueOf(gnssData[2]));
                }
                EventBus.getDefault().post(new MainActivity());
                intent.setComponent(new ComponentName("com.example.urban", "com.example.urban.MapsActivity"));//参数为包名和类名，注意类名中要包括包名
                sendBroadcast(intent);
                intent.setClass(MainActivity.this,MapsActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        // 请求权限
        int hasPermission = ContextCompat.checkSelfPermission(getApplication(),
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (hasPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    OPEN_SET_REQUEST);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.HIGH_SAMPLING_RATE_SENSORS},
                    OPEN_SET_REQUEST);
        }
        t0_stp = System.currentTimeMillis();
        MsgHandler msgHandler = new MsgHandler(); // message handler
        dataSource = new DataStore(getApplicationContext()); // data storage class
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // Define SensorManager
        Sensor sensor_gyr = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED); // define the rotation speed along x, y, z
        Sensor sensor_acc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED);// Magnetic field
        Sensor sensor_mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);//Used to sense air pressure
        Sensor sensor_pre = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);//Used to sense air pressure

        sensorManager.registerListener(new SensorEventListener() { // Register gyroscope data listener
            public void onSensorChanged(SensorEvent event) {
                wib[0] += event.values[0];
                wib[1] += event.values[1];
                wib[2] += event.values[2];
                CNT[0]++;
            }

            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        }, sensor_gyr, sensorManager.SENSOR_DELAY_UI);


        sensorManager.registerListener(new SensorEventListener() {      // Register accelerometer data monitoring

            public void onSensorChanged(SensorEvent event) {
                fsf[0] += event.values[0];
                fsf[1] += event.values[1];
                fsf[2] += event.values[2];
                CNT[1]++;
            }

            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        }, sensor_acc, sensorManager.SENSOR_DELAY_UI);


        sensorManager.registerListener(new SensorEventListener() {      // Registering Magnetometer Data Listening
            public void onSensorChanged(SensorEvent event) {
                mag[0] += event.values[0];
                mag[1] += event.values[1];
                mag[2] += event.values[2];
                CNT[2]++;
            }
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        }, sensor_mag, sensorManager.SENSOR_DELAY_UI);


        sensorManager.registerListener(new SensorEventListener() {      // Registering barometric data monitoring
            public void onSensorChanged(SensorEvent event) {
                pre += event.values[0];
                CNT[3]++;
            }
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        }, sensor_pre, sensorManager.SENSOR_DELAY_UI);


        Timer timer = new Timer();              // Timer - used to regularly sample and save data
//
        timer.schedule(new TimerTask() {        // 定义定时任务
            public void run() {
                t_stp = System.currentTimeMillis();
                t_stp = t_stp - t0_stp;         // Calculate system time
                t = t_stp / 1000.0f;
                    for (int i = 0; i < 3; i++) {
                        if(CNT[0]==0) sensordata[i]= 0;
                        if(CNT[1]==0) sensordata[i+3]= 0;
                        if(CNT[2]==0) sensordata[i+6]= 0;
                        if(sensordata[i]==NaN) sensordata[i]=0;
                        sensordata[i] = wib[i] / CNT[0] / DEG;
                        sensordata[i+3] = fsf[i] / CNT[1];
                        sensordata[i+6] = mag[i] / CNT[2];
                        fsf[i] = 0;
                        wib[i] = 0;
                        mag[i] = 0;
                    }
                    sensordata[9] = pre / CNT[3];
                    pre = 0;
                    CNT[0] = 0;
                    CNT[1] = 0;
                    CNT[2] = 0;
                    CNT[3] = 0;
                // 发送消息
                if (isSave && dataSource !=null) {
                    try {
                        dataSource.writeCSVData(sensordata, gnssData, nmeaData, t); //Save all data
                        messageEvent.writeGnss(gnssData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Message msg = msgHandler.obtainMessage(); // Send message to update sensor data
                msg.arg1 = SENSORS;
                msgHandler.sendMessage(msg);
            }
        }, 0, 10);


        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.addNmeaListener(new OnNmeaMessageListener() {
            public void onNmeaMessage(String message, long timestamp) {
                String head = message.substring(0,6);
                 if (head.equals("$GPGGA") || head.equals("$GNGGA")) {
                    String[] contents = message.split(",");
                    if (contents[1].length()>0) {
                        nmeaData[0] = Double.parseDouble(contents[1]);      // UTC
                        nmeaData[1] = Double.parseDouble(contents[2]);      // Lat
                        nmeaData[2] = Double.parseDouble(contents[4]);      // Lng
                        nmeaData[3] = Double.parseDouble(contents[6]);      // State
                        nmeaData[4] = Double.parseDouble(contents[7]);      // nS
                        Message msg = msgHandler.obtainMessage();           // Send a message to update the display
                        msg.arg1 = NMEA;
                        msgHandler.sendMessage(msg);
                    }
                }
            }
        });

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, new LocationListener() {
            public void onLocationChanged(@NonNull Location location) { // Location change callback method
                gnssData[0] = location.getTime(); // UTC time
                gnssData[1] = location.getLatitude(); // latitude
                gnssData[2] = location.getLongitude(); // longitude
                gnssData[3] = location.getAltitude(); // altitude
                gnssData[4] = location.getAccuracy(); // horizontal positioning accuracy
                Message msg = msgHandler.obtainMessage(); // Send message to update data
                msg.arg1 = GNSS;
                msgHandler.sendMessage(msg);
                EventBus.getDefault().post(new MessageEvent(gnssData[1],gnssData[2]));
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                Date date = new Date();
                @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("yyyy,MM,dd HH:mm:ss");
                String format = dateFormat.format(date);
                DatabaseReference latitude = database.getReference(format).child("latitude");
                DatabaseReference longitude = database.getReference(format).child("longtitude");
                latitude.setValue(gnssData[1]);
                longitude.setValue(gnssData[2]);
            }
        });

        btn_save.setOnClickListener(new View.OnClickListener() {            // save button listener function
            public void onClick(View v) {
                if (isSave) {
                    isSave = false;
                    dataSource.close();
                    btn_save.setText("SAVE");
                } else {
                    try {
                        dataSource.open();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    isSave = true;
                    btn_save.setText("STOP");
                }
            }
        });
    }



    private class MsgHandler extends Handler {          // message handler
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.arg1) {
                case SENSORS:
                    SensorDisp(sensordata, t);
                    break;
                case GNSS:
                    GNSSDisp(gnssData);
                    break;
                case NMEA:
                    NMEADisp(nmeaData);
                    break;
            }
        }
    }

    private void SensorDisp(float[]sensordata, float t)
    {
        tv_time.setText(String.format("%8.3f",t));
        tv_wx.setText(String.format("%8.3f",sensordata[0]));
        tv_wy.setText(String.format("%8.3f",sensordata[1]));
        tv_wz.setText(String.format("%8.3f",sensordata[2]));
        tv_fx.setText(String.format("%8.3f",sensordata[3]));
        tv_fy.setText(String.format("%8.3f",sensordata[4]));
        tv_fz.setText(String.format("%8.3f",sensordata[5]));
        tv_mx.setText(String.format("%8.3f",sensordata[6]));
        tv_my.setText(String.format("%8.3f",sensordata[7]));
        tv_mz.setText(String.format("%8.3f",sensordata[8]));
        tv_pr.setText(String.format("%8.3f",sensordata[9]));
    }

    private void GNSSDisp(double[] gnssData)
    {
        tv_gnss_tim.setText(UTCtoCST((long) gnssData[0]));
        tv_gnss_lat.setText(String.valueOf(gnssData[1]));
        tv_gnss_lng.setText(String.valueOf(gnssData[2]));
        tv_gnss_alt.setText(String.valueOf(gnssData[3]));
    }

    private void NMEADisp(double[] nmeaData)
    {
        tv_nmea_tim.setText(String.valueOf(nmeaData[0]));
        tv_nmea_lat.setText(String.valueOf(nmeaData[1]));
        tv_nmea_lng.setText(String.valueOf(nmeaData[2]));
        tv_nmea_alt.setText(String.valueOf(nmeaData[6]));
    }

    public static String UTCtoCST(long sec) {
        SimpleDateFormat format = new SimpleDateFormat("HHmmss");
        format.setTimeZone(TimeZone.getTimeZone( "UTC+1")); //Irish time zone GMT+8
        return format.format(new Date(sec));
    }

}
