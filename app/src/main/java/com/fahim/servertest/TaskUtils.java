package com.fahim.servertest;

import android.content.Context;
import android.widget.Toast;

import com.fahim.servertest.Calculation.DSP;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

public class TaskUtils {

    static String getDeviceIpAddress(Context context) {
        String deviceIpAddress = null;

        try {
            for (Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces(); enumeration.hasMoreElements();) {
                NetworkInterface networkInterface = enumeration.nextElement();

                for (Enumeration<InetAddress> enumerationIpAddr = networkInterface.getInetAddresses(); enumerationIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumerationIpAddr.nextElement();

                    if (!inetAddress.isLoopbackAddress() && inetAddress.getAddress().length == 4)
                    {
                        deviceIpAddress = "IP : " + inetAddress.getHostAddress() + ":" + context.getString(R.string.port);
                    }
                }
            }
            if(deviceIpAddress == null)
                deviceIpAddress = "IP Address not found";
        } catch (SocketException e) {
            e.printStackTrace();
            deviceIpAddress = "IP Address not found";
        }
        return deviceIpAddress;
    }




    //////////////////////////////////////////////////////////////////////////

}
