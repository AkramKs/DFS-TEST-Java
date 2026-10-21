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
