package com.fahim.servertest;

import android.app.Application;
import com.jjoe64.graphview.series.LineGraphSeries;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class MainActivityViewModel extends AndroidViewModel {

    private Repository repository;

    public MainActivityViewModel(@NonNull Application application) {
        super(application);

        repository = new Repository(getApplication());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.onDestroy();
    }

    void init(){
        repository.init();
    }

    public MutableLiveData<String> getIpAddress() {
        return repository.getIpAddress();
    }

    @NonNull
    MutableLiveData<LineGraphSeries> getGraph1Series(){
       return repository.getGraph1Series();
    }

    @NonNull
    MutableLiveData<LineGraphSeries> getGraph2Series(){
        return repository.getGraph2Series();
    }

    public MutableLiveData<String> getHeartbeat1() {
        return repository.getHeartbeat1();
    }

    public MutableLiveData<String> getHeartbeat2() {
        return repository.getHeartbeat2();
    }
}
