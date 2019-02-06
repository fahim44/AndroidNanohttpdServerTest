package com.fahim.servertest;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.fahim.servertest.Calculation.Calculation;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.IOException;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

public class Repository implements WebServerCallback, CalculationCallback {

    private MutableLiveData<String> ipAddress;

    private MutableLiveData<LineGraphSeries> graph1Series;
    private MutableLiveData<LineGraphSeries> graph2Series;

    private MutableLiveData<String> heartbeat1;
    private MutableLiveData<String> heartbeat2;


    private WebServer server;

    private DataModel dataModel1,dataModel2;

    private Context context;

    private Handler handler;

    private static final Object lockObject = new Object();

    Repository(Context context){
        this.context = context;

        handler = new Handler(context.getMainLooper());
        server = new WebServer(Integer.parseInt(context.getString(R.string.port)),this,handler);
        try {
            server.start();
        } catch(IOException ioe) {
            getIpAddress().setValue("The server could not start.");
        }
    }

    void init(){
        getIpAddress().setValue(TaskUtils.getDeviceIpAddress(context));

        dataModel1 = new DataModel(context.getString(R.string.limit));
        dataModel2  = new DataModel(context.getString(R.string.limit));
    }

    void onDestroy(){
        if(server != null)
            server.stop();
    }

    MutableLiveData<String> getIpAddress() {
        if(ipAddress == null){
            ipAddress = new MutableLiveData<>();
            ipAddress.setValue("");
        }
        return ipAddress;
    }

    @NonNull
    MutableLiveData<LineGraphSeries> getGraph1Series(){
        if(graph1Series == null){
            graph1Series = new MutableLiveData<>();
            graph1Series.setValue(new LineGraphSeries());
        }
        return graph1Series;
    }

    @NonNull
    MutableLiveData<LineGraphSeries> getGraph2Series(){
        if(graph2Series == null){
            graph2Series = new MutableLiveData<>();
            graph2Series.setValue(new LineGraphSeries());
        }
        return graph2Series;
    }

    MutableLiveData<String> getHeartbeat1() {
        if(heartbeat1 == null){
            heartbeat1 = new MutableLiveData<>();
            heartbeat1.setValue("");
        }
        return heartbeat1;
    }

    MutableLiveData<String> getHeartbeat2() {
        if(heartbeat2 == null){
            heartbeat2 = new MutableLiveData<>();
            heartbeat2.setValue("");
        }
        return heartbeat2;
    }

    @Override
    public void onResponse(String from, long time, String[] values) {
        synchronized (lockObject) {
            if (from.equalsIgnoreCase(context.getString(R.string.firstGraphKey)))
                handle1stGraphData(time, values);
            else if (from.equalsIgnoreCase(context.getString(R.string.secondGraphKey)))
                handle2ndGraphData(time, values);
        }

    }

    private void handle1stGraphData(long time, String[] values){
        getGraph1Series().getValue().appendData(new DataPoint(time,Integer.parseInt(values[0])),true,100000);
        getGraph1Series().setValue(getGraph1Series().getValue());

        dataModel1.addData(values);
        if(dataModel1.reachedlimit()){
            new Calculation(context,handler,this,1).calculate(dataModel1.getppg1(),dataModel1.getaccX(),dataModel1.getaccY(),dataModel1.getaccZ());
            dataModel1.resetAllData();
        }

    }

    private void handle2ndGraphData(long time, String[] values){
        getGraph2Series().getValue().appendData(new DataPoint(time,Integer.parseInt(values[0])),true,100000);
        getGraph2Series().setValue(getGraph2Series().getValue());

        dataModel2.addData(values);
        if(dataModel2.reachedlimit()){
           // getHeartbeat2().setValue(String.valueOf(TaskUtils.calculation(dataModel2.getAllData())));
            new Calculation(context,handler,this,2).calculate(dataModel2.getppg1(),dataModel2.getaccX(),dataModel2.getaccY(),dataModel2.getaccZ());
            dataModel2.resetAllData();
        }
    }

    @Override
    public void onCompleteCalculation(int type, double result) {
        if(type==1){
            dataModel1.reseltdatas();
            getHeartbeat1().setValue(Double.toString(result));
        }
        else if(type==2){
            dataModel2.reseltdatas();
            getHeartbeat2().setValue(Double.toString(result));
        }
    }
}
