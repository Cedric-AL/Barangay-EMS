
package barangayems.frontend;

import barangayems.backend.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Dashboard extends JPanel {

    private static final Color TEXT = UI.INK;
    private static final Color MUTED = UI.MUTED;

    public Dashboard(AppFrame app) {
        super(new BorderLayout());

        DataSnapshot s = new DataSnapshot();
        s.centers = app.store.activeCenters();
        s.people = app.store.people;
        s.supplyRules = app.store.supplyRules;
        s.supplies = app.store.supplies.stream().filter(r -> s.centers.stream().anyMatch(c -> c.id == r.centerId)).toList();

        JPanel page = UI.page("Dashboard");

        // ---------- KPI ROW ----------
        long full = s.centers.stream()
                .filter(c -> c.available() == 0)
                .count();

        long low = s.supplies.stream()
                .filter(r -> r.shortage() > 0)
                .count();

        long demandShortages = SupplyDemand.rows(s).stream().filter(r -> r.plan().deficit() > 0).count();

        int capacity = s.capacity();
        int occupants = s.occupants();
        int percent = capacity == 0 ? 0 : (int) Math.round(occupants * 100.0 / capacity);

        JPanel stats = new JPanel(new GridLayout(1, 6, 12, 0));
        stats.setOpaque(false);
        stats.add(metric("Evacuation centers", "" + s.centers.size(), "Active and registered", null));
        stats.add(metric("Evacuees", "" + occupants, "Currently accommodated", null));
        stats.add(metric("Available spaces", "" + (capacity - occupants),
                percent + "% of capacity in use", null));
        stats.add(metric("Full centers", "" + full, "No remaining capacity",
                full > 0 ? UI.RED : null));
        stats.add(metric("Low stock items", "" + low, "Below minimum reserve",
                low > 0 ? UI.AMBER : null));
        stats.add(metric("Demand shortages", "" + demandShortages, "Center needs not yet met",
                demandShortages > 0 ? UI.RED : null));

        page.add(stats, BorderLayout.NORTH);

        // ---------- LOWER PANELS ----------
        JPanel lower = new JPanel(new GridLayout(1, 2, 16, 0));
        lower.setOpaque(false);
        lower.add(createCenterPanel(s));
        lower.add(createAlertsPanel(s, low + demandShortages));

        page.add(lower, BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);
    }

    // --------------------------------------------------
    // KPI CARD
    // --------------------------------------------------

    private JPanel metric(String title, String value, String description, Color alert) {
        UI.RoundedPanel card = new UI.RoundedPanel(UI.RADIUS, Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14, 16, 14, 14));

        JLabel label = new JLabel(title);
        label.setFont(UI.font(Font.BOLD, 12));
        label.setForeground(UI.TEXT_SOFT);

        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.setOpaque(false);
        top.add(label, BorderLayout.CENTER);
        if (alert != null) {
            JLabel dot = new JLabel("●");
            dot.setForeground(alert);
            dot.setFont(UI.font(Font.BOLD, 10));
            top.add(dot, BorderLayout.EAST);
        }
        top.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel number = new JLabel(value);
        number.setFont(UI.font(Font.BOLD, 28));
        number.setForeground(alert != null ? alert : TEXT);
        number.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc = new JLabel(description);
        desc.setFont(UI.font(Font.PLAIN, 12));
        desc.setForeground(MUTED);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(top);
        card.add(Box.createVerticalStrut(6));
        card.add(number);
        card.add(Box.createVerticalStrut(2));
        card.add(desc);
        return card;
    }

    // --------------------------------------------------
    // EVACUATION CENTER OCCUPANCY
    // --------------------------------------------------

    private JPanel createCenterPanel(DataSnapshot s) {
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        if (s.centers.isEmpty()) {
            list.add(empty("No evacuation centers yet", "Add one from the Evacuation Centers page."));
        }

        boolean first = true;
        for (EvacuationCenter c : s.centers) {
            JPanel row = new JPanel(new BorderLayout(0, 8));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(first ? 2 : 14, 0, 14, 0));
            if (!first) {
                row.setBorder(new javax.swing.border.CompoundBorder(
                        new MatteBorder(1, 0, 0, 0, UI.LINE), new EmptyBorder(14, 0, 14, 0)));
            }
            first = false;

            JPanel line = new JPanel(new BorderLayout(10, 0));
            line.setOpaque(false);

            JLabel name = new JLabel(c.name);
            name.setFont(UI.font(Font.BOLD, 13));
            name.setForeground(TEXT);

            JLabel numbers = new JLabel(c.occupants + " / " + c.capacity + "   ·   " + c.available() + " free");
            numbers.setFont(UI.font(Font.PLAIN, 12));
            numbers.setForeground(MUTED);

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            right.setOpaque(false);
            right.add(numbers);
            right.add(UI.pill(c.status()));

            line.add(name, BorderLayout.CENTER);
            line.add(right, BorderLayout.EAST);

            double ratio = c.capacity == 0 ? 0 : Math.min(1.0, c.occupants / (double) c.capacity);
            Color barColor = UI.tone(c.status())[0];

            row.add(line, BorderLayout.NORTH);
            row.add(new Bar(ratio, barColor), BorderLayout.CENTER);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            list.add(row);
        }

        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(list, BorderLayout.NORTH);

        JLabel count = UI.caption(s.centers.size() + (s.centers.size() == 1 ? " center" : " centers"));

        return UI.card("Center occupancy", "Capacity used at each active evacuation center", count, scroll(holder));
    }

    /** Thin horizontal progress bar. */
    private static final class Bar extends JComponent {
        private final double ratio;
        private final Color color;

        Bar(double ratio, Color color) {
            this.ratio = ratio;
            this.color = color;
            setPreferredSize(new Dimension(100, 8));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = 8, y = (getHeight() - h) / 2;
            g.setColor(UI.GRAY_PALE);
            g.fill(new RoundRectangle2D.Float(0, y, getWidth(), h, h, h));
            int w = (int) Math.round(getWidth() * ratio);
            if (w > 0) {
                g.setColor(color);
                g.fill(new RoundRectangle2D.Float(0, y, Math.max(w, h), h, h, h));
            }
            g.dispose();
        }
    }

    // --------------------------------------------------
    // RESOURCE ALERTS
    // --------------------------------------------------

    private JPanel createAlertsPanel(DataSnapshot s, long alerts) {
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        int shown = 0;
        if (s.supplyRules.stream().noneMatch(r -> r.enabled)) {
            list.add(alertRow("Demand not assessed", "Configure Supply Rules on the Resources page to check center needs.", "Setup"));
            shown++;
        }

        for (SupplyDemand.Row row : SupplyDemand.rows(s)) {
            if (row.plan().deficit() > 0) {
                list.add(alertRow(
                        row.center().name + "  ·  " + row.rule().name,
                        "Short by " + row.plan().deficit() + " " + row.rule().unit
                                + " to cover " + row.rule().days + " day" + (row.rule().days == 1 ? "" : "s"),
                        "Shortage"));
                shown++;
            }
        }

        for (Resource r : s.supplies) {
            if (r.shortage() <= 0) {
                continue;
            }
            list.add(alertRow(
                    r.center + "  ·  " + r.name,
                    r.remaining() + " " + r.unit + " left, " + r.shortage() + " below the minimum of " + r.required,
                    "Low stock"));
            shown++;
        }

        if (shown == 0 && s.supplyRules.stream().anyMatch(r -> r.enabled)) {
            list.add(empty("All clear", "No shortages under the configured rules. Review rule coverage regularly."));
        }

        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(list, BorderLayout.NORTH);

        UI.Pill count = UI.pill(alerts == 0 ? "No alerts" : alerts + (alerts == 1 ? " alert" : " alerts"));
        if (alerts == 0) count.colors(UI.GREEN, UI.GREEN_PALE);
        else count.colors(UI.RED, UI.RED_PALE);

        return UI.card("Needs attention", "Supplies that are short or below their minimum reserve", count, scroll(holder));
    }

    private JPanel alertRow(String title, String detail, String tag) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(new javax.swing.border.CompoundBorder(
                new MatteBorder(0, 0, 1, 0, UI.LINE), new EmptyBorder(12, 0, 12, 0)));

        Color accent = tag.equals("Low stock") ? UI.AMBER : tag.equals("Setup") ? UI.BLUE : UI.RED;
        JComponent stripe = new JComponent() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(accent);
                g.fillRoundRect(0, 2, 4, getHeight() - 4, 4, 4);
                g.dispose();
            }
        };
        stripe.setPreferredSize(new Dimension(4, 34));

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 3));
        text.setOpaque(false);
        JLabel name = new JLabel(title);
        name.setFont(UI.font(Font.BOLD, 13));
        name.setForeground(TEXT);
        JLabel info = new JLabel(detail);
        info.setFont(UI.font(Font.PLAIN, 12));
        info.setForeground(UI.TEXT_SOFT);
        text.add(name);
        text.add(info);

        UI.Pill pill = UI.pill(tag);
        if (tag.equals("Low stock")) pill.colors(UI.AMBER, UI.AMBER_PALE);
        if (tag.equals("Setup")) pill.colors(UI.BLUE, UI.BLUE_PALE);
        JPanel tagWrap = new JPanel(new GridBagLayout());
        tagWrap.setOpaque(false);
        tagWrap.add(pill);

        row.add(stripe, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        row.add(tagWrap, BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    // --------------------------------------------------
    // HELPERS
    // --------------------------------------------------

    private JComponent empty(String title, String detail) {
        JLabel label = new JLabel(
                "<html><div style='text-align:center;'><b>" + title + "</b><br>"
                        + "<span style='color:#6E7B8F'>" + detail + "</span></div></html>",
                SwingConstants.CENTER);
        label.setFont(UI.font(Font.PLAIN, 13));
        label.setForeground(UI.GREEN);
        label.setBorder(new EmptyBorder(30, 10, 30, 10));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        return label;
    }

    private JScrollPane scroll(JComponent content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }
}
