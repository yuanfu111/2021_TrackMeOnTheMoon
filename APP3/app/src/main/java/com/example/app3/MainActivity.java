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
    int display_width;
    int display_height;
    int point_size=10;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button=(Button)findViewById(R.id.button);
        button.setOnClickListener(this);
        move_drawable=(Button)findViewById(R.id.move_drawable);
        move_drawable.setOnClickListener(this);

        azimuthText = (TextView) findViewById(R.id.textView1);
        textView2=(TextView) findViewById(R.id.textView2);
        // init sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        registerSensorManagerListeners();
        fuseSensor.setMode(FuseOrientation.Mode.FUSION);

        // get screen dimensions
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        display_width = size.x;
        display_height = size.y;

        // create a drawable object
        drawable = new ShapeDrawable(new OvalShape());
        drawable.getPaint().setColor(Color.BLUE);


        // create a canvas
        ImageView canvasView = (ImageView) findViewById(R.id.canvas);
        Bitmap blankBitmap = Bitmap.createBitmap(display_width,display_height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(blankBitmap);
        canvasView.setImageBitmap(blankBitmap);

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
        double distance=10;

        switch (v.getId()) {
            case R.id.button: {
                drawable.draw(canvas);
                // in the middle of the screen
                drawable.setBounds(display_width/2-point_size, display_height/2-point_size, display_width/2+point_size, display_height/2+point_size);
                //azimuthText.setText(d.format(azimuthValue));
                //Toast.makeText(this,"Click",Toast.LENGTH_LONG).show();
                break;
            }
            case R.id.move_drawable: {
                move_point_draw(distance,azimuthValue);
            }
        }
        // make the canvas all white and redraw
        canvas.drawColor(Color.WHITE);
        drawable.draw(canvas);
    }

    /** @Brief: Move the drawable point on canvas based on distance and orientation
     *  @Author: Yuan Fu (5215315)
     *  @Return: None
     */
    private void move_point_draw(double distance, double orient) {
        int dist_x = (int) (Math.sin((orient/360)*2*Math.PI)*distance);
        int dist_y = (int) (Math.cos((orient/360)*2*Math.PI)*distance);
        textView2.setText( "x: "+dist_x +" y: "+dist_y +" orient:"+ d.format(orient));
        Rect r = drawable.getBounds();
        drawable.setBounds(r.left + dist_x, r.top-dist_y, r.right + dist_x, r.bottom-dist_y);
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
        x_range=0;y_range=0;
        move_noise=0;orient_noise=0;resample_noise=0;
        num_particle=100;
    }

    /** @Brief: Create particles randomly in a restricted range
     *  @Author: Yuan Fu (5215315)
     *  @Return: None
     */
    private void creat_particles() {
        //TODO: x,y restriction
        Random r=new Random(System.currentTimeMillis());
        double x,y,orient;
        for(int i=0;i<num_particle;i++) {
            x=x_range*r.nextDouble();
            y=y_range*r.nextDouble();;
            orient=2*Math.PI*r.nextDouble();
            Particle p=new Particle();
            p.set_attr(x,y,orient);
            p.set_noise(move_noise,orient_noise,resample_noise);
            p_list.add(p);
        }
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
            if(p_list.get(i).detect_collision()==true) {
                dead_indeces.add(i);
            }
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
