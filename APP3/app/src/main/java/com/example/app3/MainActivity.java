package com.example.app3;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;


@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends Activity implements SensorEventListener, OnClickListener {
    // UI related declarations
    private Button button, move_drawable;
    private TextView azimuthText, textView2;
    // Sensor related declarations
    private SensorManager sensorManager = null;
    private FuseOrientation fuseSensor = new FuseOrientation();
    // Orientation related declarations
    private double azimuthValue;
    private DecimalFormat d = new DecimalFormat("#.###");

    // Distance related declarations
    private double aX=0, aY=0, aZ=0, mag=0;
    private String state = "idle"; // Walking or idle
    private double walk_threshold = 15; // Threshold for determining walking; personal
    private ArrayList<Double> accData = new ArrayList<>();
    private int sampleCount = 0;
    private int window = 1000; // 1000ms
    private long startTime=0, currentTime = 0;
    private double walkingTime;
    private double distance;
    private double speed = 1.5; // Yujin's walking speed is 1.5m/s
    //private TextView currentState;
    private Clock clock = Clock.systemDefaultZone();

    // Particle filter related declarations
    private int num_particle;
    List<Particle> p_list = new ArrayList<>();
    private double x_range, y_range;


    // Map related declarations
    private ShapeDrawable drawable;
    private Canvas canvas;
    private List<ShapeDrawable> virtual_lines;
    public static List<ShapeDrawable> walls;
    public static int display_width;
    public static int display_height;
    public static int center_x;
    public static int center_y;
    public static int point_size = 10;
    public static int pixelPerMeter = 100;
    public static double move_noise;
    public static double orient_noise;
    public static double resample_noise=1;

    //testing
    private Particle p;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(this);
        move_drawable = (Button) findViewById(R.id.move_drawable);
        move_drawable.setOnClickListener(this);

        azimuthText = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        // init sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        registerSensorManagerListeners();
        fuseSensor.setMode(FuseOrientation.Mode.FUSION);

        config_init();
        // create a drawable object
        drawable = new ShapeDrawable(new OvalShape());
        drawable.getPaint().setColor(Color.BLUE);

        //testing
        canvas.drawColor(Color.WHITE);
        draw_layout();

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

    // Init some global values
    private void config_init() {
        x_range = 20;
        y_range = 7; // in meters
        num_particle = 2;
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
        draw_layout();
    }

    /**
     * @Brief: Create particles randomly in a restricted range
     * @Author: Yuan Fu (5215315)
     * @Return: None
     */
    private Particle create_particle() {
        SecureRandom r = new SecureRandom();
        //Random r = new Random(System.currentTimeMillis());
        double x, y, orient;
        orient = 2 * Math.PI * r.nextDouble();
        Particle p = new Particle();
        // repeat recreating until within room and not collision
        do  {
            x = x_range * (r.nextDouble() - 0.5);
            y = y_range * (r.nextDouble() - 0.5);
            p.set_attr(x, y, orient);
        }while(!check_in_room(x, y) || p.collision);

        p.set_noise(move_noise, orient_noise, resample_noise);
//        System.out.println("particle created");
//        System.out.println("x: " +x+" y: "+y);
        return p;
    }

    /** @Brief: Check if the coordinate is within the rooms of the layout
     *  @Author: Yuan Fu (5215315)
     *  @Param: x,y
     *  @Return: false if out of range
     */
    public static boolean check_in_room(double x, double y) {
        if(x<-10 || x>10 || y<-3.5 || y>3.5)
            return false;
        // the bottom left part
        if(x<-1.5 && 2.5>y) {
            return false;
        }
        // the bottom middle part
        if(2<x && -2.5>y) {
            return false;
        }
        // the bottom right part
        if(6.5<x  && 2.5>y) {
            return false;
        }

        return true;
    }
    private void draw_particles(List<Particle> list) {
        for(Particle p : list) {
            p.get_drawable().draw(canvas);
        }
    }
    private void init_particles() {
        p_list.clear();
        for(int i =0;i<200;i++) {
            Particle new_p =create_particle();
            p_list.add(new_p);
        }

    }
    @Override
    public  void onClick(View v) {
        double distance=0.1;
        switch (v.getId()) {
            case R.id.button: {
                init_particles();
                draw_particles(p_list);
                // in the middle of the screen
                //p_draw.setBounds(display_width/2-point_size, display_height/2-point_size, display_width/2+point_size, display_height/2+point_size);

                //Toast.makeText(this,"Click",Toast.LENGTH_LONG).show();
                break;
            }
            case R.id.move_drawable: {
                for(Particle p : p_list) {
                    p.move(distance,(azimuthValue/360)*2*Math.PI);
                }
                resample();
            }
        }
       // textView2.setText("X: "+ d.format(p.get_x()) +"\nY: " +d.format(p.get_y()) + "\nOrient: " + d.format(azimuthValue));

//        if(p.collision)
//            Toast.makeText(this,"Hit wall",Toast.LENGTH_LONG).show();
        // make the canvas all white and redraw
        canvas.drawColor(Color.WHITE);
        draw_particles(p_list);
        draw_layout();
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
            System.out.println("dead "+dead_indeces);
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
                System.out.println("reborn " + reborn_around);
                // reborn
                if (dead_indeces.size() != reborn_around.size()) {
                    System.out.println("number of dead and reborn do not match");
                }

                for (int i = 0; i < dead_indeces.size(); i++) {
                    p_list.get(dead_indeces.get(i)).reborn(p_list.get(reborn_around.get(i)));
                }
        }
    }
    private void draw_layout() {
        for(ShapeDrawable wall : walls)
            wall.draw(canvas);
        for(ShapeDrawable virtual_line : virtual_lines) {
            virtual_line.draw(canvas);
        }
    }
    /** @Brief: Initialize walls based on our layout 1m=100 pixel
     *  @Author: Yuan Fu (5215315)
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
        int line_width=8;
        walls=new ArrayList<>();
        //horizontal lines
        ShapeDrawable d1 = new ShapeDrawable(new RectShape());
        d1.setBounds(center_x-pixelPerMeter*10,center_y-line_width/2-(int)(pixelPerMeter*3.5),
                center_x+pixelPerMeter*10,center_y+line_width/2-(int)(pixelPerMeter*3.5));

        // door related
        ShapeDrawable d2 = new ShapeDrawable(new RectShape());
        d2.setBounds(center_x-pixelPerMeter*10,center_y-line_width/2-(int)(pixelPerMeter*2.5),
                center_x+(int)(pixelPerMeter*2),center_y+line_width/2-(int)(pixelPerMeter*2.5));
        // door related
        ShapeDrawable d3 = new ShapeDrawable(new RectShape());
        d3.setBounds(center_x+pixelPerMeter*3,center_y-line_width/2-(int)(pixelPerMeter*2.5),
                center_x+(int)(pixelPerMeter*10),center_y+line_width/2-(int)(pixelPerMeter*2.5));

        ShapeDrawable d4 = new ShapeDrawable(new RectShape());
        d4.setBounds(center_x-(int)(pixelPerMeter*1.5),center_y-line_width/2+(int)(pixelPerMeter*0.5),
                center_x+(int)(pixelPerMeter*1.5),center_y+line_width/2+(int)(pixelPerMeter*0.5));

        ShapeDrawable d5 = new ShapeDrawable(new RectShape());
        d5.setBounds(center_x-(int)(pixelPerMeter*1.5),center_y-line_width/2+(int)(pixelPerMeter*3.5),
                center_x+(int)(pixelPerMeter*1.5),center_y+line_width/2+(int)(pixelPerMeter*3.5));

        ShapeDrawable d6 = new ShapeDrawable(new RectShape());
        d6.setBounds(center_x+(int)(pixelPerMeter*2),center_y-line_width/2+(int)(pixelPerMeter*2.5),
                center_x+(int)(pixelPerMeter*6.5),center_y+line_width/2+(int)(pixelPerMeter*2.5));

        //vetical lines
        ShapeDrawable d7 = new ShapeDrawable(new RectShape());
        d7.setBounds(center_x-line_width/2-(int)(pixelPerMeter*1.5),center_y-(int)(pixelPerMeter*2.5),
                center_x+line_width/2-(int)(pixelPerMeter*1.5),center_y+(int)(pixelPerMeter*3.5));

        // door related
        ShapeDrawable d8 = new ShapeDrawable(new RectShape());
        d8.setBounds(center_x-line_width/2+(int)(pixelPerMeter*1.5),center_y-(int)(pixelPerMeter*2.5),
                center_x+line_width/2+(int)(pixelPerMeter*1.5),center_y-(int)(pixelPerMeter*1.5));

        // door related
        ShapeDrawable d9 = new ShapeDrawable(new RectShape());
        d9.setBounds(center_x-line_width/2+(int)(pixelPerMeter*1.5),center_y-(int)(pixelPerMeter*0.5),
                center_x+line_width/2+(int)(pixelPerMeter*1.5),center_y+(int)(pixelPerMeter*2));

        // door related
        ShapeDrawable d10 = new ShapeDrawable(new RectShape());
        d10.setBounds(center_x-line_width/2+(int)(pixelPerMeter*1.5),center_y+(int)(pixelPerMeter*3),
                center_x+line_width/2+(int)(pixelPerMeter*1.5),center_y+(int)(pixelPerMeter*3.5));


        ShapeDrawable d11 = new ShapeDrawable(new RectShape());
        d11.setBounds(center_x-line_width/2+(int)(pixelPerMeter*4.5),center_y-(int)(pixelPerMeter*2.5),
                center_x+line_width/2+(int)(pixelPerMeter*4.5),center_y+(int)(pixelPerMeter*1.5));

        ShapeDrawable d12 = new ShapeDrawable(new RectShape());
        d12.setBounds(center_x-line_width/2+(int)(pixelPerMeter*6.5),center_y-(int)(pixelPerMeter*2.5),
                center_x+line_width/2+(int)(pixelPerMeter*6.5),center_y+(int)(pixelPerMeter*2.5));

        ShapeDrawable d13 = new ShapeDrawable(new RectShape());
        d13.setBounds(center_x-line_width/2-(int)(pixelPerMeter*10),center_y-(int)(pixelPerMeter*3.5),
                center_x+line_width/2-(int)(pixelPerMeter*10),center_y-(int)(pixelPerMeter*2.5));

        ShapeDrawable d14 = new ShapeDrawable(new RectShape());
        d14.setBounds(center_x-line_width/2+(int)(pixelPerMeter*10),center_y-(int)(pixelPerMeter*3.5),
                center_x+line_width/2+(int)(pixelPerMeter*10),center_y-(int)(pixelPerMeter*2.5));

        // additional bottom two lines
        // horizontal
        ShapeDrawable d15 = new ShapeDrawable(new RectShape());
        d15.setBounds(center_x+(int)(pixelPerMeter*1.5),center_y-line_width/2+(int)(pixelPerMeter*3.5),
                center_x+(int)(pixelPerMeter*2),center_y+line_width/2+(int)(pixelPerMeter*3.5));
        //vetical
        ShapeDrawable d16 = new ShapeDrawable(new RectShape());
        d16.setBounds(center_x-line_width/2+(int)(pixelPerMeter*2),center_y+(int)(pixelPerMeter*2.5),
                center_x+line_width/2+(int)(pixelPerMeter*2),center_y+(int)(pixelPerMeter*3.5));
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
        walls.add(d15);
        walls.add(d16);
        // virtual lines
        virtual_lines=new ArrayList<>();
        ShapeDrawable l1 = new ShapeDrawable(new RectShape());
        l1.setBounds(center_x-line_width/4-(int)(pixelPerMeter*5),center_y-(int)(pixelPerMeter*3.5),
                center_x+line_width/4-(int)(pixelPerMeter*5),center_y-(int)(pixelPerMeter*2.5));
        l1.getPaint().setColor(Color.GRAY);

        ShapeDrawable l2 = new ShapeDrawable(new RectShape());
        l2.setBounds(center_x-line_width/4-(int)(pixelPerMeter*0),center_y-(int)(pixelPerMeter*3.5),
                center_x+line_width/4-(int)(pixelPerMeter*0),center_y-(int)(pixelPerMeter*2.5));
        l2.getPaint().setColor(Color.GRAY);

        ShapeDrawable l3 = new ShapeDrawable(new RectShape());
        l3.setBounds(center_x-line_width/4+(int)(pixelPerMeter*5),center_y-(int)(pixelPerMeter*3.5),
                center_x+line_width/4+(int)(pixelPerMeter*5),center_y-(int)(pixelPerMeter*2.5));
        l3.getPaint().setColor(Color.GRAY);

        ShapeDrawable l4 = new ShapeDrawable(new RectShape());
        l4.setBounds(center_x+(int)(pixelPerMeter*1.5),center_y-line_width/4-(int)(pixelPerMeter*0),
                center_x+(int)(pixelPerMeter*4.5),center_y+line_width/4-(int)(pixelPerMeter*0));
        l4.getPaint().setColor(Color.GRAY);

        virtual_lines.add(l1);
        virtual_lines.add(l2);
        virtual_lines.add(l3);
        virtual_lines.add(l4);
    }
    private String DetectWalk(ArrayList<Double> accData){
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
    private double[] autocorrelation(ArrayList<Double> accData){
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
    private void get_distance(SensorEvent event) {
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
                distance = (currentTime - startTime)/1000.0 * speed; // window内移动的距离
//                System.out.println(walkingTime);
            }
            else {
                distance=0;
            }
//            System.out.println("Current state: " + state);
            accData.clear();
//            System.out.println("Samples in 1s: " + sampleCount);
            sampleCount = 0;
        }
    }
    /** @Brief: Listen to 3 sensors: ACC、 Gyro、 Compass
     *  @Author: Yuan Fu (5215315)
     *  @Return: None
     */
    public void onSensorChanged(SensorEvent event) {
        //TODO: distance related
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                fuseSensor.setAccel(event.values);
                fuseSensor.calculateAccMagOrientation();
                get_distance(event);
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
    /** @Brief: Up date the (fused)sensor value
     *  @Author: Yuan Fu (5215315)
     *  @Return: None
     */
    public void updateValue() {
        //TODO: distance related
        azimuthValue = (fuseSensor.getAzimuth()+360)%360;
        azimuthText.setText(d.format(azimuthValue));
        textView2.setText(d.format(distance));
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }


}
