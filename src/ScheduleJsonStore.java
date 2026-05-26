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
 *
 * NOTE: This class uses java.nio.file (Path, Files) and java.util.regex
 * (Pattern, Matcher), which are beyond the AP CSA curriculum. A JSON
 * library (like Gson) would simplify this, but we parse manually here
 * to avoid external dependencies.
 *
 * JSON format produced/consumed:
 * {
 *   "maxCredits": 25,
 *   "courses": [
 *     { "name": "...", "type": "THEORY", "hoursPerWeek": 3, "credits": 3, "difficultyRating": 5.0 }
 *   ]
 * }
 */
public class ScheduleJsonStore {

    /**
     * Writes the schedule model to a JSON file at the given path.
     *
     * @param path  the file path to write to
     * @param model the schedule model to serialize
     * @throws IOException if the file cannot be written
     */
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

    /**
     * Reads a JSON file and reconstructs the schedule data.
     *
     * @param path the file path to read from
     * @return a LoadedSchedule containing the max credits and course list
     * @throws IOException              if the file cannot be read
     * @throws IllegalArgumentException if the JSON structure is invalid
     */
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

    /** Parses a single JSON object string into a Course. */
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

    // ── Regex-based JSON value extractors ───────────────────────────────────
    // These use Pattern and Matcher to find "key": value pairs in raw JSON text.

    /** Extracts an integer value for the given key from a JSON string. */
    private int extractInt(String text, String key) {
        Pattern pattern = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*(-?\\d+)");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing integer key: " + key);
        }
        return Integer.parseInt(matcher.group(1));
    }

    /** Extracts a double value for the given key from a JSON string. */
    private double extractDouble(String text, String key) {
        Pattern pattern = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing decimal key: " + key);
        }
        return Double.parseDouble(matcher.group(1));
    }

    /** Extracts a quoted string value for the given key from a JSON string. */
    private String extractString(String text, String key) {
        Pattern pattern = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\\\"])*)\\\"");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing string key: " + key);
        }
        return matcher.group(1);
    }

    /** Extracts the contents of the "courses" JSON array. */
    private String extractCoursesArray(String json) {
        Pattern pattern = Pattern.compile("\\\"courses\\\"\\s*:\\s*\\[(.*)]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing courses array.");
        }
        return matcher.group(1);
    }

    /**
     * Splits a JSON array's inner text into individual object strings.
     * Tracks brace depth to correctly handle nested structures and
     * quoted strings that may contain braces.
     */
    private List<String> splitTopLevelObjects(String arrayContent) {
        List<String> objects = new ArrayList<>();
        int depth = 0;      // tracks nested brace depth
        int start = -1;     // index where the current object started
        boolean inString = false;
        boolean escaping = false;

        for (int i = 0; i < arrayContent.length(); i++) {
            char ch = arrayContent.charAt(i);

            // Inside a quoted string — only watch for end-quote or escape sequences
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

            // Track object boundaries by brace depth
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

    // ── String escape/unescape helpers ──────────────────────────────────────

    /** Escapes special characters for safe inclusion in a JSON string value. */
    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /** Converts JSON escape sequences (like \\n) back to actual characters. */
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

            // Map escape codes to their actual characters
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

    /**
     * Immutable container for data loaded from a JSON file.
     * Holds the max credit limit and the list of courses that were saved.
     */
    public static class LoadedSchedule {
        private final int maxCredits;
        private final List<Course> courses;

        /**
         * Creates a loaded schedule result.
         *
         * @param maxCredits the credit limit from the file
         * @param courses    the list of courses from the file
         */
        public LoadedSchedule(int maxCredits, List<Course> courses) {
            this.maxCredits = maxCredits;
            this.courses = List.copyOf(courses);
        }

        /** Returns the max credit limit that was saved. */
        public int getMaxCredits() {
            return maxCredits;
        }

        /** Returns an unmodifiable list of courses that were saved. */
        public List<Course> getCourses() {
            return courses;
        }
    }
}
