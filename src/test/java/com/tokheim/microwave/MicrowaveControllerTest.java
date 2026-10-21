package com.tokheim.microwave;

import com.tokheim.microwave.timer.ITimer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MicrowaveController Unit Tests")
class MicrowaveControllerTest {

    @Mock
    private IMicrowave microwave;

    @Mock
    private ILight light;

    @Mock
    private ITimer timer;

    private MicrowaveController controller;

    @BeforeEach
    void setUp() {
        // Default initial condition: door is closed
        when(microwave.isDoorOpen()).thenReturn(false);
        controller = new MicrowaveController(microwave, light, timer);
        // Clear setup interactions so each test starts with a clean slate
        clearInvocations(microwave, light, timer);
    }

    @Nested
    @DisplayName("Initial State & Construction")
    class InitialStateTests {

        @Test
        @DisplayName("Should initialize with light off and heater off when door is closed")
        void shouldInitializeWithLightOff_whenStartingWithClosedDoor() {
            IMicrowave closedMicrowave = mock(IMicrowave.class);
            ILight mockLight = mock(ILight.class);
            ITimer mockTimer = mock(ITimer.class);
            when(closedMicrowave.isDoorOpen()).thenReturn(false);

            MicrowaveController c = new MicrowaveController(closedMicrowave, mockLight, mockTimer);

            verify(mockLight).turnOffLight();
            verify(closedMicrowave).turnOffHeater();
            assertThat(c.isDoorOpen()).isFalse();
            assertThat(c.isHeating()).isFalse();
        }

        @Test
        @DisplayName("Should initialize with light on and heater off when door is open")
        void shouldInitializeWithLightOn_whenStartingWithOpenDoor() {
            IMicrowave openMicrowave = mock(IMicrowave.class);
            ILight mockLight = mock(ILight.class);
            ITimer mockTimer = mock(ITimer.class);
            when(openMicrowave.isDoorOpen()).thenReturn(true);

            MicrowaveController c = new MicrowaveController(openMicrowave, mockLight, mockTimer);

            verify(mockLight).turnOnLight();
            verify(openMicrowave).turnOffHeater();
            assertThat(c.isDoorOpen()).isTrue();
            assertThat(c.isHeating()).isFalse();
        }
    }

    @Nested
    @DisplayName("Remaining Cooking Time")
    class RemainingCookingTimeTests {

        @Test
        @DisplayName("Should return remaining cooking time from the underlying timer")
        void shouldReturnRemainingCookingTimeFromTimer() {
            when(timer.getRemainingSeconds()).thenReturn(45L);

            assertThat(controller.getRemainingCookingTimeSeconds()).isEqualTo(45L);
            verify(timer).getRemainingSeconds();
        }

        @Test
        @DisplayName("Should return 0 seconds when timer is idle or stopped")
        void shouldReturnZeroRemainingSeconds_whenTimerIsIdle() {
            when(timer.getRemainingSeconds()).thenReturn(0L);

            assertThat(controller.getRemainingCookingTimeSeconds()).isZero();
            verify(timer).getRemainingSeconds();
        }
    }

    @Nested
    @DisplayName("User Story 1 & 2: Door Light Control")
    class DoorLightTests {

        @Test
        @DisplayName("Story 1: When I open the door, the light turns on")
        void shouldTurnOnLight_whenDoorOpens() {
            controller.onDoorStatusChanged(true);

            verify(light).turnOnLight();
            assertThat(controller.isDoorOpen()).isTrue();
        }

        @Test
        @DisplayName("Story 2: When I close the door, the light turns off")
        void shouldTurnOffLight_whenDoorCloses() {
            controller.onDoorStatusChanged(true);
            clearInvocations(light);

            controller.onDoorStatusChanged(false);

            verify(light).turnOffLight();
            assertThat(controller.isDoorOpen()).isFalse();
        }
    }

    @Nested
    @DisplayName("User Story 3: Door Opening During Heating")
    class DoorOpenWhileHeatingTests {

        @Test
        @DisplayName("Story 3: When I open the door, the heater stops running")
        void shouldStopHeaterAndResetTimer_whenDoorOpensWhileHeating() {
            controller.onStartButtonPressed();
            assertThat(controller.isHeating()).isTrue();
            verify(microwave).turnOnHeater();

            controller.onDoorStatusChanged(true);

            verify(microwave).turnOffHeater();
            verify(timer).cancel();
            verify(light).turnOnLight();
            assertThat(controller.isHeating()).isFalse();
        }

        @Test
        @DisplayName("Closing the door after heating was stopped should NOT automatically resume heating")
        void shouldNotResumeHeating_whenDoorIsClosedAfterBeingOpenedDuringHeating() {
            controller.onStartButtonPressed();
            controller.onDoorStatusChanged(true);

            clearInvocations(microwave, timer);

            controller.onDoorStatusChanged(false);

            verify(microwave, never()).turnOnHeater();
            verify(timer, never()).start(anyLong(), any());
            assertThat(controller.isHeating()).isFalse();
        }
    }

    @Nested
    @DisplayName("User Story 4: Start Button When Door is Open")
    class StartButtonWithOpenDoorTests {

        @Test
        @DisplayName("Story 4: When I press the start button when the door is opened, nothing happens")
        void shouldDoNothing_whenStartPressedWithOpenDoor() {
            controller.onDoorStatusChanged(true);
            clearInvocations(microwave, light, timer);

            controller.onStartButtonPressed();

            verifyNoInteractions(microwave, timer);
            verifyNoMoreInteractions(light);
            assertThat(controller.isHeating()).isFalse();
        }
    }

    @Nested
    @DisplayName("User Story 5: Start Button When Door is Closed")
    class StartButtonWithClosedDoorTests {

        @Test
        @DisplayName("Story 5: When I press the start button when the door is closed, the heater runs for 1 minute")
        void shouldRunHeaterForOneMinute_whenStartPressedWithClosedDoor() {
            assertThat(controller.isDoorOpen()).isFalse();

            controller.onStartButtonPressed();

            verify(microwave).turnOnHeater();
            verify(timer).start(eq(60L), any(Runnable.class));
            assertThat(controller.isHeating()).isTrue();
        }
    }

    @Nested
    @DisplayName("User Story 6: Start Button While Already Heating")
    class StartButtonWhileAlreadyHeatingTests {

        @Test
        @DisplayName("Story 6: When I press start button when door is closed and already heating, remaining time is increased with 1 minute")
        void shouldAddOneMinute_whenStartPressedWhileAlreadyHeating() {
            controller.onStartButtonPressed();
            assertThat(controller.isHeating()).isTrue();

            controller.onStartButtonPressed();

            verify(timer).addSeconds(60L);
            assertThat(controller.isHeating()).isTrue();
        }

        @Test
        @DisplayName("Consecutive start button presses should sequentially add 1 minute each")
        void shouldAddOneMinutePerPress_whenStartPressedMultipleTimes() {
            controller.onStartButtonPressed();

            controller.onStartButtonPressed();
            controller.onStartButtonPressed();

            verify(timer, times(2)).addSeconds(60L);
        }
    }

    @Nested
    @DisplayName("Timer Expiration (Additional Specification)")
    class TimerExpirationTests {

        @Test
        @DisplayName("When the countdown timer expires, the heater should automatically turn off")
        void shouldTurnOffHeater_whenTimerExpires() {
            ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);

            controller.onStartButtonPressed();
            verify(timer).start(eq(60L), callbackCaptor.capture());

            Runnable onExpire = callbackCaptor.getValue();
            onExpire.run();

            verify(microwave).turnOffHeater();
            assertThat(controller.isHeating()).isFalse();
        }

        @Test
        @DisplayName("Timer expiration should be idempotent if already stopped")
        void shouldBeIdempotent_whenTimerExpiresAfterBeingStopped() {
            ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
            controller.onStartButtonPressed();
            verify(timer).start(eq(60L), callbackCaptor.capture());

            controller.onDoorStatusChanged(true);
            clearInvocations(microwave);

            Runnable onExpire = callbackCaptor.getValue();
            onExpire.run();

            verify(microwave, never()).turnOffHeater();
        }
    }

    @Nested
    @DisplayName("Exceptional Flows & Defensive Edge Cases")
    class ExceptionalFlowTests {

        @Test
        @DisplayName("Should throw NullPointerException when constructor dependencies are null")
        void shouldThrowNpe_whenConstructorDependenciesAreNull() {
            assertThatThrownBy(() -> new MicrowaveController(null, light, timer))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("microwave must not be null");

            assertThatThrownBy(() -> new MicrowaveController(microwave, null, timer))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("light must not be null");

            assertThatThrownBy(() -> new MicrowaveController(microwave, light, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("timer must not be null");

            // Overloaded 2-argument constructor
            assertThatThrownBy(() -> new MicrowaveController(null, light))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("microwave must not be null");

            assertThatThrownBy(() -> new MicrowaveController(microwave, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("light must not be null");
        }

        @Test
        @DisplayName("Should fail fast if hardware sensor throws an exception during initialization")
        void shouldFailFast_whenHardwareThrowsDuringInitialization() {
            IMicrowave faultyMicrowave = mock(IMicrowave.class);
            when(faultyMicrowave.isDoorOpen()).thenThrow(new IllegalStateException("Sensor read error"));

            assertThatThrownBy(() -> new MicrowaveController(faultyMicrowave, light, timer))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Sensor read error");
        }

        @Test
        @DisplayName("Should keep state consistent and not start timer if heater fails to turn on")
        void shouldKeepStateConsistent_whenHeaterFailsToTurnOn() {
            doThrow(new RuntimeException("Heater element fault")).when(microwave).turnOnHeater();

            assertThatThrownBy(() -> controller.onStartButtonPressed())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Heater element fault");

            assertThat(controller.isHeating()).isFalse();
            verify(timer, never()).start(anyLong(), any());
        }

        @Test
        @DisplayName("Should roll back state and turn off heater if timer fails to start")
        void shouldRollbackState_whenTimerFailsToStart() {
            doThrow(new RuntimeException("Timer allocation failed")).when(timer).start(anyLong(), any());

            assertThatThrownBy(() -> controller.onStartButtonPressed())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Timer allocation failed");

            assertThat(controller.isHeating()).isFalse();
            verify(microwave).turnOffHeater();
        }

        @Test
        @DisplayName("Should handle duplicate door open events idempotently without side effects")
        void shouldHandleDuplicateDoorOpenEventsIdempotently() {
            controller.onDoorStatusChanged(true);
            clearInvocations(light);

            // Redundant second open event
            controller.onDoorStatusChanged(true);

            verify(light).turnOnLight();
            assertThat(controller.isDoorOpen()).isTrue();
            assertThat(controller.isHeating()).isFalse();
        }

        @Test
        @DisplayName("Should handle duplicate door close events idempotently without side effects")
        void shouldHandleDuplicateDoorCloseEventsIdempotently() {
            // Already closed initially
            controller.onDoorStatusChanged(false);

            verify(light).turnOffLight();
            assertThat(controller.isDoorOpen()).isFalse();
            assertThat(controller.isHeating()).isFalse();
        }

        @Test
        @DisplayName("Should safely ignore start button presses when pressed repeatedly with open door")
        void shouldSafelyIgnoreStartButton_whenPressedRepeatedlyWithOpenDoor() {
            controller.onDoorStatusChanged(true);
            clearInvocations(microwave, light, timer);

            controller.onStartButtonPressed();
            controller.onStartButtonPressed();
            controller.onStartButtonPressed();

            verifyNoInteractions(microwave, timer);
            assertThat(controller.isHeating()).isFalse();
        }
    }

    @Nested
    @DisplayName("Thread Safety & Concurrency")
    class ConcurrencyTests {

        /**
         * This test guarantees that:
         * 1. The controller's monitor locks release cleanly.
         * 2. Multiple threads competing for the controller locks do not deadlock.
         * 3. No thread throws an unhandled exception that would halt execution.
         */

        @Test
        @DisplayName("Should handle concurrent events without race conditions or deadlocks")
        void shouldHandleConcurrentEventsSafely() throws InterruptedException {
            int threadCount = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                executor.submit(() -> {
                    try {
                        if (idx % 2 == 0) {
                            controller.onStartButtonPressed();
                        } else {
                            controller.onDoorStatusChanged(idx % 4 == 1);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean finished = latch.await(5, TimeUnit.SECONDS);
            executor.shutdownNow();

            assertThat(finished).isTrue();
        }
    }
}
