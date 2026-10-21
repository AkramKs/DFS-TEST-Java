package com.tokheim.microwave.timer;


public interface ITimer {


    void start(long durationSeconds, Runnable onExpire);

    void addSeconds(long additionalSeconds);

    void cancel();

    boolean isRunning();

    long getRemainingSeconds();
}
