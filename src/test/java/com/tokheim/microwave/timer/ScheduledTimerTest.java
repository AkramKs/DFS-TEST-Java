package com.tokheim.microwave.timer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ScheduledTimer Unit Tests")
class ScheduledTimerTest {

    private ScheduledTimer timer;

    @AfterEach
    void tearDown() {
        if (timer != null) {
            timer.close();
        }
    }

    @Test
    @DisplayName("Should start timer and track remaining seconds")
    void shouldStartTimerAndTrackRemainingSeconds() {
        timer = new ScheduledTimer();

        timer.start(60, () -> {});

        assertThat(timer.isRunning()).isTrue();
        assertThat(timer.getRemainingSeconds()).isEqualTo(60L);
    }

    @Test
    @DisplayName("Should increase remaining seconds when addSeconds is called")
    void shouldIncreaseRemainingSeconds() {
        timer = new ScheduledTimer();
        timer.start(60, () -> {});

        timer.addSeconds(60);

        assertThat(timer.getRemainingSeconds()).isGreaterThanOrEqualTo(119L);
    }

    @Test
    @DisplayName("Should reset remaining seconds to zero and stop when cancelled")
    void shouldResetToZeroWhenCancelled() {
        timer = new ScheduledTimer();
        timer.start(60, () -> {});

        timer.cancel();

        assertThat(timer.isRunning()).isFalse();
        assertThat(timer.getRemainingSeconds()).isZero();
    }

    @Test
    @DisplayName("Should execute callback when countdown finishes")
    void shouldExecuteCallbackWhenCountdownFinishes() throws InterruptedException {
        // Use a 1-second countdown to quickly verify callback firing
        CountDownLatch latch = new CountDownLatch(1);
        timer = new ScheduledTimer();

        timer.start(1, latch::countDown);

        boolean expired = latch.await(2500, TimeUnit.MILLISECONDS);
        assertThat(expired).isTrue();
        assertThat(timer.isRunning()).isFalse();
    }

    @Test
    @DisplayName("Should reject non-positive duration or seconds")
    void shouldRejectInvalidArguments() {
        timer = new ScheduledTimer();

        assertThatThrownBy(() -> timer.start(0, () -> {}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> timer.start(-10, () -> {}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> timer.addSeconds(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> timer.addSeconds(-5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
