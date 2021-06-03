package com.example.app3;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;

import java.lang.Math;
import java.util.Random;


/** @Brief: Particle class for particle filtering
 *
 *  @Author: Yuan Fu (5215315)
 *
 */
public class Particle {
    // coordinate and orientation of the particle
    private double x,y,orient;
    // std of the Gaussion noise
    private double move_noise,orient_noise,resample_noise;
    private ShapeDrawable drawable=new ShapeDrawable(new OvalShape());
    private boolean collision;

    public Particle() {
        this.x=0;
        this.y=0;
        this.orient=0;
        this.move_noise=0;
        this.orient_noise=0;
        this.resample_noise=0;
    }
    public void set_attr(double new_x, double new_y, double new_orient) {
        // TODO : throw exception if the input attr is outside the layout

        if(new_orient<0 ||new_orient> 2*Math.PI) throw new  NumberFormatException("orient must be in [0,2*pi]");

        this.x=new_x;this.y=new_y;this.orient=new_orient;
        this.set_drawable();
    }
    public void set_noise(double new_move_noise, double new_orient_noise,double new_resample_noise) {
        this.move_noise=new_move_noise;
        this.orient_noise=new_orient_noise;
        this.resample_noise=new_resample_noise;
    }

    public void move(double distance, double orientation) {
        if(distance<0) throw new  NumberFormatException("distance cannot be negative");
        if(orientation<0 ||orientation> 2*Math.PI) throw new  NumberFormatException("orient must be in [0,2*pi]");
        double new_x, new_y;
        // set the distance and orientation to move and apply noise
        Random r=new Random(System.currentTimeMillis());
        // Random r_orient=new Random();
        distance = distance +move_noise*r.nextGaussian();
        orientation = orientation +orient_noise*r.nextGaussian();
        // move
        new_x=this.x+Math.cos(orientation)*distance;
        new_y=this.y+Math.sin(orientation)*distance;
        // set the new place to a new particle so that we can resample easily
        this.set_attr(new_x,new_y,orientation);
        this.set_noise(this.move_noise,this.orient_noise,this.resample_noise);
        this.set_drawable();
    }

    public double get_x() {
        return this.x;
    }
    public double get_y() {
        return this.y;
    }
    public double get_orient() {
        return this.orient;
    }
    public void reborn(Particle newp) {
        Random r=new Random(System.currentTimeMillis());
        double newx=newp.x+this.resample_noise*r.nextGaussian();
        double newy=newp.y+this.resample_noise*r.nextGaussian();
        this.set_attr(newx,newy,newp.orient);
    }

    private void set_drawable() {
        drawable.getPaint().setColor(Color.BLUE);
        int p_x= (int) (this.x*MainActivity.pixelPerMeter);
        int p_y= (int) (this.y*MainActivity.pixelPerMeter);
        drawable.setBounds(MainActivity.center_x-MainActivity.point_size+p_x, MainActivity.center_y-MainActivity.point_size-p_y,
                MainActivity.center_x+MainActivity.point_size+p_x, MainActivity.center_y+MainActivity.point_size-p_y);
    }
    public ShapeDrawable get_drawable() {
        return this.drawable;
    }
//    // clear dead particle to save CPU resource
//    // usage first set the object = null then call System.gc()
//    protected void finalize() throws Throwable{
//        super.finalize();
//        System.out.println(this.toString() + "particle dead and cleared");
//    }
}
