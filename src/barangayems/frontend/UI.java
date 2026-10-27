
package barangayems.frontend;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Shared visual system for Barangay EMS.
 *
 * Design rules used everywhere:
 *  - One calm neutral canvas, white cards with a hairline border (no heavy shadows).
 *  - One primary (blue) action per area; everything else is a secondary (outlined) button.
 *  - Red is reserved for destructive actions and genuine alerts.
 *  - Status values are shown as small coloured pills so they can be scanned quickly.
 */
final class UI {

    // ---------- Palette ----------
    static final Color BG = new Color(243, 245, 249);          // page canvas
    static final Color SURFACE = Color.WHITE;                  // cards
    static final Color SUBTLE = new Color(248, 250, 252);      // read-only / inset areas
    static final Color LINE = new Color(226, 231, 238);        // hairlines
    static final Color LINE_STRONG = new Color(208, 215, 225); // input borders
    static final Color INK = new Color(22, 34, 51);            // primary text
    static final Color TEXT_SOFT = new Color(64, 78, 98);      // secondary text
    static final Color MUTED = new Color(110, 123, 143);       // captions
    static final Color NAVY = new Color(13, 31, 58);
    static final Color NAVY_LIGHT = new Color(24, 57, 101);
    static final Color BLUE = new Color(31, 102, 229);
    static final Color BLUE_HOVER = new Color(24, 86, 200);
    static final Color BLUE_PALE = new Color(234, 242, 255);
    static final Color SELECTED = new Color(232, 240, 254);
    static final Color GREEN = new Color(21, 128, 84);
    static final Color GREEN_PALE = new Color(226, 246, 236);
    static final Color AMBER = new Color(176, 104, 8);
    static final Color AMBER_PALE = new Color(254, 243, 219);
    static final Color RED = new Color(196, 43, 58);
    static final Color RED_PALE = new Color(253, 232, 234);
    static final Color GRAY_PALE = new Color(238, 241, 245);
    static final Color SHADOW = new Color(13, 27, 46, 14);

    static final int RADIUS = 14;

    private static String family;

    private UI() {
        // Utility class
    }

    // =========================
    // FONTS
    // =========================

    /** Picks the nicest UI font installed on this machine (Segoe UI on Windows). */
    static String family() {
        if (family == null) {
            Set<String> installed = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment()
                            .getAvailableFontFamilyNames()));
            family = "SansSerif";
            for (String candidate : new String[]{
                    "Segoe UI", "Inter", "SF Pro Text", "Helvetica Neue", "Roboto", "Noto Sans"}) {
                if (installed.contains(candidate)) {
                    family = candidate;
                    break;
                }
            }
        }
        return family;
    }

    static Font font(int style, int size) {
        return new Font(family(), style, size);
    }

    static Font mono(int size) {
        Set<String> installed = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String candidate : new String[]{"Cascadia Mono", "Consolas", "JetBrains Mono", "DejaVu Sans Mono"}) {
            if (installed.contains(candidate)) return new Font(candidate, Font.PLAIN, size);
        }
        return new Font("Monospaced", Font.PLAIN, size);
    }

    // =========================
    // GLOBAL STYLE
    // =========================

    static void setup() {
        Font base = font(Font.PLAIN, 13);

        for (String key : new String[]{
                "Label.font", "Button.font", "ToggleButton.font", "TextField.font",
                "PasswordField.font", "ComboBox.font", "Table.font", "TableHeader.font",
                "TextArea.font", "CheckBox.font", "RadioButton.font", "List.font",
                "Spinner.font", "FormattedTextField.font", "ToolTip.font",
                "OptionPane.messageFont", "OptionPane.buttonFont", "TabbedPane.font",
                "MenuItem.font", "PopupMenu.font"
        }) {
            UIManager.put(key, base);
        }

        UIManager.put("Panel.background", BG);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", INK);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Button.background", BLUE);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.foreground", INK);
        UIManager.put("TextField.selectionBackground", SELECTED);
        UIManager.put("TextField.selectionForeground", INK);
        UIManager.put("PasswordField.background", Color.WHITE);
        UIManager.put("PasswordField.foreground", INK);
        UIManager.put("TextArea.selectionBackground", SELECTED);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.foreground", INK);
        UIManager.put("ComboBox.selectionBackground", SELECTED);
        UIManager.put("ComboBox.selectionForeground", INK);
        UIManager.put("ComboBox.buttonBackground", Color.WHITE);
        UIManager.put("List.selectionBackground", SELECTED);
        UIManager.put("List.selectionForeground", INK);
        UIManager.put("CheckBox.foreground", INK);
        UIManager.put("CheckBox.icon", new FlatUI.CheckIcon());
        UIManager.put("ToolTip.background", INK);
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.border", new EmptyBorder(6, 9, 6, 9));
        UIManager.put("ScrollBarUI", FlatUI.ScrollBar.class.getName());
        UIManager.put("ComboBoxUI", FlatUI.Combo.class.getName());
        UIManager.put("SpinnerUI", FlatUI.Spinner.class.getName());
        UIManager.put("ScrollPane.background", SURFACE);
        UIManager.put("Viewport.background", SURFACE);
        UIManager.put("Spinner.border", new LineBorder(LINE_STRONG, 1, true));
        UIManager.put("PopupMenu.border", new LineBorder(LINE_STRONG, 1));
        UIManager.put("ComboBox.border", new EmptyBorder(0, 0, 0, 0));
    }

    /**
     * A layout panel. It is transparent so it blends with whichever card or
     * page it is placed on (previously grey patches appeared inside white cards).
     */
    static JPanel panel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        panel.setBackground(BG);
        return panel;
    }

    // =========================
    // PAGE CONTAINER
    // =========================

    /**
     * Page container. The page title is already shown in the application top bar,
     * so no second banner is drawn here - the content starts right away.
     */
    static JPanel page(String title) {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG);
        panel.setOpaque(true);
        panel.setBorder(new EmptyBorder(20, 24, 22, 24));
        panel.getAccessibleContext().setAccessibleName(title);
        return panel;
    }

    // =========================
    // LABELS
    // =========================

    static JLabel label(String value, int size, boolean bold) {
        JLabel label = new JLabel(value);
        label.setFont(font(bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(INK);
        return label;
    }

    /** Small grey helper text, wraps when given HTML. */
    static JLabel hint(String value) {
        JLabel label = label(value, 12, false);
        label.setForeground(MUTED);
        return label;
    }

    /** Small uppercase caption used above groups of content. */
    static JLabel caption(String value) {
        JLabel label = label(value.toUpperCase(), 11, true);
        label.setForeground(MUTED);
        return label;
    }

    // =========================
    // BUTTONS
    // =========================

    enum Kind { PRIMARY, SECONDARY, DANGER, GHOST }

    /** Primary (filled blue) button - use for the main action of an area. */
    static JButton button(String text, Runnable action) {
        return button(text, Kind.PRIMARY, action);
    }

    /** Outlined neutral button - use for every supporting action. */
    static JButton secondary(String text, Runnable action) {
        return button(text, Kind.SECONDARY, action);
    }

    /** Outlined red button - use for delete / remove actions only. */
    static JButton danger(String text, Runnable action) {
        return button(text, Kind.DANGER, action);
    }

    /** Text-only button for low-emphasis actions. */
    static JButton ghost(String text, Runnable action) {
        return button(text, Kind.GHOST, action);
    }

    static JButton button(String text, Kind kind, Runnable action) {
        FlatButton button = new FlatButton(text, kind);
        if (action != null) button.addActionListener(e -> action.run());
        return button;
    }

    static final class FlatButton extends JButton {
        private final Kind kind;
        private boolean hover;

        FlatButton(String text, Kind kind) {
            super(text);
            this.kind = kind;
            setFont(font(Font.BOLD, 13));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 16, 9, 16));
            setForeground(foregroundFor(kind));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        private static Color foregroundFor(Kind kind) {
            switch (kind) {
                case PRIMARY: return Color.WHITE;
                case DANGER: return RED;
                case GHOST: return BLUE;
                default: return INK;
            }
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(d.width, 84), Math.max(d.height, 38));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean enabled = isEnabled();
            boolean pressed = getModel().isPressed();
            int w = getWidth(), h = getHeight();
            Color fill = null, stroke = null;

            switch (kind) {
                case PRIMARY:
                    fill = !enabled ? new Color(171, 193, 232) : pressed ? BLUE_HOVER.darker() : hover ? BLUE_HOVER : BLUE;
                    break;
                case DANGER:
                    fill = !enabled ? SURFACE : hover ? RED_PALE : SURFACE;
                    stroke = enabled ? new Color(236, 180, 186) : LINE;
                    break;
                case GHOST:
                    fill = hover && enabled ? BLUE_PALE : null;
                    break;
                default:
                    fill = !enabled ? SUBTLE : pressed ? new Color(232, 236, 242) : hover ? new Color(243, 246, 250) : SURFACE;
                    stroke = LINE_STRONG;
            }

            if (fill != null) {
                g.setColor(fill);
                g.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));
            }
            if (stroke != null) {
                g.setColor(stroke);
                g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, 10, 10));
            }
            g.dispose();

            setForeground(enabled ? foregroundFor(kind) : new Color(150, 160, 175));
            if (kind == Kind.PRIMARY && !enabled) setForeground(Color.WHITE);
            super.paintComponent(graphics);
        }
    }

    /** A horizontal row of buttons, left or right aligned. */
    static JPanel actions(int align, JComponent... items) {
        JPanel row = panel(new FlowLayout(align, 8, 0));
        if (align == FlowLayout.LEFT) ((FlowLayout) row.getLayout()).setHgap(0);
        boolean first = true;
        for (JComponent item : items) {
            if (!first && align == FlowLayout.LEFT) row.add(Box.createHorizontalStrut(8));
            row.add(item);
            first = false;
        }
        return row;
    }

    // =========================
    // CARDS
    // =========================

    static JPanel card(String title, JComponent body) {
        return card(title, null, null, body);
    }

    /**
     * White card with an optional title, one-line description and actions on
     * the right of the header.
     */
    static JPanel card(String title, String subtitle, JComponent headerRight, JComponent body) {
        RoundedPanel panel = new RoundedPanel(RADIUS, SURFACE);
        panel.setLayout(new BorderLayout(0, 14));
        panel.setBorder(new EmptyBorder(18, 20, 18, 20));

        if (title != null || headerRight != null) {
            JPanel header = panel(new BorderLayout(16, 0));
            JPanel text = panel(null);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            if (title != null) {
                JLabel heading = label(title, 15, true);
                text.add(heading);
            }
            if (subtitle != null) {
                text.add(Box.createVerticalStrut(3));
                text.add(hint(subtitle));
            }
            header.add(text, BorderLayout.CENTER);
            if (headerRight != null) {
                JPanel right = panel(new GridBagLayout());
                right.add(headerRight);
                header.add(right, BorderLayout.EAST);
            }
            panel.add(header, BorderLayout.NORTH);
        }

        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    // =========================
    // STATUS PILL
    // =========================

    /** Returns {foreground, background} for a status word. */
    static Color[] tone(String value) {
        String s = value == null ? "" : value.toLowerCase();
        if (s.contains("full") || s.contains("reject") || s.contains("short") || s.contains("low") || s.contains("deactiv"))
            return new Color[]{RED, RED_PALE};
        if (s.contains("near") || s.contains("pending") || s.contains("review") || s.contains("warn"))
            return new Color[]{AMBER, AMBER_PALE};
        if (s.contains("available") || s.contains("approved") || s.contains("active") || s.contains("open")
                || s.contains("ok") || s.contains("suitable") || s.equals("true"))
            return new Color[]{GREEN, GREEN_PALE};
        if (s.contains("archiv") || s.equals("false"))
            return new Color[]{MUTED, GRAY_PALE};
        return new Color[]{BLUE, BLUE_PALE};
    }

    static Pill pill(String text) {
        Pill pill = new Pill();
        pill.set(text);
        return pill;
    }

    /** Label that paints a rounded tinted background behind its text. */
    static final class Pill extends JLabel {
        private Color tint = BLUE_PALE;

        Pill() {
            setFont(font(Font.BOLD, 11));
            setBorder(new EmptyBorder(3, 9, 3, 9));
            setOpaque(false);
        }

        void set(String text) {
            Color[] t = tone(text);
            setText(text);
            setForeground(t[0]);
            tint = t[1];
        }

        Pill colors(Color foreground, Color background) {
            setForeground(foreground);
            tint = background;
            repaint();
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FontMetrics fm = getFontMetrics(getFont());
            int textW = fm.stringWidth(getText() == null ? "" : getText());
            int w = Math.min(getWidth(), textW + 18);
            int h = fm.getHeight() + 4;
            int x = getHorizontalAlignment() == SwingConstants.CENTER ? (getWidth() - w) / 2 : getInsets().left - 9;
            x = Math.max(0, x);
            int y = (getHeight() - h) / 2;
            g.setColor(tint);
            g.fill(new RoundRectangle2D.Float(x, y, w, h, h, h));
            g.dispose();
            super.paintComponent(graphics);
        }
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

    private static boolean isStatusColumn(String name) {
        String n = name.toLowerCase();
        return n.equals("status") || n.equals("enabled");
    }

    static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);

        table.setRowHeight(44);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(237, 240, 245));
        table.setIntercellSpacing(new Dimension(0, 1));

        table.setBackground(Color.WHITE);
        table.setForeground(INK);
        table.setSelectionBackground(SELECTED);
        table.setSelectionForeground(INK);
        table.setFont(font(Font.PLAIN, 13));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setBorder(null);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(1, 38));
        header.setReorderingAllowed(false);
        header.setBackground(SUBTLE);
        header.setBorder(new MatteBorder(0, 0, 1, 0, LINE));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, false, false, r, c);
                l.setText(value == null ? "" : value.toString().toUpperCase());
                l.setToolTipText(value == null ? null : value.toString());
                l.setFont(font(Font.BOLD, 11));
                l.setForeground(MUTED);
                l.setBackground(SUBTLE);
                l.setOpaque(true);
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, LINE), new EmptyBorder(0, 14, 0, 10)));
                return l;
            }
        });

        for (int i = 0; i < model.getColumnCount(); i++) {
            String name = model.getColumnName(i);

            int width = (
                    i == 0
                    || name.equals("Assigned Center")
                    || name.equals("Household")
                    || name.equals("Location")
            ) ? 180 : name.equals("Age") ? 55 : isStatusColumn(name) ? 130 : 95;

            table.getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(width);
        }

        DefaultTableCellRenderer text = new DefaultTableCellRenderer() {

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
                                false, row, column);

                label.setBorder(new EmptyBorder(0, 14, 0, 10));
                label.setForeground(column == 0 ? INK : TEXT_SOFT);
                label.setFont(font(column == 0 ? Font.BOLD : Font.PLAIN, 13));
                label.setHorizontalAlignment(SwingConstants.LEFT);

                if (!selected) {
                    label.setBackground(Color.WHITE);
                }

                String colName = source.getColumnName(column).toLowerCase();
                if (colName.contains("deficit") && value instanceof Number
                        && ((Number) value).doubleValue() > 0) {
                    label.setForeground(RED);
                    label.setFont(font(Font.BOLD, 13));
                }

                String text = value == null
                        ? ""
                        : value.toString();

                label.setToolTipText(text.isEmpty() ? null : text);

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
        };
        table.setDefaultRenderer(Object.class, text);
        table.setDefaultRenderer(Number.class, text);
        table.setDefaultRenderer(Integer.class, text);

        TableCellRenderer pillRenderer = new TableCellRenderer() {
            private final Pill pill = new Pill();
            private final JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
            {
                wrap.add(pill);
                ((FlowLayout) wrap.getLayout()).setAlignOnBaseline(false);
            }
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int column) {
                String v = value == null ? "" : value.toString();
                if ("true".equals(v)) v = "Enabled";
                if ("false".equals(v)) v = "Disabled";
                pill.set(v);
                if (v.equals("Disabled")) { pill.setForeground(MUTED); }
                wrap.setBackground(selected ? SELECTED : Color.WHITE);
                wrap.setBorder(new EmptyBorder((t.getRowHeight(row) - pill.getPreferredSize().height) / 2, 0, 0, 0));
                return wrap;
            }
        };
        for (int i = 0; i < model.getColumnCount(); i++) {
            if (isStatusColumn(model.getColumnName(i))) {
                table.getColumnModel().getColumn(i).setCellRenderer(pillRenderer);
            }
        }

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
            area.setMargin(new Insets(8, 10, 8, 10));
            area.setForeground(INK);
            if (!area.isEditable()) area.setBackground(SUBTLE);
            styleFocus(area, null);
        }

        boolean plain = !(content instanceof JTextArea) && !(content instanceof JTable)
                && !(content instanceof Scrollable);
        FitWidth fit = plain ? new FitWidth(content) : null;
        JScrollPane scroll = new JScrollPane(plain ? fit : content);

        if (content instanceof JTextArea) {
            scroll.setHorizontalScrollBarPolicy(
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.getViewport().setBackground(content.getBackground());
            scroll.setBorder(new LineBorder(LINE_STRONG, 1, true));
            if (((JTextArea) content).isEditable()) {
                JScrollPane s = scroll;
                content.addFocusListener(new FocusAdapter() {
                    @Override public void focusGained(FocusEvent e) { s.setBorder(new LineBorder(BLUE, 1, true)); }
                    @Override public void focusLost(FocusEvent e) { s.setBorder(new LineBorder(LINE_STRONG, 1, true)); }
                });
            }
        } else if (content instanceof JTable) {
            scroll.setBorder(new LineBorder(LINE, 1, true));
            scroll.getViewport().setBackground(Color.WHITE);
            JPanel corner = new JPanel();
            corner.setBackground(SUBTLE);
            scroll.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, corner);
        } else {
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.getViewport().setBackground(SURFACE);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            if (fit != null) {
                // Leave room for the scroll bar only while it is showing.
                JScrollBar bar = scroll.getVerticalScrollBar();
                Runnable pad = () -> {
                    boolean show = bar.isVisible();
                    boolean has = fit.getBorder() != null;
                    if (show != has) {
                        fit.setBorder(show ? new EmptyBorder(0, 0, 0, 14) : null);
                        fit.revalidate();
                    }
                };
                scroll.getViewport().addChangeListener(e -> pad.run());
                bar.addComponentListener(new java.awt.event.ComponentAdapter() {
                    @Override public void componentShown(java.awt.event.ComponentEvent e) { pad.run(); }
                    @Override public void componentHidden(java.awt.event.ComponentEvent e) { pad.run(); }
                });
            }
        }
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        return scroll;
    }

    /** Holder that always matches the scroll pane's width (no sideways scrolling). */
    static final class FitWidth extends JPanel implements Scrollable {
        FitWidth(JComponent content) {
            super(new BorderLayout());
            setOpaque(false);
            add(content, BorderLayout.CENTER);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(16, r.height - 32); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
        }
    }

    // =========================
    // TABLE SEARCH
    // =========================

    static JTextField search(JTable table, String hint) {
        JTextField field = new HintField(hint, true);
        field.setColumns(24);
        field.setToolTipText(hint);
        field.putClientProperty("JTextField.placeholderText", hint);
        styleInput(field);
        field.setBorder(new CompoundBorder(
                new LineBorder(LINE_STRONG, 1, true),
                new EmptyBorder(8, 34, 8, 12)));
        styleFocus(field, new EmptyBorder(8, 34, 8, 12));
        field.setPreferredSize(new Dimension(300, 38));
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

    /** Text field that draws a grey placeholder (and optional search glyph). */
    static final class HintField extends JTextField {
        private final String hint;
        private final boolean searchIcon;

        HintField(String hint, boolean searchIcon) {
            this.hint = hint;
            this.searchIcon = searchIcon;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (searchIcon) {
                g.setColor(MUTED);
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cy = getHeight() / 2;
                g.drawOval(13, cy - 7, 11, 11);
                g.drawLine(22, cy + 2, 26, cy + 6);
            }
            if (getText().isEmpty() && hint != null) {
                g.setColor(new Color(150, 160, 176));
                g.setFont(getFont());
                Insets in = getInsets();
                FontMetrics fm = g.getFontMetrics();
                g.drawString(hint, in.left, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
            g.dispose();
        }
    }

    // =========================
    // INPUT STYLING
    // =========================

    static void styleInput(JComponent input) {
        input.setFont(font(Font.PLAIN, 13));
        input.setForeground(INK);
        input.setBackground(Color.WHITE);
        if (input instanceof JTextField) {
            JTextField f = (JTextField) input;
            f.setCaretColor(BLUE);
            f.setBorder(new CompoundBorder(
                    new LineBorder(LINE_STRONG, 1, true),
                    new EmptyBorder(7, 11, 7, 11)));
            styleFocus(f, new EmptyBorder(7, 11, 7, 11));
        } else if (input instanceof JComboBox) {
            input.setBorder(new LineBorder(LINE_STRONG, 1, true));
        } else if (input instanceof JSpinner) {
            input.setBorder(new LineBorder(LINE_STRONG, 1, true));
            JComponent editor = ((JSpinner) input).getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
                tf.setHorizontalAlignment(SwingConstants.LEFT);
                tf.setBorder(new EmptyBorder(6, 10, 6, 6));
                tf.setFont(font(Font.PLAIN, 13));
            }
        } else if (input instanceof JCheckBox) {
            input.setOpaque(false);
        }
    }

    /** Blue border while focused, grey otherwise. */
    private static void styleFocus(JComponent field, Border padding) {
        if (padding == null) return;
        if (Boolean.TRUE.equals(field.getClientProperty("ui.focusStyled"))) return;
        field.putClientProperty("ui.focusStyled", true);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                field.setBorder(new CompoundBorder(new LineBorder(BLUE, 1, true), padding));
            }
            @Override public void focusLost(FocusEvent e) {
                field.setBorder(new CompoundBorder(new LineBorder(LINE_STRONG, 1, true), padding));
            }
        });
    }

    // =========================
    // FORMS
    // =========================

    static JPanel form() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(SURFACE);
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Shared form helper.
     * Labels sit above their inputs so inputs get the full width of the card.
     */
    static void field(
            JPanel panel,
            int row,
            String labelText,
            JComponent input) {

        GridBagConstraints constraints =
                new GridBagConstraints();

        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;

        String clean = labelText.endsWith(":") ? labelText.substring(0, labelText.length() - 1) : labelText;
        JLabel label = UI.label(clean, 12, true);
        label.setForeground(TEXT_SOFT);
        label.setLabelFor(input);

        constraints.gridy = row * 2;
        constraints.insets = new Insets(0, 0, 6, 0);
        panel.add(label, constraints);

        constraints.gridy = row * 2 + 1;
        constraints.insets = new Insets(0, 0, 14, 0);

        int height = input instanceof FacilityChecklist ? 136
                : input instanceof JScrollPane ? 76
                : input instanceof JCheckBox ? 28
                : 38;

        input.setPreferredSize(new Dimension(180, height));
        input.setMinimumSize(new Dimension(100, height));
        styleInput(input);

        panel.add(input, constraints);

        // Keep the fields packed at the top of the form.
        Component filler = (Component) panel.getClientProperty("ui.filler");
        if (filler == null) {
            filler = Box.createGlue();
            panel.putClientProperty("ui.filler", filler);
        } else {
            panel.remove(filler);
        }
        GridBagConstraints fill = new GridBagConstraints();
        fill.gridx = 0;
        fill.gridy = 10_000;
        fill.weighty = 1.0;
        fill.fill = GridBagConstraints.BOTH;
        panel.add(filler, fill);
    }

    /** Wraps a password field with a small Show / Hide text toggle. */
    static JPanel password(JPasswordField field) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(true);
        row.setBackground(Color.WHITE);
        row.setBorder(new LineBorder(LINE_STRONG, 1, true));
        field.setBorder(new EmptyBorder(7, 11, 7, 11));
        field.setFont(font(Font.PLAIN, 13));
        field.setForeground(INK);
        field.setCaretColor(BLUE);
        row.add(field, BorderLayout.CENTER);
        JToggleButton toggle = new JToggleButton("Show");
        toggle.setFont(font(Font.BOLD, 12));
        toggle.setForeground(BLUE);
        toggle.setContentAreaFilled(false);
        toggle.setBorderPainted(false);
        toggle.setFocusPainted(false);
        toggle.setOpaque(false);
        toggle.setBorder(new EmptyBorder(0, 12, 0, 12));
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.getAccessibleContext().setAccessibleName("Show password");
        char mask = field.getEchoChar();
        toggle.addActionListener(e -> {
            field.setEchoChar(toggle.isSelected() ? (char) 0 : mask);
            toggle.setText(toggle.isSelected() ? "Hide" : "Show");
            toggle.getAccessibleContext().setAccessibleName(toggle.isSelected() ? "Hide password" : "Show password");
        });
        row.add(toggle, BorderLayout.EAST);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { row.setBorder(new LineBorder(BLUE, 1, true)); }
            @Override public void focusLost(FocusEvent e) { row.setBorder(new LineBorder(LINE_STRONG, 1, true)); }
        });
        return row;
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
        private Color border = LINE;

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
            border = null;
            repaint();
        }

        void setBorderColor(Color color) {
            border = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            int w = getWidth();
            int h = getHeight();

            // Very soft shadow, 1-2px, just enough to lift the card.
            g.setColor(SHADOW);
            g.fill(new RoundRectangle2D.Float(0, 1.5f, w, h - 1.5f, radius, radius));

            if (gradientStart != null && gradientEnd != null) {
                g.setPaint(new GradientPaint(
                        0, 0, gradientStart,
                        w, h, gradientEnd
                ));
            } else {
                g.setColor(fill);
            }

            g.fill(new RoundRectangle2D.Float(0, 0, w, h - 1.5f, radius, radius));

            if (border != null) {
                g.setColor(border);
                g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 2.5f, radius, radius));
            }

            g.dispose();
            super.paintComponent(graphics);
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
