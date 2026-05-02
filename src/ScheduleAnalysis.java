import java.util.Collections;
import java.util.List;

public class ScheduleAnalysis {
    private final int totalHours;
    private final int totalCredits;
    private final double hoursScore;
    private final double creditsScore;
    private final double difficultyScore;
    private final double finalScore;
    private final List<String> warnings;

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

    public static ScheduleAnalysis empty() {
        return new ScheduleAnalysis(0, 0, 0.0, 0.0, 0.0, 0.0, Collections.emptyList());
    }

    public int getTotalHours() {
        return totalHours;
    }

    public int getTotalCredits() {
        return totalCredits;
    }

    public double getHoursScore() {
        return hoursScore;
    }

    public double getCreditsScore() {
        return creditsScore;
    }

    public double getDifficultyScore() {
        return difficultyScore;
    }

    public double getFinalScore() {
        return finalScore;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
