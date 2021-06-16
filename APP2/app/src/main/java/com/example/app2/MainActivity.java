package com.example.app2;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.IntentFilter;


import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.zip.DataFormatException;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private WifiManager wifiManager;
    //private Handler handler;
    //private Runnable runnable;
    private List<ScanResult> scan_results =new ArrayList<>();
    private int numTables;
    private List<List<List<Float>>> mac_tables = new ArrayList<List<List<Float>>>();
    private List<Float[][]> trans_mtrixs =new ArrayList<>();
    private int numCells = 9;
    private Float[] prior_serial = new Float[numCells];
    private Float[] posterior_serial = new Float[numCells];
    private int prediction;
    private List<String> scanned_MACs = new ArrayList<String>();
    private List<Integer> scanned_RSS = new ArrayList<Integer>();
    private TextView CellA, CellB, CellC, CellD, CellE, CellF, CellG, CellH, CellI, target,textView3;
    private List<String> chosen_macs = new ArrayList<String>();
    private Drawable drawable_orange, drawable_white;
    private List<List<Integer>> online_test = new ArrayList<>();
    //for offline testing
    private List<List<Integer>> testing_sample = new ArrayList<>();
    private List<Integer> testing_target = new ArrayList<>();
    private boolean move_pause;
    private FuseOrientation fuseSensor = new FuseOrientation();
    private double azimuthValue;
    private int offset=-65;
    private double distance;
    private DecimalFormat d = new DecimalFormat("#");
    String dir;
    private double aX=0, aY=0, aZ=0, mag=0;
    private String state = "idle"; // Walking or idle
    private double walk_threshold = 15; // Threshold for determining walking; personal
    private ArrayList<Double> accData = new ArrayList<>();
    private int sampleCount = 0;
    private long startTime=0, currentTime = 0;
    private double walkingTime;
    private boolean sampling_done;
    private Clock clock = Clock.systemDefaultZone();
    private int window = 1000; // 1000ms
    private double speed = 1; // Yujin's walking speed is 1.5m/s
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //target = (TextView) findViewById(R.id.target);
        textView3=(TextView) findViewById(R.id.textView3);
        CellA = (TextView)findViewById(R.id.CellA);
        CellB = (TextView)findViewById(R.id.CellB);
        CellC = (TextView)findViewById(R.id.CellC);
        CellD = (TextView)findViewById(R.id.CellD);
        CellE = (TextView)findViewById(R.id.CellE);
        CellF = (TextView)findViewById(R.id.CellF);
        CellG = (TextView)findViewById(R.id.CellG);
        CellH = (TextView)findViewById(R.id.CellH);
        CellI = (TextView)findViewById(R.id.CellI);
        drawable_orange = getResources().getDrawable(R.drawable.rectangle_orange);
        drawable_white = getResources().getDrawable(R.drawable.rectangle);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        loadData(false);
        move_pause=true;
        dir="None";
        //Toast.makeText(this,"Initializing..please wait",Toast.LENGTH_LONG).show();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiManager.isWifiEnabled() == false) {
            Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);
        fuseSensor.setMode(FuseOrientation.Mode.FUSION);
    }
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);
    }
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    private void get_distance(SensorEvent event) {
        if (sampleCount == 0) {
            sampleCount++;
            startTime = clock.millis();
            currentTime = startTime;
//            System.out.println("startTime: " + startTime);
        }else{
            sampleCount++;
            currentTime = clock.millis();
//            System.out.println("currentTime: " + currentTime);
        }
        aX = event.values[0];
        aY = event.values[1];
        aZ = event.values[2];
        mag = Math.sqrt(aX*aX + aY*aY + aZ*aZ); // magnitude of acceleration
//        if (accData.size()<sampleSize){
        // angleSum+=azimuthValue;
        if (currentTime - startTime < window){
            accData.add(mag);
            sampling_done=false;
            //inputAngle=angleSum/sampleCount;

        }
        else{
            state = DetectWalk(accData);
            sampling_done=true;
            //angleSum=0;
            if (state == "walking") {
                walkingTime = (currentTime - startTime)/1000.0; // walking during the last sampling window
                distance = walkingTime * speed; // window内移动的距离
                //distance=0.2;
//                System.out.println(walkingTime);
            }
            else {
                distance=0;
            }
//            System.out.println("Current state: " + state);
            accData.clear();
//            System.out.println("Samples in 1s: " + sampleCount);
            sampleCount = 0;
        }
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                fuseSensor.setAccel(event.values);
                fuseSensor.calculateAccMagOrientation();
                get_distance(event);
                break;
            case Sensor.TYPE_GYROSCOPE:
                fuseSensor.gyroFunction(event);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                fuseSensor.setMagnet(event.values);
                break;
        }
        azimuthValue = (fuseSensor.getAzimuth()+360+offset)%360;
        if(sampling_done && !move_pause) {
            if(distance>speed*1) {
                if(azimuthValue<45 ||azimuthValue>=315) {
                    dir="up";
                }
                if(45<=azimuthValue &&azimuthValue<135) {
                    dir="right";
                }
                if(135<=azimuthValue&&azimuthValue<225) {
                    dir="down";
                }
                if(225<=azimuthValue&&azimuthValue<315) {
                    dir="left";
                }
            }
        }
        textView3.setText(d.format(azimuthValue) +" "+dir);
    }

    //Load the local training data
    private void loadData(boolean offtest) {
        InputStream in1 = this.getResources().openRawResource(R.raw.macs);
        BufferedReader reader1 = new BufferedReader(new InputStreamReader(in1));
        if (in1 != null) {
            String line;
            try {
                //load chosen macs
                while ((line = reader1.readLine()) != null) {
                    chosen_macs.add(line);
                }
                Log.d("success", "Mac Loaded");
                reader1.close();

                //load mac tables
                numTables = chosen_macs.size();
                String[] ids = new String[numTables];
                for (int i = 0; i < numTables; i++) {
                    ids[i] = "table_mac" + i;
                }
                for (int i = 0; i < numTables; i++) {
                    int k = getResources().getIdentifier(ids[i], "raw", getPackageName());
                    InputStream in2 = this.getResources().openRawResource(k);
                    BufferedReader reader2 = new BufferedReader(new InputStreamReader(in2));
                    List<List<Float>> mac_table = new ArrayList<>();
                    while ((line = reader2.readLine()) != null) {
                        //System.out.println(line);
                        String[] line_split = line.split("\\s+");
                        List<Float> sample_split = new ArrayList<>();
                        //get rid of the first element which is ""
                        for (int j = 1; j < line_split.length; j++) {
                            sample_split.add(Float.parseFloat(line_split[j]));
                        }
                        mac_table.add(sample_split);
                    }
                    mac_tables.add(mac_table);
                    reader2.close();
                }
                Log.d("success", "Mac_tables Loaded");
                if(offtest) {
                    //load offline testing data
                    InputStream in3 = this.getResources().openRawResource(R.raw.testing_target);
                    BufferedReader reader3 = new BufferedReader(new InputStreamReader(in3));
                    while ((line = reader3.readLine()) != null) {
                        testing_target.add((int) Float.parseFloat(line));
                    }
                    reader3.close();
                    //System.out.println(testing_target);
                    InputStream in4 = this.getResources().openRawResource(R.raw.testing_sample);
                    BufferedReader reader4 = new BufferedReader(new InputStreamReader(in4));
                    while ((line = reader4.readLine()) != null) {
                        //System.out.println(line);
                        String[] line_split = line.split("\\s+");
                        List<Integer> sample_split = new ArrayList<>();
                        //get rid of the first element which is ""
                        for (int j = 1; j < line_split.length; j++) {
                            sample_split.add((int) Float.parseFloat(line_split[j]));
                        }
                        testing_sample.add(sample_split);
                    }
                }
                String[] motion_id = new String[4];
                motion_id[0]="motionup";
                motion_id[1]="motiondown";
                motion_id[2]="motionleft";
                motion_id[3]="motionright";
                for (int i = 0; i < 4; i++) {
                    int k = getResources().getIdentifier(motion_id[i], "raw", getPackageName());
                    InputStream in5 = this.getResources().openRawResource(k);
                    BufferedReader reader5 = new BufferedReader(new InputStreamReader(in5));
                    Float[][] trans_mtrix = new Float[numCells][numCells];
                    int n=0;
                    while ((line = reader5.readLine()) != null) {
                        String[] line_split = line.split("\\s+");
                        //get rid of the first element which is ""
                        for (int j = 1; j < line_split.length; j++) {
                            trans_mtrix[n][j-1]=Float.parseFloat(line_split[j]);
                        }
                        n++;
                    }
                    trans_mtrixs.add(trans_mtrix);
                    reader5.close();
                }
                //System.out.println(testing_sample);
                Log.d("success", "transition_matrix loaded");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private String DetectWalk(ArrayList<Double> accData){
        String state = "idle";
        double[] results = autocorrelation(accData);
//        for (int i=0; i<results.length; ++i) {
//            System.out.println("Result" + i + ": " + results[i]);
//        }
        // Find the maximum of autocorrelation results
        double max = results[0];
        for (int i=1; i<results.length; ++i) {
            if (results[i] > max) {
                max = results[i];
//                System.out.println("Max correlation: " + max);
            }
        }
        if (max > walk_threshold) {
            state = "walking";
        }
        return state;
    }
    // Brute force autocorrelation
    private double[] autocorrelation(ArrayList<Double> accData){
        double sum = 0;
        for (int i=0; i<accData.size(); ++i) {
            sum += accData.get(i);
        }
        double avg = sum/accData.size();
        double[] results = new double[accData.size()];
        for (int i=0; i<accData.size(); ++i){
            results[i] = 0; // i: lag
            for (int j=0; j<accData.size(); ++j){
                results[i] += (accData.get(j) - avg) * (accData.get((j+i)%accData.size()) - avg);
            }
        }
//        save2file(results);
        return results;
    }
    private void enableView(View v, boolean b) {
        if (b == true) {
            v.setEnabled(true);
            v.getBackground().setColorFilter(null);
        } else {
            v.setEnabled(false);
            v.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.ADD);
        }
    }
    private void scanWifi() {
        scanned_MACs.clear();
        scanned_RSS.clear();
        if (scan_results != null) scan_results.clear();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        boolean success =wifiManager.startScan();
        //Toast.makeText(this, "Scanning WiFi ...", Toast.LENGTH_SHORT).show();
        Log.d("success", "Scanning WiFi ...");
    }
    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("scan complete");
            //Toast.makeText(getApplicationContext(), "Complete scan!", Toast.LENGTH_SHORT).show();
            scan_results = wifiManager.getScanResults();
            if (scan_results != null) {
                for (ScanResult scanResult : scan_results) {
                    scanned_MACs.add(scanResult.BSSID);
                    scanned_RSS.add(scanResult.level);
                }
                clean_scan_result(scanned_MACs, scanned_RSS);
                execute_serial_filtering();
                System.out.println("Hello, the results are ready!");
                System.out.println("Size of the result: " + scan_results.size());
            } else {
                Toast.makeText(getApplicationContext(), "scan again", Toast.LENGTH_SHORT).show();
            }

            unregisterReceiver(this);
        }
    };
    private void offline_test(){
        scanned_MACs=chosen_macs;
        int count = 0;
        for(int i =0;i<testing_sample.size();i++) {
            scanned_RSS = testing_sample.get(i);
            execute_serial_filtering();
            init_belief();
            //execute_parallel_filtering();
            if (prediction == testing_target.get(i))
                count++;
            online_test.add(Arrays.asList(prediction,testing_target.get(i)));
        }
        float accuracy = count*1.0f/testing_sample.size();
        System.out.println("Sample size: "+testing_sample.size()+"\nCorrect prediction: "+count+"\nAccuracy: "+accuracy);
        System.out.println(online_test);
    }
    public void start_move(View v) {
        Button btn=(Button)v;
        move_pause=!move_pause;
        if(move_pause==false) {
            btn.setText("Disable Motion");
        } else {
            btn.setText("Enable Motion");
            //dir="None";
            //move(dir);
        }
    }

    public void locate_me(View v) {

//        List<String> scaned_mac=new ArrayList<>();
//        List<Integer> scaned_rss=new ArrayList<>();
//
//        scaned_mac.add("00:4a:77:6a:6f:2f");
//        scaned_mac.add("00:5a:13:74:59:10");
//        scaned_mac.add("08:26:97:e7:a0:d1");//this one does not show in tha mac table. We need to clean it
//        scaned_mac.add("48:f8:b3:25:4f:6c");
//        scaned_mac.add("68:ff:7b:a9:67:94");
//        scaned_mac.add("34:e8:94:bd:dd:d3");
//        scaned_mac.add("34:e8:94:bd:dd:d4");
//        scaned_mac.add("48:f8:b3:40:f8:8d");
//        scaned_mac.add("00:4a:77:6a:6f:2e");
//        scaned_mac.add("68:ff:7b:a9:67:93");
//        scaned_mac.add("24:f5:a2:dd:25:34");
//        scaned_mac.add("58:8b:f3:4e:cc:e4");
//        scaned_mac.add("08:26:97:e3:2c:81");
//        scaned_mac.add("82:2a:a8:11:e2:3f");
//        scaned_mac.add("00:4a:77:6a:6f:2f");
//        scaned_rss.add(-44);
//        scaned_rss.add(-45);
//        scaned_rss.add(-47);
//        scaned_rss.add(-49);
//        scaned_rss.add(-52);
//        scaned_rss.add(-60);
//        scaned_rss.add(-60);
//        scaned_rss.add(-63);
//        scaned_rss.add(-63);
//        scaned_rss.add(-68);
//        scaned_rss.add(-72);
//        scaned_rss.add(-73);
//        scaned_rss.add(-77);
//        scaned_rss.add(-78);
//        scaned_rss.add(-78);
//        scanned_MACs=scaned_mac;
//        scanned_RSS=scaned_rss;
          //if(move_pause) {
        if(dir!="None") {
            move(dir);
        }
        scanWifi();
          //}
        //execute_serial_filtering();
       // offline_test();
    }
    private void execute_serial_filtering() {
        Toast.makeText(this, "serial", Toast.LENGTH_SHORT).show();
        Integer[] sorted_indexes = sort(scanned_RSS);
        int max_serial_itr = 5;
        if(max_serial_itr>scanned_RSS.size())
        {
            max_serial_itr=scanned_RSS.size();
        }
        for (int i = 0; i < max_serial_itr; i++) {
            System.out.println("Iteration: " + (i + 1));
            System.out.println("Scanned MACs: " + scanned_MACs.get(sorted_indexes[i]));
            System.out.println("Scanned RSS: " + scanned_RSS.get(sorted_indexes[i]));
            posterior_serial = sense_serial(prior_serial, scanned_MACs.get(sorted_indexes[0]), scanned_RSS.get(sorted_indexes[0]));

            if (check_steady_state()) {
                Toast.makeText(this, "Steady State reached", Toast.LENGTH_SHORT).show();
                System.out.println("Steady State reached");
                break;//if reaches steady state
            }
            updata_serial_prior();
            if(i==max_serial_itr){
                Toast.makeText(this, "max interation reached", Toast.LENGTH_SHORT).show();
            }
        }

        prediction = getMaxIndex(posterior_serial);
        //System.out.println(prediction);
        show_result(prediction,true);
    }
    private void show_result(int prediction,boolean serial){
        switch (prediction) {
            case 0:
                resetBg();
                CellA.setBackground(drawable_orange);
                break;
            case 1:
                resetBg();
                CellB.setBackground(drawable_orange);
                break;
            case 2:
                resetBg();
                CellC.setBackground(drawable_orange);
                break;
            case 3:
                resetBg();
                CellD.setBackground(drawable_orange);
                break;
            case 4:
                resetBg();
                CellE.setBackground(drawable_orange);
                break;
            case 5:
                resetBg();
                CellF.setBackground(drawable_orange);
                break;
            case 6:
                resetBg();
                CellG.setBackground(drawable_orange);
                break;
            case 7:
                resetBg();
                CellH.setBackground(drawable_orange);
                break;
            case 8:
                resetBg();
                CellI.setBackground(drawable_orange);
                break;
        }
        //Integer t = Integer.parseInt(target.getText().toString());
        if(!serial) {
            //online_test.add(Arrays.asList(prediction, t));
            //System.out.println(online_test);
        }else{update_prior_txt();
        }
    }
//    public void serial_done(View v){
//        Integer t = Integer.parseInt(target.getText().toString());
//        online_test.add(Arrays.asList(prediction, t));
//    }
    private void execute_parallel_filtering(){

        List<Float[]> prior_parallel=new ArrayList<>();
        for(int i=0;i<scanned_RSS.size();i++)
        {
            prior_parallel.add(new Float[]{1/9f,1/9f,1/9f,1/9f,1/9f,1/9f,1/9f,1/9f,1/9f});
        }

        prediction = sense_parallel(prior_parallel,scanned_MACs,scanned_RSS);
        show_result(prediction,false);
        System.out.println(prediction);
    }
    public void init_belief(View v)
    {
        dir="None";
        Float[] inital_prior=new Float[]{1/9f,1/9f,1/9f,1/9f,1/9f,1/9f,1/9f,1/9f,1/9f};
        prior_serial=inital_prior;
        posterior_serial=inital_prior;
        resetBg();
        update_prior_txt();
    }
    private void update_prior_txt(){
        DecimalFormat decimalFormat= new  DecimalFormat( ".000" );
        CellA.setText("A "+ decimalFormat.format(prior_serial[0]));
        CellB.setText("B "+decimalFormat.format(prior_serial[1]));
        CellC.setText("C "+decimalFormat.format(prior_serial[2]));
        CellD.setText("D "+decimalFormat.format(prior_serial[3]));
        CellE.setText("E "+ decimalFormat.format(prior_serial[4]));
        CellF.setText("F "+decimalFormat.format(prior_serial[5]));
        CellG.setText("G "+decimalFormat.format(prior_serial[6]));
        CellH.setText("H "+decimalFormat.format(prior_serial[7]));
        CellI.setText("I "+decimalFormat.format(prior_serial[8]));
    }
    private void init_belief()
    {
        Float[] inital_prior=new Float[]{1/9f,1/9f,1/9f,1/9f,1/9f,1/9f,1/9f,1/9f,1/9f};
        resetBg();
        prior_serial=inital_prior;

    }

    private boolean check_steady_state(){
        boolean steady=true;
        //if any big change happens, we should keep the iteration
        for(int i=0;i<prior_serial.length;i++)
        {
            System.out.println(posterior_serial[i]);
            if(prior_serial[i]==0 || posterior_serial[i]==0) continue;
            float change_rate=Math.abs(prior_serial[i]-posterior_serial[i])/prior_serial[i];
            System.out.println("change rate: "+change_rate);
            if(change_rate>0.01 || posterior_serial[i]<0.85 )
            {
                steady=false;
            }
        }
        return steady;
        //if(prior_serial[0]-posterior_serial[0])/

    }
    private void resetBg(){
        CellA.setBackground(drawable_white);
        CellB.setBackground(drawable_white);
        CellC.setBackground(drawable_white);
        CellD.setBackground(drawable_white);
        CellE.setBackground(drawable_white);
        CellF.setBackground(drawable_white);
        CellG.setBackground(drawable_white);
        CellH.setBackground(drawable_white);
        CellI.setBackground(drawable_white);
    }

    private void clean_scan_result(List<String> raw_mac,List<Integer> raw_rss){
        Iterator<String> it_mac=raw_mac.iterator();
        Iterator<Integer> it_rss=raw_rss.iterator();
        while(it_mac.hasNext() && it_rss.hasNext())
        {
            String i=it_mac.next();
            it_rss.next();
            if(!(chosen_macs.contains(i))){
                it_mac.remove();
                it_rss.remove();
            }
        }
    }
    public void updata_serial_prior(){
        prior_serial=posterior_serial;
    }
    private void move(String orientation) {
        Float[][] transition_matrx = new Float[numCells][numCells];
        boolean move=true;
        switch (orientation) {
            case "up":
                transition_matrx = trans_mtrixs.get(0);
                break;
            case "down":
                transition_matrx = trans_mtrixs.get(1);
                break;
            case "left":
                transition_matrx = trans_mtrixs.get(2);
                break;
            case "right":
                transition_matrx = trans_mtrixs.get(3);
                break;
            default://no movement scanning
                move=false;
        }
        if(move==true) {
            // matrix multiplication
            Float[] prior = new Float[]{0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f};

            for (int col = 0; col < numCells; col++) {
                for (int row = 0; row < numCells; row++) {
                    prior[col] += prior_serial[row] * transition_matrx[row][col];
                }
            }
            prior_serial=prior;
        }
        // show the result of motion
        prediction=getMaxIndex(prior_serial);
        show_result(prediction,true);
    }
    public Float[] sense_serial(Float[] prior,String rss_mac,Integer rss)
    {
        Float[] posterior=new Float[numCells];
        int index_table =-1;
        for(int i=0;i<numTables;i++){
            //System.out.println(chosen_macs.get(i));
            if(rss_mac.equals(chosen_macs.get(i))){
                index_table=i;
            }
        }
        if(index_table==-1){
            System.out.println("Mac:"+rss_mac+" does not exist in the table list");
            return null;
        }
        int column=100+rss;
        Float sum = 0f;
        for(int i=0;i<numCells;i++){

            posterior[i]=prior[i]*mac_tables.get(index_table).get(i).get(column);
            sum+= posterior[i];
            if(posterior[i]==0&&sum==0)
            {
                System.out.println("Prior"+i+":"+prior[i]);
                System.out.println("table:"+index_table+" col: "+column+" "+mac_tables.get(index_table).get(i).get(column));

            }
        }
        //normalization
        if(sum==0)
            sum =0.0000001f;
        for(int i=0;i<numCells;i++)
        {
            posterior[i]/=sum;

        }

        return posterior;
    }
    private int getMaxIndex(Float[] arr) {
        int minIndex=0;
        for(int i =1;i<arr.length;i++){
            if(arr[minIndex]<arr[i]){
                minIndex=i;
            }
        }
        return minIndex;
    }
    private int getMaxIndex(int[] arr) {
        int minIndex=0;
        for(int i =1;i<arr.length;i++){
            if(arr[minIndex]<arr[i]){
                minIndex=i;
            }
        }
        return minIndex;
    }
    private int sense_parallel(List<Float[]> prior_parallel,List<String> rss_mac, List<Integer> rss){
        int num_mac=rss_mac.size();

        //List<Float[]> posteriors=new ArrayList<>();
        for(int i=0;i<num_mac;i++){
            Float[] prior=prior_parallel.get(i);
            //update the prior
            prior_parallel.set(i,sense_serial(prior,rss_mac.get(i),rss.get(i)));
        }
        //Majority vote
        int[] vote=new int[numCells];
        for(int i=0;i<num_mac;i++){
            int max_index=getMaxIndex(prior_parallel.get(i));
            vote[max_index]+=1;
        }
        return getMaxIndex(vote);
    }

    private Integer[] sort(List<Integer> input)
    {
        Integer[] indices = new Integer[input.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        Arrays.sort(indices, new Comparator<Integer>() {
            public int compare(Integer i1, Integer i2) {
                return input.get(i2).compareTo(input.get(i1));
            }
        });
        return indices;
    }
    public void saveTest(View v){
        FileOutputStream out ;
        BufferedWriter writer = null;
        try{
            out = openFileOutput("test_result.txt",Context.MODE_PRIVATE);
            writer=new BufferedWriter(new OutputStreamWriter(out));
            for(List<Integer> each_test: online_test){
                String line=Integer.toString(each_test.get(0)) +' '+each_test.get(1)+'\n';
                writer.write(line);
            }
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try{
                writer.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }

        //show the accuracy
        int right_count=0;
        for(List<Integer> each_test:online_test){
            if(each_test.get(0)==each_test.get(1))
                right_count+=1;
        }
        float accuracy=right_count*1.0f/online_test.size();
        Toast.makeText(this,"Accuracy "+accuracy,Toast.LENGTH_LONG).show();
    }
}
