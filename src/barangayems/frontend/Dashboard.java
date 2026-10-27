
package barangayems.frontend;

import barangayems.backend.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Dashboard extends JPanel {

    private final Color NAVY = new Color(10, 22, 43);
    private final Color BLUE = new Color(48, 119, 255);
    private final Color CYAN = new Color(67, 211, 230);
    private final Color GREEN = new Color(35, 190, 135);
    private final Color ORANGE = new Color(255, 166, 67);
    private final Color RED = new Color(245, 86, 103);
    private final Color TEXT = new Color(31, 46, 69);
    private final Color MUTED = new Color(126, 143, 165);
    private final Color BORDER = new Color(229, 235, 244);
    private final Color CANVAS = new Color(244, 247, 252);

    public Dashboard(AppFrame app) {
        super(new BorderLayout());

        DataSnapshot s = new DataSnapshot();
        s.centers = app.store.activeCenters();
        s.people = app.store.people;
        s.supplyRules = app.store.supplyRules;
        s.supplies = app.store.supplies.stream().filter(r -> s.centers.stream().anyMatch(c -> c.id == r.centerId)).toList();

        JPanel page = UI.page("2 — DASHBOARD");
        page.setLayout(new BorderLayout());
        page.setBackground(CANVAS);
        page.setBorder(new EmptyBorder(16, 20, 18, 20));

        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setOpaque(false);

        // COMMAND CENTER HEADER
        content.add(createHero(s), BorderLayout.NORTH);

        // MAIN DASHBOARD CONTENT
        JPanel main = new JPanel(new BorderLayout(0, 20));
        main.setOpaque(false);
        main.setAlignmentX(Component.LEFT_ALIGNMENT);
        main.setBorder(new EmptyBorder(0, 0, 0, 0));
        main.setMinimumSize(new Dimension(0, 0));
        main.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));


        // METRIC CARDS
        long full = s.centers.stream()
                .filter(c -> c.available() == 0)
                .count();

        long low = s.supplies.stream()
                .filter(r -> r.shortage() > 0)
                .count();

        String[][] metrics = {
            {"TOTAL CENTERS", "" + s.centers.size(), "⌂",
                "Registered evacuation centers", "#3077FF"},
            {"TOTAL EVACUEES", "" + s.occupants(), "◉",
                "Currently accommodated", "#26B995"},
            {"AVAILABLE SPACES", "" + (s.capacity() - s.occupants()), "↗",
                "Remaining accommodation", "#32BDE8"},
            {"FULL CENTERS", "" + full, "!",
                "No remaining capacity", "#F56B79"},
            {"LOW RESOURCES", "" + low, "▤",
                "Items needing attention", "#F5A742"},
            {"DEMAND SHORTAGES", "" + SupplyDemand.rows(s).stream().filter(r -> r.plan().deficit()>0).count(), "↻",
                "Center/item requirements unmet", "#9775FA"}
        };

        JPanel stats = new JPanel(new GridLayout(2, 3, 14, 14));
        stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 270));

        for (String[] metric : metrics) {
            stats.add(createMetric(
                    metric[0], metric[1], metric[2],
                    metric[3], Color.decode(metric[4])
            ));
        }

        main.add(stats, BorderLayout.NORTH);

        // LOWER PANELS
        JPanel lower = new JPanel(new GridLayout(1, 2, 16, 0));
        lower.setOpaque(false);
        lower.setAlignmentX(Component.LEFT_ALIGNMENT);

        lower.add(createCenterPanel(s));
        lower.add(createAlertsPanel(s));

        lower.setPreferredSize(new Dimension(1200, 360));
        lower.setMinimumSize(new Dimension(0, 360));
        lower.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));

        main.add(lower, BorderLayout.CENTER);
        



       
        
        JScrollPane mainScroll = new JScrollPane(main);
        mainScroll.setBorder(null);
        mainScroll.setOpaque(false);
        mainScroll.getViewport().setOpaque(false);
        mainScroll.getVerticalScrollBar().setUnitIncrement(18);
        mainScroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        content.add(mainScroll, BorderLayout.CENTER);
        page.add(content, BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);
        }


    // --------------------------------------------------
    // COMMAND CENTER HERO
    // --------------------------------------------------

    private JPanel createHero(DataSnapshot s) {
        RoundedPanel hero = new RoundedPanel(24);
        hero.setLayout(new BorderLayout(20, 0));
        hero.setGradient(NAVY, new Color(24, 57, 105));
        hero.setBorder(new EmptyBorder(24, 28, 24, 28));
        hero.setPreferredSize(new Dimension(900, 155));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 155));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("BARANGAY EMERGENCY MANAGEMENT");
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 10));
        eyebrow.setForeground(CYAN);

        JLabel title = new JLabel("Operations Command Center");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel(
                "Evacuation monitoring  /  Resource readiness  /  Situational awareness"
        );
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subtitle.setForeground(new Color(190, 207, 230));

        left.add(eyebrow);
        left.add(Box.createVerticalStrut(9));
        left.add(title);
        left.add(Box.createVerticalStrut(7));
        left.add(subtitle);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setAlignmentY(Component.CENTER_ALIGNMENT);

        JPanel status = new JPanel(new FlowLayout(
                FlowLayout.CENTER, 9, 7
        ));
        status.setOpaque(false);
        status.setBorder(BorderFactory.createLineBorder(
                new Color(74, 207, 170), 1, true
        ));

        JLabel dot = new JLabel("●");
        dot.setForeground(new Color(74, 225, 171));
        dot.setFont(new Font("SansSerif", Font.BOLD, 12));

        JLabel statusText = new JLabel("SYSTEM OVERVIEW");
        statusText.setForeground(Color.WHITE);
        statusText.setFont(new Font("SansSerif", Font.BOLD, 10));

        status.add(dot);
        status.add(statusText);

        JLabel date = new JLabel(
                java.time.LocalDate.now().format(
                        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")
                )
        );
        date.setForeground(new Color(190, 207, 230));
        date.setFont(new Font("SansSerif", Font.PLAIN, 11));
        date.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel time = new JLabel(
                LocalTime.now().format(
                        DateTimeFormatter.ofPattern("hh:mm a")
                )
        );
        time.setForeground(Color.WHITE);
        time.setFont(new Font("SansSerif", Font.BOLD, 17));
        time.setAlignmentX(Component.CENTER_ALIGNMENT);

        right.add(status);
        right.add(Box.createVerticalStrut(9));
        right.add(date);
        right.add(Box.createVerticalStrut(4));
        right.add(time);

        hero.add(left, BorderLayout.CENTER);
        hero.add(right, BorderLayout.EAST);

        return hero;
    }

    // --------------------------------------------------
    // METRIC CARDS
    // --------------------------------------------------

    private JPanel createMetric(
            String title,
            String value,
            String symbol,
            String description,
            Color accent
    ) {
        RoundedPanel card = new RoundedPanel(19);
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout(12, 10));
        card.setBorder(new EmptyBorder(12, 16, 12, 16));
        card.setPreferredSize(new Dimension(220, 140));
        card.setMinimumSize(new Dimension(150, 140));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel label = new JLabel(title);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setForeground(MUTED);

        JLabel icon = new JLabel(symbol, SwingConstants.CENTER);
        icon.setFont(new Font("SansSerif", Font.BOLD, 19));
        icon.setForeground(accent);
        icon.setPreferredSize(new Dimension(38, 34));
        icon.setOpaque(true);
        icon.setBackground(new Color(
                accent.getRed(),
                accent.getGreen(),
                accent.getBlue(),
                22
        ));

        top.add(label, BorderLayout.CENTER);
        top.add(icon, BorderLayout.EAST);

        JLabel number = new JLabel(value);
        number.setFont(new Font("SansSerif", Font.BOLD, 26));
        number.setForeground(TEXT);
        number.setPreferredSize(new Dimension(180, 34));
        number.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel desc = new JLabel(description);
        desc.setFont(new Font("SansSerif", Font.PLAIN, 10));
        desc.setForeground(MUTED);
        desc.setPreferredSize(new Dimension(190, 20));
        desc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(number);
        center.add(Box.createVerticalStrut(3));
        center.add(desc);

        JPanel accentBar = new JPanel();
        accentBar.setBackground(accent);
        accentBar.setPreferredSize(new Dimension(5, 3));
        accentBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3));

        card.add(top, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(accentBar, BorderLayout.SOUTH);

        return card;
    }

    // --------------------------------------------------
    // EVACUATION CENTER MONITORING
    // --------------------------------------------------

    private JPanel createCenterPanel(DataSnapshot s) {
        JPanel panel = createWhitePanel();

        panel.setLayout(new BorderLayout(0, 13));
        panel.setBorder(new EmptyBorder(19, 19, 15, 19));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);

        JLabel title = sectionTitle("Evacuation Center Monitoring");
        JLabel subtitle = smallText("CAPACITY & OCCUPANCY STATUS");

        JPanel headingText = new JPanel();
        headingText.setOpaque(false);
        headingText.setLayout(new BoxLayout(
                headingText, BoxLayout.Y_AXIS
        ));
        headingText.add(title);
        headingText.add(Box.createVerticalStrut(5));
        headingText.add(subtitle);

        JLabel count = new JLabel(s.centers.size() + " CENTERS");
        count.setFont(new Font("SansSerif", Font.BOLD, 10));
        count.setForeground(BLUE);

        heading.add(headingText, BorderLayout.CENTER);
        heading.add(count, BorderLayout.EAST);

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{
                    "CENTER", "CAPACITY", "OCCUPANTS",
                    "AVAILABLE", "STATUS"
                }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (EvacuationCenter c : s.centers) {
            model.addRow(new Object[]{
                c.name,
                c.capacity,
                c.occupants,
                c.available(),
                c.status()
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(39);
        table.setFont(new Font("SansSerif", Font.PLAIN, 11));
        table.setForeground(TEXT);
        table.setBackground(Color.WHITE);
        table.setGridColor(BORDER);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(231, 240, 255));
        table.setSelectionForeground(TEXT);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        table.getTableHeader().setFont(
                new Font("SansSerif", Font.BOLD, 9)
        );
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setBackground(
                new Color(246, 248, 252)
        );
        table.getTableHeader().setPreferredSize(
                new Dimension(0, 34)
        );
        table.getTableHeader().setBorder(
                BorderFactory.createMatteBorder(
                        0, 0, 1, 0, BORDER
                )
        );

        // Color-coded status column
        table.getColumnModel().getColumn(4).setCellRenderer(
                new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(
                            JTable t, Object value,
                            boolean selected, boolean focus,
                            int row, int column
                    ) {
                        JLabel label = (JLabel) super
                                .getTableCellRendererComponent(
                                        t, value, selected, focus,
                                        row, column
                                );

                        label.setHorizontalAlignment(
                                SwingConstants.CENTER
                        );
                        label.setFont(new Font(
                                "SansSerif", Font.BOLD, 10
                        ));

                        String status = value == null
                                ? "" : value.toString().toLowerCase();

                        if (status.contains("full")) {
                            label.setForeground(RED);
                        } else if (status.contains("available")
                                || status.contains("open")) {
                            label.setForeground(GREEN);
                        } else {
                            label.setForeground(ORANGE);
                        }

                        if (!selected) {
                            label.setBackground(Color.WHITE);
                        }

                        return label;
                    }
                }
        );

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(Color.WHITE);

        panel.add(heading, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // --------------------------------------------------
    // RESOURCE ALERTS
    // --------------------------------------------------

    private JPanel createAlertsPanel(DataSnapshot s) {
        JPanel panel = createWhitePanel();
        panel.setLayout(new BorderLayout(0, 13));
        panel.setBorder(new EmptyBorder(19, 19, 15, 19));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);

        JPanel headingText = new JPanel();
        headingText.setOpaque(false);
        headingText.setLayout(new BoxLayout(
                headingText, BoxLayout.Y_AXIS
        ));
        headingText.add(sectionTitle("Resource Readiness"));
        headingText.add(Box.createVerticalStrut(5));
        headingText.add(smallText("SUPPLIES REQUIRING ATTENTION"));

        long low = s.supplies.stream()
                .filter(r -> r.shortage() > 0)
                .count();

        long demandAlerts=SupplyDemand.rows(s).stream().filter(r->r.plan().deficit()>0).count();
        JLabel count = new JLabel((low+demandAlerts) + " ALERTS");
        count.setFont(new Font("SansSerif", Font.BOLD, 10));
        count.setForeground(low + demandAlerts > 0 ? RED : GREEN);

        heading.add(headingText, BorderLayout.CENTER);
        heading.add(count, BorderLayout.EAST);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        int shown = 0;
        if(s.supplyRules.stream().noneMatch(r->r.enabled))list.add(new JLabel("Demand not assessed: configure Supply Rules."));
        for(SupplyDemand.Row row:SupplyDemand.rows(s))if(row.plan().deficit()>0){
            JTextArea demand=new JTextArea(row.center().name+" / "+row.rule().name+"\nDemand shortage: "+row.plan().deficit()+" "+row.rule().unit+" ("+row.rule().days+" days)");
            demand.setEditable(false);demand.setLineWrap(true);demand.setWrapStyleWord(true);list.add(demand);list.add(Box.createVerticalStrut(8));shown++;
        }

        for (Resource r : s.supplies) {
            if (r.shortage() <= 0) {
                continue;
            }

            RoundedPanel alert = new RoundedPanel(13);
            alert.setBackground(new Color(255, 248, 248));
            alert.setBorder(new EmptyBorder(12, 13, 12, 13));
            alert.setLayout(new BorderLayout(10, 0));
            alert.setMaximumSize(new Dimension(
                    Integer.MAX_VALUE, 78
            ));
            alert.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel warning = new JLabel("!");
            warning.setFont(new Font("SansSerif", Font.BOLD, 15));
            warning.setForeground(RED);
            warning.setHorizontalAlignment(SwingConstants.CENTER);
            warning.setPreferredSize(new Dimension(30, 30));
            warning.setOpaque(true);
            warning.setBackground(new Color(255, 225, 229));

            JPanel details = new JPanel();
            details.setOpaque(false);
            details.setLayout(new BoxLayout(
                    details, BoxLayout.Y_AXIS
            ));

            JLabel name = new JLabel(r.name);
            name.setFont(new Font("SansSerif", Font.BOLD, 12));
            name.setForeground(TEXT);

            JLabel shortage = new JLabel(
                    "Low stock deficit: " + r.shortage() + " " + r.unit
            );
            shortage.setFont(new Font("SansSerif", Font.PLAIN, 10));
            shortage.setForeground(new Color(197, 73, 85));

            details.add(name);
            details.add(Box.createVerticalStrut(5));
            details.add(shortage);

            JLabel badge = new JLabel("LOW");
            badge.setFont(new Font("SansSerif", Font.BOLD, 9));
            badge.setForeground(RED);

            alert.add(warning, BorderLayout.WEST);
            alert.add(details, BorderLayout.CENTER);
            alert.add(badge, BorderLayout.EAST);

            list.add(alert);
            list.add(Box.createVerticalStrut(10));
            shown++;
        }

        if (shown == 0 && s.supplyRules.stream().anyMatch(r -> r.enabled)) {
            JLabel clear = new JLabel(
                    "<html><div style='text-align:center;'>"
                    + "<br><b>All clear</b><br>"
                    + "No shortages under configured rules. Review rule coverage."
                    + "</div></html>",
                    SwingConstants.CENTER
            );
            clear.setForeground(GREEN);
            clear.setFont(new Font("SansSerif", Font.PLAIN, 13));
            list.add(clear);
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(15);
        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        panel.add(heading, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // --------------------------------------------------
    // REUSABLE DESIGN COMPONENTS
    // --------------------------------------------------

    private JPanel createWhitePanel() {
        RoundedPanel panel = new RoundedPanel(19);
        panel.setBackground(Color.WHITE);
        return panel;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 15));
        label.setForeground(TEXT);
        return label;
    }

    private JLabel smallText(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 9));
        label.setForeground(MUTED);
        return label;
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private Color gradientStart;
        private Color gradientEnd;

        RoundedPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
        }

        void setGradient(Color start, Color end) {
            gradientStart = start;
            gradientEnd = end;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            if (gradientStart != null && gradientEnd != null) {
                g.setPaint(new GradientPaint(
                        0, 0, gradientStart,
                        getWidth(), getHeight(), gradientEnd
                ));
            } else {
                g.setColor(getBackground());
            }

            g.fill(new RoundRectangle2D.Float(
                    0, 0, getWidth(), getHeight(),
                    radius, radius
            ));

            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
