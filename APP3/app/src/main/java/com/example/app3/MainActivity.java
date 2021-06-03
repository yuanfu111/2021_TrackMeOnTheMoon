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
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity implements SensorEventListener, OnClickListener {

    private Button activity;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float aX=0, aY=0, aZ=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activity = (Button)findViewById(R.id.activity);
        activity.setOnClickListener(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
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
            Toast.makeText(this,"Click",Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        aX = event.values[0];
        aY = event.values[1];
        aZ = event.values[2];

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing.
    }

    // Detect walking using autocorrelation
    public String DetectWalk(){
        String prediction = null;
        // call autocorrelation
        return prediction;
    }

    // Brute force autocorrelation
    public double[] autocorrelation(ArrayList<Float> accData){
        double[] autocorrelation = new double[accData.size()];
        for (int i=0; i<accData.size(); ++i){
            autocorrelation[i] = 0;
            for (int j=0; j<accData.size(); ++j){
                autocorrelation[i] += accData.get(j) * accData.get((j+i)%accData.size());
            }
        }
        return autocorrelation;
    }

}