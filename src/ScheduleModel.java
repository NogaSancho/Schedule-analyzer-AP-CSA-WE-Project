import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScheduleModel {
    private final List<Course> courses = new ArrayList<>();
    private int maxCredits = 25;

    public List<Course> getCoursesSnapshot() {
        return Collections.unmodifiableList(courses);
    }

    public int getMaxCredits() {
        return maxCredits;
    }

    public void setMaxCredits(int maxCredits) {
        if (maxCredits < 1) {
            throw new IllegalArgumentException("Max credits must be at least 1.");
        }
        this.maxCredits = maxCredits;
    }

    public void addCourse(Course course) {
        courses.add(course);
    }

    public void updateCourse(int index, Course course) {
        courses.set(index, course);
    }

    public void removeCourse(int index) {
        courses.remove(index);
    }

    public void clearCourses() {
        courses.clear();
    }
}
