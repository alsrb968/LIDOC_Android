package com.heartsafety.app;

public class Log {
    private static final String TAG = "LiDoc";
    private static boolean on = true;

    private static String getInfo() {
        StackTraceElement[] ste = new Throwable().getStackTrace();
        StackTraceElement realMethod = ste[2];
        return "[" + realMethod.getFileName() + ":" + realMethod.getLineNumber() + ":" + realMethod.getMethodName() + "()] ";
    }

    public static void on() {
        on = true;
    }

    public static void off() {
        on = false;
    }

    public static void v(String msg) {
        if (on) {
            android.util.Log.v(TAG, getInfo() + msg);
        }
    }

    public static void d(String msg) {
        if (on) {
            android.util.Log.d(TAG, getInfo() + msg);
        }
    }

    public static void i(String msg) {
        if (on) {
            android.util.Log.i(TAG, getInfo() + msg);
        }
    }

    public static void w(String msg) {
        if (on) {
            android.util.Log.w(TAG, getInfo() + msg);
        }
    }

    public static void e(String msg) {
        if (on) {
            android.util.Log.e(TAG, getInfo() + msg);
        }
    }

    public static void v(String tag, String msg) {
        if (on) {
            android.util.Log.v(tag, getInfo() + msg);
        }
    }

    public static void d(String tag, String msg) {
        if (on) {
            android.util.Log.d(tag, getInfo() + msg);
        }
    }

    public static void i(String tag, String msg) {
        if (on) {
            android.util.Log.i(tag, getInfo() + msg);
        }
    }

    public static void w(String tag, String msg) {
        if (on) {
            android.util.Log.w(tag, getInfo() + msg);
        }
    }

    public static void e(String tag, String msg) {
        if (on) {
            android.util.Log.e(tag, getInfo() + msg);
        }
    }
}
