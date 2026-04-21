public class schedule {
    private Course[] courses;
    private int courseCount;
    private static final int MAX_HOURS = 30;
    private int maxCredits;
    private static final double SCORE_THRESHOLD = 80.0;

    public schedule() {
        this.courses = new Course[20]; // Default capacity
        this.courseCount = 0;
        this.maxCredits = 25; // Default max credits
    }

    public void setMaxCredits(int maxCredits) {
        this.maxCredits = maxCredits;
    }

    public int getMaxCredits() {
        return maxCredits;
    }

    public void addCourse(Course c) {
        if (courseCount < courses.length) {
            courses[courseCount] = c;
            courseCount++;
        } else {
            System.out.println("Schedule is full. Cannot add more courses.");
        }
    }

    public int getTotalHours() {
        int totalHours = 0;
        for (int i = 0; i < courseCount; i++) {
            totalHours += courses[i].getHoursPerWeek();
        }
        return totalHours;
    }

    public int getTotalCredits() {
        int totalCredits = 0;
        for (int i = 0; i < courseCount; i++) {
            totalCredits += courses[i].getCredits();
        }
        return totalCredits;
    }

    public double computeScore() {
        int totalHours = getTotalHours();
        int totalCredits = getTotalCredits();
        double difficultyScore = calculateDifficultyScore();

        // Calculate individual scores (0-100 scale)
        double hoursScore = (double) totalHours / MAX_HOURS * 100;
        double creditsScore = (double) totalCredits / maxCredits * 100;

        // Cap scores at 100
        hoursScore = Math.min(hoursScore, 100);
        creditsScore = Math.min(creditsScore, 100);
        difficultyScore = Math.min(difficultyScore, 100);

        // Calculate final score with weights: 40% hours, 30% credits, 30% difficulty
        double finalScore = (hoursScore * 0.4) + (creditsScore * 0.3) + (difficultyScore * 0.3);

        return finalScore;
    }

    private double calculateDifficultyScore() {
        double weightedDifficulty = 0.0;
        for (int i = 0; i < courseCount; i++) {
            double difficulty = courses[i].getDifficultyRating();
            double multiplier = courses[i].getType().getMultiplier();
            weightedDifficulty += difficulty * multiplier;
        }
        // Normalize to 0-100 scale (assuming max difficulty ~10 with max multiplier 1.4)
        return (weightedDifficulty / (10 * 1.4)) * 100;
    }

    public void printReport() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("COURSE SCHEDULE REPORT");
        System.out.println("=".repeat(70));

        // Print course list
        System.out.println("\nCOURSES:");
        System.out.println("-".repeat(70));
        for (int i = 0; i < courseCount; i++) {
            System.out.println((i + 1) + ". " + courses[i].toString());
        }

        // Calculate metrics
        int totalHours = getTotalHours();
        int totalCredits = getTotalCredits();
        double hoursScore = (double) totalHours / MAX_HOURS * 100;
        double creditsScore = (double) totalCredits / maxCredits * 100;
        double difficultyScore = calculateDifficultyScore();
        double finalScore = computeScore();

        // Cap scores for display
        hoursScore = Math.min(hoursScore, 100);
        creditsScore = Math.min(creditsScore, 100);
        difficultyScore = Math.min(difficultyScore, 100);

        // Print totals
        System.out.println("\n" + "-".repeat(70));
        System.out.println("TOTALS:");
        System.out.println("-".repeat(70));
        System.out.printf("Total Hours/Week: %d hours (MAX: %d)%n", totalHours, MAX_HOURS);
        System.out.printf("Total Credits: %d credits (MAX: %d)%n", totalCredits, maxCredits);

        // Print score breakdown
        System.out.println("\n" + "-".repeat(70));
        System.out.println("SCORE BREAKDOWN:");
        System.out.println("-".repeat(70));
        System.out.printf("Hours Score (40%% weight): %.2f / 100%n", hoursScore);
        System.out.printf("Credits Score (30%% weight): %.2f / 100%n", creditsScore);
        System.out.printf("Difficulty Score (30%% weight): %.2f / 100%n", difficultyScore);
        System.out.printf("%nFinal Score: %.2f / 100%n", finalScore);

        // Print warnings
        System.out.println("\n" + "-".repeat(70));
        System.out.println("WARNINGS:");
        System.out.println("-".repeat(70));

        boolean hasWarnings = false;

        // Check if credits exceed maximum
        if (totalCredits > maxCredits) {
            System.out.printf("⚠ CREDIT LIMIT WARNING: Total credits (%d) exceed maximum allowed (%d)%n",
                            totalCredits, maxCredits);
            hasWarnings = true;
        }

        // Check for LAB courses with difficulty >= 4
        for (int i = 0; i < courseCount; i++) {
            if (courses[i].getType() == Course.CourseType.LAB 
                    && courses[i].getDifficultyRating() >= 4.0) {
                System.out.printf("⚠ LAB COURSE WARNING: '%s' has high difficulty (%.1f)%n",
                                courses[i].getName(), courses[i].getDifficultyRating());
                hasWarnings = true;
            }
        }

        // Check if finalScore exceeds threshold
        if (finalScore > SCORE_THRESHOLD) {
            System.out.printf("⚠ WORKLOAD WARNING: Final score (%.2f) exceeds threshold (%.2f)%n",
                            finalScore, SCORE_THRESHOLD);
            System.out.println("If you exceed the threshold, it means that your current schedule may be too demanding. Consider changing some courses to balance your workload.");
            hasWarnings = true;
        }

        if (!hasWarnings) {
            System.out.println("✓ No warnings. Schedule looks balanced.");
        }

        System.out.println("=".repeat(70) + "\n");
    }

    public static void main(String[] args) {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        schedule mySchedule = new schedule();

        // Get maximum credits from user
        System.out.println("===== COURSE SCHEDULE PLANNER =====\n");
        System.out.print("Enter maximum credits allowed (default 25): ");
        if (scanner.hasNextInt()) {
            int maxCredits = scanner.nextInt();
            scanner.nextLine(); // Consume newline
            mySchedule.setMaxCredits(maxCredits);
            System.out.println("Maximum credits set to: " + maxCredits + "\n");
        } else {
            scanner.nextLine(); // Clear invalid input
            mySchedule.setMaxCredits(25);
            System.out.println("Using default maximum credits: 25\n");
        }

        // Add courses loop
        boolean addingCourses = true;
        while (addingCourses) {
            System.out.println("--- Add Course ---");
            
            // Get course name
            System.out.print("Enter course name: ");
            String courseName = scanner.nextLine().trim();
            
            if (courseName.isEmpty()) {
                System.out.println("Course name cannot be empty. Try again.\n");
                continue;
            }

            // Get course type
            System.out.println("Select course type:");
            System.out.println("1. THEORY (multiplier: 1.0)");
            System.out.println("2. LAB (multiplier: 1.4)");
            System.out.println("3. WORKSHOP (multiplier: 1.2)");
            System.out.print("Enter choice (1-3): ");
            
            Course.CourseType courseType = Course.CourseType.THEORY;
            if (scanner.hasNextInt()) {
                int typeChoice = scanner.nextInt();
                scanner.nextLine(); // Consume newline
                
                if (typeChoice == 1) {
                    courseType = Course.CourseType.THEORY;
                } else if (typeChoice == 2) {
                    courseType = Course.CourseType.LAB;
                } else if (typeChoice == 3) {
                    courseType = Course.CourseType.WORKSHOP;
                } else {
                    System.out.println("Invalid choice. Using THEORY.\n");
                    courseType = Course.CourseType.THEORY;
                }
            } else {
                scanner.nextLine(); // Clear invalid input
                System.out.println("Invalid input. Using THEORY.\n");
            }

            // Get hours per week
            System.out.print("Enter hours per week: ");
            int hoursPerWeek = 0;
            if (scanner.hasNextInt()) {
                hoursPerWeek = scanner.nextInt();
                scanner.nextLine(); // Consume newline
            } else {
                scanner.nextLine(); // Clear invalid input
                System.out.println("Invalid input. Using 0 hours.\n");
            }

            // Get credits
            System.out.print("Enter credits: ");
            int credits = 0;
            if (scanner.hasNextInt()) {
                credits = scanner.nextInt();
                scanner.nextLine(); // Consume newline
            } else {
                scanner.nextLine(); // Clear invalid input
                System.out.println("Invalid input. Using 0 credits.\n");
            }

            // Get difficulty rating
            System.out.print("Enter difficulty rating (0.0-10.0): ");
            double difficultyRating = 0.0;
            if (scanner.hasNextDouble()) {
                difficultyRating = scanner.nextDouble();
                scanner.nextLine(); // Consume newline
                if (difficultyRating < 0 || difficultyRating > 10) {
                    System.out.println("Invalid rating. Using 0.0.\n");
                    difficultyRating = 0.0;
                }
            } else {
                scanner.nextLine(); // Clear invalid input
                System.out.println("Invalid input. Using 0.0.\n");
            }

            // Create and add course
            Course newCourse = new Course(courseName, courseType, hoursPerWeek, credits, difficultyRating);
            mySchedule.addCourse(newCourse);
            System.out.println("✓ Course added successfully!\n");

            // Ask if user wants to add more courses
            System.out.print("Add another course? (yes/no): ");
            String response = scanner.nextLine().trim().toLowerCase();
            if (!response.startsWith("y")) {
                addingCourses = false;
            }
            System.out.println();
        }

        // Print the report
        System.out.println();
        mySchedule.printReport();

        scanner.close();
    }
}