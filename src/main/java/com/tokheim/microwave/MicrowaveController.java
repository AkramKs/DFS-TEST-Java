package com.tokheim.microwave;

import com.tokheim.microwave.timer.ITimer;
import com.tokheim.microwave.timer.ScheduledTimer;

import java.util.Objects;

public class MicrowaveController implements IMicrowave.EventListener {

    public static final long COOK_DURATION_SECONDS = 60L;

    private final IMicrowave microwave;
    private final ILight light;
    private final ITimer timer;

    private boolean isDoorOpen;
    private boolean isHeating;

    public MicrowaveController(IMicrowave microwave, ILight light) {
        this(microwave, light, new ScheduledTimer());
    }

    public MicrowaveController(IMicrowave microwave, ILight light, ITimer timer) {
        this.microwave = Objects.requireNonNull(microwave, "microwave must not be null");
        this.light = Objects.requireNonNull(light, "light must not be null");
        this.timer = Objects.requireNonNull(timer, "timer must not be null");

        // Ensures only one thread can touch the microwave's state at a time.
        synchronized (this) {
            this.isDoorOpen = microwave.isDoorOpen();
            this.isHeating = false;

            if (this.isDoorOpen) {
                this.light.turnOnLight();
            } else {
                this.light.turnOffLight();
            }
            this.microwave.turnOffHeater();
        }
    }

    /**
     * User Story 1: When I open the door, the light turns on.
     * User Story 2: When I close the door, the light turns off.
     * User Story 3: When I open the door, the heater stops running.
     */
    @Override
    public synchronized void onDoorStatusChanged(boolean open) {
        this.isDoorOpen = open;

        if (open) {
            light.turnOnLight();

            if (isHeating) {
                stopHeatingAndResetTimer();
            }
        } else {
            light.turnOffLight();
        }
    }

    /**
     * User Story 4: When I press the start button when the door is opened, nothing happens.
     * User Story 5: When I press the start button when the door is closed, the heater runs for 1 minute.
     * User Story 6: When I press the start button when the door is closed and already heating, the remaining time is increased with 1 minute.
     */
    @Override
    public synchronized void onStartButtonPressed() {
        if (isDoorOpen) {
            return;
        }

        if (!isHeating) {
            startHeating();
        } else {
            timer.addSeconds(COOK_DURATION_SECONDS);
        }
    }

    private void startHeating() {
        microwave.turnOnHeater();
        this.isHeating = true;
        try {
            timer.start(MicrowaveController.COOK_DURATION_SECONDS, this::onTimerExpired);
        } catch (RuntimeException e) {
            this.isHeating = false;
            microwave.turnOffHeater();
            throw e;
        }
    }

    private void stopHeatingAndResetTimer() {
        this.isHeating = false;
        microwave.turnOffHeater();
        timer.cancel();
    }

    private synchronized void onTimerExpired() {
        if (isHeating) {
            this.isHeating = false;
            microwave.turnOffHeater();
        }
    }

    public synchronized boolean isHeating() {
        return isHeating;
    }

    public synchronized boolean isDoorOpen() {
        return isDoorOpen;
    }

    public synchronized long getRemainingCookingTimeSeconds() {
        return timer.getRemainingSeconds();
    }
}
