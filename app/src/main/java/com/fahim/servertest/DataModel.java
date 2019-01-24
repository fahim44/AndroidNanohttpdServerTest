package com.fahim.servertest;

import java.util.ArrayList;
import java.util.List;

class DataModel {
    private int limit;
    private List<int[]> allData;

    DataModel(String limit){
        allData = new ArrayList<>();
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

    void addData(String[] data){
        int[] tempArr = new int[data.length];

        for(int i=0;i<data.length;i++)
            tempArr[i] = Integer.parseInt(data[i]);

        allData.add(tempArr);
    }
}
