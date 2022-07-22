package com.meidical.utils;

import android.util.Log;

/**
 * @author DasonYu
 * @date 2020/11/9  13:38
 * @descprition
 */
public class LogUtils {
    private static LogUtils mLogUtils = null;
    private static boolean allowDebug;

    private LogUtils() {
    }

    public static LogUtils getInstance() {
        if (mLogUtils == null) {
            synchronized (LogUtils.class) {
                if (mLogUtils == null) {
                    mLogUtils = new LogUtils();
                }
            }
        }
        return mLogUtils;
    }

    public static void e(String tag, String message) {
        if (allowDebug) {
            Log.e(tag, message);
        }
    }

    public static void d(String tag, String message) {
        if (allowDebug) {
            Log.d(tag, message);
        }
    }

    public void setDebug(boolean allowDebug) {
        LogUtils.allowDebug = allowDebug;
    }

}
