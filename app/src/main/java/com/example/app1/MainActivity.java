package com.example.app1;

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
import java.util.List;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private WifiManager wifiManager;
    private WifiInfo wifiInfo;
    private Handler handler;
    private Runnable runnable;
    private Float aX=0f,aY=0f,aZ=0f;
    private List<ScanResult> scan_results;
    private ArrayList<String> scanned_MACs = new ArrayList<String>();
    private ArrayList<Integer> scanned_RSS = new ArrayList<Integer>();
    private int interval=20,step=15,window=30,sample_numer=0;//ms
    private TextView  activity,CellA,CellB,CellC,CellD;
    private boolean start_scan=false,scan_complete = false;
    private boolean start_get_data=false;//set to 1 if the data in a window starts to be collected
    private List<List<Float>> data_per_window=new ArrayList<>();
    private List<List<Float>> sample_table_act = new ArrayList<List<Float>>();
    private List<List<Float>> sample_table_loc = new ArrayList<List<Float>>();
    private List<String> chosen_macs = new ArrayList<String>();
    private Drawable drawable_orange,drawable_white;
    private KNN knn_act,knn_loc;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity=findViewById(R.id.activity);
        CellA=findViewById(R.id.CellA);
        CellB=findViewById(R.id.CellB);
        CellC=findViewById(R.id.CellC);
        CellD=findViewById(R.id.CellD);
        drawable_orange = getResources().getDrawable(R.drawable.rectangle_orange);
        drawable_white = getResources().getDrawable(R.drawable.rectangle);
        sensorManager=(SensorManager) getSystemService(Context.SENSOR_SERVICE);
        loadData();
        get_current_act_data();
        //Toast.makeText(this,"Initializing..please wait",Toast.LENGTH_LONG).show();
        if(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!=null){
            accelerometer=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            sensorManager.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
        }
        else{

        }
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        if (wifiManager.isWifiEnabled()==false) {
//            Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
//            wifiManager.setWifiEnabled(true);
//        }
        Float[] labels_act=new Float[]{0.0f,1.0f,2.0f},
                labels_loc=new Float[]{0.0f,1.0f,2.0f,3.0f};
        knn_act=new KNN(3,labels_act,sample_table_act);
        knn_loc=new KNN(5,labels_loc,sample_table_loc);
    }
    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
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
        aX = event.values[0];
        aY = event.values[1];
        aZ = event.values[2];


        if(data_per_window.size()!=window){
            if(start_get_data==false){
                get_current_act_data();
            }
        }
        else{
            //System.out.println(data_per_window);
            List<Float> acc_data=new ArrayList<Float>(Arrays.asList(aX,aY,aZ));
            List<Float> input_data = new ArrayList<>(featureExtraction(data_per_window));
            Float prediction =knn_act.predict(input_data);
            activity.setText(set_activity_text(prediction));
            for(int i =0;i<step;i++){
                data_per_window.remove(i);
            }
            //data_per_window.clear();
        }

    }
    private String set_activity_text(Float prediction){
        String result = "";
        if(Math.abs(prediction-0.0)<0.1)
            result= "Still";
        if(Math.abs(prediction-1.0)<0.1)
            result= "Walking";
        if(Math.abs(prediction-2.0)<0.1)
            result= "Jumping";
        return result;

    }
    //Load the local training data
    private void loadData(){

        //StringBuffer sbuffer = new StringBuffer();
        InputStream in1=this.getResources().openRawResource(R.raw.data_act),
                    in2=this.getResources().openRawResource(R.raw.data_loc),
                    in3=this.getResources().openRawResource(R.raw.chosen_macs);
        BufferedReader reader1=new BufferedReader(new InputStreamReader(in1)),
                       reader2=new BufferedReader(new InputStreamReader(in2)),
                       reader3=new BufferedReader(new InputStreamReader(in3));

        //List<Float> each_sample=new ArrayList<>();
        if(in1!=null && in2!=null){
            String line;
         try{
             //Reading activity table
             while((line=reader1.readLine())!=null){
                 String[] line_split=line.split("\\s+");
                 List<Float> sample_split=new ArrayList<>();
                 //get rid of the first element which is ""
                 //we start from 1 because the first element is '' because Matlab outputs are like this
                 for(int i=1;i<line_split.length;i++){
                     sample_split.add(Float.parseFloat(line_split[i]));
                 }
                 //System.out.println(sample_split);
                 sample_table_act.add(sample_split);
             }
             Log.d("success","Act Sample Loaded");
             reader1.close();
             //Reading localization table
             while((line=reader2.readLine())!=null){
                 String[] line_split=line.split("\\s+");
                 List<Float> sample_split=new ArrayList<>();
                 //get rid of the first element which is ""
                 for(int i=0;i<line_split.length;i++){
                     sample_split.add(Float.parseFloat(line_split[i]));
                 }
                 sample_table_loc.add(sample_split);
             }
             Log.d("success","Loc Sample Loaded");
             reader2.close();
             //Reading chosen macs list i.e. features for localization
             while((line=reader3.readLine())!=null){
                 chosen_macs.add(line);
             }
             Log.d("success","Macs list Loaded");
             reader3.close();

         }catch(IOException e) {
             e.printStackTrace();
         }
         }
    }
    //get the acc data in a window and return the data after feature extraction
    private void get_current_act_data(){
        start_get_data=true;
        handler =new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if(data_per_window.size()!=window) {
                    List<Float> acc_data=new ArrayList<Float>(Arrays.asList(aX,aY,aZ));
                    //sample_numer++;
                    //System.out.println(acc_data);
                    data_per_window.add(acc_data);
                    handler.postDelayed(this, interval);//iteratively call itself with delay
                }
                else {
                    //sample_numer=0;
                    start_get_data=false;
                    handler.removeCallbacks(runnable);
                }
            }
        };
        if(sample_numer<window) {
            handler.postDelayed(runnable, interval);//start
        }
    }
    private List<Float> featureExtraction(List<List<Float>> data_per_window){
        //calculate the means(ax,ay,az) medians(ax,ay,az) stds(ax,ay,az) maxs(ax,ay,az) mins(ax,ay,az) speeds(sqr(ax^2+ay^2+az^2)). 16 features
        List<Float> input_data =new ArrayList<>();
        List<Float> ax =new ArrayList<>();
        List<Float> ay =new ArrayList<>();
        List<Float> az =new ArrayList<>();
        for(List<Float> each:data_per_window){
            ax.add(each.get(0));
            ay.add(each.get(1));
            az.add(each.get(2));
        }
        input_data.add(get_mean(ax));
        input_data.add(get_mean(ay));
        input_data.add(get_mean(az));
        input_data.add(get_median(ax));
        input_data.add(get_median(ay));
        input_data.add(get_median(az));
        input_data.add(get_std(ax));
        input_data.add(get_std(ay));
        input_data.add(get_std(az));

        input_data.add(Collections.max(ax));
        input_data.add(Collections.max(ay));
        input_data.add(Collections.max(az));
        input_data.add(Collections.min(ax));
        input_data.add(Collections.min(ay));
        input_data.add(Collections.min(az));
        Double speed = Math.pow(get_mean(ax),2)+Math.pow(get_mean(ay),2)+Math.pow(get_mean(az),2);
        speed=Math.sqrt(speed);
        input_data.add(speed.floatValue());
        input_data.add(-1f);
        return input_data;
    }
    private float get_mean(List<Float> list){
        float sum=0f;
        for(int i = 0; i < list.size(); ++i)
        {
            sum +=list.get(i);
        }
        return sum/list.size();
    }
    private float get_median(List<Float> list){
        Double median;
        Collections.sort(list);
        if (list.size() % 2 == 1) {
            median = list.get(list.size()/2)*1.0;
        } else {
            median = (list.get(list.size()/2)+ list.get(list.size()/2-1)) / 2.0;
        }
        return median.floatValue();
    }
    private float get_std(List<Float> list){
        Double std=0d;
        float avg=get_mean(list);
        for(int i=0;i<list.size();i++){
            Double s=(list.get(i) - avg)*1.0;
            std+=Math.pow(s,2);
        }
        return std.floatValue();
    }

    private void enableView(View v,boolean b){
        if(b==true){
            v.setEnabled(true);
            v.getBackground().setColorFilter(null);
        }
        else{
            v.setEnabled(false);
            v.getBackground().setColorFilter(Color.WHITE, PorterDuff.Mode.OVERLAY);
        }
    }

    private List<Float>  get_current_rss(List<String> chosen_macs) {
        //System.out.println(chosen_macs);
        scanWifi();
//        System.out.println(scanned_MACs);
//        System.out.println(scanned_RSS);
//        System.out.println(chosen_macs);
        Float[] rss_values=new Float[chosen_macs.size()+1];//leave the last spot for the label
        Arrays.fill(rss_values,-100f);

        // Compare scanned MACs against chosen MACs
        if(scanned_MACs.size()!=0) {
            /* for testing
            scanned_MACs.remove(1);
            scanned_MACs.add("c0:a0:bb:e9:87:87");
            */
            for (int i = 0; i < chosen_macs.size(); ++i) {
                for(int j=0;j<scanned_MACs.size();++j) {
                    /* for testing
//                    System.out.println("chosen_macs(i)"+chosen_macs.get(i));
//                    System.out.println("scanned_mac(j)"+scanned_MACs.get(j));
                     */
                    if ((chosen_macs.get(i)).equals(scanned_MACs.get(j))) {
                        rss_values[i] = scanned_RSS.get(j).floatValue();

                    }
                }
            }
            /* for testing
            System.out.println(scanned_MACs);
            System.out.println(scanned_RSS);
            System.out.println(chosen_macs);
            for(Float each:rss_values){
                System.out.println(each);
            }
             */

            // Compare chosen MACs against scanned MACS
//            for (int i = 0; i < chosen_macs.size(); ++i) {
//                if (scanned_MACs.contains(chosen_macs.get(i)) == false) {
//                    rss_values.set(i,-100f);
//                }
//            }
        }
        //List<Float> output=new ArrayList<Float>(Arrays.asList(rss_values));
        //output.add(-1f);
        return Arrays.asList(rss_values);
    }

    private void scanWifi() {
        scanned_MACs.clear();
        scanned_RSS.clear();
        //start_scan=true;
        start_scan = false;
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
//            unregisterReceiver(this);
//
//            for (ScanResult scanResult : scan_results) {
//                scanned_MACs.add(scanResult.BSSID);
//                scanned_RSS.add(scanResult.level);
//            }
//            start_scan = true;
//            //start_scan=false;
//        }
//    };


    public void locate_me(View v) {
//        System.out.println(sample_table_act);
//        System.out.println(sample_table_loc);
        List<Float> rss_values = new ArrayList<Float>(get_current_rss(chosen_macs));
        //List<Float> test_point= new ArrayList<Float>(Arrays.asList(-3.600000000000000000e+01f,-3.100000000000000000e+01f, -1.000000000000000000e+02f, -4.500000000000000000e+01f, -1.000000000000000000e+02f, -1.000000000000000000e+02f, -1.000000000000000000e+02f, -4.900000000000000000e+01f, -1.000000000000000000e+02f, -4.600000000000000000e+01f, -1.000000000000000000e+02f, -1.000000000000000000e+02f,-1f));
        float prediction=knn_loc.predict(rss_values);
        System.out.println(prediction);
        if(Math.abs(prediction-0.0)<0.1) {
            resetBg();
            CellA.setBackground(drawable_orange);
        }
        if(Math.abs(prediction-1.0)<0.1){
            resetBg();
            CellB.setBackground(drawable_orange);
        }

        if(Math.abs(prediction-2.0)<0.1){
            resetBg();
            CellC.setBackground(drawable_orange);
        }
        if(Math.abs(prediction-3.0)<0.1) {
            resetBg();
            CellD.setBackground(drawable_orange);
        }
    }
    private void resetBg(){
        CellA.setBackground(drawable_white);
        CellB.setBackground(drawable_white);
        CellC.setBackground(drawable_white);
        CellD.setBackground(drawable_white);
    }
}
