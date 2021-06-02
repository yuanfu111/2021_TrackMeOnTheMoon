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

import java.util.Timer;
import java.util.TimerTask;

import static android.util.Half.EPSILON;
import static java.lang.Math.sin;
import static java.lang.Math.cos;
import static java.lang.Math.sqrt;
public class MainActivity extends Activity implements SensorEventListener, OnClickListener {

    private Button button;
    private SensorManager msensorManager;
    private Sensor sensor_gyro,sensor_compass;
    public static final int TIME_CONSTANT = 30;
    // coefficient of weighted avg of gyro angle and compass angle
    public static final float FILTER_COEFFICIENT = 0.98f;
    private Timer fuseTimer = new Timer();
    // declarations for gyro processing
    private float[] gyro = new float[3];
    private float[] gyroMatrix = new float[9];
    private float[] rotationMatrix = new float[9];
    private float[] accMagOrientation = new float[3];
    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float timestamp;
    private boolean initState = true;

    // Orientation related declarations
    private float[] accel = new float[3];
    private float[] magnetOrientation = new float[3];
    private float[] gyroOrientation = new float[3];
    private float[] fusedOrientation = new float[3];
    private  Butterworth butterLowPass=new Butterworth();
    private  Butterworth butterHighPass=new Butterworth();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button=(Button)findViewById(R.id.button);
        button.setOnClickListener(this);
       // rsensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        if (rsensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null) {
//
//            rotation_vector = rsensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
//
//            rsensorManager.registerListener(this, rotation_vector,SensorManager.SENSOR_DELAY_NORMAL);
//        } else {
//
//        }
        msensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (msensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {

            sensor_gyro = msensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            msensorManager.registerListener(this, sensor_gyro,SensorManager.SENSOR_DELAY_FASTEST);
        }
        else {

        }

//        csensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (msensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {

            sensor_compass = msensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            msensorManager.registerListener(this, sensor_compass,SensorManager.SENSOR_DELAY_NORMAL);
        }
        else {

        }
        init_gyro_matrix();
        //first order low-pass filter fs=1000HZ fc=50HZ
        butterLowPass.set_coefficient(new float[]{0.1367f,0.1367f},new float[]{1f,-0.7365f});
        //first order low-pass filter fs=1000HZ fc=400HZ
        butterHighPass.set_coefficient(new float[]{0.2452f,-0.2452f},new float[]{1f,0.5095f});

        fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
                1000, TIME_CONSTANT);

    }
    private void init_gyro_matrix() {
        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;

        // initialise gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;
    }

    protected void onResume() {
        super.onResume();
        msensorManager.registerListener(this, sensor_gyro,SensorManager.SENSOR_DELAY_NORMAL);
        msensorManager.registerListener(this, sensor_compass,SensorManager.SENSOR_DELAY_NORMAL);

    }
    protected void onPause() {
        super.onPause();
        msensorManager.unregisterListener(this);
    }

    @Override
    public  void onClick(View v) {
        if(v.getId()==R.id.button){
            Toast.makeText(this,"Click",Toast.LENGTH_LONG).show();

        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // rotation_vector
   //     if(event.sensor==rotation_vector) {
//        float[] quat = new float[4];
//        rsensorManager.getQuaternionFromVector(quat, event.values);
//        Log.d("Success", "x:"+quat[0] +";y:"+quat[1]+";z:"+quat[2]+" fff"+quat[3]);
 //       }
        // compass
        switch(event.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                for(int i=0;i<3;i++) {
                    magnetOrientation[i]= butterLowPass.filter(event.values[i]);
                }
                //System.arraycopy(event.values, 0, magnetOrientation, 0, 3);
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroFunction(event);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                // copy new accelerometer data into accel array
                // then calculate new orientation
                for(int i=0;i<3;i++) {
                    accel[i]= butterLowPass.filter(event.values[i]);
                }
                //System.arraycopy(event.values, 0, accel, 0, 3);
                calculateAccMagOrientation();
                break;
        }

//        if(event.sensor==compass) {
//            float axisX = event.values[0];
//            float axisY = event.values[1];
//            float axisZ = event.values[2];
//            Log.d("Success", "x_raw:"+axisX+" x_filtered:"+butterLowPass.filter(axisX) );
//        }

    }




    // gyro related functions
    private void calculateAccMagOrientation() {
        if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnetOrientation)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }

    private void getRotationVectorFromGyro(float[] gyroValues,
                                           float[] deltaRotationVector,
                                           float timeFactor)
    {
        float[] normValues = new float[3];

        // Calculate the angular speed of the sample
        float omegaMagnitude =
                (float)Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector if it's big enough to get the axis
        if(omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }


    public void gyroFunction(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (accMagOrientation == null)
            return;

        // initialisation of the gyroscope based rotation matrix

        if(initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if(timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyro, 0, 3);
            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }

        // measurement done, save current time for next interval
        timestamp = event.timestamp;

        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);

        //apply high pass filter
        for(int i=0;i<3;i++) {
            gyroOrientation[i]=butterHighPass.filter(gyroOrientation[i]);
        }
    }
    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }
    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing.
    }
    class calculateFusedOrientationTask extends TimerTask {
        public void run() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
            fusedOrientation[0] =
                    FILTER_COEFFICIENT * gyroOrientation[0]
                            + oneMinusCoeff * accMagOrientation[0];

            fusedOrientation[1] =
                    FILTER_COEFFICIENT * gyroOrientation[1]
                            + oneMinusCoeff * accMagOrientation[1];

            fusedOrientation[2] =
                    FILTER_COEFFICIENT * gyroOrientation[2]
                            + oneMinusCoeff * accMagOrientation[2];

            // overwrite gyro matrix and orientation with fused orientation
            // to comensate gyro drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
        }
    }

}
