import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;

public class ScheduleAnalyzerSwingApp extends JFrame {

    private final ScheduleModel model = new ScheduleModel();
    private final ScheduleService service = new ScheduleService();
    private final ScheduleJsonStore jsonStore = new ScheduleJsonStore();

    // Top bar
    private JSpinner maxCreditsSpinner;

    // Course form
    private JTextField nameField;
    private JComboBox<Course.CourseType> typeCombo;
    private JSpinner hoursSpinner;
    private JSpinner creditsSpinner;
    private JSpinner difficultySpinner;
    private JLabel formErrorLabel;
    private JButton addOrUpdateButton;

    // Table
    private JTable courseTable;
    private DefaultTableModel tableModel;

    // Report labels
    private JLabel totalHoursValue;
    private JLabel totalCreditsValue;
    private JLabel hoursScoreValue;
    private JLabel creditsScoreValue;
    private JLabel difficultyScoreValue;
    private JLabel finalScoreValue;

    // Warnings list
    private DefaultListModel<String> warningsListModel;
    private JList<String> warningsList;

    private int editingIndex = -1;

    public ScheduleAnalyzerSwingApp() {
        super("Course Schedule Analyzer - Swing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 720);
        setMinimumSize(new Dimension(900, 600));

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        root.add(buildTopBar(), BorderLayout.NORTH);
        root.add(buildMainContent(), BorderLayout.CENTER);

        setContentPane(root);
        refreshReport();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // -------------------------------------------------------------------------
    // Top bar
    // -------------------------------------------------------------------------

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setBorder(new EmptyBorder(0, 0, 8, 0));

        bar.add(new JLabel("Max Credits:"));
        maxCreditsSpinner = new JSpinner(new SpinnerNumberModel(model.getMaxCredits(), 1, 60, 1));
        maxCreditsSpinner.setPreferredSize(new Dimension(70, 26));
        maxCreditsSpinner.addChangeListener(e -> {
            int value = (int) maxCreditsSpinner.getValue();
            model.setMaxCredits(value);
            refreshReport();
        });
        bar.add(maxCreditsSpinner);

        JButton saveButton = new JButton("Save JSON");
        saveButton.addActionListener(e -> saveSchedule());
        bar.add(saveButton);

        JButton loadButton = new JButton("Load JSON");
        loadButton.addActionListener(e -> loadSchedule());
        bar.add(loadButton);

        return bar;
    }

    // -------------------------------------------------------------------------
    // Main content: left form + right table/report
    // -------------------------------------------------------------------------

    private JSplitPane buildMainContent() {
        JPanel leftPane = buildLeftPane();
        JPanel rightPane = buildRightPane();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPane, rightPane);
        split.setDividerLocation(360);
        split.setResizeWeight(0.0);
        split.setBorder(null);
        return split;
    }

    // -------------------------------------------------------------------------
    // Left pane: form
    // -------------------------------------------------------------------------

    private JPanel buildLeftPane() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(0xF6F8FC));
        panel.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD9DEE8), 1, true),
                new EmptyBorder(12, 12, 12, 12)));

        panel.add(buildCourseForm());
        panel.add(Box.createVerticalStrut(10));
        panel.add(buildFormButtons());
        panel.add(Box.createVerticalStrut(6));
        panel.add(buildFormErrorLabel());
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel buildCourseForm() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints labelC = new GridBagConstraints();
        labelC.anchor = GridBagConstraints.WEST;
        labelC.insets = new Insets(4, 0, 4, 8);
        labelC.gridx = 0;

        GridBagConstraints fieldC = new GridBagConstraints();
        fieldC.fill = GridBagConstraints.HORIZONTAL;
        fieldC.weightx = 1.0;
        fieldC.insets = new Insets(4, 0, 4, 0);
        fieldC.gridx = 1;

        nameField = new JTextField();
        nameField.setToolTipText("Course name");

        typeCombo = new JComboBox<>(Course.CourseType.values());
        typeCombo.setSelectedItem(Course.CourseType.THEORY);

        hoursSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 40, 1));
        creditsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 12, 1));
        difficultySpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.0, 10.0, 0.1));
        JSpinner.NumberEditor diffEditor = new JSpinner.NumberEditor(difficultySpinner, "0.0");
        difficultySpinner.setEditor(diffEditor);

        String[] labels = {"Course Name", "Course Type", "Hours / Week", "Credits", "Difficulty (0-10)"};
        JComponent[] fields = {nameField, typeCombo, hoursSpinner, creditsSpinner, difficultySpinner};

        for (int i = 0; i < labels.length; i++) {
            labelC.gridy = i;
            fieldC.gridy = i;
            grid.add(new JLabel(labels[i]), labelC);
            grid.add(fields[i], fieldC);
        }

        return grid;
    }

    private JPanel buildFormButtons() {
        addOrUpdateButton = new JButton("Add Course");
        addOrUpdateButton.addActionListener(e -> saveCourseFromForm());

        JButton clearButton = new JButton("Clear Form");
        clearButton.addActionListener(e -> resetForm());

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.add(addOrUpdateButton);
        row.add(clearButton);
        return row;
    }

    private JLabel buildFormErrorLabel() {
        formErrorLabel = new JLabel(" ");
        formErrorLabel.setForeground(new Color(0x9F1239));
        formErrorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return formErrorLabel;
    }

    // -------------------------------------------------------------------------
    // Right pane: table + report
    // -------------------------------------------------------------------------

    private JPanel buildRightPane() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(new EmptyBorder(0, 10, 0, 0));

        JPanel tableSection = buildTableSection();
        JPanel reportPanel = buildReportPanel();

        JSplitPane vertSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableSection, reportPanel);
        vertSplit.setResizeWeight(0.55);
        vertSplit.setBorder(null);

        panel.add(vertSplit, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTableSection() {
        String[] columns = {"Name", "Type", "Hours/Week", "Credits", "Difficulty"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        courseTable = new JTable(tableModel);
        courseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        courseTable.setFillsViewportHeight(true);
        courseTable.getTableHeader().setReorderingAllowed(false);

        courseTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = courseTable.getSelectedRow();
            if (row < 0) return;
            List<Course> courses = model.getCoursesSnapshot();
            if (row < courses.size()) {
                editingIndex = row;
                loadCourseIntoForm(courses.get(row));
            }
        });

        JScrollPane scroll = new JScrollPane(courseTable);

        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.setToolTipText("Delete the currently selected course from the table.");
        deleteButton.addActionListener(e -> deleteSelectedCourse());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonRow.add(deleteButton);

        JPanel section = new JPanel(new BorderLayout(0, 6));
        section.add(new JLabel("Courses"), BorderLayout.NORTH);
        section.add(scroll, BorderLayout.CENTER);
        section.add(buttonRow, BorderLayout.SOUTH);
        return section;
    }

    private JPanel buildReportPanel() {
        totalHoursValue = new JLabel("0");
        totalCreditsValue = new JLabel("0");
        hoursScoreValue = new JLabel("0.00 / 100");
        creditsScoreValue = new JLabel("0.00 / 100");
        difficultyScoreValue = new JLabel("0.00 / 100");
        finalScoreValue = new JLabel("0.00 / 100");

        JPanel stats = new JPanel(new GridBagLayout());
        stats.setOpaque(false);
        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(2, 0, 2, 10);
        lc.gridx = 0;

        GridBagConstraints vc = new GridBagConstraints();
        vc.anchor = GridBagConstraints.WEST;
        vc.weightx = 1.0;
        vc.insets = new Insets(2, 0, 2, 0);
        vc.gridx = 1;

        String[] statLabels = {
            "Total Hours / Week:", "Total Credits:",
            "Hours Score (40%):", "Credits Score (30%):",
            "Difficulty Score (30%):", "Final Score:"
        };
        JLabel[] statValues = {
            totalHoursValue, totalCreditsValue,
            hoursScoreValue, creditsScoreValue,
            difficultyScoreValue, finalScoreValue
        };
        for (int i = 0; i < statLabels.length; i++) {
            lc.gridy = i;
            vc.gridy = i;
            stats.add(new JLabel(statLabels[i]), lc);
            stats.add(statValues[i], vc);
        }

        warningsListModel = new DefaultListModel<>();
        warningsList = new JList<>(warningsListModel);
        warningsList.setVisibleRowCount(5);
        JScrollPane warningsScroll = new JScrollPane(warningsList);

        JPanel report = new JPanel(new BorderLayout(0, 8));
        report.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD7D7D7), 1, true),
                new EmptyBorder(10, 10, 10, 10)));
        report.setBackground(new Color(0xF9F9F9));

        JPanel inner = new JPanel(new BorderLayout(0, 6));
        inner.setOpaque(false);
        inner.add(new JLabel("Live Analysis"), BorderLayout.NORTH);
        inner.add(stats, BorderLayout.CENTER);

        JPanel warningsSection = new JPanel(new BorderLayout(0, 4));
        warningsSection.setOpaque(false);
        warningsSection.add(new JLabel("Warnings"), BorderLayout.NORTH);
        warningsSection.add(warningsScroll, BorderLayout.CENTER);

        report.add(inner, BorderLayout.NORTH);
        report.add(warningsSection, BorderLayout.CENTER);

        return report;
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    private void saveCourseFromForm() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        Course.CourseType type = (Course.CourseType) typeCombo.getSelectedItem();
        int hours = (int) hoursSpinner.getValue();
        int credits = (int) creditsSpinner.getValue();
        double difficulty = ((Number) difficultySpinner.getValue()).doubleValue();

        ValidationResult validation = service.validateCourseInput(name, type, hours, credits, difficulty);
        if (!validation.isValid()) {
            formErrorLabel.setText("<html>" + String.join("<br>", validation.getErrors()) + "</html>");
            return;
        }

        Course course = new Course(name, type, hours, credits, difficulty);
        if (editingIndex >= 0) {
            model.updateCourse(editingIndex, course);
        } else {
            model.addCourse(course);
        }

        resetForm();
        refreshReport();
    }

    private void deleteSelectedCourse() {
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a course to delete.",
                    "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        model.removeCourse(selectedRow);
        resetForm();
        refreshReport();
    }

    private void refreshReport() {
        // Rebuild table rows
        tableModel.setRowCount(0);
        for (Course c : model.getCoursesSnapshot()) {
            tableModel.addRow(new Object[]{
                c.getName(),
                c.getType(),
                c.getHoursPerWeek(),
                c.getCredits(),
                String.format("%.1f", c.getDifficultyRating())
            });
        }

        ScheduleAnalysis analysis = service.analyze(model);
        totalHoursValue.setText(String.valueOf(analysis.getTotalHours()));
        totalCreditsValue.setText(String.valueOf(analysis.getTotalCredits()));
        hoursScoreValue.setText(String.format("%.2f / 100", analysis.getHoursScore()));
        creditsScoreValue.setText(String.format("%.2f / 100", analysis.getCreditsScore()));
        difficultyScoreValue.setText(String.format("%.2f / 100", analysis.getDifficultyScore()));
        finalScoreValue.setText(String.format("%.2f / 100", analysis.getFinalScore()));

        warningsListModel.clear();
        List<String> warnings = analysis.getWarnings();
        if (warnings.isEmpty()) {
            warningsListModel.addElement("No warnings. Schedule looks balanced.");
        } else {
            for (String w : warnings) {
                warningsListModel.addElement(w);
            }
        }
    }

    private void resetForm() {
        nameField.setText("");
        typeCombo.setSelectedItem(Course.CourseType.THEORY);
        hoursSpinner.setValue(3);
        creditsSpinner.setValue(3);
        difficultySpinner.setValue(5.0);

        editingIndex = -1;
        courseTable.clearSelection();
        addOrUpdateButton.setText("Add Course");
        formErrorLabel.setText(" ");
    }

    private void loadCourseIntoForm(Course course) {
        nameField.setText(course.getName());
        typeCombo.setSelectedItem(course.getType());
        hoursSpinner.setValue(course.getHoursPerWeek());
        creditsSpinner.setValue(course.getCredits());
        difficultySpinner.setValue(course.getDifficultyRating());

        addOrUpdateButton.setText("Update Course");
        formErrorLabel.setText("Editing selected course");
    }

    private void saveSchedule() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Schedule as JSON");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON Files", "json"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".json")) {
            file = new File(file.getParentFile(), file.getName() + ".json");
        }

        try {
            jsonStore.save(file.toPath(), model);
            JOptionPane.showMessageDialog(this, "Schedule saved successfully.",
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Save Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSchedule() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load Schedule JSON");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON Files", "json"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        try {
            ScheduleJsonStore.LoadedSchedule loaded = jsonStore.load(file.toPath());
            if (loaded.getMaxCredits() < 1) {
                throw new IllegalArgumentException("Invalid maxCredits in file. Must be at least 1.");
            }

            model.setMaxCredits(loaded.getMaxCredits());
            maxCreditsSpinner.setValue(loaded.getMaxCredits());

            model.clearCourses();
            for (Course course : loaded.getCourses()) {
                ValidationResult vr = service.validateCourseInput(
                        course.getName(), course.getType(),
                        course.getHoursPerWeek(), course.getCredits(),
                        course.getDifficultyRating());
                if (!vr.isValid()) {
                    throw new IllegalArgumentException(
                            "JSON contains invalid course data: " + String.join("; ", vr.getErrors()));
                }
                model.addCourse(course);
            }

            resetForm();
            refreshReport();
            JOptionPane.showMessageDialog(this, "Schedule loaded successfully.",
                    "Loaded", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Load Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ScheduleAnalyzerSwingApp::new);
    }
}
