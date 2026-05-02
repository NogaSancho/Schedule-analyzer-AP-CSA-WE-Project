import java.util.ArrayList;
import java.util.List;

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

    private double cap(double value) {
        return Math.min(value, 100.0);
    }
}
