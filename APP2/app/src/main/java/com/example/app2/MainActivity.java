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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.IntentFilter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private WifiManager wifiManager;
    private Handler handler;
    private Runnable runnable;
    private List<ScanResult> scan_results;
    private int numTables;
    private List<String> macs = new ArrayList<String>();
    private List<List<List<Float>>> mac_tables = new ArrayList<List<List<Float>>>();
    private int numCells=4;
    private Float[] prior_serial=new Float[numCells];
    private Float[] posterior_serial=new Float[numCells];
    private ArrayList<String> scanned_MACs = new ArrayList<String>();
    private ArrayList<Integer> scanned_RSS = new ArrayList<Integer>();
    private TextView  CellA,CellB,CellC,CellD;
    private List<List<Float>> sample_table_loc = new ArrayList<List<Float>>();
    private List<String> chosen_macs = new ArrayList<String>();
    private Drawable drawable_orange,drawable_white;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CellA=findViewById(R.id.CellA);
        CellB=findViewById(R.id.CellB);
        CellC=findViewById(R.id.CellC);
        CellD=findViewById(R.id.CellD);
        drawable_orange = getResources().getDrawable(R.drawable.rectangle_orange);
        drawable_white = getResources().getDrawable(R.drawable.rectangle);
        sensorManager=(SensorManager) getSystemService(Context.SENSOR_SERVICE);
        loadData();
        //Toast.makeText(this,"Initializing..please wait",Toast.LENGTH_LONG).show();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        if (wifiManager.isWifiEnabled()==false) {
//            Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
//            wifiManager.setWifiEnabled(true);
//        }
        Float[] labels_act=new Float[]{0.0f,1.0f,2.0f},
                labels_loc=new Float[]{0.0f,1.0f,2.0f,3.0f};

    }
    protected void onResume(){
        super.onResume();

    }
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    @Override
    public void onSensorChanged(SensorEvent event){
        //activity.setText("0.0");
        //get the the x,y,z values of the accelerometer


    }
    //Load the local training data
    private void loadData(){
        InputStream in1=this.getResources().openRawResource(R.raw.macs);
        //in3=this.getResources().openRawResource(R.raw.chosen_macs);
        BufferedReader reader1=new BufferedReader(new InputStreamReader(in1));
        //reader2=new BufferedReader(new InputStreamReader(in2)),
        //reader3=new BufferedReader(new InputStreamReader(in3));

        //List<Float> each_sample=new ArrayList<>();
        if(in1!=null){
            String line;
            try{
                //Reading activity table
                while((line=reader1.readLine())!=null){
                    macs.add(line);
                }
                Log.d("success","Mac Loaded");
                reader1.close();
                numTables=macs.size();

                String[] ids=new String[numTables];
                for(int i=0;i<numTables;i++)
                {
                    ids[i]="table_mac"+i;
                }

                System.out.println(line);
                for(int i=0;i<numTables;i++){
                    int k=getResources().getIdentifier(ids[i],"raw",getPackageName());
                    InputStream in2=this.getResources().openRawResource(k);
                    BufferedReader reader2=new BufferedReader(new InputStreamReader(in2));
                    List<List<Float>> mac_table=new ArrayList<>();
                    while ((line = reader2.readLine()) != null) {
                        //System.out.println(line);
                        String[] line_split=line.split("\\s+");
                        List<Float> sample_split=new ArrayList<>();
                        //get rid of the first element which is ""
                        for(int j=1;j<line_split.length;j++){
                            sample_split.add(Float.parseFloat(line_split[j]));
                        }
                        mac_table.add(sample_split);
                    }
                    mac_tables.add(mac_table);
                    reader2.close();
                }
            }catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void enableView(View v,boolean b){
        if(b==true){
            v.setEnabled(true);
            v.getBackground().setColorFilter(null);
        }
        else{
            v.setEnabled(false);
            v.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.ADD);
        }
    }
    private void scanWifi() {
        scanned_MACs.clear();
        scanned_RSS.clear();
        if(scan_results!=null) scan_results.clear();
        //registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
        Toast.makeText(this, "Scanning WiFi ...", Toast.LENGTH_SHORT).show();
        scan_results = wifiManager.getScanResults();
        if(scan_results!=null) {
            for (ScanResult scanResult : scan_results) {
                scanned_MACs.add(scanResult.BSSID);
                scanned_RSS.add(scanResult.level);
            }
        }
        else{
        }
        Toast.makeText(getApplicationContext(), "Complete scan!", Toast.LENGTH_SHORT).show();
    }
//    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            Toast.makeText(getApplicationContext(), "Complete scan!", Toast.LENGTH_SHORT).show();
//            scan_results = wifiManager.getScanResults();
//            System.out.println("Hello, the results are ready!");
//            System.out.println("Size of the result: "+scan_results.size());
//            unregisterReceiver(this);
//        }
//    };

    public void locate_me(View v) {
        scanWifi();
        for (ScanResult scanResult : scan_results) {
            System.out.println("The results are being stored!");
            scanned_MACs.add(scanResult.BSSID);
            scanned_RSS.add(scanResult.level);
            System.out.println(scanResult.BSSID);
            System.out.println(scanResult.level);
        }
        System.out.println("All results are stored.");
        Float[] inital_prior=new Float[]{0.25f,0.25f,0.25f,0.25f};
        prior_serial=inital_prior;
        List<String> scaned_mac=new ArrayList<>();
        List<Integer> scaned_rss=new ArrayList<>();
        scaned_mac.add("c0:a0:bb:e9:87:85");
        scaned_mac.add("28:d1:27:d8:0c:3e");
        scaned_mac.add("68:ff:7b:a9:67:94");//this one does not show in tha mac table. We need to clean it
        scaned_mac.add("48:f8:b3:40:f8:8d");
        scaned_mac.add("28:d1:27:d8:0c:3f");
        scaned_mac.add("c0:a0:bb:e9:87:87");
        scaned_mac.add("00:4a:77:6a:6f:2e");
        scaned_mac.add("58:8b:f3:4e:cc:e4");
        scaned_mac.add("34:e8:94:bd:dd:d4");
        scaned_mac.add("34:e8:94:bd:dd:d3");
        scaned_mac.add("20:e8:82:f0:4b:3c");
        scaned_mac.add("68:ff:7b:a9:67:93");
        scaned_mac.add("b0:4e:26:1d:d9:6b");
        scaned_mac.add("80:2a:a8:11:e2:3f");
        scaned_mac.add("00:4a:77:6a:6f:2f");
        scaned_rss.add(-37);
        scaned_rss.add(-46);
        scaned_rss.add(-53);
        scaned_rss.add(-54);
        scaned_rss.add(-57);
        scaned_rss.add(-59);
        scaned_rss.add(-66);
        scaned_rss.add(-69);
        scaned_rss.add(-70);
        scaned_rss.add(-72);
        scaned_rss.add(-74);
        scaned_rss.add(-75);
        scaned_rss.add(-79);
        scaned_rss.add(-83);
        scaned_rss.add(-88);

        clean_scan_result(scaned_mac,scaned_rss);
        //scaned_mac=scanned_MACs;
        //scaned_rss=scanned_RSS;

        Integer[] sorted_indexes=sort(scaned_rss);
        for(int index:sorted_indexes)
            System.out.println(scaned_rss.get(index));
        int max_serial_itr=10;

        for(int i=0;i<max_serial_itr;i++)
        {
            System.out.println("Iteration: "+(i+1));
            System.out.println(scaned_mac.get(sorted_indexes[i]));
            System.out.println(scaned_rss.get(sorted_indexes[i]));
            posterior_serial=sense_serial(prior_serial,scaned_mac.get(sorted_indexes[i]),scaned_rss.get(sorted_indexes[i]));
            if(check_steady_state()) {
                System.out.println("Steady State reached");
                break;//if reaches steady state
            }


            updata_serial_prior();
        }

        int pred_serial=getMaxIndex(posterior_serial);
        System.out.println(pred_serial);
////
////        List<Float[]> prior_parallel=new ArrayList<>();
////        for(int i=0;i<scaned_rss.size();i++)
////        {
////            prior_parallel.add(new Float[]{0.25f,0.25f,0.25f,0.25f});
////        }
////
////        int pred_parallel = sense_parallel(prior_parallel,scaned_mac,scaned_rss);
//        //System.out.println(pred_parallel);
//
//        float prediction=pred_serial;
//
//        if (Math.abs(prediction - 0.0) < 0.1) {
//            resetBg();
//            CellA.setBackground(drawable_orange);
//        }
//        if (Math.abs(prediction - 1.0) < 0.1) {
//            resetBg();
//            CellB.setBackground(drawable_orange);
//        }
//
//        if (Math.abs(prediction - 2.0) < 0.1) {
//            resetBg();
//            CellC.setBackground(drawable_orange);
//        }
//        if (Math.abs(prediction - 3.0) < 0.1) {
//            resetBg();
//            CellD.setBackground(drawable_orange);
//        }

    }
    private boolean check_steady_state(){
        boolean steady=true;
        //if any big change happens, we should keep the iteration
        for(int i=0;i<prior_serial.length;i++)
        {
            float change_rate=Math.abs(prior_serial[i]-posterior_serial[i]);///prior_serial[i];
            System.out.println("change rate: "+change_rate);
            if(change_rate>0.000000001)
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
    }

    private  void clean_scan_result(List<String> raw_mac,List<Integer> raw_rss){
        Iterator<String> it_mac=raw_mac.iterator();
        Iterator<Integer> it_rss=raw_rss.iterator();
        while(it_mac.hasNext() && it_rss.hasNext())
        {
            String i=it_mac.next();
            it_rss.next();
            if(!(macs.contains(i))){
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
            //System.out.println(macs.get(i));
            if(rss_mac.equals(macs.get(i))){
                index_table=i;
            }
        }
        if(index_table==-1){
            System.out.println("Mac:"+rss_mac+" does not exist in the table list");
            return null;
        }
        int column=(int)(100+rss);
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

}
