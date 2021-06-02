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
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import static android.util.Half.EPSILON;
import static java.lang.Math.sin;
import static java.lang.Math.cos;
import static java.lang.Math.sqrt;
public class MainActivity extends Activity implements SensorEventListener, OnClickListener {

    private Button button;
    private SensorManager sensorManager = null;
    private FuseOrientation fuseSensor= new FuseOrientation();

    private double azimuthValue;
    private TextView azimuthText;
    private DecimalFormat d = new DecimalFormat("#.###");
    // filters
    private  Butterworth butterLowPass=new Butterworth();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button=(Button)findViewById(R.id.button);
        button.setOnClickListener(this);
        azimuthText = (TextView) findViewById(R.id.textView1);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        registerSensorManagerListeners();
        //first order low-pass filter fs=1000HZ fc=50HZ
        butterLowPass.set_coefficient(new float[]{0.1367f,0.1367f},new float[]{1f,-0.7365f});


        fuseSensor.setMode(FuseOrientation.Mode.ACC_MAG);

    }
    public void registerSensorManagerListeners() {
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
    protected void onResume() {
        super.onResume();
        registerSensorManagerListeners();

    }
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public  void onClick(View v) {
        if(v.getId()==R.id.button){
            azimuthText.setText(d.format(azimuthValue));
            Toast.makeText(this,"Click",Toast.LENGTH_LONG).show();
        }

    }
    public void onSensorChanged(SensorEvent event) {
            // rotation_vector
            //     if(event.sensor==rotation_vector) {
//        float[] quat = new float[4];
//        rsensorManager.getQuaternionFromVector(quat, event.values);
//        Log.d("Success", "x:"+quat[0] +";y:"+quat[1]+";z:"+quat[2]+" fff"+quat[3]);
            //       }
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                fuseSensor.setAccel(event.values);
                fuseSensor.calculateAccMagOrientation();
                break;

            case Sensor.TYPE_GYROSCOPE:
                fuseSensor.gyroFunction(event);
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                fuseSensor.setMagnet(event.values);
                break;
        }
        updateOrientationDisplay();
    }
    public void updateOrientationDisplay() {

        azimuthValue = fuseSensor.getAzimuth();

        //azimuthText.setText(d.format(azimuthValue));
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
