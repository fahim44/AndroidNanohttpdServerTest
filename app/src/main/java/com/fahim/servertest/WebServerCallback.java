package com.fahim.servertest;

public interface WebServerCallback {
    void onResponse(String from, long time, String[] values);
}
