# Course Schedule Analyzer

A Java console application that helps students plan and evaluate their academic schedule by analyzing workload based on hours, credits, and course difficulty.

## Project Structure

- **`Course.java`** — Defines the `Course` class and the `CourseType` enum (`THEORY`, `LAB`, `WORKSHOP`), each with a difficulty multiplier.
- **`schedule.java`** — Main class that manages a collection of courses, computes a workload score, and prints a detailed report.

## How It Works

### 1. Adding Courses
On startup, the program prompts you to set a maximum credit limit, then enter courses one by one. For each course you provide:
- **Name**
- **Type** — `THEORY` (×1.0), `LAB` (×1.4), or `WORKSHOP` (×1.2)
- **Hours per week**
- **Credits**
- **Difficulty rating** (0.0 – 10.0)

### 2. Score Calculation
Once all courses are added, a final workload score (0–100) is computed using three weighted components:

| Component | Weight | Basis |
|---|---|---|
| Hours Score | 40% | Total hours / 30 max hours |
| Credits Score | 30% | Total credits / max credits set |
| Difficulty Score | 30% | Sum of (difficulty × type multiplier), normalized |

### 3. Report & Warnings
The program prints a full report including score breakdown and any warnings:
- Credit limit exceeded
- LAB courses with difficulty ≥ 4.0
- Final score above **80.0** (schedule may be too demanding)

## Running the Project

```bash
javac Course.java schedule.java
java schedule
```
