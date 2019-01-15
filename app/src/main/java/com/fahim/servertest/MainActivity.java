package com.fahim.servertest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    private static final int PORT = 7777;

    private TextView tv_ip, tv_perameters;

    private WebServer server;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_ip = findViewById(R.id.tv_ip);
        tv_perameters = findViewById(R.id.tv_perameters);

        //getIp();

        getDeviceIpAddress();

        server = new WebServer();
        try {
            server.start();
        } catch(IOException ioe) {
            Log.w(TAG, "The server could not start.");
        }
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

            runOnUiThread(()->
                    tv_perameters.append("\n\n"+DateFormat.getDateTimeInstance().format(new Date())+" : "+postBody));

            /*String[] arr = postBody.split(",");*/
            Log.w(TAG, postBody);
            return newFixedLengthResponse( "" );
        }
    }
}