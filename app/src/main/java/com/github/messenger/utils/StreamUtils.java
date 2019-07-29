package com.github.messenger.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamUtils {

    public static final String END = "@end@";

    public static String input2String(InputStream in) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) != -1) {
            result.write(buffer, 0, length);
            if (new String(buffer, 0, length).contains("@end@")) {
                return toResponse(result.toString("UTF-8"));
            }
        }
        return toResponse(result.toString("UTF-8"));
    }

    private static String toResponse(String s) {
        if (s == null || s.length() == 0) return null;
        if (s.contains(END))
            return s.substring(0, s.indexOf(END));
        else return s;
    }
}
