package barangayems.frontend;

import barangayems.backend.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AppFrame extends JFrame {

    DataSnapshot store = new DataSnapshot();
    DataManager data;

    private String role = "Barangay Official";
    private boolean busy;

    @FunctionalInterface
    interface DatabaseAction { void run() throws Exception; }

    <T> void task(java.util.concurrent.Callable<T> work, java.util.function.Consumer<T> success) {
        if (busy) return;
        busy = true;

        JPanel overlay = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(13, 27, 46, 70));
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        overlay.setOpaque(false);
        UI.RoundedPanel box = new UI.RoundedPanel(16, Color.WHITE);
        box.setLayout(new BorderLayout(12, 0));
        box.setBorder(new EmptyBorder(18, 22, 18, 26));
        JProgressBar spinner = new JProgressBar();
        spinner.setIndeterminate(true);
        spinner.setPreferredSize(new Dimension(54, 6));
        spinner.setBorderPainted(false);
        spinner.setForeground(UI.BLUE);
        spinner.setBackground(UI.BLUE_PALE);
        JPanel spinWrap = new JPanel(new GridBagLayout());
        spinWrap.setOpaque(false);
        spinWrap.add(spinner);
        JLabel waiting = UI.label("Connecting to Supabase\u2026", 14, true);
        JLabel sub = UI.hint("Please wait a moment");
        JPanel texts = new JPanel(new GridLayout(2, 1, 0, 2));
        texts.setOpaque(false);
        texts.add(waiting);
        texts.add(sub);
        box.add(texts, BorderLayout.CENTER);
        box.add(spinWrap, BorderLayout.EAST);
        overlay.add(box);
        overlay.addMouseListener(new java.awt.event.MouseAdapter() {});
        overlay.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {});
        overlay.setFocusable(true);
        setGlassPane(overlay);
        overlay.setVisible(true);
        overlay.requestFocusInWindow();

        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return work.call(); }
            @Override protected void done() {
                busy = false;
                overlay.setVisible(false);
                try {
                    success.accept(get());
                } catch (Exception e) {
                    Throwable cause = e instanceof java.util.concurrent.ExecutionException ? e.getCause() : e;
                    UI.error(AppFrame.this, cause != null && cause.getMessage() != null
                            ? cause.getMessage() : "The operation failed.");
                }
            }
        }.execute();
    }

    private static final Color NAVY = new Color(13, 31, 58);
    private static final Color NAVY_END = new Color(9, 21, 41);
    private static final Color BLUE = new Color(37, 113, 245);
    private static final Color CYAN = new Color(64, 202, 255);
    private static final Color TEXT = new Color(29, 44, 65);
    private static final Color MUTED = new Color(126, 143, 165);
    private static final Color LINE = new Color(226, 233, 242);
    private static final Color CANVAS = UI.BG;

    public AppFrame() {
        super("Barangay EMS | Emergency Operations Center");

        UI.setup();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1450, 900);
        setMinimumSize(new Dimension(1000, 680));
        if (Logo.image() != null) setIconImage(Logo.image());
        setLocationRelativeTo(null);

        showLogin();
    }

    void showLogin() {
        setContentPane(new Login(this));
        revalidate();
        repaint();
    }

    void showRegistration() {
        setContentPane(new StaffRegistration(this));
        revalidate();
        repaint();
    }

    void login(String email, char[] password, String selectedRole) {
        task(() -> {
            data = new DataManager(SupabaseConfig.load());
            try {
                data.login(email, password, selectedRole);
                return data.load();
            } catch (Exception e) {
                data.clear();
                throw e;
            } finally {
                java.util.Arrays.fill(password, '\0');
            }
        }, snapshot -> {
            store = snapshot;
            role = data.user().role;
            renderPage("Dashboard");
        });
    }

    void mutate(DatabaseAction action, String destination) {
        task(() -> {
            action.run();
            try {
                return data.load();
            } catch (Exception e) {
                throw new java.io.IOException(
                        "The change was saved, but refreshing failed. Click Refresh before submitting it again. " + e.getMessage(), e);
            }
        }, snapshot -> {
            store = snapshot;
            renderPage(destination);
        });
    }

    boolean isOfficial() {
        return data != null && data.user() != null && data.user().isOfficial();
    }

    public void showPage(String name) {
        if (data == null) {
            showLogin();
            return;
        }
        task(() -> data.load(), snapshot -> {
            store = snapshot;
            renderPage(name);
        });
    }

    private static String describe(String page) {
        switch (page) {
            case "Evacuation Centers": return "Manage shelters, their capacity and available facilities";
            case "Evacuees": return "Register, update and transfer evacuees between centers";
            case "Resources": return "Track stock levels and minimum reserves at each center";
            case "Allocation": return "Plan quantities and move supplies between centers";
            case "Reports": return "Generate a report, review it, then export it as a text file";
            case "Staff Applications": return "Review center staff sign-ups and manage their access";
            default: return "Live overview of shelters, evacuees and supplies";
        }
    }

    private void renderPage(String name) {

        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(CANVAS);

        // ================= SIDEBAR =================
        JPanel side = new GradientPanel(NAVY, NAVY_END);
        side.setLayout(new BorderLayout());
        side.setPreferredSize(new Dimension(248, 0));

        // Brand
        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);
        brand.setBorder(new EmptyBorder(24, 20, 22, 16));

        JPanel emblem = new JPanel(new GridBagLayout());
        emblem.setOpaque(false);
        emblem.setPreferredSize(new Dimension(44, 44));
        emblem.setMinimumSize(new Dimension(44, 44));
        ImageIcon brandIcon = Logo.icon(42);
        if (brandIcon != null) {
            JLabel logoLabel = new JLabel(brandIcon);
            logoLabel.setToolTipText("Official Barangay-EMS logo");
            emblem.add(logoLabel);
        } else {
            // Graceful fallback only if the supplied logo asset is missing.
            emblem.setOpaque(true);
            emblem.setBackground(new Color(39, 91, 166));
            JLabel emblemText = new JLabel("B");
            emblemText.setForeground(Color.WHITE);
            emblemText.setFont(UI.font(Font.BOLD, 24));
            emblem.add(emblemText);
        }

        JPanel brandText = new JPanel(new GridLayout(2, 1, 0, 2));
        brandText.setOpaque(false);

        JLabel brandName = new JLabel("Barangay EMS");
        brandName.setForeground(Color.WHITE);
        brandName.setFont(UI.font(Font.BOLD, 16));

        JLabel brandSub = new JLabel("Evacuation Management");
        brandSub.setForeground(new Color(150, 172, 204));
        brandSub.setFont(UI.font(Font.PLAIN, 11));

        brandText.add(brandName);
        brandText.add(brandSub);

        brand.add(emblem, BorderLayout.WEST);
        brand.add(brandText, BorderLayout.CENTER);

        side.add(brand, BorderLayout.NORTH);

        // Navigation
        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBorder(new EmptyBorder(4, 12, 10, 12));

        menu.add(sectionLabel("Menu"));

        String[] routes = {
            "Dashboard",
            "Evacuation Centers",
            "Evacuees",
            "Resources",
            "Allocation",
            "Reports"
        };

        for (String route : routes) {
            menu.add(navButton(route, route.equals(name), () -> showPage(route)));
            menu.add(Box.createVerticalStrut(4));
        }

        if (isOfficial()) {
            menu.add(Box.createVerticalStrut(14));
            menu.add(sectionLabel("Administration"));
            menu.add(navButton("Staff Applications", "Staff Applications".equals(name),
                    () -> showPage("Staff Applications")));
        }

        side.add(menu, BorderLayout.CENTER);

        // Bottom status and actions
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 22)),
                new EmptyBorder(12, 12, 18, 12)));

        JPanel status = new JPanel(new BorderLayout(8, 0));
        status.setOpaque(false);
        status.setBorder(new EmptyBorder(4, 14, 10, 8));
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        status.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel dot = new JLabel("●");
        dot.setForeground(new Color(74, 224, 163));
        dot.setFont(UI.font(Font.BOLD, 10));

        JLabel statusText = new JLabel("Connected to Supabase");
        statusText.setForeground(new Color(160, 182, 212));
        statusText.setFont(UI.font(Font.PLAIN, 12));

        status.add(dot, BorderLayout.WEST);
        status.add(statusText, BorderLayout.CENTER);

        JButton refresh = navButton("Refresh data", false, () -> showPage(name));
        refresh.setIcon(new NavIcon("Refresh", false));
        refresh.setToolTipText("Reload the latest records from the database");

        JButton logout = navButton("Sign out", false, null);
        logout.setIcon(new NavIcon("Sign out", false));

        logout.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(
                    this,
                    "Log out of Barangay-EMS?",
                    "Confirm sign out",
                    JOptionPane.YES_NO_OPTION
            ) == JOptionPane.YES_OPTION) {
                task(() -> {
                    String warning = null;
                    try { data.logout(); }
                    catch (Exception ex) { warning = "Logged out locally. Supabase could not confirm session revocation."; }
                    return warning;
                }, warning -> {
                    store = new DataSnapshot();
                    data = null;
                    showLogin();
                    if (warning != null) UI.error(this, warning);
                });
            }
        });

        bottom.add(status);
        bottom.add(refresh);
        bottom.add(Box.createVerticalStrut(4));
        bottom.add(logout);

        side.add(bottom, BorderLayout.SOUTH);

        // ================= MAIN AREA =================
        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(CANVAS);

        // Top bar: page title + short description, user on the right
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(Color.WHITE);
        topbar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, LINE),
                new EmptyBorder(16, 24, 16, 24)
        ));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel pageTitle = new JLabel(name);
        pageTitle.setForeground(TEXT);
        pageTitle.setFont(UI.font(Font.BOLD, 21));

        JLabel pageDesc = new JLabel(describe(name));
        pageDesc.setForeground(MUTED);
        pageDesc.setFont(UI.font(Font.PLAIN, 13));

        heading.add(pageTitle);
        heading.add(Box.createVerticalStrut(2));
        heading.add(pageDesc);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        userPanel.setOpaque(false);

        JLabel date = new JLabel(
                LocalDate.now().format(
                        DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")));
        date.setForeground(MUTED);
        date.setFont(UI.font(Font.PLAIN, 12));
        date.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 0, 1, LINE),
                new EmptyBorder(0, 0, 0, 16)));

        JLabel avatar = new JLabel(userInitial(), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(UI.BLUE_PALE);
                g.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        avatar.setPreferredSize(new Dimension(36, 36));
        avatar.setForeground(BLUE);
        avatar.setFont(UI.font(Font.BOLD, 15));

        JPanel userText = new JPanel(new GridLayout(2, 1, 0, 1));
        userText.setOpaque(false);

        JLabel username = new JLabel(data != null && data.user() != null ? data.user().fullName : "User");
        username.setForeground(TEXT);
        username.setFont(UI.font(Font.BOLD, 13));

        JLabel userRole = new JLabel(role);
        userRole.setForeground(MUTED);
        userRole.setFont(UI.font(Font.PLAIN, 11));

        userText.add(username);
        userText.add(userRole);

        userPanel.add(date);
        userPanel.add(Box.createHorizontalStrut(4));
        userPanel.add(avatar);
        userPanel.add(userText);

        JPanel userWrap = new JPanel(new GridBagLayout());
        userWrap.setOpaque(false);
        userWrap.add(userPanel);

        topbar.add(heading, BorderLayout.WEST);
        topbar.add(userWrap, BorderLayout.EAST);

        area.add(topbar, BorderLayout.NORTH);

        // Preserve the existing page classes and their functionality
        JPanel content;

        switch (name) {
            case "Staff Applications":
                content = isOfficial() ? new StaffApplications(this) : new Dashboard(this);
                break;
            case "Evacuation Centers":
                content = new EvacuationCenters(this);
                break;

            case "Evacuees":
                content = new Evacuees(this);
                break;

            case "Resources":
                content = new Resources(this);
                break;

            case "Allocation":
                content = new Allocation(this);
                break;

            case "Reports":
                content = new Reports(this);
                break;

            default:
                content = new Dashboard(this);
                break;
        }

        area.add(content, BorderLayout.CENTER);

        shell.add(side, BorderLayout.WEST);
        shell.add(area, BorderLayout.CENTER);

        setContentPane(shell);
        revalidate();
        repaint();
    }

    private static JComponent sectionLabel(String text) {
        JLabel section = new JLabel(text.toUpperCase());
        section.setForeground(new Color(110, 136, 172));
        section.setFont(UI.font(Font.BOLD, 10));
        section.setBorder(new EmptyBorder(6, 14, 8, 0));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return section;
    }

    private static JButton navButton(String route, boolean selected, Runnable action) {
        JButton button = new JButton(route) {
            private boolean hover;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) { hover = true; repaint(); }
                    @Override public void mouseExited(java.awt.event.MouseEvent e) { hover = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics graphics) {
                if (selected || hover) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(selected ? new Color(255, 255, 255, 30) : new Color(255, 255, 255, 14));
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    if (selected) {
                        g.setColor(new Color(96, 184, 255));
                        g.fillRoundRect(0, 10, 3, getHeight() - 20, 3, 3);
                    }
                    g.dispose();
                }
                super.paintComponent(graphics);
            }
        };
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setPreferredSize(new Dimension(220, 42));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIcon(new NavIcon(route, selected));
        button.setIconTextGap(12);
        button.setFont(UI.font(selected ? Font.BOLD : Font.PLAIN, 13));
        button.setForeground(selected ? Color.WHITE : new Color(186, 202, 224));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setBorder(new EmptyBorder(0, 14, 0, 10));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (action != null) button.addActionListener(e -> action.run());
        return button;
    }

    private String userInitial() {
        if (data == null || data.user() == null || data.user().fullName == null || data.user().fullName.isBlank()) return "U";
        return data.user().fullName.substring(0, 1).toUpperCase();
    }

    // Smooth gradient backgrounds
    private static class GradientPanel extends JPanel {

        private final Color first;
        private final Color second;

        GradientPanel(Color first, Color second) {
            this.first = first;
            this.second = second;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            g.setPaint(new GradientPaint(
                    0, 0, first,
                    0, getHeight(), second));

            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();

            super.paintComponent(graphics);
        }
    }

    // Custom navigation icons
    private static final class NavIcon implements Icon {

        private final String type;
        private final boolean selected;

        NavIcon(String type, boolean selected) {
            this.type = type;
            this.selected = selected;
        }

        public int getIconWidth() {
            return 20;
        }

        public int getIconHeight() {
            return 20;
        }

        public void paintIcon(
                Component component,
                Graphics graphics,
                int x,
                int y) {

            Graphics2D g = (Graphics2D) graphics.create();

            g.translate(x, y);
            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(selected
                    ? new Color(130, 204, 255)
                    : new Color(140, 165, 199));

            g.setStroke(new BasicStroke(
                    1.6f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));

            switch (type) {

                case "Dashboard":
                    g.drawRoundRect(2, 2, 7, 7, 2, 2);
                    g.drawRoundRect(11, 2, 7, 7, 2, 2);
                    g.drawRoundRect(2, 11, 7, 7, 2, 2);
                    g.drawRoundRect(11, 11, 7, 7, 2, 2);
                    break;

                case "Evacuation Centers":
                    g.drawPolyline(new int[]{2, 10, 18}, new int[]{9, 2, 9}, 3);
                    g.drawRect(4, 8, 12, 10);
                    g.drawRect(8, 12, 4, 6);
                    break;

                case "Evacuees":
                    g.drawOval(4, 2, 6, 6);
                    g.drawArc(1, 10, 12, 12, 0, 180);
                    g.drawOval(12, 4, 5, 5);
                    g.drawArc(11, 11, 8, 9, 0, 150);
                    break;

                case "Resources":
                    g.drawRoundRect(2, 6, 16, 12, 3, 3);
                    g.drawLine(2, 11, 18, 11);
                    g.drawRoundRect(7, 2, 6, 4, 2, 2);
                    break;

                case "Staff Applications":
                    g.drawOval(4, 2, 8, 8);
                    g.drawArc(1, 11, 14, 12, 0, 180);
                    g.drawPolyline(new int[]{13, 15, 19}, new int[]{9, 11, 6}, 3);
                    break;

                case "Reports":
                    g.drawRoundRect(3, 2, 14, 16, 3, 3);
                    g.drawLine(7, 7, 13, 7);
                    g.drawLine(7, 10, 13, 10);
                    g.drawLine(7, 13, 11, 13);
                    break;

                case "Refresh":
                    g.drawArc(3, 3, 14, 14, 30, 280);
                    g.drawPolyline(new int[]{13, 17, 17}, new int[]{4, 5, 1}, 3);
                    break;

                case "Sign out":
                    g.drawPolyline(new int[]{11, 3, 3, 11}, new int[]{3, 3, 17, 17}, 4);
                    g.drawLine(8, 10, 18, 10);
                    g.drawPolyline(new int[]{14, 18, 14}, new int[]{6, 10, 14}, 3);
                    break;

                default:
                    g.drawLine(2, 6, 18, 6);
                    g.drawPolyline(
                            new int[]{13, 18, 13},
                            new int[]{2, 6, 10}, 3);
                    g.drawLine(18, 14, 2, 14);
                    g.drawPolyline(
                            new int[]{7, 2, 7},
                            new int[]{10, 14, 18}, 3);
                    break;
            }

            g.dispose();
        }
    }
}
