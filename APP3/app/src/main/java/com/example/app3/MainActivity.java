package com.example.app3;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.hardware.SensorEventListener;
public class MainActivity extends Activity implements SensorEventListener, OnClickListener {

    private Button button;
    private SensorManager sensorManager;
    private Sensor rotation_vector;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button=(Button)findViewById(R.id.button);
        button.setOnClickListener(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {

            rotation_vector = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            sensorManager.registerListener(this, rotation_vector,SensorManager.SENSOR_DELAY_NORMAL);
        } else {

        }
    }
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, rotation_vector,SensorManager.SENSOR_DELAY_NORMAL);

    }
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public  void onClick(View v) {
        if(v.getId()==R.id.button){
            Toast.makeText(this,"Click",Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float xValue = event.values[0];
        float yValue = event.values[1];
        float zValue = event.values[2];
        //Log.d("Success", "x:"+xValue +";y:"+yValue+";z:"+zValue);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing.
    }


}