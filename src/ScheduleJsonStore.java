import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleJsonStore {
    public void save(Path path, ScheduleModel model) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"maxCredits\": ").append(model.getMaxCredits()).append(",\n");
        builder.append("  \"courses\": [\n");

        List<Course> courses = model.getCoursesSnapshot();
        for (int i = 0; i < courses.size(); i++) {
            Course course = courses.get(i);
            builder.append("    {\n");
            builder.append("      \"name\": \"").append(escape(course.getName())).append("\",\n");
            builder.append("      \"type\": \"").append(course.getType().name()).append("\",\n");
            builder.append("      \"hoursPerWeek\": ").append(course.getHoursPerWeek()).append(",\n");
            builder.append("      \"credits\": ").append(course.getCredits()).append(",\n");
            builder.append("      \"difficultyRating\": ").append(course.getDifficultyRating()).append("\n");
            builder.append("    }");
            if (i < courses.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }

        builder.append("  ]\n");
        builder.append("}\n");

        Files.writeString(path, builder.toString(), StandardCharsets.UTF_8);
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

    private Course parseCourseObject(String objectJson) {
        String name = unescape(extractString(objectJson, "name"));
        String typeText = extractString(objectJson, "type");
        int hoursPerWeek = extractInt(objectJson, "hoursPerWeek");
        int credits = extractInt(objectJson, "credits");
        double difficultyRating = extractDouble(objectJson, "difficultyRating");

        return new Course(
                name,
                Course.CourseType.valueOf(typeText),
                hoursPerWeek,
                credits,
                difficultyRating);
    }

    private int extractInt(String text, String key) {
        Pattern pattern = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*(-?\\d+)");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing integer key: " + key);
        }
        return Integer.parseInt(matcher.group(1));
    }

    private double extractDouble(String text, String key) {
        Pattern pattern = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing decimal key: " + key);
        }
        return Double.parseDouble(matcher.group(1));
    }

    private String extractString(String text, String key) {
        Pattern pattern = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\\\"])*)\\\"");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing string key: " + key);
        }
        return matcher.group(1);
    }

    private String extractCoursesArray(String json) {
        Pattern pattern = Pattern.compile("\\\"courses\\\"\\s*:\\s*\\[(.*)]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing courses array.");
        }
        return matcher.group(1);
    }

    private List<String> splitTopLevelObjects(String arrayContent) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;
        boolean inString = false;
        boolean escaping = false;

        for (int i = 0; i < arrayContent.length(); i++) {
            char ch = arrayContent.charAt(i);

            if (inString) {
                if (escaping) {
                    escaping = false;
                } else if (ch == '\\') {
                    escaping = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }

            if (ch == '"') {
                inString = true;
                continue;
            }

            if (ch == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    objects.add(arrayContent.substring(start, i + 1));
                    start = -1;
                }
            }
        }

        return objects;
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescape(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaping = false;

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (!escaping) {
                if (ch == '\\') {
                    escaping = true;
                } else {
                    result.append(ch);
                }
                continue;
            }

            if (ch == 'n') {
                result.append('\n');
            } else if (ch == 'r') {
                result.append('\r');
            } else if (ch == 't') {
                result.append('\t');
            } else if (ch == '"') {
                result.append('"');
            } else if (ch == '\\') {
                result.append('\\');
            } else {
                result.append(ch);
            }
            escaping = false;
        }

        return result.toString();
    }

    public static class LoadedSchedule {
        private final int maxCredits;
        private final List<Course> courses;

        public LoadedSchedule(int maxCredits, List<Course> courses) {
            this.maxCredits = maxCredits;
            this.courses = List.copyOf(courses);
        }

        public int getMaxCredits() {
            return maxCredits;
        }

        public List<Course> getCourses() {
            return courses;
        }
    }
}
