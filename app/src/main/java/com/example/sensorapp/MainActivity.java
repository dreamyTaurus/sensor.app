package com.example.sensorapp;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.CHANGE_WIFI_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.SystemClock.elapsedRealtime;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView tv;
    FloatingActionButton fab,fab2;
    boolean flag_running = false;

    //file
    FileModule fileModule;
    private long begin_time;

    //Module
    ImageModule imageModule;
    SensorModule sensorModule;
    WifiModule wifiModule;


    //permission-related
    private boolean is_permission_granted = false;

    //Thread
    Looper display_update_looper;
    int count = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //사용권한에 대해 승인을 받겠다
        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, CHANGE_WIFI_STATE, ACCESS_WIFI_STATE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, 1);



        tv = (TextView) findViewById(R.id.tv);
        //btn = (Button) findViewById(R.id.btn);
        //btn2 = (Button) findViewById(R.id.btn2);
        //btn.setText("Start");
        //btn2.setText("Wi-fi");

        fab = (FloatingActionButton) findViewById(R.id.fab);



        //Sensor module class를 인스턴스화 시킴
        sensorModule = new SensorModule(this);
        wifiModule = new WifiModule(getApplicationContext());
        imageModule = new ImageModule(this);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flag_running)
                    stop();
                else
                    start();
            }
        });

        /*btn2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                wifiManager.startScan();
            }
        });*/

        HandlerThread handlerThread = new HandlerThread("DISPLAY_UPDATE_THREAD", Process.THREAD_PRIORITY_DISPLAY);
        handlerThread.start();
        display_update_looper = handlerThread.getLooper();



        Handler handler = new Handler(display_update_looper);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                display_update_thread();
            }
        },0);

    }

    private void display_update_thread(){
        count +=1;
        tv.setText(count + "");

        if (flag_running){
            float deg = sensorModule.get_heading();
            //imageModule.plot_arrow(0,0, deg);

            String str = "";
            str += "[WiFi]\n" + wifiModule.get_latest_state() + "\n";
            str += "[Sensor]\n" + sensorModule.get_latest_state();
            tv.setText(str);

        }


        Handler handler = new Handler(display_update_looper);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                display_update_thread();
            }
        },100);
    }

    private void thread_test(){
        Thread thread = Thread.currentThread();
        Log.d("THREAD_TEST",thread.getName() + ", "+thread.getId());
        for(int i=0;i<10;i++){
            Log.d("THREAD_TEST",thread.getName()+":"+i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void stop(){
        flag_running = false;

        sensorModule.stop();
        wifiModule.stop();
        // btn.setText("Start");
        fab.setImageTintList(ColorStateList.valueOf(Color.rgb(0,0,0)));

    }

    private void start(){
        if(!is_permission_granted) {
            Toast.makeText(getApplicationContext(), "Permission is not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        begin_time = elapsedRealtime();
        fileModule = new FileModule(this,"test",true,true,".txt");

        //각각의 모듈 실행시킴
        sensorModule.start(begin_time, fileModule);
        wifiModule.start();

        flag_running = true;
        fab.setImageTintList(ColorStateList.valueOf(Color.rgb(57,155,226)));



    }

//    @Override
//    public void onAccuracyChanged(Sensor sensor, int i) {
//
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResult) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResult);
        for (int i = 0; i < grantResult.length; i++)
            if (grantResult[i] != 0) {
                Toast.makeText(getApplicationContext(), "Warning: " + permissions[i] + "is not granted", Toast.LENGTH_SHORT).show();
                return;
            }
        is_permission_granted = true;
    }


}