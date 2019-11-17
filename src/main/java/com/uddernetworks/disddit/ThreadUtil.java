package com.uddernetworks.disddit;

public class ThreadUtil {

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {}
    }

    public static <T> T sleepReturn(long millis) {
        sleep(millis);
        return null;
    }

    public static <T> T hang() {
        sleep(Long.MAX_VALUE);
        return null;
    }

}
