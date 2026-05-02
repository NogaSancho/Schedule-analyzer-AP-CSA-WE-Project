# Course Schedule Analyzer

A Java application for planning and evaluating course workload. Includes a Swing GUI, a JavaFX GUI, and an original console version.

## Project Structure

```
├── src/                        # Java source files (GUI versions)
│   ├── Course.java             # Domain model for course data and CourseType multipliers
│   ├── ScheduleModel.java      # In-memory schedule state
│   ├── ScheduleService.java    # Validation, score computation, and warning generation
│   ├── ScheduleAnalysis.java   # Immutable analysis result object
│   ├── ValidationResult.java   # Validation result object used by add/edit flows
│   ├── ScheduleJsonStore.java  # JSON save/load for schedules
│   ├── ScheduleAnalyzerSwingApp.java  # Swing application entry point
│   └── ScheduleAnalyzerApp.java       # JavaFX application entry point
├── legacy/                     # Original console-based implementation
│   └── schedule.java
├── out/                        # Compiled .class files (build output)
├── LICENSE
└── README.md
```

## Features

1. Add, edit, and delete courses in a GUI table.
2. Set max credits dynamically.
3. Real-time analysis of:
   - Total hours and credits
   - Weighted scores (hours 40%, credits 30%, difficulty 30%)
   - Final workload score (0-100)
4. Real-time warnings for:
   - Credit limit exceeded
   - LAB difficulty >= 4.0
   - Final score above 80.0
5. Save and load schedules as JSON.

## Validation Rules

- Course name cannot be empty.
- Hours per week must be at least 1.
- Credits must be at least 1.
- Difficulty rating must be between 0.0 and 10.0.

---

## Compile & Run — Swing GUI (recommended)

Swing is bundled with the JDK — no extra dependencies needed.

**Compile:**
```bash
javac -d out src/Course.java src/ValidationResult.java src/ScheduleAnalysis.java \
  src/ScheduleModel.java src/ScheduleService.java src/ScheduleJsonStore.java \
  src/ScheduleAnalyzerSwingApp.java
```

**Run:**
```bash
java -cp out ScheduleAnalyzerSwingApp
```

---

## Compile & Run — Legacy Console Version

```bash
javac -d out src/Course.java legacy/schedule.java
java -cp out schedule
```
