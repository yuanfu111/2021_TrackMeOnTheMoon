package com.example.app3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.view.View;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

public class MyDrawableView extends View
{
    private List<ShapeDrawable> virtual_lines;
    private List<ShapeDrawable> walls;
    public MyDrawableView(Context context)
    {
        super(context);
    }

    public void set_attr (List<ShapeDrawable> virtual_lines,List<ShapeDrawable> walls ) {
        this.virtual_lines=virtual_lines;
        this.walls=walls;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void onDraw(Canvas canvas)
    {
        //RectF oval = new RectF(MainActivity.x, MainActivity.y, MainActivity.x + width, MainActivity.y
         //       + height); // set bounds of rectangle
       // Paint p = new Paint(); // set some paint options
        //p.setColor(Color.BLUE);
        //canvas.drawOval(oval, p);
//        System.out.println("onDraw");
//        if(MainActivity.p_list.size()!=0)
//            for(Particle p : MainActivity.p_list) {
//                ShapeDrawable drawable=p.get_drawable();
//                drawable.draw(canvas);
//            }
        invalidate();
    }
}
