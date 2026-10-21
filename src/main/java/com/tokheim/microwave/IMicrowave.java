package com.tokheim.microwave;

/**
 * Interface representing the hardware of the microwave unit.
 * Provided by Dover Fueling Solutions (Tokheim).
 */
public interface IMicrowave {
    /** Turns on the microwave heater element. */
    void turnOnHeater();

    /** Turns off the microwave heater element. */
    void turnOffHeater();

    /** Indicates if the door to the microwave is open or closed. */
    boolean isDoorOpen();

    /**
     * Listener interface to receive hardware event signals.
     */
    interface EventListener {
        /** Signals that the door is opened or closed */
        void onDoorStatusChanged(boolean open);

        /** Signals that the start button is pressed */
        void onStartButtonPressed();
    }
}
