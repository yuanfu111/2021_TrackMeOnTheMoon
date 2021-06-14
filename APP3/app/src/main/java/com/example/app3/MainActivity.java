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
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends Activity implements SensorEventListener, OnClickListener {

    private Button start, pause, resume, stop;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private double aX = 0, aY = 0, aZ = 0, mag = 0;
    private String state = "idle"; // Walking or idle
    private double walk_threshold = 0.5; // Threshold for determining walking; personal
    private List<Double> accData1 = new ArrayList<>(); // Former window of data
    private List<Double> accData2 = new ArrayList<>(); // Current window of data
    private int sampleSize = 30;
    private double step_length = 0.58;
    private int steps = 0;
    private double distance = 0;
    private TextView currentState, stepCount;
    private Clock clock = Clock.systemDefaultZone();
    private int sampling_rate = 20000; // 20 ms -> 50 Hz

    // Variables for testing
    private List<Double> test_results = new ArrayList<>();
    private int sampleCount = 0;
    private long startTime=0, currentTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start = (Button) findViewById(R.id.start);
        pause = (Button) findViewById(R.id.pause);
        resume = (Button) findViewById(R.id.resume);
        stop = (Button) findViewById(R.id.stop);
        start.setOnClickListener(this);
        pause.setOnClickListener(this);
        resume.setOnClickListener(this);
        stop.setOnClickListener(this);
        currentState = findViewById(R.id.currentState);
        stepCount = findViewById(R.id.steps);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, sampling_rate);
    }

    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start:
                Toast.makeText(this, "Start sensing...", Toast.LENGTH_LONG).show();
                if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                    accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    sensorManager.registerListener(this, accelerometer, sampling_rate);
                }
                steps = 0;
                distance = 0;
                accData1.clear();
                accData2.clear();
                test_results.clear();
                break;
            case R.id.pause:
                Toast.makeText(this, "Pause sensing...", Toast.LENGTH_LONG).show();
                sensorManager.unregisterListener(this);
                currentState.setText("Paused");
                break;
            case R.id.resume:
                Toast.makeText(this, "Resume sensing...", Toast.LENGTH_LONG).show();
                sensorManager.registerListener(this, accelerometer, sampling_rate);
                break;
            case R.id.stop:
                Toast.makeText(this, "Stop sensing...", Toast.LENGTH_LONG).show();
                sensorManager.unregisterListener(this);
                currentState.setText("Stopped");
//                test_results.add((double) steps);
//                test_results.add(distance);
//                saveArrayList(test_results, "Steps_Distance");
//                saveArrayList(test_results, "window time");
//                saveArrayList(test_results, "std_dev");
//                  saveArrayList(test_results, "max_index");
                saveArrayList(test_results, "max");
            default:
                break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        aX = event.values[0];
        aY = event.values[1];
        aZ = event.values[2]-9.8;
        mag = Math.sqrt(aX * aX + aY * aY + aZ * aZ); // magnitude of acceleration
//        test_results.add(mag);

        if (accData2.size()==0){
            startTime = clock.millis();
            currentTime = startTime;
        }else{
            currentTime = clock.millis();
        }

        // Store the first window in accData1
        if (accData1.size()<sampleSize) {
            accData1.add(mag);
            sampleCount++;
        }
        // Store the second window in accData2
        else if (accData2.size()<sampleSize){
            accData2.add(mag);
            sampleCount++;
        }
        if (accData1.size() == sampleSize && accData2.size() == sampleSize) {
//            test_results.add((double)(currentTime-startTime));
            state = DetectWalk(accData1, accData2);
            if (state == "walking") {
              steps += 1;
              distance += step_length;
            }
            currentState.setText(state);
            stepCount.setText("Steps: "+steps);
            // Copy accData2 to accData1
            for (int i=0; i<sampleSize; ++i) {
                accData1.set(i, accData2.get(i));
            }
            // Clear accData2 to store the next window of data
            accData2.clear();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing.
    }

    // Detect walking using autocorrelation
    public String DetectWalk(List<Double> accData1, List<Double> accData2) {
        String state = "idle";
        double mean1 = get_mean(accData1);
        double mean2 = get_mean(accData2);
        double std_dev1 = get_std_dev(accData1, mean1);
        double std_dev2 = get_std_dev(accData2, mean2);
//        test_results.add(std_dev2);
        if (std_dev2 < 0.1) {
            state = "idle";
            return state;
        }
        double[] results = autocorrelation(accData1, mean1, std_dev1, accData2, mean2, std_dev2);
        // Find the maximum of autocorrelation results
        double max = results[0];
        int index = 0;
        for (int i = 1; i < results.length; ++i) {
            if (results[i] > max) {
                max = results[i];
                index = i;
                test_results.add(max);
//                test_results.add((double) index);
            }
        }
        if (max > walk_threshold) {
            state = "walking";
        }
        return state;
    }

    public double[] autocorrelation(List<Double> accData1, double mean1, double std_dev1, List<Double> accData2, double mean2, double std_dev2)
    {
        double[] results = new double[sampleSize];
        for (int i=0; i<3; ++i){
            results[i] = 0; // i: lag
            for (int j=0; j<sampleSize; ++j){
                results[i] += (accData1.get(j) - mean1) * (accData2.get((j+i)%sampleSize) - mean2)/(sampleSize * std_dev1 * std_dev2);
            }
        }
        return results;
    }

    public double get_mean(List<Double> data) {
        double mean = 0;
        double sum = 0;
        for (int i = 0; i < data.size(); ++i) {
            sum += data.get(i);
        }
        mean = sum / data.size();
        return mean;
    }

    public double get_std_dev(List<Double> data, double mean) {
        double std_dev = 0;
        double sum_square = 0;
        for (int i = 0; i < data.size(); ++i) {
            sum_square += Math.pow(data.get(i) - mean, 2);
        }
        std_dev = Math.sqrt(sum_square / data.size());
        return std_dev;
    }


    private void saveArray(double[] results, String filename) {
        FileOutputStream out;
        BufferedWriter writer = null;
        try {
            out = openFileOutput(filename + clock.millis() + ".txt", Context.MODE_PRIVATE);
            writer = new BufferedWriter(new OutputStreamWriter(out));
            for (Double result : results) {
                writer.write(String.valueOf(result) + "\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveArrayList(List<Double> results, String filename) {
        FileOutputStream out;
        BufferedWriter writer = null;
        try {
            out = openFileOutput(filename + clock.millis() + ".txt", Context.MODE_PRIVATE);
            writer = new BufferedWriter(new OutputStreamWriter(out));
            for (Double result : results) {
                writer.write(String.valueOf(result)+ "\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}