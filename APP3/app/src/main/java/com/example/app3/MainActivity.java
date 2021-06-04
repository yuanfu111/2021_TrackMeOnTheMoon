package com.example.app3;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.Clock;
import java.util.ArrayList;
import java.lang.Math;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends Activity implements SensorEventListener, OnClickListener {

    private Button activity;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private double aX=0, aY=0, aZ=0, mag=0;
    private String state; // Walking or idle
    private double walk_threshold=100; // Threshold for determining walking
    private ArrayList<Double> accData = new ArrayList<>();
//    private int sampleSize = 50;
    private int sampleCount = 0;
    private int window = 1000; // 1000ms
    private long startTime=0, currentTime = 0;
    private TextView currentState;
    private Clock clock = Clock.systemDefaultZone();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activity = (Button)findViewById(R.id.activity);
        activity.setOnClickListener(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        currentState = findViewById(R.id.currentState);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {

            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            sensorManager.registerListener(this, accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
        }

    }

    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer,SensorManager.SENSOR_DELAY_NORMAL);

    }
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public  void onClick(View v) {
        if(v.getId()==R.id.activity){
            Toast.makeText(this,"Start sensing...",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
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
        if (currentTime - startTime < window){
            accData.add(mag);
        }
        else{
            state = DetectWalk(accData);
            System.out.println("Current state: " + state);
            currentState.setText(state);
            accData.clear();
//            System.out.println("Samples in 1s: " + sampleCount);
            sampleCount = 0;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing.
    }

    // Detect walking using autocorrelation
    public String DetectWalk(ArrayList<Double> accData){
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
                System.out.println("Max correlation: " + max);
            }
        }
        if (max > walk_threshold) {
            state = "walking";
        }
        return state;
    }

    // Brute force autocorrelation
    public double[] autocorrelation(ArrayList<Double> accData){
        double sum = 0;
        for (int i=0; i<accData.size(); ++i) {
            sum += accData.get(i);
        }
        double avg = sum/accData.size();
        double[] results = new double[accData.size()];
        for (int i=0; i<accData.size(); ++i){
            results[i] = 0; // i: offset
            for (int j=0; j<accData.size(); ++j){
                results[i] += (accData.get(j) - avg) * (accData.get((j+i)%accData.size()) - avg);
            }
        }
//        save2file(results);
        return results;
    }

    private void save2file(double[] results){
        FileOutputStream out ;
        BufferedWriter writer = null;
        try{
            out = openFileOutput("Results" + clock.millis() + ".txt", Context.MODE_PRIVATE);
            writer = new BufferedWriter(new OutputStreamWriter(out));
            for(Double result : results){
                writer.write(String.valueOf(result)+"\n");
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