package com.example.app3;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;

import java.lang.Math;
import java.security.SecureRandom;
import java.util.Random;

import static com.example.app3.MainActivity.check_in_room;


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
    public boolean collision;

    public Particle() {
        this.x=0;
        this.y=0;
        this.orient=0;
        this.move_noise=MainActivity.move_noise;
        this.orient_noise=MainActivity.orient_noise;
        this.resample_noise=MainActivity.resample_noise;
        this.collision=false;
    }
    public void set_attr(double new_x, double new_y, double new_orient) {
        // TODO : throw exception if the input attr is outside the layout
       //if(new_x<-10 || new_x>10 || new_y<3.5 ||new_y>3.5) throw new  NumberFormatException("particle x y out of range");
        if(new_orient<0 ||new_orient> 2*Math.PI) throw new  NumberFormatException("orient must be in [0,2*pi]");

        this.x=new_x;this.y=new_y;this.orient=new_orient;
        this.set_drawable();
        this.detect_collision();
    }
    public void set_noise(double new_move_noise, double new_orient_noise,double new_resample_noise) {
        this.move_noise=new_move_noise;
        this.orient_noise=new_orient_noise;
        this.resample_noise=new_resample_noise;
    }
    /** @Brief: Move the particle and reset the corresponding drawable
     *
     */
    public void move(double distance, double orientation) {
        if(distance<0) throw new  NumberFormatException("distance cannot be negative");
        if(orientation<0 ||orientation> 2*Math.PI) throw new  NumberFormatException("orient must be in [0,2*pi]");
        double new_x, new_y;
        // set the distance and orientation to move and apply noise
        SecureRandom r = new SecureRandom();
        // Random r_orient=new Random();
        distance = distance +move_noise*r.nextGaussian();
        orientation = orientation +orient_noise*r.nextGaussian();
        // move
        new_x=this.x+Math.cos(orientation)*distance;
        new_y=this.y+Math.sin(orientation)*distance;
        // set the new place to a new particle so that we can resample easily
        this.set_attr(new_x,new_y,orientation);
        //this.set_noise(this.move_noise,this.orient_noise,this.resample_noise);
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
    /** @Brief: reborn the particle around an alive particle
     *  @Param: particle to reborn around
     */
    public void reborn(Particle newp) {
        this.collision=false;
        SecureRandom r = new SecureRandom();
        double newx;
        double newy;
        do  {
            newx = newp.x+this.resample_noise*r.nextGaussian();
            newy = newp.y+this.resample_noise*r.nextGaussian();
            this.set_attr(newx, newy, this.orient);
        }while(!check_in_room(newx, newy) || this.collision);
        this.set_attr(newx, newy, this.orient);
        //this.set_attr(newx,newy,newp.orient);
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
    /** @Brief: Detect if a p particle collide with any walls
     */
    private void detect_collision() {
        this.collision=false;
        for(ShapeDrawable wall : MainActivity.walls) {
            Rect wallRect = new Rect(wall.getBounds());
            if(wallRect.intersect(this.get_drawable().getBounds()))
                this.collision=true;
        }
    }
}
