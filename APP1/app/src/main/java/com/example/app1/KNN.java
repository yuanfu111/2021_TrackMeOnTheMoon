package com.example.app1;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public final class KNN {
    private int k;
    private Float[] labels;//unique label values
    private int feature_num;
    private List<List<Float>> sample =new ArrayList<>();
    public KNN(int k,Float[] labels,List<List<Float>> sample){
        this.k=k;
        this.labels=labels;
        this.sample=sample;
        this.feature_num=sample.get(0).size()-1;
    }

    private float Euc_distance(List<Float> x1,List<Float> x2){
        Double result=0d;
        if(x1.size()!=x2.size()){
            throw new ArithmeticException("sample size is not equal"+ "x1: " +x1.size()+" x2: " +x2.size());
        }
        //calculate the distance (the last column is the label)
       for(int i=0;i<x1.size()-1;i++){
           result+=Math.pow(x1.get(i)-x2.get(i),2);
       }
        result=Math.sqrt(result);
        Float result_conversion = result.floatValue();
        return result_conversion;
    }
    //returns the sorted sample based the distance to the input data
    public float predict(List<Float> data) {
        List<List<Float>> sample_sorted = new ArrayList<>(sample);
        Collections.sort(sample_sorted, new Comparator<List<Float>>() {
            @Override
            public int compare(List<Float> o1, List<Float> o2) {
                float distance1 = Euc_distance(o1,data);
                float distance2 = Euc_distance(o2,data);
                return (distance1 < distance2) ? -1 : ((distance1 == distance2) ? 0 : 1);
            }
        });

        int[] label_frequency=new int[labels.length];

        for(int i=0;i<this.k;i++){
            for(int j=0;j<labels.length;j++){
                //System.out.println("label: "+labels[j]+"sample: "+sample_sorted.get(i).get(feature_num));
                if(Math.abs(labels[j]-sample_sorted.get(i).get(feature_num))<0.1){
                    label_frequency[j]++;
                }
            }

        }
        //find the max_index
        int max_index=0;
        for(int i=1;i<label_frequency.length;i++){
            if(label_frequency[i-1]<label_frequency[i]){
                max_index=i;
            }
        }
        return labels[max_index];
    }


}
