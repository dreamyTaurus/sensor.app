package com.example.sensorapp;

import static android.content.Context.WIFI_SERVICE;
import static android.os.SystemClock.elapsedRealtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.util.List;

public class WifiModule {

    WifiManager wifiManager;
    WifiReceiver wifiReceiver;

    //Wifi 스캔 관련 변수들
    boolean flag_running = false;
    int scan_counter = 0;
    long last_scan_time_ms = elapsedRealtime();
    final int scan_interval_ms = 5000;

    // 스래드 관련
    Looper wifi_scan_looper;
    //마지막 새리해
    String current_state = "";

    String TAG = "WIFI_MODULE";


    WifiModule(Context context){
        //casting을 하면 사용가능한 함수를 알려줌
        wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        wifiReceiver = new WifiReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(wifiReceiver, intentFilter);

        HandlerThread handlerThread = new HandlerThread("WIFI_THREAD", Process.THREAD_PRIORITY_DISPLAY);
        handlerThread.start();
        wifi_scan_looper = handlerThread.getLooper();
    }

    public String get_latest_state(){
        return current_state;
    }


    public void start(){
        flag_running = true;
        scan_counter = 0;

        invoke_wifi_scan_thread();
    }
    private void invoke_wifi_scan_thread(){
        if(!flag_running)
            return;

        wifiManager.startScan();
        scan_counter += 1;
        last_scan_time_ms = elapsedRealtime();

        Handler handler = new Handler(wifi_scan_looper);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                invoke_wifi_scan_thread();
            }
        }, scan_interval_ms);
    }

    public void stop(){

        flag_running = false;

    }

    class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Scan results receiver");
            List<ScanResult> scanResults = wifiManager.getScanResults();

            float P0 = -30;
            float eta = 2;
            float dist;

            String str = "";
            str += "Found " + scanResults.size() + " APs\n";
            for (int k=0;k<scanResults.size();k++){
                str += scanResults.get(k).SSID; //SSID 는 와이파이 이름
                str += ", " + scanResults.get(k).BSSID; //BSSID는 맥 address, wifi-AP의 주소
                str += ", " + scanResults.get(k).frequency + "MHz";
                str += ", " + scanResults.get(k).level + "dBm";

                float curr_p = scanResults.get(k).level;
                dist = (float) Math.pow(10,(P0 - curr_p)/eta/10);
                str += String.format(", distance : %.2f m\n", dist);

            }
            current_state = "Scan counter: " + scan_counter + ", # Aps" + scanResults.size();
        }
    }


}
