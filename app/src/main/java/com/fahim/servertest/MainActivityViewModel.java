package com.fahim.servertest;

import android.app.Application;
import android.os.Handler;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class MainActivityViewModel extends AndroidViewModel implements WebServerCallback {

    private MutableLiveData<String> ipAddress;

    private MutableLiveData<LineGraphSeries> graph1Series;
    private MutableLiveData<LineGraphSeries> graph2Series;

    private MutableLiveData<String> heartbeat1;
    private MutableLiveData<String> heartbeat2;


    private WebServer server;

    private DataModel dataModel1,dataModel2;

    public MainActivityViewModel(@NonNull Application application) {
        super(application);

        getHeartbeat1();
        getHeartbeat2();

        Handler handler = new Handler(getApplication().getMainLooper());
        server = new WebServer(Integer.parseInt(getApplication().getString(R.string.port)),this,handler);
        try {
            server.start();
        } catch(IOException ioe) {
            getIpAddress().setValue("The server could not start.");
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if(server != null)
            server.stop();
    }

    void init(){
        getIpAddress().setValue(TaskUtils.getDeviceIpAddress(getApplication()));



        dataModel1 = new DataModel(getApplication().getString(R.string.limit));
        dataModel2  = new DataModel(getApplication().getString(R.string.limit));
    }

    public MutableLiveData<String> getIpAddress() {
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

    public MutableLiveData<String> getHeartbeat1() {
        if(heartbeat1 == null){
            heartbeat1 = new MutableLiveData<>();
            heartbeat1.setValue("");
        }
        return heartbeat1;
    }

    public MutableLiveData<String> getHeartbeat2() {
        if(heartbeat2 == null){
            heartbeat2 = new MutableLiveData<>();
            heartbeat2.setValue("");
        }
        return heartbeat2;
    }

    @Override
    public void onResponse(String from, long time, String[] values) {
        if(from.equalsIgnoreCase(getApplication().getString(R.string.firstGraphKey)))
            handle1stGraphdata(time,values);
        else if(from.equalsIgnoreCase(getApplication().getString(R.string.secondGraphKey)))
            handle2ndGraphdata(time,values);

    }

    private void handle1stGraphdata(long time,String[] values){
        getGraph1Series().getValue().appendData(new DataPoint(time,Integer.parseInt(values[0])),true,100000);
        getGraph1Series().setValue(getGraph1Series().getValue());

        dataModel1.addData(values);
        if(dataModel1.reachedlimit()){
            getHeartbeat1().setValue(Integer.toString(TaskUtils.calculation(dataModel1.getAllData())));
            dataModel1.resetAllData();
        }

    }



    private void handle2ndGraphdata(long time,String[] values){
        getGraph2Series().getValue().appendData(new DataPoint(time,Integer.parseInt(values[0])),true,100000);
        getGraph2Series().setValue(getGraph2Series().getValue());

        dataModel2.addData(values);
        if(dataModel2.reachedlimit()){
            getHeartbeat2().setValue(String.valueOf(TaskUtils.calculation(dataModel2.getAllData())));
            dataModel2.resetAllData();
        }
    }

}
