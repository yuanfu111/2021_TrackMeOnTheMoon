package com.example.app3;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.lang.Math;

public class MainActivity extends Activity implements SensorEventListener, OnClickListener {

    private Button activity;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private double aX=0, aY=0, aZ=0, mag=0;
    private String state; // Walking or idle
    private double walk_threshold=2; // Threshold for determining walking
    private ArrayList<Double> accData;
    private int sampleSize = 50;
    private TextView currentState;

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
        aX = event.values[0];
        aY = event.values[1];
        aZ = event.values[2];
        mag = Math.sqrt(aX*aX + aY*aY + aZ*aZ); // magnitude of acceleration
        if (accData.size()<sampleSize){
            accData.add(mag);
        }
        else{
            state = DetectWalk(accData);
            System.out.println("Current state: " + state);
            currentState.setText(state);
            accData.clear();
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
        // Find the maximum of autocorrelation results
        double max = results[0];
        for (int i=1; i<results.length; ++i) {
            if (results[i] > max) {
                max = results[i];
            }
        }
        if (max > walk_threshold) {
            state = "walking";
        }
        return state;
    }

    // Brute force autocorrelation
    public double[] autocorrelation(ArrayList<Double> accData){
        double[] results = new double[accData.size()];
        for (int i=0; i<accData.size(); ++i){
            results[i] = 0;
            for (int j=0; j<accData.size(); ++j){
                results[i] += accData.get(j) * accData.get((j+i)%accData.size());
            }
        }
        return results;
    }
}