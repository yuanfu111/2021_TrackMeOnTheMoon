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
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.content.IntentFilter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private WifiManager wifiManager;
    //private Handler handler;
    //private Runnable runnable;
    private List<ScanResult> scan_results =new ArrayList<>();
    private int numTables;
    private List<List<List<Float>>> mac_tables = new ArrayList<List<List<Float>>>();
    private int numCells = 9;
    private Float[] prior_serial = new Float[numCells];
    private Float[] posterior_serial = new Float[numCells];
    private int prediction;
    private List<String> scanned_MACs = new ArrayList<String>();
    private List<Integer> scanned_RSS = new ArrayList<Integer>();
    private TextView CellA, CellB, CellC, CellD, CellE, CellF, CellG, CellH, CellI, target;
    private List<String> chosen_macs = new ArrayList<String>();
    private Drawable drawable_orange, drawable_white;
    private List<List<Integer>> online_test = new ArrayList<>();
    //for offline testing
    private List<List<Integer>> testing_sample = new ArrayList<>();
    private List<Integer> testing_target = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        target = (TextView) findViewById(R.id.target);
        CellA = (TextView)findViewById(R.id.CellA);
        CellB = (TextView)findViewById(R.id.CellB);
        CellC = (TextView)findViewById(R.id.CellC);
        CellD = (TextView)findViewById(R.id.CellD);
        CellE = (TextView)findViewById(R.id.CellF);
        CellF = (TextView)findViewById(R.id.CellG);
        CellG = (TextView)findViewById(R.id.CellH);
        CellH = (TextView)findViewById(R.id.CellI);
        CellI = (TextView)findViewById(R.id.CellE);
        drawable_orange = getResources().getDrawable(R.drawable.rectangle_orange);
        drawable_white = getResources().getDrawable(R.drawable.rectangle);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        loadData(false);
        //Toast.makeText(this,"Initializing..please wait",Toast.LENGTH_LONG).show();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiManager.isWifiEnabled() == false) {
            Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

    }

    protected void onResume() {
        super.onResume();

    }
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        //activity.setText("0.0");
        //get the the x,y,z values of the accelerometer
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
                    reader4.close();
                    //System.out.println(testing_sample);
                    Log.d("success", "Testing Loaded");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
    public void locate_me(View v) {

//        List<String> scaned_mac=new ArrayList<>();
//        List<Integer> scaned_rss=new ArrayList<>();
//
//        scaned_mac.add("28:d1:27:d8:0c:3e");
//        scaned_mac.add("c0:a0:bb:e9:87:85");
//        scaned_mac.add("c0:a0:bb:e9:87:87");//this one does not show in tha mac table. We need to clean it
//        scaned_mac.add("28:d1:27:d8:0c:3f");
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
//        scaned_mac.add("0a:26:97:e3:2c:81");
//        scaned_rss.add(-45);
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
          scanWifi();
          //execute_serial_filtering();
       // offline_test();
    }
    private void execute_serial_filtering() {
        Toast.makeText(this, "serial", Toast.LENGTH_SHORT).show();
        Integer[] sorted_indexes = sort(scanned_RSS);
        for (int index : sorted_indexes)
            System.out.println(scanned_RSS.get(index));
        int max_serial_itr = 5;
        if(max_serial_itr>scanned_RSS.size())
        {
            max_serial_itr=scanned_RSS.size();
        }
        for (int i = 0; i < max_serial_itr; i++) {
            System.out.println("Iteration: " + (i + 1));
            System.out.println("Scanned MACs: " + scanned_MACs.get(sorted_indexes[i]));
            System.out.println("Scanned RSS: " + scanned_RSS.get(sorted_indexes[i]));
            posterior_serial = sense_serial(prior_serial, scanned_MACs.get(sorted_indexes[i]), scanned_RSS.get(sorted_indexes[i]));
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
        System.out.println(prediction);



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
        Integer t = Integer.parseInt(target.getText().toString());
        if(!serial) {
            online_test.add(Arrays.asList(prediction, t));
            //System.out.println(online_test);
        }else{update_prior_txt();
        }
    }
    public void serial_done(View v){
        Integer t = Integer.parseInt(target.getText().toString());
        online_test.add(Arrays.asList(prediction, t));
    }
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
        Float[] inital_prior=new Float[]{1/9f,1/9f,1/9f,1/9f,1/9f,1/9f,1/9f,1/9f,1/9f};
        prior_serial=inital_prior;
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
            //System.out.println("Poster "+posterior[i]);
            //System.out.println("sum "+sum);
        }

        //normalization
        if(sum==0)
            sum =0.0000001f;
        for(int i=0;i<numCells;i++)
        {
            posterior[i]/=sum;
        }

        // for(int i=0;i<numCells;i++)
        // {
        //     System.out.println(posterior[i]);
        // }
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

        // for(int i=0;i<num_mac;i++)
        // {   System.out.println("mac"+i);
        //     for(int j=0;j<numCells;j++){
        //         System.out.println((prior_parallel.get(i))[j]);
        //     }
        // }
        //Majority vote
        int[] vote=new int[numCells];
        for(int i=0;i<num_mac;i++){
            int max_index=getMaxIndex(prior_parallel.get(i));
            vote[max_index]+=1;
        }
        return getMaxIndex(vote);
    }

//    private int[] sort_MACs (List<Integer> rss_values) {
//        int[] ranks = new int[rss_values.size()];
//        Arrays.fill(ranks, -101);
//        List<Integer> rss_temp = new ArrayList<>();
//        for (int i=0; i<rss_values.size(); ++i) {
//            rss_temp.add(rss_values.get(i));
//        }
//        // 1. Find the max value, obtain index_max
//        // 2. Update ranks[index_max]
//        // 3. Set the current max value to -101f, go back to step 1
//        for (int i=0; i<rss_values.size(); ++i) {
//            Integer max = Collections.max(rss_temp);
//            int index_max = rss_temp.indexOf(max);
//            ranks[index_max] = i;
//            rss_temp.set(index_max, -101);
//        }
//        return ranks;
//    }

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
