package com.tokheim.microwave.timer;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ScheduledTimer implements ITimer, AutoCloseable {

    private final ScheduledExecutorService scheduler;
    private final boolean ownsScheduler;
    private final AtomicLong remainingSeconds = new AtomicLong(0);
    private final Object lock = new Object();

    private ScheduledFuture<?> scheduledTask;
    private Runnable onExpireCallback;

    public ScheduledTimer() {
        this(Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "microwave-timer-thread");
            t.setDaemon(true);
            return t;
        }), true);
    }

    public ScheduledTimer(ScheduledExecutorService scheduler) {
        this(scheduler, false);
    }

    private ScheduledTimer(ScheduledExecutorService scheduler, boolean ownsScheduler) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
        this.ownsScheduler = ownsScheduler;
    }

    @Override
    public void start(long durationSeconds, Runnable onExpire) {
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("durationSeconds must be positive: " + durationSeconds);
        }
        Objects.requireNonNull(onExpire, "onExpire callback must not be null");

        synchronized (lock) {
            cancelInternal();
            this.onExpireCallback = onExpire;
            this.remainingSeconds.set(durationSeconds);
            this.scheduledTask = scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
        }
    }

    @Override
    public void addSeconds(long additionalSeconds) {
        if (additionalSeconds <= 0) {
            throw new IllegalArgumentException("additionalSeconds must be positive: " + additionalSeconds);
        }
        synchronized (lock) {
            if (isRunning()) {
                remainingSeconds.addAndGet(additionalSeconds);
            }
        }
    }

    @Override
    public void cancel() {
        synchronized (lock) {
            cancelInternal();
        }
    }

    private void cancelInternal() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
        remainingSeconds.set(0);
        onExpireCallback = null;
    }

    @Override
    public boolean isRunning() {
        synchronized (lock) {
            return scheduledTask != null && !scheduledTask.isDone() && remainingSeconds.get() > 0;
        }
    }

    @Override
    public long getRemainingSeconds() {
        return Math.max(0, remainingSeconds.get());
    }

    private void tick() {
        Runnable callbackToRun = null;
        synchronized (lock) {
            if (scheduledTask == null || scheduledTask.isCancelled()) {
                return;
            }
            long remaining = remainingSeconds.decrementAndGet();
            if (remaining <= 0) {
                callbackToRun = onExpireCallback;
                cancelInternal();
            }
        }

        if (callbackToRun != null) {
            try {
                callbackToRun.run();
            } catch (Throwable t) {
                // Log or handle any callback errors without terminating the scheduler thread
                System.err.println("Error in timer expiration callback: " + t.getMessage());
            }
        }
    }

    @Override
    public void close() {
        cancel();
        if (ownsScheduler) {
            scheduler.shutdownNow();
        }
    }
}
