package com.example.urban;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

public class MessageEvent {
    private String message1;
    private String message2;
    private static double context1;
    private static double context2;
    public String TAG="position";
    public MessageEvent(double message1,double message2) {
        context1 = message1;
        context2 = message2;
    }
    public void writeGnss(double[] gnssdata) throws IOException {
        message1=String.valueOf(gnssdata[1]);
        message2=String.valueOf(gnssdata[2]);
        Log.d(TAG, String.valueOf(gnssdata[2]));
    }
    public Double getMessage1() {
        return context1;
    }
    public Double getMessage2() {
        return context2;
    }
}

