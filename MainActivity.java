package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final int Default_Update_Interval = 1;
    public static final int Fast_Update_Interval = 1;
    private static final int PERMISSIONS_FINE_LOCATION = 99;

    private ScrollView sv_log;
    private TextView tv_log;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "notification";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("channel1", name, importance);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this.
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();

        // 화면 꺼짐 방지
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        // component 변수
        // location 설정 == 위치 요청
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000 * Default_Update_Interval);
        locationRequest.setFastestInterval(1000 * Fast_Update_Interval);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("1", "notification", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // location 콜백 함수 등록 == 위치 정보를 얻으면 해야할 행동
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Location location = locationResult.getLastLocation();
//                updateUIValue(location);
                getMyTrafficLight(location);
//                sv_log.fullScroll(ScrollView.FOCUS_DOWN);
            }
        };

        //위치 정보를 얻기 위한 객체
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, PERMISSIONS_FINE_LOCATION);
            }
        }
        else {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    public void notificationButton(View view) {

        final String CHANNEL_ID = "channel1";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(1, builder.build());
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_FINE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
            } else {
                Toast.makeText(this, "This app requires permission to be granted in order to work properly", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

//    private void updateUIValue(Location location)
//    {
//        String tempStr;
//
//        // 시간 표시
//        Date dt = new Date();
//        tv_log.append("\n\n" + dt.toString());
//        double MyLatitude = location.getLatitude();
//        double MyLongitude = location.getLongitude();
//        // 현재 location 정보 표시
//        tempStr = String.format("\nLat : %.8f, Lon : %.8f ",MyLatitude, MyLongitude);
//        tv_log.append(tempStr);
//        sv_log.fullScroll(ScrollView.FOCUS_DOWN);
//    }
    public void letsWalk(View v) {
        Toast toast = Toast.makeText(MainActivity.this, "알림 울리기", Toast.LENGTH_SHORT);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setText("건너세요!");
        toast.setMargin(0.1f, 0.1f);
        toast.show();
    }

    private List<Test> testList = new ArrayList<>();

    private void getMyTrafficLight(Location location) {
        InputStream is = getResources().openRawResource(R.raw.semitrafficlight);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, Charset.forName("UTF-8"))
        );

        String line = "";

        double minDist = Double.MAX_VALUE;
        double minLat = 0;
        double minLon = 0;
        int LightNum = 0;
        String sequence = "";

        try {
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                Log.d("MyActivity", "Line: " + line);
                String[] tokens = line.split(",");

                double trafficLat = Double.parseDouble(tokens[9]);
                double trafficLon = Double.parseDouble(tokens[10]);

                double myLat = location.getLatitude();
                double myLon = location.getLongitude();

                double dist = calculateDistance(myLat, myLon, trafficLat, trafficLon);
                // Update the nearest point if necessary
                if (dist < minDist) {
                    minDist = dist;
                    minLat = trafficLat;
                    minLon = trafficLon;
                    LightNum = Integer.parseInt(tokens[14]);
                    sequence = tokens[19];
                }
                Log.d("MyActivity", "Just created: ");
                
                //timeToken[0]은 무조건 녹색
                String[] timeToken = sequence.split("\\+");
            }

        } catch (IOException e) {
            Log.d("MyActivity", "Error reading data file on line" + line, e);
            e.printStackTrace();
        }
    }

    //Earth radius in kilometers
    public static final double Ra = 6371;

    //calculate distance
    public static double calculateDistance(double myLat, double myLon, double traffLat, double traffLon) {
        myLat = Math.toRadians(myLat);
        myLon = Math.toRadians(myLon);
        traffLat = Math.toRadians(traffLat);
        traffLon = Math.toRadians(traffLon);

        // Calculate the differences
        double dLat = myLat - traffLat;
        double dLon = myLon - traffLon;

        // Apply the formula
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(myLat) * Math.cos(traffLat) * Math.pow(Math.sin(dLon / 2), 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double d = Ra * c;

        // Return the distance
        return d;
    }

    //notification




}
