# Prompt for Claude (web) — Generate UML class diagrams

> Copy everything below the line into claude.ai. The full Java source is embedded,
> so Claude has all it needs without access to the repository.

---

You are a software design assistant. I will give you the complete Java source for a
small AP Computer Science A project called **Course Schedule Analyzer**. I want you to
produce accurate **UML class diagrams**.

## What I want you to produce

1. **One combined UML class diagram** showing every class, enum, and nested class, with
   all relationships between them (this is the most important deliverable).
2. **One detailed diagram per class** showing every field and method individually.
3. A short **legend/explanation** of the notation and relationships you used.

Output the diagrams as **PlantUML** code (inside ```plantuml fenced blocks) so I can
render them. After the PlantUML, also give me a **Mermaid `classDiagram`** version of the
*combined* diagram as an easy-to-render alternative.

## Notation rules — follow strictly

- Visibility: `+` public, `-` private, `#` protected, `~` package-private.
- Mark **static** members as static (PlantUML `{static}`) and render them underlined.
- Mark **abstract** members/classes as abstract (italic).
- Use the correct UML element type per class:
  - `enum` for `Course.CourseType`.
  - regular `class` for the rest.
- Show the **type of every field** and the **return type + parameter types of every method**
  (including constructors).
- Represent relationships with the correct UML arrows, and prefer the most specific one:
  - **Generalization** (solid line, hollow triangle) for `extends` — e.g. the Swing app extends `JFrame`.
  - **Composition** (filled diamond) when an object owns instances created/held for its
    whole lifetime (e.g. a class that holds `List<Course>` it manages, or `final` fields
    instantiated inline).
  - **Aggregation** (hollow diamond) for looser "has-a" holding of objects created elsewhere.
  - **Dependency** (dashed open arrow) when a class only *uses* another as a method
    parameter, return type, or local variable.
  - **Nested/inner type** containment for `Course.CourseType` inside `Course` and
    `ScheduleJsonStore.LoadedSchedule` inside `ScheduleJsonStore` (use PlantUML `+--` containment
    or a note making the nesting explicit).
- Put **multiplicities** on associations (e.g. `ScheduleModel "1" o-- "0..*" Course`).
- For collection fields like `List<Course>` or `List<String>`, model the element type
  relationship (e.g. association to `Course`); you may keep `List<String>` as a plain
  attribute since `String` is a JDK type.
- For JDK/library types (`JFrame`, `JTable`, `List`, `Path`, etc.), do **not** draw full
  boxes for them — either reference them as external types or show only the one
  generalization that matters (`JFrame`). Keep the focus on the project's own classes.

## Scope

Diagram these project classes (defined below): `Course`, `Course.CourseType`,
`ScheduleModel`, `ScheduleService`, `ScheduleAnalysis`, `ValidationResult`,
`ScheduleJsonStore`, `ScheduleJsonStore.LoadedSchedule`, and `ScheduleAnalyzerSwingApp`.

Also include the legacy console class `schedule` in a **separate** diagram (do not mix it
with the GUI design — it is an older array-based version kept for reference).

For the GUI view class (`ScheduleAnalyzerSwingApp`), it has many private Swing widget
fields and many private helper methods. In the **combined** diagram, summarize these
(e.g. group the Swing widget fields and the UI-builder/helper methods) to keep it readable,
but in its **detailed per-class diagram**, list every field and method.

## Relationships to verify you captured

- `Course` **contains** the enum `CourseType`.
- `ScheduleModel` **composes** `Course` (holds and manages a `List<Course>`, 0..*).
- `ScheduleService` **depends on** `ScheduleModel`, `Course`, and `Course.CourseType`
  (method params), and **creates/returns** `ScheduleAnalysis` and `ValidationResult`.
- `ScheduleAnalysis` holds a `List<String> warnings`; `ValidationResult` holds a
  `List<String> errors` (both immutable).
- `ScheduleJsonStore` **contains** the nested static class `LoadedSchedule`, **depends on**
  `ScheduleModel` and `Course`; `LoadedSchedule` holds a `List<Course>` (composition, 0..*).
- `ScheduleAnalyzerSwingApp` **extends `JFrame`**, **composes** one `ScheduleModel`, one
  `ScheduleService`, and one `ScheduleJsonStore` (all `final`, created inline), and
  **depends on** `Course`, `ScheduleAnalysis`, and `ValidationResult`.

Render the diagrams. Then give me the legend. Be precise about field types and visibility —
match the source exactly.

---

## Source code

### `src/Course.java`
```java
/**
 * Represents a single course in a student's schedule.
 * Each course has a name, type, weekly hours, credit value, and difficulty rating.
 * The course type affects how difficulty is weighted during schedule analysis.
 */
public class Course {
    private String name;
    private CourseType type;
    private int hoursPerWeek;
    private int credits;
    private double difficultyRating;

    /**
     * Enum representing the different formats a course can take.
     * Each type has a multiplier that scales difficulty during analysis —
     * lab courses weigh more heavily than theory courses.
     */
    public enum CourseType {
        THEORY(1.0),
        LAB(1.4),
        WORKSHOP(1.2);

        private final double multiplier;

        CourseType(double multiplier) {
            this.multiplier = multiplier;
        }

        /** Returns the difficulty multiplier for this course type. */
        public double getMultiplier() {
            return multiplier;
        }
    }

    /**
     * Creates a new Course with the given attributes.
     */
    public Course(String name, CourseType type, int hoursPerWeek, int credits, double difficultyRating) {
        this.name = name;
        this.type = type;
        this.hoursPerWeek = hoursPerWeek;
        this.credits = credits;
        this.difficultyRating = difficultyRating;
    }

    public String getName() { return name; }
    public CourseType getType() { return type; }
    public int getHoursPerWeek() { return hoursPerWeek; }
    public int getCredits() { return credits; }
    public double getDifficultyRating() { return difficultyRating; }

    @Override
    public String toString() {
        return String.format("%s [Type: %s, Hours: %d/week, Credits: %d, Difficulty: %.1f]",
                            name, type, hoursPerWeek, credits, difficultyRating);
    }
}
```

### `src/ScheduleModel.java`
```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores the current list of courses and the maximum credit limit.
 * Acts as the central data model for the schedule analyzer.
 */
public class ScheduleModel {
    private final List<Course> courses = new ArrayList<>();
    private int maxCredits = 25;  // default credit cap

    public List<Course> getCoursesSnapshot() {
        return Collections.unmodifiableList(courses);
    }

    public int getMaxCredits() { return maxCredits; }

    public void setMaxCredits(int maxCredits) {
        if (maxCredits < 1) {
            throw new IllegalArgumentException("Max credits must be at least 1.");
        }
        this.maxCredits = maxCredits;
    }

    public void addCourse(Course course) { courses.add(course); }
    public void updateCourse(int index, Course course) { courses.set(index, course); }
    public void removeCourse(int index) { courses.remove(index); }
    public void clearCourses() { courses.clear(); }
}
```

### `src/ScheduleService.java`
```java
import java.util.ArrayList;
import java.util.List;

/**
 * Contains the business logic for the schedule analyzer:
 *   1. Validating course input before it is added to the model
 *   2. Analyzing the full schedule to produce scores and warnings
 *
 * Scoring: finalScore = hoursScore*0.40 + creditsScore*0.30 + difficultyScore*0.30
 */
public class ScheduleService {
    public static final int MAX_HOURS = 30;
    public static final double SCORE_THRESHOLD = 80.0;

    public ValidationResult validateCourseInput(
            String name,
            Course.CourseType type,
            int hoursPerWeek,
            int credits,
            double difficultyRating) {
        List<String> errors = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) errors.add("Course name is required.");
        if (type == null) errors.add("Course type is required.");
        if (hoursPerWeek < 1) errors.add("Hours per week must be at least 1.");
        if (credits < 1) errors.add("Credits must be at least 1.");
        if (difficultyRating < 0.0 || difficultyRating > 10.0)
            errors.add("Difficulty rating must be between 0.0 and 10.0.");
        return new ValidationResult(errors.isEmpty(), errors);
    }

    public ScheduleAnalysis analyze(ScheduleModel model) {
        List<Course> courses = model.getCoursesSnapshot();
        int totalHours = 0;
        int totalCredits = 0;
        double weightedDifficulty = 0.0;
        for (Course course : courses) {
            totalHours += course.getHoursPerWeek();
            totalCredits += course.getCredits();
            weightedDifficulty += course.getDifficultyRating() * course.getType().getMultiplier();
        }
        double hoursScore = cap((double) totalHours / MAX_HOURS * 100.0);
        double creditsScore = cap((double) totalCredits / model.getMaxCredits() * 100.0);
        double difficultyScore = cap((weightedDifficulty / (10.0 * 1.4)) * 100.0);
        double finalScore = (hoursScore * 0.4) + (creditsScore * 0.3) + (difficultyScore * 0.3);

        List<String> warnings = new ArrayList<>();
        if (totalCredits > model.getMaxCredits()) {
            warnings.add(String.format(
                    "CREDIT LIMIT WARNING: Total credits (%d) exceed maximum allowed (%d)",
                    totalCredits, model.getMaxCredits()));
        }
        for (Course course : courses) {
            if (course.getType() == Course.CourseType.LAB && course.getDifficultyRating() >= 4.0) {
                warnings.add(String.format(
                        "LAB COURSE WARNING: '%s' has high difficulty (%.1f)",
                        course.getName(), course.getDifficultyRating()));
            }
        }
        if (finalScore > SCORE_THRESHOLD) {
            warnings.add(String.format(
                    "WORKLOAD WARNING: Final score (%.2f) exceeds threshold (%.2f). Consider balancing your schedule.",
                    finalScore, SCORE_THRESHOLD));
        }
        return new ScheduleAnalysis(totalHours, totalCredits, hoursScore, creditsScore,
                difficultyScore, finalScore, warnings);
    }

    private double cap(double value) { return Math.min(value, 100.0); }
}
```

### `src/ScheduleAnalysis.java`
```java
import java.util.Collections;
import java.util.List;

/**
 * Immutable holder of the results of analyzing a student's course schedule.
 */
public class ScheduleAnalysis {
    private final int totalHours;
    private final int totalCredits;
    private final double hoursScore;       // 0–100, weighted 40%
    private final double creditsScore;     // 0–100, weighted 30%
    private final double difficultyScore;  // 0–100, weighted 30%
    private final double finalScore;       // weighted combination
    private final List<String> warnings;

    public ScheduleAnalysis(int totalHours, int totalCredits, double hoursScore,
            double creditsScore, double difficultyScore, double finalScore, List<String> warnings) {
        this.totalHours = totalHours;
        this.totalCredits = totalCredits;
        this.hoursScore = hoursScore;
        this.creditsScore = creditsScore;
        this.difficultyScore = difficultyScore;
        this.finalScore = finalScore;
        this.warnings = List.copyOf(warnings);
    }

    public static ScheduleAnalysis empty() {
        return new ScheduleAnalysis(0, 0, 0.0, 0.0, 0.0, 0.0, Collections.emptyList());
    }

    public int getTotalHours() { return totalHours; }
    public int getTotalCredits() { return totalCredits; }
    public double getHoursScore() { return hoursScore; }
    public double getCreditsScore() { return creditsScore; }
    public double getDifficultyScore() { return difficultyScore; }
    public double getFinalScore() { return finalScore; }
    public List<String> getWarnings() { return warnings; }
}
```

### `src/ValidationResult.java`
```java
import java.util.List;

/**
 * Immutable outcome of validating course input data.
 * If valid is true, the errors list is empty.
 */
public class ValidationResult {
    private final boolean valid;
    private final List<String> errors;

    public ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = List.copyOf(errors);
    }

    public boolean isValid() { return valid; }
    public List<String> getErrors() { return errors; }
}
```

### `src/ScheduleJsonStore.java`
```java
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles saving and loading schedule data to/from JSON files.
 * Parses JSON manually with regex to avoid external dependencies.
 */
public class ScheduleJsonStore {

    public void save(Path path, ScheduleModel model) throws IOException {
        // builds JSON text from model and writes it with Files.writeString(...)
    }

    public LoadedSchedule load(Path path) throws IOException {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        int maxCredits = extractInt(json, "maxCredits");
        String coursesArray = extractCoursesArray(json);
        List<Course> courses = new ArrayList<>();
        for (String object : splitTopLevelObjects(coursesArray)) {
            courses.add(parseCourseObject(object));
        }
        return new LoadedSchedule(maxCredits, courses);
    }

    private Course parseCourseObject(String objectJson) { /* ... returns new Course(...) */ return null; }
    private int extractInt(String text, String key) { return 0; }
    private double extractDouble(String text, String key) { return 0; }
    private String extractString(String text, String key) { return null; }
    private String extractCoursesArray(String json) { return null; }
    private List<String> splitTopLevelObjects(String arrayContent) { return null; }
    private String escape(String value) { return null; }
    private String unescape(String value) { return null; }

    /**
     * Immutable container for data loaded from a JSON file (nested static class).
     */
    public static class LoadedSchedule {
        private final int maxCredits;
        private final List<Course> courses;

        public LoadedSchedule(int maxCredits, List<Course> courses) {
            this.maxCredits = maxCredits;
            this.courses = List.copyOf(courses);
        }

        public int getMaxCredits() { return maxCredits; }
        public List<Course> getCourses() { return courses; }
    }
}
```
> Note: the bodies of the private regex helpers and `save(...)` above are elided for brevity,
> but their **signatures, visibility, parameters, and return types are exact** — use them as-is.

### `src/ScheduleAnalyzerSwingApp.java`
```java
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
// ... other java.awt / javax.swing imports

/**
 * Main Swing GUI window (the "view") for the Course Schedule Analyzer.
 * Follows a Model–Service–View pattern: ScheduleModel holds data,
 * ScheduleService computes analysis/validation, this class displays and handles input.
 */
public class ScheduleAnalyzerSwingApp extends JFrame {

    // Dark-theme color constants (all: private static final Color)
    private static final Color BG_DEEP, BG_PANEL, BG_CARD, BG_ROW_ALT, ACCENT, ACCENT_HOV,
            SUCCESS, WARNING, DANGER, TEXT_PRI, TEXT_SEC, BORDER_CLR;

    // Core application objects (all private final, created inline)
    private final ScheduleModel model = new ScheduleModel();
    private final ScheduleService service = new ScheduleService();
    private final ScheduleJsonStore jsonStore = new ScheduleJsonStore();

    // Top bar
    private JSpinner maxCreditsSpinner;

    // Course form
    private JTextField nameField;
    private JComboBox<Course.CourseType> typeCombo;
    private JSpinner hoursSpinner;
    private JSpinner creditsSpinner;
    private JSpinner difficultySpinner;
    private JLabel formErrorLabel;
    private JButton addOrUpdateButton;

    // Course table
    private JTable courseTable;
    private DefaultTableModel tableModel;

    // Analysis report labels
    private JLabel totalHoursValue;
    private JLabel totalCreditsValue;
    private JLabel hoursScoreValue;
    private JLabel creditsScoreValue;
    private JLabel difficultyScoreValue;
    private JLabel finalScoreValue;

    // Warnings display
    private DefaultListModel<String> warningsListModel;
    private JList<String> warningsList;

    private int editingIndex = -1;

    public ScheduleAnalyzerSwingApp() { /* builds UI, shows frame */ }

    // ── UI builders (all private) ──
    private void applyGlobalDefaults() {}
    private JPanel buildTopBar() { return null; }
    private JSplitPane buildMainContent() { return null; }
    private JPanel buildLeftPane() { return null; }
    private JPanel buildCourseForm() { return null; }
    private JPanel buildFormButtons() { return null; }
    private JLabel buildFormErrorLabel() { return null; }
    private JPanel buildRightPane() { return null; }
    private JPanel buildTableSection() { return null; }
    private JPanel buildReportPanel() { return null; }

    // ── Actions (all private) ──
    private void saveCourseFromForm() {}
    private void deleteSelectedCourse() {}
    private void refreshReport() {}
    private void resetForm() {}
    private void loadCourseIntoForm(Course course) {}
    private void saveSchedule() {}
    private void loadSchedule() {}

    // ── UI helper factories (all private) ──
    private JButton styledButton(String text, Color bg, java.awt.event.ActionListener action) { return null; }
    private JLabel styledValueLabel(String text) { return null; }
    private void styleTextField(JTextField field) {}
    private void styleComboBox(JComboBox<?> combo) {}
    private void styleSpinner(JSpinner spinner) {}

    public static void main(String[] args) {}
}
```
> Note: the method bodies above are elided, but **every field and method signature
> (name, visibility, static/final, parameter types, return type) is exact** — use them as-is.

### `legacy/schedule.java` (separate diagram — original console version)
```java
public class schedule {
    private Course[] courses;
    private int courseCount;
    private static final int MAX_HOURS = 30;
    private int maxCredits;
    private static final double SCORE_THRESHOLD = 80.0;

    public schedule() { /* courses = new Course[20]; courseCount = 0; maxCredits = 25; */ }

    public void setMaxCredits(int maxCredits) {}
    public int getMaxCredits() { return maxCredits; }
    public void addCourse(Course c) {}
    public int getTotalHours() { return 0; }
    public int getTotalCredits() { return 0; }
    public double computeScore() { return 0; }
    private double calculateDifficultyScore() { return 0; }
    public void printReport() {}
    public static void main(String[] args) {}
}
```
> This legacy class uses a fixed-size `Course[]` array (capacity 20) instead of a `List`,
> and prints its report to the console instead of using a GUI. It also depends on `Course`
> and `Course.CourseType`.
