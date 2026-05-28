package com.sug.sugojcodesandbox.unsafe;

public class SleepError {
    public static void main(String[] args) throws InterruptedException {
        Long ONE_HOUR=60*60*1000L;
        Thread.sleep(ONE_HOUR);
        System.out.println("OVER");
    }
}
