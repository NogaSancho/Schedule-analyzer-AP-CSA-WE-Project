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
     *
     * @param name             the display name of the course
     * @param type             the course format (THEORY, LAB, or WORKSHOP)
     * @param hoursPerWeek     weekly time commitment (must be >= 1)
     * @param credits          credit value of the course (must be >= 1)
     * @param difficultyRating difficulty on a 0.0–10.0 scale
     */
    public Course(String name, CourseType type, int hoursPerWeek, int credits, double difficultyRating) {
        this.name = name;
        this.type = type;
        this.hoursPerWeek = hoursPerWeek;
        this.credits = credits;
        this.difficultyRating = difficultyRating;
    }

    /** Returns the course name. */
    public String getName() {
        return name;
    }

    /** Returns the course type (THEORY, LAB, or WORKSHOP). */
    public CourseType getType() {
        return type;
    }

    /** Returns the number of hours per week this course requires. */
    public int getHoursPerWeek() {
        return hoursPerWeek;
    }

    /** Returns the credit value of this course. */
    public int getCredits() {
        return credits;
    }

    /** Returns the difficulty rating (0.0 to 10.0). */
    public double getDifficultyRating() {
        return difficultyRating;
    }

    /** Returns a human-readable summary of this course. */
    @Override
    public String toString() {
        return String.format("%s [Type: %s, Hours: %d/week, Credits: %d, Difficulty: %.1f]",
                            name, type, hoursPerWeek, credits, difficultyRating);
    }
}