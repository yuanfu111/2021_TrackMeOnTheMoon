/*
1. Do one WiFi scan
2. Sort access points according to RSS values
 */
package com.example.app2;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import android.app.Activity;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.content.Context;
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

public class MainActivity extends Activity {

    private WifiManager wifiManager;
    private Button buttonLocate;
    private TextView CellA, CellB, CellC, CellD, CellE, CellF, CellG, CellH, CellI;
    private List<ScanResult> scan_results;
    private ArrayList<String> scanned_MACs = new ArrayList<String>();
    private ArrayList<Integer> scanned_RSS = new ArrayList<Integer>();
    private List<Float> rss_values = new ArrayList<Float>();
    private List<String> chosen_macs = Arrays.asList("68:ff:7b:a9:67:94", "28:d1:27:d8:0c:3f", "28:d1:27:d8:0c:3e");
    private boolean scan_complete = false;
    private int[] ranks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create items.
        buttonLocate = findViewById(R.id.Locate_me);
        CellA = findViewById(R.id.CellA);
        CellB = findViewById(R.id.CellB);
        CellC = findViewById(R.id.CellC);
        CellD = findViewById(R.id.CellD);
        CellE = findViewById(R.id.CellE);
        CellF = findViewById(R.id.CellF);
        CellG = findViewById(R.id.CellG);
        CellH = findViewById(R.id.CellH);
        CellI = findViewById(R.id.CellI);

        // Set listener for the Start button.
        buttonLocate.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                scanWifi();
//                System.out.println(scan_complete);
                if (scan_complete) {
//                    rss_values = get_current_rss(chosen_macs);
                    rss_values = Arrays.asList(-10f, -30f, -15f);
                    ranks = sort_MACs(rss_values);
                    Toast.makeText(MainActivity.this, "Ranks: " + ranks, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set wifi manager.
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }
    }

    private List<Float> get_current_rss(List<String> chosen_macs) {
        Float[] rss_values = new Float[chosen_macs.size()];
        Arrays.fill(rss_values, -100f);

        // Compare scanned MACs against chosen MACs
        if (scanned_MACs.size() != 0) {
            for (int i = 0; i < chosen_macs.size(); ++i) {
                for (int j = 0; j < scanned_MACs.size(); ++j) {
                    if ((chosen_macs.get(i)).equals(scanned_MACs.get(j))) {
                        rss_values[i] = scanned_RSS.get(j).floatValue();
                    }
                }
            }
        }
        return Arrays.asList(rss_values);
    }

    // Return the rank of MACs.
    // For example, if rss_values = [-10, -30, -15], the returned result is index = [0, 2, 1]
    public int[] sort_MACs (List<Float> rss_values) {
        int[] ranks = new int[rss_values.size()];
        Arrays.fill(ranks, -101);
        // 1. Find the max value, obtain index_max
        // 2. Update ranks[index_max]
        // 3. Set the current max value to -101f, go back to step 1
        List<Float> rss_temp = rss_values;
        for (int i=0; i<rss_values.size(); ++i) {
            Float max = Collections.max(rss_temp);
            int index_max = rss_temp.indexOf(max);
            ranks[index_max] = i;
            rss_temp.set(index_max, -101f);
        }
        return ranks;
    }

    private void scanWifi() {
//        scanned_MACs.clear();
//        scanned_RSS.clear();
//        if (scan_results!=null) scan_results.clear();
        scan_complete = false;
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
        Toast.makeText(this, "Scanning WiFi ...", Toast.LENGTH_SHORT).show();
    }

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(getApplicationContext(), "Complete scan!", Toast.LENGTH_SHORT).show();
            scan_results = wifiManager.getScanResults();
            System.out.println("Hello, the results are ready!");
            System.out.println("Size of the result: "+scan_results.size());
            unregisterReceiver(this);

            for (ScanResult scanResult : scan_results) {
                System.out.println("The results are being stored!");
                scanned_MACs.add(scanResult.BSSID);
                scanned_RSS.add(scanResult.level);
                System.out.println(scanResult.BSSID);
                System.out.println(scanResult.level);
            }
            scan_complete = true;
        }
    };
}