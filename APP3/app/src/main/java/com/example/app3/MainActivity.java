package com.example.app3;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


public class MainActivity extends Activity implements SensorEventListener, OnClickListener {
    // UI related declarations
    private Button button,move_drawable;
    private TextView azimuthText,textView2;
    // Sensor related declarations
    private SensorManager sensorManager = null;
    private FuseOrientation fuseSensor= new FuseOrientation();
    // Orientation
    private double azimuthValue;
    private DecimalFormat d = new DecimalFormat("#.###");
    // Particle filter related declarations
    private int num_particle;
    List<Particle> p_list=new ArrayList<>();
    private double x_range,y_range;
    private double move_noise,orient_noise,resample_noise;

    // Map related declarations
    private ShapeDrawable drawable;
    private Canvas canvas;
    private List<ShapeDrawable> walls;

    public static int display_width;
    public static int display_height;
    public static int point_size=10;
    public static int pixelPerMeter=100;
    public static int center_x;
    public static int center_y;

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
        p =  create_particle();
        p.get_drawable().draw(canvas);
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
    /** @Brief: Create particles randomly in a restricted range
     *  @Author: Yuan Fu (5215315)
     *  @Return: None
     */
    private Particle create_particle() {
        //TODO: x,y restriction
        Random r=new Random(System.currentTimeMillis());
        double x,y,orient;
        // for(int i=0;i<num_particle;i++) {
        x=x_range*(r.nextDouble()-0.5);
        y=y_range*(r.nextDouble()-0.5);;
        orient=2*Math.PI*r.nextDouble();
        Particle p=new Particle();
        p.set_attr(x,y,orient);
        p.set_noise(move_noise,orient_noise,resample_noise);


        return p;
        //}
    }
    @Override
    public  void onClick(View v) {
        double distance=0.1;
        switch (v.getId()) {
            case R.id.button: {
                p.get_drawable().draw(canvas);
                // in the middle of the screen
                //p_draw.setBounds(display_width/2-point_size, display_height/2-point_size, display_width/2+point_size, display_height/2+point_size);
                //azimuthText.setText(d.format(azimuthValue));
                //Toast.makeText(this,"Click",Toast.LENGTH_LONG).show();
                break;
            }
            case R.id.move_drawable: {
                p.move(distance,(azimuthValue/360)*2*Math.PI);
            }
        }

        if(detect_collision(p))
        System.out.println("hit wall");
        // make the canvas all white and redraw
        canvas.drawColor(Color.WHITE);
        p.get_drawable().draw(canvas);
        draw_walls();
    }
    private void draw_walls() {
        for(ShapeDrawable wall : walls)
            wall.draw(canvas);
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
    private void init_walls() {
        int line_width=8;


        walls=new ArrayList<>();
        //horizontal lines
        ShapeDrawable d1 = new ShapeDrawable(new RectShape());
        d1.setBounds(center_x-pixelPerMeter*10,center_y-line_width/2-(int)(pixelPerMeter*3.5),
                center_x+pixelPerMeter*10,center_y+line_width/2-(int)(pixelPerMeter*3.5));

        ShapeDrawable d2 = new ShapeDrawable(new RectShape());
        d2.setBounds(center_x-pixelPerMeter*10,center_y-line_width/2-(int)(pixelPerMeter*2.5),
                center_x+(int)(pixelPerMeter*2.5),center_y+line_width/2-(int)(pixelPerMeter*2.5));

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
        d6.setBounds(center_x+(int)(pixelPerMeter*2),center_y-line_width/2+(int)(pixelPerMeter*3),
                center_x+(int)(pixelPerMeter*6.5),center_y+line_width/2+(int)(pixelPerMeter*3));

        //vetical lines
        ShapeDrawable d7 = new ShapeDrawable(new RectShape());
        d7.setBounds(center_x-line_width/2-(int)(pixelPerMeter*1.5),center_y-(int)(pixelPerMeter*2.5),
                center_x+line_width/2-(int)(pixelPerMeter*1.5),center_y+(int)(pixelPerMeter*3.5));

        ShapeDrawable d8 = new ShapeDrawable(new RectShape());
        d8.setBounds(center_x-line_width/2+(int)(pixelPerMeter*1.5),center_y-(int)(pixelPerMeter*2.5),
                center_x+line_width/2+(int)(pixelPerMeter*1.5),center_y+(int)(pixelPerMeter*0));

        ShapeDrawable d9 = new ShapeDrawable(new RectShape());
        d9.setBounds(center_x-line_width/2+(int)(pixelPerMeter*1.5),center_y+(int)(pixelPerMeter*0.5),
                center_x+line_width/2+(int)(pixelPerMeter*1.5),center_y+(int)(pixelPerMeter*3));

        ShapeDrawable d10 = new ShapeDrawable(new RectShape());
        d10.setBounds(center_x-line_width/2+(int)(pixelPerMeter*4.5),center_y-(int)(pixelPerMeter*2.5),
                center_x+line_width/2+(int)(pixelPerMeter*4.5),center_y+(int)(pixelPerMeter*2.5));

        ShapeDrawable d11 = new ShapeDrawable(new RectShape());
        d10.setBounds(center_x-line_width/2+(int)(pixelPerMeter*6.5),center_y-(int)(pixelPerMeter*2.5),
                center_x+line_width/2+(int)(pixelPerMeter*6.5),center_y+(int)(pixelPerMeter*3));

        ShapeDrawable d12 = new ShapeDrawable(new RectShape());
        d12.setBounds(center_x-line_width/2-(int)(pixelPerMeter*10),center_y-(int)(pixelPerMeter*3.5),
                center_x+line_width/2-(int)(pixelPerMeter*10),center_y-(int)(pixelPerMeter*2.5));

        ShapeDrawable d13 = new ShapeDrawable(new RectShape());
        d13.setBounds(center_x-line_width/2+(int)(pixelPerMeter*10),center_y-(int)(pixelPerMeter*3.5),
                center_x+line_width/2+(int)(pixelPerMeter*10),center_y-(int)(pixelPerMeter*2.5));
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
    }
    //TODO: collision detection
    public boolean detect_collision(Particle p) {
        ShapeDrawable p_drawable = new ShapeDrawable(new OvalShape());
        //p_drawable.getPaint().setColor(Color.BLUE);
        for(ShapeDrawable wall : walls) {
            if(isCollision(wall,p.get_drawable()))

                return true;
        }
        return false;
    }
    private boolean isCollision(ShapeDrawable first, ShapeDrawable second) {
        Rect firstRect = new Rect(first.getBounds());
        return firstRect.intersect(second.getBounds());
    }
//    /** @Brief: Move the drawable point on canvas based on distance and orientation
//     *  @Author: Yuan Fu (5215315)
//     *  @Return: None
//     */
//    private void move_point_draw(double distance, double orient) {
//        int dist_x = (int) (Math.sin((orient/360)*2*Math.PI)*distance);
//        int dist_y = (int) (Math.cos((orient/360)*2*Math.PI)*distance);
//        textView2.setText( "x: "+dist_x +" y: "+dist_y +" orient:"+ d.format(orient));
//        Rect r = drawable.getBounds();
//        drawable.setBounds(r.left + dist_x, r.top-dist_y, r.right + dist_x, r.bottom-dist_y);
//    }
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
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    // Init some global values
    private void config_init() {
        x_range=20;y_range=7; // in meters
        move_noise=0;orient_noise=0;resample_noise=0;
        num_particle=100;
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        display_width = size.x;
        display_height = size.y;
        center_x=display_width/2;
        center_y=display_height/2;
        ImageView canvasView = (ImageView) findViewById(R.id.canvas);
        Bitmap blankBitmap = Bitmap.createBitmap(display_width, display_height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(blankBitmap);
        canvasView.setImageBitmap(blankBitmap);
        init_walls();
        draw_walls();
    }


    /** @Brief: Resample the current particle list. Identify dead particles and reborn them from random alive particles
     *  @Author: Yuan Fu (5215315)
     *  @Return: None
     */
    private void resample() {
        // TODO: Test it. Maybe try array instead of list to decrease searching time
        // identify dead particles
        List<Integer> dead_indeces=new ArrayList<>();
        for(int i=0;i<num_particle;i++) {
           // if(detect_collision(p_list.get(i))==true) {
           //     dead_indeces.add(i);
           // }
        }
        // reborn the dead particles new alive particles
        Random r=new Random(System.currentTimeMillis());
        int random_index;
        random_index=r.nextInt(num_particle);
        // list store the index of the particle around which dead will be reborn
        List<Integer> reborn_around=new ArrayList<>();
        for(int i=0;i<dead_indeces.size();i++) {
            // continue the generate random number
            // until the number does not match dead particle index
            while(dead_indeces.contains(random_index)) {
                random_index=r.nextInt(num_particle);
            }
            reborn_around.add(random_index);
        }
        // reborn
        if(dead_indeces.size()!=reborn_around.size()) {
            System.out.println("number of dead and reborn do not match");
        }
        for(int i=0;i<dead_indeces.size();i++) {
            p_list.get(dead_indeces.get(i)).reborn(p_list.get(reborn_around.get(i)));
        }
    }

}
