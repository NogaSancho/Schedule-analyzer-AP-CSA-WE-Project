public class Course {
    private String name;
    private CourseType type;
    private int hoursPerWeek;
    private int credits;
    private double difficultyRating;

    public enum CourseType {
    THEORY(1.0),
    LAB(1.4),
    WORKSHOP(1.2);

    private final double multiplier;

    CourseType(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return multiplier;
    }
}

    public Course(String name, CourseType type, int hoursPerWeek, int credits, double difficultyRating) {
        this.name = name;
        this.type = type;
        this.hoursPerWeek = hoursPerWeek;
        this.credits = credits;
        this.difficultyRating = difficultyRating;
    }
    
    public String getName() {
        return name;
    }

    public CourseType getType() {
        return type;
    }
    
    public int getHoursPerWeek() {
        return hoursPerWeek;
    }

    public int getCredits() {
        return credits;
    }

    public double getDifficultyRating() {
        return difficultyRating;
    }

    @Override
    public String toString() {
        return String.format("%s [Type: %s, Hours: %d/week, Credits: %d, Difficulty: %.1f]", 
                            name, type, hoursPerWeek, credits, difficultyRating);
    }
}