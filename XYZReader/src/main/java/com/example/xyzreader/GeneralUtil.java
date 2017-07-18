package com.example.xyzreader;

import android.util.Log;

/**
 * Created by mulya on 19/07/2017.
 */

public class GeneralUtil {

    static final boolean DEBUG = true;


    public static void debugLog(String logTag, String logMsg){
        if(DEBUG) Log.v(logTag, logMsg);
    }
}
