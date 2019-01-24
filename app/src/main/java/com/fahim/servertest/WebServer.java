package com.fahim.servertest;

import android.os.Handler;
import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {

    private WebServerCallback callback;
    private long start_time;
    private Handler handler;

    private String from ="";

    WebServer(int port, WebServerCallback callback, Handler handler)
    {
        super(port);
        this.callback = callback;
        start_time = 0;
        this.handler = handler;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String postBody = session.getQueryParameterString();

        from = "";

        int index = postBody.indexOf("=");
        if(index != -1) {
            from = postBody.substring(0, index);
            postBody = postBody.substring(index+1);
        }

        String[] values = postBody.split(",");


        if(start_time == 0)
            start_time = System.currentTimeMillis();
        handler.post(() -> callback.onResponse(from,System.currentTimeMillis()-start_time,values));

        return newFixedLengthResponse( "" );
    }
}