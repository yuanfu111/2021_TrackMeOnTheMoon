package com.example.app3;

/** First order butterworth filter
*
*   Author: Yuan Fu
*
* **/
public class Butterworth {
    private float[] a=new float[2];
    private float[] b=new float[2];
    private float[] x=new float[2];
    private float[] y=new float[2];
    private int order=1;
    public Butterworth() {
        for(int i=0;i<order+1;i++) {
            this.a[i]=0;
            this.b[i]=0;
            this.x[i]=0;
            this.y[i]=0;
        }
    }
    public void set_coefficient(float[] a, float[] b) {
        if(a.length!=order+1 || b.length!=order+1) {
            throw new  NumberFormatException("Input coefficient length is not 2");
        }
        for(int i=0;i<order+1;i++) {
            this.a[i] = a[i];
            this.b[i] = b[i];
        }
    }
    public float filter(float sample) {
        // read sample
        x[0]=sample;
        // butterworth
        y[0]=(x[0]*a[0]+x[1]*a[1]-y[1]*b[1])/b[0];
        // move the queue
        for(int i=order;i>0;i--) {
            x[i]=x[i-1];
            y[i]=y[i-1];
        }
        return y[0];
    }
}
