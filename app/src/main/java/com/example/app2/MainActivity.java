package com.example.app2;
//import APP2.Bayesian_parellel;

import java.util.ArrayList;
import java.util.List;
import java.lang.String;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class MainActivity {

    private static List<String> macs = new ArrayList<String>();
    private static List<List<List<Float>>> mac_tables = new ArrayList<List<List<Float>>>();
    private static int numTables;
    public static void main(String args[]){
//        System.out.println("Hello");
        loadMacs();
        numTables=macs.size();
        loadMacsTables();
        //Bayesian_parellel b=new Bayesian_parellel();
        //System.out.println(b.name);
        // int i=0;
        // for(List<List<Float>> mac_table:mac_tables){
        //     System.out.println(i++);
        //     //System.out.println(mac_table);
        // }
    }
    private static void loadMacsTables(){
        String[] pathes=new String[numTables];
        for(int i=0;i<numTables;i++)
        {
            pathes[i]="./data/table_mac"+i+".txt";
        }
        String line;
        try {
            for(int i=0;i<numTables;i++){
                InputStreamReader reader = new InputStreamReader(new FileInputStream(pathes[i]));
                BufferedReader br = new BufferedReader(reader);
                List<List<Float>> mac_table=new ArrayList<>();
                while ((line = br.readLine()) != null) {
                    //System.out.println(line);
                    String[] line_split=line.split("\\s+");
                    List<Float> sample_split=new ArrayList<>();
                    //get rid of the first element which is ""
                    for(int j=1;j<line_split.length;j++){
                        sample_split.add(Float.parseFloat(line_split[j]));
                    }
                    mac_table.add(sample_split);
                }
                mac_tables.add(mac_table);
                br.close();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void loadMacs(){
        String path="./data/macs.txt";
        try {
            File file=new File(path);
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
            BufferedReader br = new BufferedReader(reader);
            String line = "";
            line = br.readLine();
            while (line != null) {
                //System.out.println(line);
                macs.add(line);
                line = br.readLine();
            }
            br.close();
        }catch(Exception e){
            e.printStackTrace();
        }

    }
}
