import java.util.ArrayList;
import java.util.List;

/**
 * Contains the business logic for the schedule analyzer.
 * Provides two main capabilities:
 *   1. Validating course input before it is added to the model
 *   2. Analyzing the full schedule to produce scores and warnings
 *
 * Scoring formula:
 *   finalScore = hoursScore * 0.40 + creditsScore * 0.30 + difficultyScore * 0.30
 * Each component is scored on a 0–100 scale. A higher final score means a
 * heavier workload.
 */
public class ScheduleService {
    /** The reference maximum for weekly hours (used to scale the hours score). */
    public static final int MAX_HOURS = 30;

    /** If the final score exceeds this value, a workload warning is generated. */
    public static final double SCORE_THRESHOLD = 80.0;

    /**
     * Validates raw course input fields and returns any errors found.
     *
     * @param name             course name (must not be blank)
     * @param type             course type (must not be null)
     * @param hoursPerWeek     weekly hours (must be >= 1)
     * @param credits          credit value (must be >= 1)
     * @param difficultyRating difficulty (must be 0.0–10.0)
     * @return a ValidationResult indicating pass/fail and any error messages
     */
    public ValidationResult validateCourseInput(
            String name,
            Course.CourseType type,
            int hoursPerWeek,
            int credits,
            double difficultyRating) {
        List<String> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add("Course name is required.");
        }
        if (type == null) {
            errors.add("Course type is required.");
        }
        if (hoursPerWeek < 1) {
            errors.add("Hours per week must be at least 1.");
        }
        if (credits < 1) {
            errors.add("Credits must be at least 1.");
        }
        if (difficultyRating < 0.0 || difficultyRating > 10.0) {
            errors.add("Difficulty rating must be between 0.0 and 10.0.");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Analyzes the schedule model and produces scores and warnings.
     *
     * The three component scores are:
     *   - hoursScore:      totalHours / MAX_HOURS * 100       (capped at 100)
     *   - creditsScore:    totalCredits / maxCredits * 100    (capped at 100)
     *   - difficultyScore: weighted difficulty sum normalized  (capped at 100)
     *
     * Warnings are generated when:
     *   - Total credits exceed the model's max credit limit
     *   - A LAB course has difficulty >= 4.0
     *   - The final score exceeds SCORE_THRESHOLD
     *
     * @param model the schedule model containing courses and credit limit
     * @return a ScheduleAnalysis with all computed scores and warnings
     */
    public ScheduleAnalysis analyze(ScheduleModel model) {
        List<Course> courses = model.getCoursesSnapshot();

        // Accumulate totals across all courses
        int totalHours = 0;
        int totalCredits = 0;
        double weightedDifficulty = 0.0;

        for (Course course : courses) {
            totalHours += course.getHoursPerWeek();
            totalCredits += course.getCredits();
            // Each course's difficulty is scaled by its type multiplier
            weightedDifficulty += course.getDifficultyRating() * course.getType().getMultiplier();
        }

        // Compute individual component scores (each 0–100)
        double hoursScore = cap((double) totalHours / MAX_HOURS * 100.0);
        double creditsScore = cap((double) totalCredits / model.getMaxCredits() * 100.0);
        // Normalize against the maximum possible single-course difficulty (10.0 * 1.4 for LAB)
        double difficultyScore = cap((weightedDifficulty / (10.0 * 1.4)) * 100.0);

        // Weighted combination: 40% hours, 30% credits, 30% difficulty
        double finalScore = (hoursScore * 0.4) + (creditsScore * 0.3) + (difficultyScore * 0.3);

        // Build warning messages
        List<String> warnings = new ArrayList<>();

        if (totalCredits > model.getMaxCredits()) {
            warnings.add(String.format(
                    "CREDIT LIMIT WARNING: Total credits (%d) exceed maximum allowed (%d)",
                    totalCredits,
                    model.getMaxCredits()));
        }

        for (Course course : courses) {
            if (course.getType() == Course.CourseType.LAB && course.getDifficultyRating() >= 4.0) {
                warnings.add(String.format(
                        "LAB COURSE WARNING: '%s' has high difficulty (%.1f)",
                        course.getName(),
                        course.getDifficultyRating()));
            }
        }

        if (finalScore > SCORE_THRESHOLD) {
            warnings.add(String.format(
                    "WORKLOAD WARNING: Final score (%.2f) exceeds threshold (%.2f). Consider balancing your schedule.",
                    finalScore,
                    SCORE_THRESHOLD));
        }

        return new ScheduleAnalysis(
                totalHours,
                totalCredits,
                hoursScore,
                creditsScore,
                difficultyScore,
                finalScore,
                warnings);
    }

    /** Caps a value at 100.0 so component scores never exceed the maximum. */
    private double cap(double value) {
        return Math.min(value, 100.0);
    }
}
