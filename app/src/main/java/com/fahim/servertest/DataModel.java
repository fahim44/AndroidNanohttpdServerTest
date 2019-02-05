package com.fahim.servertest;

import java.util.ArrayList;
import java.util.List;

class DataModel {
    private int limit;
    private List<int[]> allData;


    private static final int N = 1000;
    List<Double> accX ;
    List<Double> accY ;
    List<Double> accZ ;
    List<Double> ppg1 ;


    DataModel(String limit){
        allData = new ArrayList<>();

        accX = new ArrayList<>();
        accY = new ArrayList<>();
        accZ = new ArrayList<>();
        ppg1 = new ArrayList<>();

        this.limit = Integer.parseInt(limit);
    }

    List<int[]> getAllData() {
        return allData;
    }

    boolean reachedlimit(){
        return allData.size() >= limit;
    }

    void resetAllData(){
        allData = new ArrayList<>();


    }

    void reseltdatas(){
        accX = new ArrayList<>();
        accY = new ArrayList<>();
        accZ = new ArrayList<>();
        ppg1 = new ArrayList<>();

        for(int[] a: allData){
            accX.add((double)a[1]);
            accY.add((double)a[2]);
            accZ.add((double)a[3]);
            ppg1.add((double)a[0]);
        }
    }

    void addData(String[] data){
        int[] tempArr = new int[data.length];

        for(int i=0;i<data.length;i++)
            tempArr[i] = Integer.parseInt(data[i]);

        accX.add((double)tempArr[1]);
        accY.add((double)tempArr[2]);
        accZ.add((double)tempArr[3]);
        ppg1.add((double)tempArr[0]);

        allData.add(tempArr);
    }

    double[] getppg1(){
        double[] target = new double[ppg1.size()];
        for (int i = 0; i < target.length; i++) {
            target[i] = ppg1.get(i);                // java 1.5+ style (outboxing)
        }
        return target;
    }

    double[] getaccX(){
        double[] target = new double[accX.size()];
        for (int i = 0; i < target.length; i++) {
            target[i] = accX.get(i);                // java 1.5+ style (outboxing)
        }
        return target;
    }

    double[] getaccY(){
        double[] target = new double[accY.size()];
        for (int i = 0; i < target.length; i++) {
            target[i] = accY.get(i);                // java 1.5+ style (outboxing)
        }
        return target;
    }

    double[] getaccZ(){
        double[] target = new double[accZ.size()];
        for (int i = 0; i < target.length; i++) {
            target[i] = accZ.get(i);                // java 1.5+ style (outboxing)
        }
        return target;
    }
}
