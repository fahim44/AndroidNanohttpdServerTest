package com.fahim.servertest.Calculation;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import com.fahim.servertest.CalculationCallback;

public class Calculation {

    public Context context;

    double[] p1,p2,ax,ay,az;
    double heart_rate = 0;
    PpgMainClass ppgMainClass;

    private Handler mHandler;

    int countDebug = 0;

    int accSensorCount = 0;
    int ppgSensorCount = 0;
    private static final int N = 1000;
    double[] accX;
    double[] accY;
    double[] accZ;
    double[] ppg1;
    double[] ppg2;

    long initTimeStmpAcc = 0;
    long initTimeStmpPpg = 0;

    int flag = 0;


    int testFlag = 0;

    public Calculation(Context context, Handler handler, CalculationCallback callback, int v){
        this.context = context;
        this.mHandler = handler;

        heart_rate = 0;

        this.ppgMainClass = new PpgMainClass(context,callback,v);
        ppgMainClass.setHandler(mHandler);
    }


    public void init(){
        ppgSensorCount = 0;
        accSensorCount = 0;
        flag = 0;
    }

    public void calculate(double[] ppg1, double[] accX, double[] accY, double[] accZ){
        ppg1 = DSP.normalize(ppg1);
        double[] ppg2 = ppg1;

        ppgMainClass.setData(ppg1,ppg2,accX,accY,accZ);

        // check valid data
        if(!ppgMainClass.checkIfData(ppg1)){
            Toast.makeText(context, " No valid data " , Toast.LENGTH_LONG).show();
            init();
            // mSensorManager.registerListener(MainActivity.this, accSensor , SensorManager.SENSOR_DELAY_FASTEST);
            // mSensorManager.registerListener(MainActivity.this, hrmSensor , SensorManager.SENSOR_DELAY_FASTEST);
            return;
        }

        new Thread( ppgMainClass ).start();
    }
}
