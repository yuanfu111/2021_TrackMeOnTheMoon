package com.example.app3;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;


@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends Activity implements SensorEventListener, OnClickListener {
    // UI related declarations
    private Button init, move_drawable,pause;
    private TextView azimuthText, textView2;
    private boolean is_pase;
    // Sensor related declarations
    private SensorManager sensorManager = null;
    private FuseOrientation fuseSensor = new FuseOrientation();
    // Orientation related declarations
    private double azimuthValue;
    private DecimalFormat d = new DecimalFormat("#.###");
    private double angle_start, angle_end, delta_angle; // angle within a sampling window
    // Distance related declarations
    private double aX=0, aY=0, aZ=0, mag=0;
    private String state = "idle"; // Walking or idle
    private double walk_threshold = 0.5;
    private List<Double> accData1 = new ArrayList<>(); // Former window of data
    private List<Double> accData2 = new ArrayList<>(); // Current window of data
    private int sampleSize = 35; // 35 samples can capture one step
    private double step_length = 0.5;
    private int steps = 0;
    private double distance = 0; // Total distance
    private double delta_d = 0; // Change in distance
    private int sampling_rate = 20000; // 20 ms -> 50 Hz
    private boolean measure_dist_done;
    //private TextView currentState;
    private Clock clock = Clock.systemDefaultZone();
    // Particle filter related declarations
    List<Particle> p_list = new ArrayList<>();
    private double x_range, y_range;
    // Map related declarations
    private ShapeDrawable drawable;
    private Canvas canvas;
    private List<ShapeDrawable> virtual_lines;
    public static List<ShapeDrawable> walls;
    myView v;
    // private int redraw_interval=1000;
    // some global variables
    private int offset=-80;
    public static int display_width;
    public static int display_height;
    public static int center_x;
    public static int center_y;
    public static int point_size = 5;
    public static int pixelPerMeter = 85;
    public static double move_noise=0.03;
    public static double orient_noise=10;
    public static double resample_noise=0.1;
    private int num_particle=1000;
    private String current_cell = null;
    private Butterworth butterworth_lowpass = new Butterworth();
    // Test variable
    private List<Double> mags = new ArrayList<>();
    private List<Double> accData = new ArrayList<>();
    private String dir="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init = (Button) findViewById(R.id.init);
        init.setOnClickListener(this);
//        move_drawable = (Button) findViewById(R.id.move_drawable);
//        move_drawable.setOnClickListener(this);
        pause = (Button) findViewById(R.id.Pause);
        pause.setOnClickListener(this);
//        azimuthText = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        // init sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        registerSensorManagerListeners();
        fuseSensor.setMode(FuseOrientation.Mode.FUSION);
        float[] a = {1, -0.5095f};
        float[] b = {0.2452f, 0.2452f};
        butterworth_lowpass.set_coefficient(b, a);
        config_init();
    }

    public void registerSensorManagerListeners() {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), sampling_rate);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), sampling_rate);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), sampling_rate);
    }

    protected void onResume() {
        super.onResume();
        registerSensorManagerListeners();
    }

    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    /**
     * @Brief: Initialize some global variables, create a customized view class and add it to the layout
     * @Author: Yuan Fu (5215315), modified by Yujin
     * @Return: None
     */
    private void config_init() {
        x_range = 21.8;
        y_range = 7; // in meters
        is_pase=false;
        //angleSum=0;
        //inputAngle=0;
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        display_width = size.x;
        display_height = size.y;
        center_x = display_width / 2;
        center_y = display_height / 2;
        ImageView canvasView = (ImageView) findViewById(R.id.canvas);
        Bitmap blankBitmap = Bitmap.createBitmap(display_width, display_height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(blankBitmap);
        canvasView.setImageBitmap(blankBitmap);
        init_layout();
        ConstraintLayout canvas_layout=(ConstraintLayout)findViewById(R.id.canvas_layout);
        ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.FILL_PARENT);
        v = new myView(this);
        v.setLayoutParams(lp);
        canvas_layout.addView(v);
    }

    /**
     * @Brief: Create particles randomly in a restricted range
     * @Author: Yuan Fu (5215315)
     * @Return: None
     */
    private Particle create_particle() {
        SecureRandom r = new SecureRandom();
        double x, y, orient;
        orient = 2 * Math.PI * r.nextDouble();
        Particle p = new Particle();
        // repeat recreating until within room and not collision
        do {
            x = x_range * (r.nextDouble() - 0.5);
            y = y_range * (r.nextDouble() - 0.5);
            p.set_attr(x, y, orient);
        }while(!check_in_room(x, y) || p.collision);
        return p;
    }

    /** @Brief: Check if the coordinate is within the rooms of the layout
     *  @Author: Yuan Fu (5215315), modified by Yujin
     *  @Param: x,y coordinates
     *  @Return: false if out of range
     */
    public static boolean check_in_room(double x, double y) {
        if(x<-10.9 || x>10.9 || y<-3.5 || y>3.5)
            return false;
        // the bottom left part
        if(x<2.8 && y<2.5) {
            return false;
        }
        // the bottom middle part
        if((2.8<x && x<5.49 && y<-3.5) || (x>5.49 && x<9.72 && y<-2.15)) {
            return false;
        }
        // the bottom right part
        if(9.72<x  && y<2.5) {
            return false;
        }
        return true;
    }

    private void init_particles() {
        p_list.clear();
        for(int i =0;i<num_particle;i++) {
            Particle new_p =create_particle();
            p_list.add(new_p);
        }
    }

    /** @Brief: Resample the current particle list. Identify dead particles and reborn them from random alive particles
     *  @Author: Yuan Fu (5215315)
     *  @Return: None
     */
    private void resample( ) {
        // TODO: Test it. Maybe try array instead of list to decrease searching time
        // identify dead particles
        List<Integer> dead_indeces=new ArrayList<>();
        for(int i=0;i<p_list.size();i++) {
            if(p_list.get(i).collision) {
                dead_indeces.add(i);
            }
        }
        // if all particles are dead, intialize particles
        if(dead_indeces.size()==p_list.size()) {
            init_particles();
        }
        else {
//            System.out.println("dead "+dead_indeces);
            // reborn the dead particles new alive particles
            SecureRandom r = new SecureRandom();
            int random_index;
            random_index=r.nextInt(p_list.size());
            // list store the index of the particle around which dead will be reborn
            List<Integer> reborn_around=new ArrayList<>();
            for(int i=0;i<dead_indeces.size();i++) {
                // continue the generate random number
                // until the number does not match dead particle index
                while(dead_indeces.contains(random_index)) {
                    random_index=r.nextInt(p_list.size());
                }
                reborn_around.add(random_index);
            }
            // reborn
            if (dead_indeces.size() != reborn_around.size()) {
                System.out.println("number of dead and reborn do not match");
            }

            for (int i = 0; i < dead_indeces.size(); i++) {
                p_list.get(dead_indeces.get(i)).reborn(p_list.get(reborn_around.get(i)));
            }
        }
    }

    private void draw_particle_on_map() {
        v.invalidate();
    }

    /** @Brief: Initialize walls based on our layout 1m=100 pixel
     *  @Author: Yuan Fu (5215315), modified by Yujin
     *  @Return: None
     *  -------------------------------------------
     *  -         -          -          -         -
     *  -------------------------  ----------------
     *  -                  -   -      -   -
     *  -                  -   -      -   -
     *  -                  -          -   -
     *  -                  -----      -   -
     *  -                  -   -      -   -
     *  -                  -      ---------
     *  -                  -----
     */
    private void init_layout() {
        int line_width=4;
        walls=new ArrayList<>();
        //horizontal lines
        ShapeDrawable d1 = new ShapeDrawable(new RectShape());
        d1.setBounds(center_x-(int)(pixelPerMeter*10.9),center_y-line_width/2-(int)(pixelPerMeter*3.5),
                center_x+(int)(pixelPerMeter*10.9),center_y+line_width/2-(int)(pixelPerMeter*3.5));

        // door related
        ShapeDrawable d2 = new ShapeDrawable(new RectShape());
        d2.setBounds(center_x-(int)(pixelPerMeter*10.9),center_y-line_width/2-(int)(pixelPerMeter*2.5),
                center_x+(int)(pixelPerMeter*4.54),center_y+line_width/2-(int)(pixelPerMeter*2.5));
        // door related
        ShapeDrawable d3 = new ShapeDrawable(new RectShape());
        d3.setBounds(center_x+(int)(pixelPerMeter*5.39),center_y-line_width/2-(int)(pixelPerMeter*2.5),
                center_x+(int)(pixelPerMeter*10.9),center_y+line_width/2-(int)(pixelPerMeter*2.5));

        ShapeDrawable d4 = new ShapeDrawable(new RectShape());
        d4.setBounds(center_x+(int)(pixelPerMeter*2.8),center_y-line_width/2+(int)(pixelPerMeter*0.5),
                center_x+(int)(pixelPerMeter*4.42),center_y+line_width/2+(int)(pixelPerMeter*0.5));

        ShapeDrawable d5 = new ShapeDrawable(new RectShape());
        d5.setBounds(center_x+(int)(pixelPerMeter*2.8),center_y-line_width/2+(int)(pixelPerMeter*3.5),
                center_x+(int)(pixelPerMeter*5.49),center_y+line_width/2+(int)(pixelPerMeter*3.5));

        ShapeDrawable d6 = new ShapeDrawable(new RectShape());
        d6.setBounds(center_x+(int)(pixelPerMeter*5.49),center_y-line_width/2+(int)(pixelPerMeter*2.15),
                center_x+(int)(pixelPerMeter*9.72),center_y+line_width/2+(int)(pixelPerMeter*2.15));

        //vertical lines
        ShapeDrawable d7 = new ShapeDrawable(new RectShape());
        d7.setBounds(center_x-line_width/2+(int)(pixelPerMeter*2.8),center_y-(int)(pixelPerMeter*2.5),
                center_x+line_width/2+(int)(pixelPerMeter*2.8),center_y+(int)(pixelPerMeter*3.5));

        // door related
        ShapeDrawable d8 = new ShapeDrawable(new RectShape());
        d8.setBounds(center_x-line_width/2+(int)(pixelPerMeter*4.42),center_y-(int)(pixelPerMeter*2.5),
                center_x+line_width/2+(int)(pixelPerMeter*4.42),center_y-(int)(pixelPerMeter*1.13));

        // door related
        ShapeDrawable d9 = new ShapeDrawable(new RectShape());
        d9.setBounds(center_x-line_width/2+(int)(pixelPerMeter*4.42),center_y-(int)(pixelPerMeter*0.3),
                center_x+line_width/2+(int)(pixelPerMeter*4.42),center_y+(int)(pixelPerMeter*2.38));

        // door related
        ShapeDrawable d10 = new ShapeDrawable(new RectShape());
        d10.setBounds(center_x-line_width/2+(int)(pixelPerMeter*4.42),center_y+(int)(pixelPerMeter*3.11),
                center_x+line_width/2+(int)(pixelPerMeter*4.42),center_y+(int)(pixelPerMeter*3.5));

        ShapeDrawable d11 = new ShapeDrawable(new RectShape());
        d11.setBounds(center_x-line_width/2+(int)(pixelPerMeter*7.49),center_y-(int)(pixelPerMeter*2.5),
                center_x+line_width/2+(int)(pixelPerMeter*7.49),center_y+(int)(pixelPerMeter*1.35));

        ShapeDrawable d12 = new ShapeDrawable(new RectShape());
        d12.setBounds(center_x-line_width/2+(int)(pixelPerMeter*9.72),center_y-(int)(pixelPerMeter*2.5),
                center_x+line_width/2+(int)(pixelPerMeter*9.72),center_y+(int)(pixelPerMeter*2.15));

        ShapeDrawable d13 = new ShapeDrawable(new RectShape());
        d13.setBounds(center_x-line_width/2-(int)(pixelPerMeter*10.9),center_y-(int)(pixelPerMeter*3.5),
                center_x+line_width/2-(int)(pixelPerMeter*10.9),center_y-(int)(pixelPerMeter*2.5));

        ShapeDrawable d14 = new ShapeDrawable(new RectShape());
        d14.setBounds(center_x-line_width/2+(int)(pixelPerMeter*10.9),center_y-(int)(pixelPerMeter*3.5),
                center_x+line_width/2+(int)(pixelPerMeter*10.9),center_y-(int)(pixelPerMeter*2.5));

        // Additional vertical lines
//        ShapeDrawable d15 = new ShapeDrawable(new RectShape());
//        d15.setBounds(center_x-line_width/2+(int)(pixelPerMeter*5.56),center_y-(int)(pixelPerMeter*2.5),
//                center_x+line_width/2+(int)(pixelPerMeter*5.56),center_y-(int)(pixelPerMeter*1.13));

        ShapeDrawable d16 = new ShapeDrawable(new RectShape());
        d16.setBounds(center_x-line_width/2+(int)(pixelPerMeter*5.49),center_y+(int)(pixelPerMeter*2.15),
                center_x+line_width/2+(int)(pixelPerMeter*5.49),center_y+(int)(pixelPerMeter*3.5));

        walls.add(d1);
        walls.add(d2);
        walls.add(d3);
        walls.add(d4);
        walls.add(d5);
        walls.add(d6);
        walls.add(d7);
        walls.add(d8);
        walls.add(d9);
        walls.add(d10);
        walls.add(d11);
        walls.add(d12);
        walls.add(d13);
        walls.add(d14);
//        walls.add(d15);
        walls.add(d16);

        // virtual lines
        virtual_lines=new ArrayList<>();
        ShapeDrawable l1 = new ShapeDrawable(new RectShape());
        l1.setBounds(center_x-line_width/4-(int)(pixelPerMeter*5.45),center_y-(int)(pixelPerMeter*3.5),
                center_x+line_width/4-(int)(pixelPerMeter*5.45),center_y-(int)(pixelPerMeter*2.5));
        l1.getPaint().setColor(Color.GRAY);

        ShapeDrawable l2 = new ShapeDrawable(new RectShape());
        l2.setBounds(center_x-line_width/4-(int)(pixelPerMeter*0),center_y-(int)(pixelPerMeter*3.5),
                center_x+line_width/4-(int)(pixelPerMeter*0),center_y-(int)(pixelPerMeter*2.5));
        l2.getPaint().setColor(Color.GRAY);

        ShapeDrawable l3 = new ShapeDrawable(new RectShape());
        l3.setBounds(center_x-line_width/4+(int)(pixelPerMeter*5.45),center_y-(int)(pixelPerMeter*3.5),
                center_x+line_width/4+(int)(pixelPerMeter*5.45),center_y-(int)(pixelPerMeter*2.5));
        l3.getPaint().setColor(Color.GRAY);

        ShapeDrawable l4 = new ShapeDrawable(new RectShape());
        l4.setBounds(center_x+(int)(pixelPerMeter*4.42),center_y-line_width/4-(int)(pixelPerMeter*0.15),
                center_x+(int)(pixelPerMeter*7.49),center_y+line_width/4-(int)(pixelPerMeter*0.15));
        l4.getPaint().setColor(Color.GRAY);

        virtual_lines.add(l1);
        virtual_lines.add(l2);
        virtual_lines.add(l3);
        virtual_lines.add(l4);
    }

    // Detect walking using autocorrelation
    // accData1: former window; accData2: current window
    public String DetectWalk(List<Double> accData1, List<Double> accData2) {
        String state = "idle";
        double mean1 = get_mean(accData1);
        double mean2 = get_mean(accData2);
        double std_dev1 = get_std_dev(accData1, mean1);
        double std_dev2 = get_std_dev(accData2, mean2);
        if (std_dev2 < 0.1) {
            state = "idle";
            return state;
        }
        double[] results = autocorrelation(accData1, mean1, std_dev1, accData2, mean2, std_dev2);
        // Find the maximum of autocorrelation results
        double max = results[0];
        for (int i = 1; i < results.length; ++i) {
            if (results[i] > max) {
                max = results[i];
            }
        }
        if (max > walk_threshold) {
            state = "walking";
        }
        return state;
    }

    // Calculate the normalized autocorrelation of two windows of data
    public double[] autocorrelation(List<Double> accData1, double mean1, double std_dev1, List<Double> accData2, double mean2, double std_dev2)
    {
        double[] results = new double[sampleSize];
        // If walking, the lag between two windows is between 0 and 10.
        // This value is obtained by measurement.
        for (int i=0; i<sampleSize; ++i){
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

    private void get_distance(SensorEvent event) {
        delta_d = 0;
        aX = event.values[0];
        aY = event.values[1];
        aZ = event.values[2]-9.81;
        mag = Math.sqrt(aX*aX + aY*aY + aZ*aZ); // magnitude of acceleration
        mags.add(mag);
        // Filter magnitude
//        mag = butterworth_lowpass.filter((float)mag);
        if (accData2.size()==0){
            measure_dist_done = false;
            angle_start = azimuthValue;
        }
        // Store the first window in accData1
        if (accData1.size()<sampleSize) {
            accData1.add(mag);
            accData.add(mag);
        }
        // Store the second window in accData2
        else if (accData2.size()<sampleSize){
            accData2.add(mag);
            accData.add(mag);
        }
        if (accData1.size() == sampleSize && accData2.size() == sampleSize) {
            angle_end = azimuthValue;
            delta_angle = Math.abs(angle_end - angle_start);
            // When angle_end = 355 and angle_start = 5, the actual angle should be 10.
            if (delta_angle > 180) {
                delta_angle = 360-delta_angle;
            }
            // When turning around, the distance shouldn't change.
            if (delta_angle>45) {
                delta_d = 0;
                state = "turning";
            }else{
                state = DetectWalk(accData1, accData2);
                if (state == "walking") {
                    steps += 1;
                    distance += step_length;
                    delta_d = step_length;
                }
            }
            // Copy accData2 to accData1
            for (int i=0; i<sampleSize; ++i) {
                accData1.set(i, accData2.get(i));
            }
            // Clear accData2 to store the next window of data
            accData2.clear();
            measure_dist_done = true;
        }
    }

    @Override
    public  void onClick(View v) {
        switch (v.getId()) {
            case R.id.init: {
                init_particles();
                draw_particle_on_map();
                state = "idle";
                steps = 0;
                distance = 0;
                accData1.clear();
                accData2.clear();
                current_cell = null;
                measure_dist_done=false;
                textView2.setText("State: "+state+ "\nDistance: "+d.format(distance)+ "\nSteps: "+ steps + "\nAvg angle: " + d.format(azimuthValue)+ "\nCurrent cell: "+ current_cell);
                Toast.makeText(this,"init",Toast.LENGTH_LONG).show();
                break;
            }
//            case R.id.move_drawable: {
//                for(Particle p : p_list) {
//                    p.move(0.1,azimuthValue);
//                }
//                resample();
//                break;
//            }
            case R.id.Pause: {
                if(is_pase) {
                    pause.setText("Pause");
                    is_pase=false;
                }else{
                    pause.setText("Resume");
                    is_pase=true;
                    saveArrayList(accData,"accData_unfiltered");
                    saveArrayList(mags, "mags_unfiltered");
                }
            }
        }
    }

    /** @Brief: Listen to 3 sensors: ACC、 Gyro、 Compass
     *  @Author: Yuan Fu (5215315)
     *  @Return: None
     */
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                fuseSensor.setAccel(event.values);
                fuseSensor.calculateAccMagOrientation();

                if (!is_pase) {
                    get_distance(event);
                }
                break;
            case Sensor.TYPE_GYROSCOPE:
                fuseSensor.gyroFunction(event);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                fuseSensor.setMagnet(event.values);
                break;
        }
        updateValue();
    }

    private double clamp_direction(double azimuth) {
        double inputAngle=0;
        dir="Up";
        if(45<=azimuth && azimuth<135) {
            inputAngle=90;
            dir="Right";
        }
        else if (135<=azimuth && azimuth<225){
            inputAngle=180;
            dir="Down";
        }
        else if (225<=azimuth && azimuth<315){
            inputAngle=270;
            dir="Left";
        }
        return inputAngle;
    }
    /** @Brief: Update the (fused) sensor value, move the particles, and redraw the
     *          map when distance sampling is done
     *  @Author: Yuan Fu (5215315), modified by Yujin
     *  @Return: None
     */
    public void updateValue() {
        azimuthValue = (fuseSensor.getAzimuth()+360+offset)%360;
//        azimuthText.setText("Angle: "+d.format(azimuthValue));

        if(measure_dist_done && p_list.size()!=0 && !is_pase) {
            double inputAngle=clamp_direction(azimuthValue);
            textView2.setText("State: "+state+ "\nDistance: "+d.format(distance) + "\nSteps: "+ steps + "\nAngle: " + d.format(azimuthValue) +" Dir: "+ dir+"\nCurrent cell: "+ current_cell);
            if (delta_d > 0) {
                for (Particle p : p_list) {
                    p.move((delta_d-0.28), azimuthValue);
                }
            }else{
                for (Particle p : p_list) {
                    p.move(0, inputAngle);
                }
            }
            resample();
            draw_particle_on_map();
            current_cell = check_converge();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    /** @Brief: Customized view class for drawing the map
     *  @Author: Yuan Fu (5215315)
     */
    public class myView extends View {
        public myView(Context context) {
            super(context);
        }
        @Override
        public void onDraw(final Canvas canvas) {
            super.onDraw(canvas);  //IMPORTANT to draw the background
            for(ShapeDrawable wall:walls) {
                wall.draw(canvas);
            }
            for(ShapeDrawable virtual_line:virtual_lines) {
                virtual_line.draw(canvas);
            }
            if(p_list.size()!=0) {
                for (Particle p : p_list) {
                    p.get_drawable().draw(canvas);
                }
            }
        }
    }

    // If particles in one cell > 80% of total particles, converge.
    // If converge, return the current cell; else, return null.
    public String check_converge() {
        // number of particles in each cell
        int[] counts = {0, 0, 0, 0, 0, 0, 0, 0, 0};
        int total = p_list.size();
        double percent = 0.7; // threshold for convergence
        int location = -1; // 0->cell A, 1->cell B, 2->cell C...
        String current_cell = null;

        for (Particle p: p_list) {
            // Obtain the x and y of a particle; check if it's in cell A, B, C...
            double x = p.get_x();
            double y = p.get_y();
            // if (x,y) is in the range of cell A
            if (x>2.8 && x<4.42 && y>-0.5 && y<2.5) {
                counts[0] += 1;
                continue;
            }
            // if (x,y) is in the range of cell B
            if (x>4.42 && x<7.49 && y>-0.15 && y<2.5) {
                counts[1] += 1;
                continue;
            }
            // if (x,y) is in the range of cell C
            if ((x>4.42 && x<5.49 && -3.5<y && y<-0.15) || (5.49<=x && x<7.49 && -2.15<y && y<= 0.15)) {
                counts[2] += 1;
                continue;
            }
            // if (x,y) is in the range of cell D
            if (7.49<x && x<9.72 && -2.15<y && y<2.5) {
                counts[3] += 1;
                continue;
            }
            // if (x,y) is in the range of cell E
            if (2.8<x && x<4.42 && -3.5<y && y<-0.5) {
                counts[4] += 1;
                continue;
            }
            // if (x,y) is in the range of cell F
            if (-10.9<x && x<-5.45 && 2.5<y && y<3.5) {
                counts[5] += 1;
                continue;
            }
            // if (x,y) is in the range of cell G
            if (-5.45<x && x<0 && 2.5<y && y<3.5) {
                counts[6] += 1;
                continue;
            }
            // if (x,y) is in the range of cell H
            if (0<x && x<5.45 && 2.5<y && y<3.5) {
                counts[7] += 1;
                continue;
            }
            // if (x,y) is in the range of cell I
            if (5.45<x && x<10.9 && 2.5<y && y<3.5) {
                counts[8] += 1;
                continue;
            }
        }

//        System.out.println("Total particles: "+total);
//        for (int i=0; i<9; ++i){
//            System.out.println("Cell " +i + ": " + counts[i]);
//        }
//        System.out.println("Threshold: " + percent*total);

        // Check convergence
        for (int i=0; i<counts.length; ++i) {
            if (counts[i] > (int) (percent*total)) {
                location = i;
                break;
            }
        }

        // Convert index to string
        switch (location) {
            case 0:
                current_cell = "A";
                break;
            case 1:
                current_cell = "B";
                break;
            case 2:
                current_cell = "C";
                break;
            case 3:
                current_cell = "D";
                break;
            case 4:
                current_cell = "E";
                break;
            case 5:
                current_cell = "F";
                break;
            case 6:
                current_cell = "G";
                break;
            case 7:
                current_cell = "H";
                break;
            case 8:
                current_cell = "I";
                break;
            default:
                current_cell = null;
        }
        return current_cell;
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

