package com.fahim.servertest;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;

import fi.iki.elonen.NanoHTTPD;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    private static final int PORT = 7777;

    private TextView tv_ip, tv_perameters;

    private GraphView graphView;

    private int[] value1_arr = new int[256];
    private long[] time_arr = new long[256];
    long start_time = 0;

    private int counter =0;

    private WebServer server;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_ip = findViewById(R.id.tv_ip);
        tv_perameters = findViewById(R.id.tv_perameters);
        graphView = findViewById(R.id.graph);

        //getIp();

        getDeviceIpAddress();

        server = new WebServer();
        try {
            server.start();
        } catch(IOException ioe) {
            tv_perameters.append("\n===================\n");
            tv_perameters.append("The server could not start.");
            tv_perameters.append("\n===================\n");
            Log.w(TAG, "The server could not start.");
        }
        tv_perameters.append("\n===================\n");
        tv_perameters.append("Web server initialized.");
        tv_perameters.append("\n===================\n");
        Log.w(TAG, "Web server initialized.");
    }


    // DON'T FORGET to stop the server
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (server != null)
            server.stop();
    }


    private void getDeviceIpAddress( ) {
        String deviceIpAddress ;

        try {
            for (Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces(); enumeration.hasMoreElements();) {
                NetworkInterface networkInterface = enumeration.nextElement();

                for (Enumeration<InetAddress> enumerationIpAddr = networkInterface.getInetAddresses(); enumerationIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumerationIpAddr.nextElement();

                    if (!inetAddress.isLoopbackAddress() && inetAddress.getAddress().length == 4)
                    {
                        deviceIpAddress = inetAddress.getHostAddress();

                        Log.w(TAG, "deviceIpAddress: " + deviceIpAddress);

                        tv_ip.append(deviceIpAddress+":"+PORT);
                    }
                }
            }
        } catch (SocketException e) {
            Log.w(TAG, "SocketException:" + e.getMessage());
        }

        //return deviceIpAddress;
    }

    private void drawGraph(){
        DataPoint[] dataPoints = new DataPoint[256];
        for(int i=0;i<256;i++){
            dataPoints[i] = new DataPoint(time_arr[i],value1_arr[i]);
        }
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoints);
        graphView.addSeries(series);

        server.stop();
        server = null;
    }

    private class WebServer extends NanoHTTPD {

        WebServer()
        {
            super(PORT);
        }

        @Override
        public Response serve(IHTTPSession session) {
            /*Map<String, String> files = new HashMap<>();
            Method method = session.getMethod();
            if (Method.PUT.equals(method) || Method.POST.equals(method)) {
                try {
                    session.parseBody(files);
                }catch (ResponseException | IOException ex) {
                    return newFixedLengthResponse( "" );
                }
            }*/
            String postBody = session.getQueryParameterString();
            postBody = postBody.replace("sensor_reading=","");
            String[] values = postBody.split(",");

            if(counter>=256)
                return newFixedLengthResponse( "" );
            if(counter==0)
                start_time = System.currentTimeMillis();
            value1_arr[counter] = Integer.parseInt(values[0]);
            time_arr[counter] = System.currentTimeMillis() - start_time;
            counter++;

            runOnUiThread(()->
                    tv_perameters.append("\n\n"+DateFormat.getDateTimeInstance().format(new Date())+" : "+values[0]));
            if(counter>=256)
                runOnUiThread(()->
                        drawGraph());

            /*String[] arr = postBody.split(",");*/
            Log.w(TAG, postBody);
            return newFixedLengthResponse( "" );
        }
    }
}