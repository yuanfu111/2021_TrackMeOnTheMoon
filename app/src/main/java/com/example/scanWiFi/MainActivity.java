package com.example.scanWiFi;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.Clock;
import java.util.List;
import java.util.ArrayList;


import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.content.BroadcastReceiver;
import android.widget.ArrayAdapter;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.ListView;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.PorterDuff;
/**
 * Reference: https://ssaurel.medium.com/develop-a-wifi-scanner-android-application-daa3b77feb73
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private Button buttonScan, buttonReset;
    private Button CellA_Btn, CellB_Btn, CellC_Btn, CellD_Btn;
    private String label;
    private ListView listView;
    private List<ScanResult> results;
    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayAdapter adapter;
    private int count = 0;
    private Clock clock = Clock.systemDefaultZone();
    private Handler handler;
    private Runnable runnable;
    private int interval = 1000;
    private int max_sample = 100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create items.
        buttonScan = findViewById(R.id.scanBtn);
        buttonReset = findViewById(R.id.restBtn);
        CellA_Btn = findViewById(R.id.CellA_Btn);
        CellB_Btn = findViewById(R.id.CellB_Btn);
        CellC_Btn = findViewById(R.id.CellC_Btn);
        CellD_Btn = findViewById(R.id.CellD_Btn);

        // Set listener for the button.
        // Push the button once, scan multiple times with given interval
        buttonScan.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                handler = new Handler();
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (count < max_sample) {
                            scanWifi();
                            handler.postDelayed(this, interval);
                        }
                        else {
                            handler.removeCallbacks(runnable);
                            buttonScan.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.OVERLAY);
                        }
                    }
                };
                handler.postDelayed(runnable, interval);
            }
        });

        // Push the button once, scan once
//        buttonScan.setOnClickListener(new View.OnClickListener() {
//          @Override
//          public void onClick(View view) {
//              scanWifi();
//          }
//        });

        buttonReset.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                arrayList.clear();
                count = 0;
                adapter.notifyDataSetChanged();
            }
        });

        CellA_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                label = "A";
            }
        });

        CellB_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                label = "B";
            }
        });

        CellC_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                label = "C";
            }
        });

        CellD_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                label = "D";
            }
        });

        listView = findViewById(R.id.wifiList);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);
        Toast.makeText(getApplicationContext(), "Please select the cell and then press the scan button.", Toast.LENGTH_SHORT).show();
    }

    private void scanWifi() {
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
        Toast.makeText(this, "Scanning WiFi ...", Toast.LENGTH_SHORT).show();
        }

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            count ++;
            if (count > 1){
                Toast.makeText(getApplicationContext(), "Complete " + count + " scans!", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(getApplicationContext(), "Complete 1 scan!", Toast.LENGTH_SHORT).show();
            }
            results = wifiManager.getScanResults();
            unregisterReceiver(this);

            for (int i=0; i<results.size(); ++i) {
                arrayList.add(results.get(i).SSID + " " + results.get(i).BSSID + " " + results.get(i).level + " " + label +'\n');
                if (i == results.size()-1) {
                    arrayList.add("---\n");
                }
                adapter.notifyDataSetChanged();
            }
            if (count == max_sample) {
                save2file(arrayList);
            }
        }
    };

    private void save2file(ArrayList<String> scanResults){
        FileOutputStream out ;
        BufferedWriter writer = null;
        try{
            out = openFileOutput("RSS_" + clock.millis() + ".txt", Context.MODE_PRIVATE);
            writer = new BufferedWriter(new OutputStreamWriter(out));
            for(String scanResult : scanResults){
                writer.write(scanResult);
            }
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            try{
                writer.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}