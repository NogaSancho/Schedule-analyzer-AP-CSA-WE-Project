import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores the current list of courses and the maximum credit limit.
 * Acts as the central data model for the schedule analyzer —
 * the UI reads from it and the service layer analyzes it.
 */
public class ScheduleModel {
    private final List<Course> courses = new ArrayList<>();
    private int maxCredits = 25;  // default credit cap

    /**
     * Returns a read-only view of the current course list.
     * Changes to the model are not reflected in previously returned lists.
     */
    public List<Course> getCoursesSnapshot() {
        return Collections.unmodifiableList(courses);
    }

    /** Returns the maximum number of credits allowed in the schedule. */
    public int getMaxCredits() {
        return maxCredits;
    }

    /**
     * Sets the maximum credit limit.
     *
     * @param maxCredits the new limit (must be at least 1)
     * @throws IllegalArgumentException if maxCredits is less than 1
     */
    public void setMaxCredits(int maxCredits) {
        if (maxCredits < 1) {
            throw new IllegalArgumentException("Max credits must be at least 1.");
        }
        this.maxCredits = maxCredits;
    }

    /** Adds a course to the end of the schedule. */
    public void addCourse(Course course) {
        courses.add(course);
    }

    /** Replaces the course at the given index with a new course. */
    public void updateCourse(int index, Course course) {
        courses.set(index, course);
    }

    /** Removes the course at the given index. */
    public void removeCourse(int index) {
        courses.remove(index);
    }

    /** Removes all courses from the schedule. */
    public void clearCourses() {
        courses.clear();
    }
}
