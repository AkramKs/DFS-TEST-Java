# Microwave Controller

A simple Java-based microwave oven controller logic implementation and unit test suite for Dover Fueling Solutions (Tokheim).

## Requirements

- **Java**: JDK 17 or higher
- **Maven**: 3.8+ (or use the included `./mvnw` wrapper)

---

## Getting Started

### 1. Build the Project
Compile the code and package the JAR:
```bash
./mvnw clean compile
```

### 2. Run Tests
Execute the JUnit 5 test suite:
```bash
./mvnw test
```

### 3. Package
Package the application into a JAR:
```bash
./mvnw package
```

---

## Project Structure

```text
.
├── pom.xml                                   # Maven dependencies and build configuration
├── mvnw / mvnw.cmd                           # Maven wrapper scripts
└── src/
    ├── main/java/com/tokheim/microwave/
    │   ├── IMicrowave.java                   # Hardware interface (door, heater, event listener)
    │   ├── ILight.java                       # Interior light interface
    │   ├── MicrowaveController.java          # Core microwave controller logic
    │   └── timer/
    │       ├── ITimer.java                   # Timer abstraction interface
    │       └── ScheduledTimer.java           # Concrete timer using ScheduledExecutorService
    └── test/java/com/tokheim/microwave/
        ├── MicrowaveControllerTest.java      # Unit tests for controller logic and requirements
        └── timer/
            └── ScheduledTimerTest.java       # Unit tests for the timer implementation
```

---

## Core Components

- **`MicrowaveController`**: Manages microwave state (door, heater, light) and responds to hardware events (door status changes, start button presses, timer expirations).
- **`IMicrowave` & `ILight`**: Interfaces abstracting hardware interactions.
- **`ITimer` & `ScheduledTimer`**: Provides countdown and duration tracking with pause/resume/add capabilities.

---

## Assumptions & Design Decisions

Per the specification (*"Any additional requirements that may be missed can be assumed by you, but please note those down"*), the following assumptions were made:

1. **Initial Power-On State:**
   - The controller interrogates `IMicrowave.isDoorOpen()` upon construction to initialize the light (`ON` if open, `OFF` if closed).
   - The heater is explicitly ensured to be `OFF` during initialization.

2. **Door Safety & Heating Resumption:**
   - Opening the door while heating immediately stops the heater and **resets/cancels** the timer (per User Story 3).
   - Closing the door afterward **does not automatically resume** heating. Resuming requires an explicit user action (pressing the Start button again) to prevent accidental unattended operation.

3. **Timer Expiration:**
   - When the countdown finishes (reaches 0 seconds), the controller automatically turns off the heater element.
   - Timer expiration callbacks are idempotent and safe against late/delayed invocations.

4. **Defensive Programming & Fail-Fast:**
   - **Null Validation:** Constructor parameters (`IMicrowave`, `ILight`, `ITimer`) reject `null` values with informative `NullPointerException` messages.
   - **Value Validation:** Timer durations and additional increments must be positive (> 0); zero or negative values throw `IllegalArgumentException`.

5. **Fault Isolation & Safety Rollback:**
   - If `microwave.turnOnHeater()` throws a hardware error, the controller leaves `isHeating = false` and avoids starting the timer.
   - If `timer.start(...)` fails after the heater has engaged, the controller rolls back state and commands `microwave.turnOffHeater()` immediately to prevent runaway heating.

6. **Hardware Event Idempotency:**
   - Consecutive identical signals (e.g., redundant door-open or door-close notifications) are handled cleanly without duplicate actions or state corruption.
   - Start button presses while the door is open are safely ignored without any side effects.

7. **Concurrency & Thread Safety:**
   - In real-world hardware, sensor interrupts, user button presses, and timer callbacks occur on different threads. All state mutations in `MicrowaveController` are synchronized to prevent race conditions and deadlocks.

