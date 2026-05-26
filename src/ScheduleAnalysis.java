import java.util.Collections;
import java.util.List;

/**
 * Holds the results of analyzing a student's course schedule.
 * Contains totals, individual component scores, a weighted final score,
 * and any warnings about schedule balance.
 *
 * This object is immutable — once created, its values cannot change.
 */
public class ScheduleAnalysis {
    private final int totalHours;
    private final int totalCredits;
    private final double hoursScore;       // 0–100, weighted 40% in final score
    private final double creditsScore;     // 0–100, weighted 30% in final score
    private final double difficultyScore;  // 0–100, weighted 30% in final score
    private final double finalScore;       // weighted combination of all three scores
    private final List<String> warnings;

    /**
     * Constructs an analysis result with the given scores and warnings.
     *
     * @param totalHours      sum of weekly hours across all courses
     * @param totalCredits    sum of credits across all courses
     * @param hoursScore      hours component score (0–100)
     * @param creditsScore    credits component score (0–100)
     * @param difficultyScore difficulty component score (0–100)
     * @param finalScore      weighted final score
     * @param warnings        list of warning messages (may be empty)
     */
    public ScheduleAnalysis(
            int totalHours,
            int totalCredits,
            double hoursScore,
            double creditsScore,
            double difficultyScore,
            double finalScore,
            List<String> warnings) {
        this.totalHours = totalHours;
        this.totalCredits = totalCredits;
        this.hoursScore = hoursScore;
        this.creditsScore = creditsScore;
        this.difficultyScore = difficultyScore;
        this.finalScore = finalScore;
        this.warnings = List.copyOf(warnings);
    }

    /** Returns an analysis with all values at zero and no warnings. */
    public static ScheduleAnalysis empty() {
        return new ScheduleAnalysis(0, 0, 0.0, 0.0, 0.0, 0.0, Collections.emptyList());
    }

    /** Returns the total weekly hours across all courses. */
    public int getTotalHours() {
        return totalHours;
    }

    /** Returns the total credits across all courses. */
    public int getTotalCredits() {
        return totalCredits;
    }

    /** Returns the hours component score (0–100). */
    public double getHoursScore() {
        return hoursScore;
    }

    /** Returns the credits component score (0–100). */
    public double getCreditsScore() {
        return creditsScore;
    }

    /** Returns the difficulty component score (0–100). */
    public double getDifficultyScore() {
        return difficultyScore;
    }

    /** Returns the weighted final score combining all three components. */
    public double getFinalScore() {
        return finalScore;
    }

    /** Returns an unmodifiable list of warning messages about schedule balance. */
    public List<String> getWarnings() {
        return warnings;
    }
}
