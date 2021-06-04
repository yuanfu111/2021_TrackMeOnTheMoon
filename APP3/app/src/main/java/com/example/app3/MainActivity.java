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

    private Button start, pause, resume, stop;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private double aX=0, aY=0, aZ=0, mag=0;
    private String state = "idle"; // Walking or idle
    private double walk_threshold = 15; // Threshold for determining walking; personal
    private ArrayList<Double> accData = new ArrayList<>();
//    private int sampleSize = 50;
    private int sampleCount = 0;
    private int window = 1000; // 1000ms
    private long startTime=0, currentTime = 0;
    private double walkingTime;
    private double distance;
    private double speed = 1.5; // Yujin's walking speed is 1.5m/s
    private TextView currentState;
    private Clock clock = Clock.systemDefaultZone();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start = (Button)findViewById(R.id.start);
        pause = (Button)findViewById(R.id.pause);
        resume = (Button)findViewById(R.id.resume);
        stop = (Button)findViewById(R.id.stop);
        start.setOnClickListener(this);
        pause.setOnClickListener(this);
        resume.setOnClickListener(this);
        stop.setOnClickListener(this);
        currentState = findViewById(R.id.currentState);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
//
//            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

//            sensorManager.registerListener(this, accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
//        }

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
        switch (v.getId()){
            case R.id.start:
                Toast.makeText(this,"Start sensing...",Toast.LENGTH_LONG).show();
                if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                    accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    sensorManager.registerListener(this, accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
                }
                distance = 0;
                walkingTime = 0;
                state = "idle";
                sampleCount = 0;
                accData.clear();
                break;
            case R.id.pause:
                Toast.makeText(this,"Pause sensing...",Toast.LENGTH_LONG).show();
                sensorManager.unregisterListener(this);
                currentState.setText("Paused");
                break;
            case R.id.resume:
                Toast.makeText(this,"Resume sensing...",Toast.LENGTH_LONG).show();
                sensorManager.registerListener(this, accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
                break;
            case R.id.stop:
                Toast.makeText(this,"Stop sensing...",Toast.LENGTH_LONG).show();
                sensorManager.unregisterListener(this);
                currentState.setText("Stopped");
                distance = walkingTime * speed;
//                System.out.println(distance);
                double[] results = {walkingTime, distance};
                save2file(results);
            default:
                break;
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
            if (state == "walking") {
                walkingTime += (currentTime - startTime)/1000.0; // walking during the last sampling window
                double d = (currentTime - startTime)/1000.0 * speed; // window内移动的距离
//                System.out.println(walkingTime);
            }
//            System.out.println("Current state: " + state);
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
//                System.out.println("Max correlation: " + max);
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
            results[i] = 0; // i: lag
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