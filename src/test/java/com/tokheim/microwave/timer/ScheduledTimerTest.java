package com.tokheim.microwave.timer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    @Nested
    @DisplayName("Exceptional Flows & Boundary Conditions")
    class ExceptionalFlowTests {

        @Test
        @DisplayName("Should throw NullPointerException when scheduler is null")
        void shouldThrowNpe_whenSchedulerIsNull() {
            assertThatThrownBy(() -> new ScheduledTimer(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("scheduler must not be null");
        }

        @Test
        @DisplayName("Should throw NullPointerException when start callback is null")
        void shouldThrowNpe_whenStartCallbackIsNull() {
            timer = new ScheduledTimer();

            assertThatThrownBy(() -> timer.start(60, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("onExpire callback must not be null");
        }

        @Test
        @DisplayName("Should reject non-positive duration or additional seconds")
        void shouldRejectInvalidSeconds() {
            timer = new ScheduledTimer();

            assertThatThrownBy(() -> timer.start(0, () -> {}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("durationSeconds must be positive");

            assertThatThrownBy(() -> timer.start(-10, () -> {}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("durationSeconds must be positive");

            assertThatThrownBy(() -> timer.addSeconds(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("additionalSeconds must be positive");

            assertThatThrownBy(() -> timer.addSeconds(-5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("additionalSeconds must be positive");
        }

        @Test
        @DisplayName("Should safely ignore addSeconds when timer is not running")
        void shouldSafelyIgnoreAddSeconds_whenTimerIsNotRunning() {
            timer = new ScheduledTimer();

            timer.addSeconds(30);

            assertThat(timer.isRunning()).isFalse();
            assertThat(timer.getRemainingSeconds()).isZero();
        }

        @Test
        @DisplayName("Should safely handle cancel when timer is not running")
        void shouldSafelyHandleCancel_whenTimerIsNotRunning() {
            timer = new ScheduledTimer();

            // Calling cancel on an idle timer should not throw
            timer.cancel();

            assertThat(timer.isRunning()).isFalse();
            assertThat(timer.getRemainingSeconds()).isZero();
        }

        @Test
        @DisplayName("Should use custom scheduler and not shut it down on close")
        void shouldUseCustomScheduler_andNotShutdownOnClose() {
            java.util.concurrent.ScheduledExecutorService customScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
            try {
                ScheduledTimer customTimer = new ScheduledTimer(customScheduler);
                customTimer.close();
                assertThat(customScheduler.isShutdown()).isFalse();
            } finally {
                customScheduler.shutdownNow();
            }
        }

        @Test
        @DisplayName("Should be idempotent when closed multiple times")
        void shouldBeIdempotent_whenClosedMultipleTimes() {
            timer = new ScheduledTimer();

            timer.close();
            timer.close(); // No exception should occur
        }

        @Test
        @DisplayName("Should catch and handle callback exceptions without failing the timer")
        void shouldHandleCallbackExceptionGracefully() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            timer = new ScheduledTimer();

            timer.start(1, () -> {
                latch.countDown();
                throw new RuntimeException("Simulated unexpected callback crash");
            });

            boolean expired = latch.await(2500, TimeUnit.MILLISECONDS);
            assertThat(expired).isTrue();
            assertThat(timer.isRunning()).isFalse();
        }
    }
}
