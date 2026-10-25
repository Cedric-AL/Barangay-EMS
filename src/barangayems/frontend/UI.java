
package barangayems.frontend;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.*;
import java.util.regex.Pattern;

/**
 * Shared visual system for Barangay EMS.
 */
final class UI {

    static final Color BG = new Color(244, 247, 251);
    static final Color SURFACE = Color.WHITE;
    static final Color LINE = new Color(223, 230, 238);
    static final Color INK = new Color(29, 45, 67);
    static final Color MUTED = new Color(108, 122, 146);
    static final Color NAVY = new Color(15, 39, 74);
    static final Color NAVY_LIGHT = new Color(24, 57, 101);
    static final Color BLUE = new Color(37, 111, 245);
    static final Color BLUE_PALE = new Color(236, 245, 255);
    static final Color SELECTED = new Color(225, 238, 255);
    static final Color GREEN = new Color(22, 133, 96);
    static final Color AMBER = new Color(185, 117, 20);
    static final Color SHADOW = new Color(13, 27, 46, 26);
    static final Color TEXT_SOFT = new Color(59, 72, 91);

    private UI() {
        // Utility class
    }

    // =========================
    // GLOBAL STYLE
    // =========================

    static void setup() {
        Font base = new Font("SansSerif", Font.PLAIN, 13);

        for (String key : new String[]{
                "Label.font",
                "Button.font",
                "TextField.font",
                "PasswordField.font",
                "ComboBox.font",
                "Table.font",
                "TableHeader.font",
                "TextArea.font"
        }) {
            UIManager.put(key, base);
        }

        UIManager.put("Panel.background", BG);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Button.background", BLUE);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.foreground", INK);
        UIManager.put("PasswordField.background", Color.WHITE);
        UIManager.put("PasswordField.foreground", INK);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("OptionPane.messageForeground", INK);
    }

    static JPanel panel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(BG);
        return panel;
    }

    // =========================
    // PAGE HEADER
    // =========================

    static JPanel page(String title) {
        JPanel panel = panel(new BorderLayout(0, 18));
        panel.setBorder(new EmptyBorder(16, 20, 18, 20));

        RoundedPanel header =
                new RoundedPanel(22, new Color(10, 22, 43));

        header.setBorder(new EmptyBorder(18, 24, 18, 24));
        header.setGradient(
                new Color(10, 22, 43),
                new Color(24, 57, 105)
        );
        header.setLayout(new BorderLayout(20, 0));

        JLabel heading = label(title, 25, true);
        heading.setForeground(Color.WHITE);

        JLabel subtitle = label(
                "Evacuation monitoring  /  Resource readiness  /  "
                + "Situational awareness",
                11,
                false
        );
        subtitle.setForeground(new Color(190, 207, 230));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(heading);
        text.add(Box.createVerticalStrut(6));
        text.add(subtitle);

        JPanel status = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 8, 0));
        status.setOpaque(false);

        JLabel dot = new JLabel("\u25CF");
        dot.setForeground(new Color(74, 225, 171));
        dot.setFont(new Font("SansSerif", Font.BOLD, 12));

        JLabel tag = new JLabel("SYSTEM OVERVIEW");
        tag.setForeground(Color.WHITE);
        tag.setFont(new Font("SansSerif", Font.BOLD, 10));

        status.add(dot);
        status.add(tag);

        header.add(text, BorderLayout.CENTER);
        header.add(status, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);

        return panel;
    }

    // =========================
    // LABELS
    // =========================

    static JLabel label(String value, int size, boolean bold) {
        JLabel label = new JLabel(value);

        label.setFont(new Font(
                "SansSerif",
                bold ? Font.BOLD : Font.PLAIN,
                size
        ));

        label.setForeground(INK);

        return label;
    }

    // =========================
    // BUTTONS
    // =========================

    static JButton button(String text, Runnable action) {
        JButton button = new JButton(text);

        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR));

        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(36, 104, 214));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setRolloverEnabled(true);
        button.setMargin(new Insets(8, 14, 8, 14));

        button.setBorder(new CompoundBorder(
                new LineBorder(
                        new Color(52, 120, 255), 1, true),
                new EmptyBorder(10, 16, 10, 16)
        ));

        button.addActionListener(e -> action.run());

        return button;
    }

    // =========================
    // CARDS
    // =========================

    static JPanel card(String title, JComponent body) {
        RoundedPanel panel = new RoundedPanel(18, SURFACE);

        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(new EmptyBorder(16, 18, 16, 18));

        if (title != null) {
            JLabel heading = label(title, 15, true);
            heading.setForeground(INK);
            panel.add(heading, BorderLayout.NORTH);
        }

        panel.add(body, BorderLayout.CENTER);

        return panel;
    }

    // =========================
    // TABLE MODELS
    // =========================

    static DefaultTableModel model(String... headers) {
        return new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    // =========================
    // TABLES
    // =========================

    static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);

        table.setRowHeight(42);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));

        table.setBackground(Color.WHITE);
        table.setForeground(INK);
        table.setSelectionBackground(SELECTED);
        table.setSelectionForeground(INK);

        table.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setRowMargin(1);
        table.setBorder(null);

        JTableHeader header = table.getTableHeader();

        header.setPreferredSize(new Dimension(1, 42));
        header.setReorderingAllowed(false);
        header.setBackground(new Color(239, 244, 250));
        header.setForeground(new Color(75, 95, 122));
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setBorder(new MatteBorder(0, 0, 1, 0, LINE));

        for (int i = 0; i < model.getColumnCount(); i++) {
            String name = model.getColumnName(i);

            int width = (
                    i == 0
                    || name.equals("Assigned Center")
                    || name.equals("Household")
            ) ? 180 : name.equals("Age") ? 55 : 95;

            table.getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(width);
        }

        table.setDefaultRenderer(
                Object.class,
                new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(
                    JTable source,
                    Object value,
                    boolean selected,
                    boolean focus,
                    int row,
                    int column) {

                JLabel label = (JLabel) super
                        .getTableCellRendererComponent(
                                source, value, selected,
                                focus, row, column);

                label.setBorder(
                        new EmptyBorder(0, 12, 0, 12));
                label.setForeground(INK);

                if (!selected) {
                    label.setBackground(
                            row % 2 == 0
                            ? Color.WHITE
                            : new Color(249, 251, 254)
                    );
                }

                String text = value == null
                        ? ""
                        : value.toString();

                label.setToolTipText(text);

                int room = Math.max(
                        30,
                        source.getColumnModel()
                                .getColumn(column).getWidth() - 26
                );

                if (value instanceof String
                        && label.getFontMetrics(label.getFont())
                                .stringWidth(text) > room) {

                    int split = text.lastIndexOf(' ');

                    while (split > 0
                            && label.getFontMetrics(label.getFont())
                                    .stringWidth(
                                            text.substring(0, split))
                                    > room) {

                        split = text.lastIndexOf(' ', split - 1);
                    }

                    if (split > 0) {
                        String first = escapeHtml(
                                text.substring(0, split));
                        String second = escapeHtml(
                                text.substring(split + 1));

                        label.setText(
                                "<html>" + first
                                + "<br>" + second
                                + "</html>"
                        );
                    }
                }

                return label;
            }
        });

        return table;
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    // =========================
    // SCROLL PANES
    // =========================

    static JScrollPane scroll(JComponent content) {
        if (content instanceof JTextArea) {
            JTextArea area = (JTextArea) content;
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
        }

        JScrollPane scroll = new JScrollPane(content);

        if (content instanceof JTextArea) {
            scroll.setHorizontalScrollBarPolicy(
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }

        scroll.setBorder(new LineBorder(LINE, 1, true));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        return scroll;
    }

    // =========================
    // TABLE SEARCH
    // =========================

    static JTextField search(JTable table, String hint) {
        JTextField field = new JTextField(22);

        field.setToolTipText(hint);
        field.putClientProperty(
                "JTextField.placeholderText", hint);

        field.setBorder(new CompoundBorder(
                new LineBorder(
                        new Color(221, 228, 238), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        field.setBackground(Color.WHITE);
        field.setForeground(TEXT_SOFT);
        field.getAccessibleContext().setAccessibleName(hint);

        field.getDocument().addDocumentListener(
                new DocumentListener() {

            private void filterTable() {
                if (!(table.getModel()
                        instanceof DefaultTableModel)) {
                    return;
                }

                if (!(table.getRowSorter()
                        instanceof TableRowSorter<?>)) {
                    return;
                }

                @SuppressWarnings("unchecked")
                TableRowSorter<DefaultTableModel> sorter =
                        (TableRowSorter<DefaultTableModel>)
                                table.getRowSorter();

                String query = field.getText().trim();

                sorter.setRowFilter(
                        query.isEmpty()
                        ? null
                        : RowFilter.regexFilter(
                                "(?i)" + Pattern.quote(query))
                );
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTable();
            }
        });

        return field;
    }

    // =========================
    // FORMS
    // =========================

    static JPanel form() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(SURFACE);
        panel.setOpaque(true);
        return panel;
    }

    /**
     * Shared form helper.
     * Keeps labels on the left and input components on the right.
     */
    static void field(
            JPanel panel,
            int row,
            String labelText,
            JComponent input) {

        GridBagConstraints constraints =
                new GridBagConstraints();

        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 14, 12);

        JLabel label = UI.label(labelText, 12, true);
        label.setForeground(MUTED);
        label.setLabelFor(input);

        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 14, 0);

        int height = input instanceof JScrollPane ? 70 : 40;

        input.setPreferredSize(new Dimension(180, height));
        input.setMinimumSize(new Dimension(100, height));
        input.setForeground(INK);
        input.setBackground(Color.WHITE);
        input.setFont(new Font("SansSerif", Font.PLAIN, 13));

        if (input instanceof JTextField) {
            input.setBorder(new CompoundBorder(
                    new LineBorder(LINE, 1, true),
                    new EmptyBorder(7, 10, 7, 10)
            ));
        }

        panel.add(input, constraints);
    }

    // =========================
    // ERROR DIALOG
    // =========================

    static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                "Check information",
                JOptionPane.WARNING_MESSAGE
        );
    }

    // =========================
    // ROUNDED PANEL
    // =========================

    static class RoundedPanel extends JPanel {

        private final int radius;
        private final Color fill;

        private Color gradientStart;
        private Color gradientEnd;

        RoundedPanel(int radius, Color fill) {
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }

        void setGradient(Color start, Color end) {
            gradientStart = start;
            gradientEnd = end;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g = (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            int shadowX = 6;
            int shadowY = 6;
            int shadowWidth = Math.max(0, getWidth() - shadowX);
            int shadowHeight = Math.max(0, getHeight() - shadowY);

            g.setColor(new Color(15, 30, 60, 26));

            g.fillRoundRect(
                    shadowX,
                    shadowY,
                    shadowWidth,
                    shadowHeight,
                    radius,
                    radius
            );

            if (gradientStart != null && gradientEnd != null) {
                g.setPaint(new GradientPaint(
                        0, 0, gradientStart,
                        getWidth(), getHeight(), gradientEnd
                ));
            } else {
                g.setColor(fill);
            }

            g.fillRoundRect(
                    0,
                    0,
                    Math.max(0, getWidth() - shadowX),
                    Math.max(0, getHeight() - shadowY),
                    radius,
                    radius
            );

            g.dispose();
        }
    }

    // =========================
    // VALIDATION HELPERS
    // =========================

    static int number(
            String value,
            String label,
            int minimum) {

        try {
            int number = Integer.parseInt(value.trim());

            if (number < minimum) {
                throw new NumberFormatException();
            }

            return number;

        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    label + " must be a whole number of at least "
                    + minimum + "."
            );
        }
    }

    static String required(String value, String label) {
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    label + " is required.");
        }

        return value.trim();
    }
}
