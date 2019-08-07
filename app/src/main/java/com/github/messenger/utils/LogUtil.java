package com.github.messenger.utils;

import com.alibaba.fastjson.JSON;

public class LogUtil {

    private static boolean debug = false;

    public void setDebug(boolean d) {
        debug = d;
    }

    public static void log(Object o) {
        if (debug) {
            System.out.println(o == null ? "null" : o.toString());
        }
    }

    public static void log(String m) {
        if (debug) {
            System.out.println(m == null ? "null" : m);
        }
    }

    public static void logJson(Object o) {
        if (debug) {
            try {
                System.out.println(o == null ? "null" : JSON.toJSONString(o));
            } catch (Exception e) {
                log(o);
            }
        }
    }
}
