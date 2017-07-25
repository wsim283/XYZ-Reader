package com.example.xyzreader;

import android.util.Log;

/**
 * Created by mulya on 19/07/2017.
 */

public class GeneralUtil {

    //set to true to see log messages(only apply to my logs, not the one that came with the starter code)
    private static final boolean DEBUG = false;


    public static void debugLog(String logTag, String logMsg){
        if(DEBUG) Log.v(logTag, logMsg);
    }
}
