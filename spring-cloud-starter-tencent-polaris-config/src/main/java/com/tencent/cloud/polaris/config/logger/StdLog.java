package com.tencent.cloud.polaris.config.logger;

import java.io.PrintStream;
import java.util.Calendar;

/**
 * internal output logger
 * @author juanyinyang
 */
public class StdLog {

    private static  final String CLASS_INFO = StdLog.class.getName();


    protected static boolean debugEnabled = false;

    /**
     * enable info level log
     */
    protected static boolean infoEnabled = true;

    /**
     * quite mode will out put nothing
     */
    protected static boolean quietMode = false;


    private static final String DEBUG_FIX = "StdLog:DEBUG: ";

    private static final String INFO_FIX = "StdLog:INFO: ";

    private static final String WARN_FIX = "StdLog:WARN: ";

    public static void setQuietMode(boolean quietMode) {
        StdLog.quietMode = quietMode;
    }

    public static void setInfoEnabled(boolean infoEnabled ){
        StdLog.infoEnabled = infoEnabled;
    }

    public static void setDebugEnabled(boolean debugEnabled) {
        StdLog.debugEnabled = debugEnabled;
    }

    public static void debug(String msg) {

        if (debugEnabled && !quietMode) {
            println(System.out,DEBUG_FIX + msg);
        }
    }

    public static void info(String msg) {
        if (infoEnabled && !quietMode) {
            println(System.out,INFO_FIX + msg);
        }
    }

    public static void warn(String msg) {
        if (infoEnabled && !quietMode) {
            println(System.err,WARN_FIX + msg);
        }
    }

    public static void warn(String msg, Throwable t) {
        if (quietMode) {
            return;
        }
        println(System.err,WARN_FIX + msg);
        if (t != null) {
            t.printStackTrace();
        }
    }

    private static void println(PrintStream out, String msg) {
        out.println(Calendar.getInstance().getTime().toString() + " " + CLASS_INFO + " " + msg);
    }

}
