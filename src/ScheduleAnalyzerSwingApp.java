import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class ScheduleAnalyzerSwingApp extends JFrame {

    // ── Dark theme palette ──────────────────────────────────────────────────
    private static final Color BG_DEEP    = new Color(0x12131A);
    private static final Color BG_PANEL   = new Color(0x1C1E2B);
    private static final Color BG_CARD    = new Color(0x232635);
    private static final Color BG_ROW_ALT = new Color(0x1E2030);
    private static final Color ACCENT     = new Color(0x7C83FD);
    private static final Color ACCENT_HOV = new Color(0x9DA3FF);
    private static final Color SUCCESS    = new Color(0x4ADE80);
    private static final Color WARNING    = new Color(0xFBBF24);
    private static final Color DANGER     = new Color(0xF87171);
    private static final Color TEXT_PRI   = new Color(0xE2E8F0);
    private static final Color TEXT_SEC   = new Color(0x94A3B8);
    private static final Color BORDER_CLR = new Color(0x2E3148);

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
        super("Course Schedule Analyzer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 740);
        setMinimumSize(new Dimension(900, 620));

        applyGlobalDefaults();

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(BG_DEEP);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        root.add(buildTopBar(), BorderLayout.NORTH);
        root.add(buildMainContent(), BorderLayout.CENTER);

        setContentPane(root);
        refreshReport();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /** Push dark defaults into UIManager so all Swing components inherit them. */
    private void applyGlobalDefaults() {
        UIManager.put("Panel.background",           BG_PANEL);
        UIManager.put("OptionPane.background",      BG_CARD);
        UIManager.put("OptionPane.messageForeground",TEXT_PRI);
        UIManager.put("Label.foreground",           TEXT_PRI);
        UIManager.put("TextField.background",       BG_CARD);
        UIManager.put("TextField.foreground",       TEXT_PRI);
        UIManager.put("TextField.caretForeground",  ACCENT);
        UIManager.put("TextField.border",
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_CLR, 1),
                        BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        UIManager.put("ComboBox.background",        BG_CARD);
        UIManager.put("ComboBox.foreground",        TEXT_PRI);
        UIManager.put("ComboBox.selectionBackground", ACCENT);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("Spinner.background",         BG_CARD);
        UIManager.put("Spinner.foreground",         TEXT_PRI);
        UIManager.put("FormattedTextField.background", BG_CARD);
        UIManager.put("FormattedTextField.foreground", TEXT_PRI);
        UIManager.put("List.background",            BG_CARD);
        UIManager.put("List.foreground",            TEXT_PRI);
        UIManager.put("List.selectionBackground",   ACCENT);
        UIManager.put("List.selectionForeground",   Color.WHITE);
        UIManager.put("ScrollPane.background",      BG_CARD);
        UIManager.put("Viewport.background",        BG_CARD);
        UIManager.put("ScrollBar.background",       BG_PANEL);
        UIManager.put("ScrollBar.thumb",            BORDER_CLR);
        UIManager.put("SplitPane.background",       BG_DEEP);
        UIManager.put("SplitPaneDivider.background",BG_DEEP);
        UIManager.put("SplitPane.border",           BorderFactory.createEmptyBorder());
    }

    // -------------------------------------------------------------------------
    // Top bar
    // -------------------------------------------------------------------------

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setBackground(BG_DEEP);
        bar.setBorder(new EmptyBorder(0, 0, 8, 0));

        JLabel title = new JLabel("📅 Schedule Analyzer");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(ACCENT);
        bar.add(title);

        bar.add(Box.createHorizontalStrut(16));

        JLabel maxLbl = new JLabel("Max Credits:");
        maxLbl.setForeground(TEXT_SEC);
        bar.add(maxLbl);

        maxCreditsSpinner = new JSpinner(new SpinnerNumberModel(model.getMaxCredits(), 1, 60, 1));
        maxCreditsSpinner.setPreferredSize(new Dimension(70, 28));
        styleSpinner(maxCreditsSpinner);
        maxCreditsSpinner.addChangeListener(e -> {
            int value = (int) maxCreditsSpinner.getValue();
            model.setMaxCredits(value);
            refreshReport();
        });
        bar.add(maxCreditsSpinner);

        bar.add(Box.createHorizontalStrut(8));
        bar.add(styledButton("💾  Save JSON", ACCENT, e -> saveSchedule()));
        bar.add(styledButton("📂  Load JSON", BG_CARD, e -> loadSchedule()));

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
        split.setBackground(BG_DEEP);
        split.setDividerSize(6);
        return split;
    }

    // -------------------------------------------------------------------------
    // Left pane: form
    // -------------------------------------------------------------------------

    private JPanel buildLeftPane() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PANEL);
        panel.setBorder(new CompoundBorder(
                new LineBorder(BORDER_CLR, 1, true),
                new EmptyBorder(16, 14, 14, 14)));

        JLabel heading = new JLabel("Add / Edit Course");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 14));
        heading.setForeground(ACCENT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(heading);
        panel.add(Box.createVerticalStrut(10));
        panel.add(buildCourseForm());
        panel.add(Box.createVerticalStrut(12));
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
        labelC.insets = new Insets(5, 0, 5, 10);
        labelC.gridx = 0;

        GridBagConstraints fieldC = new GridBagConstraints();
        fieldC.fill = GridBagConstraints.HORIZONTAL;
        fieldC.weightx = 1.0;
        fieldC.insets = new Insets(5, 0, 5, 0);
        fieldC.gridx = 1;

        nameField = new JTextField();
        nameField.setToolTipText("Course name");
        styleTextField(nameField);

        typeCombo = new JComboBox<>(Course.CourseType.values());
        typeCombo.setSelectedItem(Course.CourseType.THEORY);
        styleComboBox(typeCombo);

        hoursSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 40, 1));
        creditsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 12, 1));
        difficultySpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.0, 10.0, 0.1));
        JSpinner.NumberEditor diffEditor = new JSpinner.NumberEditor(difficultySpinner, "0.0");
        difficultySpinner.setEditor(diffEditor);
        styleSpinner(hoursSpinner);
        styleSpinner(creditsSpinner);
        styleSpinner(difficultySpinner);

        String[] labels = {"Course Name", "Course Type", "Hours / Week", "Credits", "Difficulty (0-10)"};
        JComponent[] fields = {nameField, typeCombo, hoursSpinner, creditsSpinner, difficultySpinner};

        for (int i = 0; i < labels.length; i++) {
            labelC.gridy = i;
            fieldC.gridy = i;
            JLabel lbl = new JLabel(labels[i]);
            lbl.setForeground(TEXT_SEC);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            grid.add(lbl, labelC);
            grid.add(fields[i], fieldC);
        }

        return grid;
    }

    private JPanel buildFormButtons() {
        addOrUpdateButton = styledButton("＋  Add Course", ACCENT, e -> saveCourseFromForm());
        JButton clearButton = styledButton("✕  Clear Form", BG_CARD, e -> resetForm());

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.add(addOrUpdateButton);
        row.add(clearButton);
        return row;
    }

    private JLabel buildFormErrorLabel() {
        formErrorLabel = new JLabel(" ");
        formErrorLabel.setForeground(DANGER);
        formErrorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        formErrorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return formErrorLabel;
    }

    // -------------------------------------------------------------------------
    // Right pane: table + report
    // -------------------------------------------------------------------------

    private JPanel buildRightPane() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(BG_DEEP);
        panel.setBorder(new EmptyBorder(0, 10, 0, 0));

        JPanel tableSection = buildTableSection();
        JPanel reportPanel = buildReportPanel();

        JSplitPane vertSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableSection, reportPanel);
        vertSplit.setResizeWeight(0.55);
        vertSplit.setBorder(null);
        vertSplit.setBackground(BG_DEEP);
        vertSplit.setDividerSize(6);

        panel.add(vertSplit, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTableSection() {
        String[] columns = {"Name", "Type", "Hours/Week", "Credits", "Difficulty"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        courseTable = new JTable(tableModel);
        courseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        courseTable.setFillsViewportHeight(true);
        courseTable.getTableHeader().setReorderingAllowed(false);
        courseTable.setBackground(BG_CARD);
        courseTable.setForeground(TEXT_PRI);
        courseTable.setSelectionBackground(ACCENT);
        courseTable.setSelectionForeground(Color.WHITE);
        courseTable.setGridColor(BORDER_CLR);
        courseTable.setRowHeight(26);
        courseTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        courseTable.setShowHorizontalLines(true);
        courseTable.setShowVerticalLines(false);
        courseTable.setIntercellSpacing(new Dimension(0, 1));

        // Styled header
        JTableHeader header = courseTable.getTableHeader();
        header.setBackground(BG_PANEL);
        header.setForeground(TEXT_SEC);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_CLR));

        // Alternating row renderer
        courseTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? BG_CARD : BG_ROW_ALT);
                    setForeground(TEXT_PRI);
                }
                return this;
            }
        });

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
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_CLR, 1));
        scroll.getViewport().setBackground(BG_CARD);

        JButton deleteButton = styledButton("🗑  Delete Selected", DANGER, e -> deleteSelectedCourse());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonRow.setBackground(BG_DEEP);
        buttonRow.add(deleteButton);

        JLabel heading = new JLabel("Courses");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 13));
        heading.setForeground(TEXT_PRI);

        JPanel section = new JPanel(new BorderLayout(0, 6));
        section.setBackground(BG_DEEP);
        section.add(heading, BorderLayout.NORTH);
        section.add(scroll, BorderLayout.CENTER);
        section.add(buttonRow, BorderLayout.SOUTH);
        return section;
    }

    private JPanel buildReportPanel() {
        totalHoursValue    = styledValueLabel("0");
        totalCreditsValue  = styledValueLabel("0");
        hoursScoreValue    = styledValueLabel("0.00 / 100");
        creditsScoreValue  = styledValueLabel("0.00 / 100");
        difficultyScoreValue = styledValueLabel("0.00 / 100");
        finalScoreValue    = styledValueLabel("0.00 / 100");
        finalScoreValue.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JPanel stats = new JPanel(new GridBagLayout());
        stats.setOpaque(false);
        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(3, 0, 3, 12);
        lc.gridx = 0;

        GridBagConstraints vc = new GridBagConstraints();
        vc.anchor = GridBagConstraints.WEST;
        vc.weightx = 1.0;
        vc.insets = new Insets(3, 0, 3, 0);
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
            JLabel lbl = new JLabel(statLabels[i]);
            lbl.setForeground(TEXT_SEC);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            stats.add(lbl, lc);
            stats.add(statValues[i], vc);
        }

        warningsListModel = new DefaultListModel<>();
        warningsList = new JList<>(warningsListModel);
        warningsList.setVisibleRowCount(4);
        warningsList.setBackground(BG_CARD);
        warningsList.setForeground(WARNING);
        warningsList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        warningsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean hasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
                String text = value.toString();
                setForeground(text.startsWith("No warnings") ? SUCCESS : WARNING);
                setBackground(isSelected ? ACCENT : BG_CARD);
                setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
                return this;
            }
        });
        JScrollPane warningsScroll = new JScrollPane(warningsList);
        warningsScroll.setBorder(BorderFactory.createLineBorder(BORDER_CLR, 1));
        warningsScroll.getViewport().setBackground(BG_CARD);

        JPanel report = new JPanel(new BorderLayout(0, 10));
        report.setBorder(new CompoundBorder(
                new LineBorder(BORDER_CLR, 1, true),
                new EmptyBorder(12, 12, 12, 12)));
        report.setBackground(BG_PANEL);

        JLabel analysisHeading = new JLabel("📊  Live Analysis");
        analysisHeading.setFont(new Font("Segoe UI", Font.BOLD, 13));
        analysisHeading.setForeground(ACCENT);

        JPanel inner = new JPanel(new BorderLayout(0, 6));
        inner.setOpaque(false);
        inner.add(analysisHeading, BorderLayout.NORTH);
        inner.add(stats, BorderLayout.CENTER);

        JLabel warningsHeading = new JLabel("⚠  Warnings");
        warningsHeading.setFont(new Font("Segoe UI", Font.BOLD, 12));
        warningsHeading.setForeground(WARNING);

        JPanel warningsSection = new JPanel(new BorderLayout(0, 4));
        warningsSection.setOpaque(false);
        warningsSection.add(warningsHeading, BorderLayout.NORTH);
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

        double fs = analysis.getFinalScore();
        finalScoreValue.setText(String.format("%.2f / 100", fs));
        finalScoreValue.setForeground(fs >= 75 ? SUCCESS : fs >= 50 ? WARNING : DANGER);

        warningsListModel.clear();
        List<String> warnings = analysis.getWarnings();
        if (warnings.isEmpty()) {
            warningsListModel.addElement("No warnings. Schedule looks balanced.");
        } else {
            for (String w : warnings) warningsListModel.addElement(w);
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
        addOrUpdateButton.setText("＋  Add Course");
        formErrorLabel.setText(" ");
    }

    private void loadCourseIntoForm(Course course) {
        nameField.setText(course.getName());
        typeCombo.setSelectedItem(course.getType());
        hoursSpinner.setValue(course.getHoursPerWeek());
        creditsSpinner.setValue(course.getCredits());
        difficultySpinner.setValue(course.getDifficultyRating());

        addOrUpdateButton.setText("✎  Update Course");
        formErrorLabel.setText("Editing selected course");
        formErrorLabel.setForeground(ACCENT);
    }

    private void saveSchedule() {
        FileDialog dialog = new FileDialog(this, "Save Schedule as JSON", FileDialog.SAVE);
        dialog.setFile("*.json");
        dialog.setVisible(true);

        String dir  = dialog.getDirectory();
        String name = dialog.getFile();
        if (dir == null || name == null) return;

        if (!name.toLowerCase().endsWith(".json")) name += ".json";
        File file = new File(dir, name);

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
        FileDialog dialog = new FileDialog(this, "Load Schedule JSON", FileDialog.LOAD);
        dialog.setFile("*.json");
        dialog.setVisible(true);

        String dir  = dialog.getDirectory();
        String name = dialog.getFile();
        if (dir == null || name == null) return;

        File file = new File(dir, name);
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
    // UI helper factory methods
    // -------------------------------------------------------------------------

    /** Creates a styled rounded button with hover effect. */
    private JButton styledButton(String text, Color bg, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.brighter() : bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        btn.addActionListener(action);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { btn.repaint(); }
        });
        return btn;
    }

    private JLabel styledValueLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(TEXT_PRI);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return lbl;
    }

    private void styleTextField(JTextField field) {
        field.setBackground(BG_CARD);
        field.setForeground(TEXT_PRI);
        field.setCaretColor(ACCENT);
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER_CLR, 1, true),
                new EmptyBorder(4, 8, 4, 8)));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    }

    private void styleComboBox(JComboBox<?> combo) {
        combo.setBackground(BG_CARD);
        combo.setForeground(TEXT_PRI);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        combo.setBorder(new LineBorder(BORDER_CLR, 1, true));
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBackground(BG_CARD);
        spinner.setForeground(TEXT_PRI);
        spinner.setBorder(new LineBorder(BORDER_CLR, 1, true));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(BG_CARD);
            tf.setForeground(TEXT_PRI);
            tf.setCaretColor(ACCENT);
            tf.setBorder(new EmptyBorder(2, 6, 2, 6));
            tf.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        }
    }

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(ScheduleAnalyzerSwingApp::new);
    }
}
