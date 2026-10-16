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

        JPanel overlay = new JPanel(new GridBagLayout());
        overlay.setOpaque(false);
        JLabel waiting = UI.label("Connecting to Supabase…", 16, true);
        waiting.setOpaque(true);
        waiting.setBackground(UI.BG);
        waiting.setBorder(new CompoundBorder(
                new LineBorder(UI.LINE, 1, true),
                new EmptyBorder(20, 30, 20, 30)));
        overlay.add(waiting);
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

    private static final Color NAVY = new Color(8, 17, 35);
    private static final Color NAVY_END = new Color(16, 35, 65);
    private static final Color BLUE = new Color(37, 113, 245);
    private static final Color CYAN = new Color(64, 202, 255);
    private static final Color TEXT = new Color(29, 44, 65);
    private static final Color MUTED = new Color(126, 143, 165);
    private static final Color LINE = new Color(226, 233, 242);
    private static final Color CANVAS = new Color(244, 247, 252);

    public AppFrame() {
        super("Barangay EMS | Emergency Operations Center");

        UI.setup();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1450, 900);
        setMinimumSize(new Dimension(1120, 720));
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

    private void renderPage(String name) {

        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(CANVAS);

        // SIDEBAR
        JPanel side = new GradientPanel(NAVY, NAVY_END);
        side.setLayout(new BorderLayout());
        side.setPreferredSize(new Dimension(260, 0));
        side.setBorder(new MatteBorder(
                0, 0, 0, 1, new Color(37, 61, 94)));

        // Brand
        JPanel brand = new JPanel(new BorderLayout(13, 0));
        brand.setOpaque(false);
        brand.setBorder(new EmptyBorder(28, 22, 30, 16));

        JPanel emblem = new GradientPanel(
                new Color(39, 117, 255),
                new Color(31, 66, 170));

        emblem.setLayout(new GridBagLayout());
        emblem.setPreferredSize(new Dimension(48, 48));
        emblem.setBorder(new LineBorder(
                new Color(105, 177, 255), 1, true));

        JLabel emblemText = new JLabel("B");
        emblemText.setForeground(Color.WHITE);
        emblemText.setFont(new Font("SansSerif", Font.BOLD, 28));
        emblem.add(emblemText);

        JPanel brandText = new JPanel(new GridLayout(2, 1, 0, 5));
        brandText.setOpaque(false);

        JLabel brandName = new JLabel("BARANGAY EMS");
        brandName.setForeground(Color.WHITE);
        brandName.setFont(new Font("SansSerif", Font.BOLD, 16));

        JLabel brandSub = new JLabel("EMERGENCY OPERATIONS");
        brandSub.setForeground(new Color(146, 174, 213));
        brandSub.setFont(new Font("SansSerif", Font.BOLD, 9));

        brandText.add(brandName);
        brandText.add(brandSub);

        brand.add(emblem, BorderLayout.WEST);
        brand.add(brandText, BorderLayout.CENTER);

        side.add(brand, BorderLayout.NORTH);

        // Navigation
        JPanel navWrap = new JPanel(new BorderLayout());
        navWrap.setOpaque(false);
        navWrap.setBorder(new EmptyBorder(4, 14, 10, 14));

        JLabel section = new JLabel("   MAIN WORKSPACE");
        section.setForeground(new Color(115, 145, 184));
        section.setFont(new Font("SansSerif", Font.BOLD, 10));
        section.setBorder(new EmptyBorder(10, 0, 17, 0));

        navWrap.add(section, BorderLayout.NORTH);

        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));

        String[] routes = {
            "Dashboard",
            "Evacuation Centers",
            "Evacuees",
            "Resources",
            "Allocation",
            "Reports"
        };

        for (String route : routes) {

            boolean selected = route.equals(name);

            JButton button = new JButton(route);
            button.setMaximumSize(
                    new Dimension(Integer.MAX_VALUE, 49));
            button.setPreferredSize(new Dimension(225, 49));

            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setIcon(new NavIcon(route, selected));
            button.setIconTextGap(15);

            button.setFont(new Font(
                    "SansSerif",
                    selected ? Font.BOLD : Font.PLAIN,
                    13
            ));

            button.setForeground(selected
                    ? Color.WHITE
                    : new Color(184, 202, 226));

            button.setBackground(selected
                    ? new Color(39, 91, 166)
                    : new Color(12, 27, 49));

            button.setOpaque(true);
            button.setBorder(new EmptyBorder(0, 16, 0, 10));
            button.setFocusPainted(false);
            button.setContentAreaFilled(true);
            button.setCursor(
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            if (selected) {
                button.setBorder(new CompoundBorder(
                        new MatteBorder(
                                0, 3, 0, 0,
                                new Color(76, 177, 255)),
                        new EmptyBorder(0, 13, 0, 10)
                ));
            }

            button.addActionListener(e -> showPage(route));

            menu.add(button);
            menu.add(Box.createVerticalStrut(7));
        }

        if (isOfficial()) {
            JButton staff = UI.button("Staff Applications", () -> showPage("Staff Applications"));
            staff.setMaximumSize(new Dimension(Integer.MAX_VALUE, 49));
            menu.add(staff);
        }

        navWrap.add(menu, BorderLayout.CENTER);
        side.add(navWrap, BorderLayout.CENTER);

        // Bottom status and logout
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(new EmptyBorder(10, 16, 22, 16));

        JPanel status = new JPanel(new BorderLayout(10, 0));
        status.setBackground(new Color(22, 45, 74));
        status.setBorder(new CompoundBorder(
                new LineBorder(new Color(48, 76, 111), 1, true),
                new EmptyBorder(13, 12, 13, 12)
        ));
        status.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 66));

        JLabel dot = new JLabel("●");
        dot.setForeground(new Color(74, 224, 163));
        dot.setFont(new Font("SansSerif", Font.BOLD, 13));

        JPanel statusText = new JPanel(
                new GridLayout(2, 1, 0, 5));
        statusText.setOpaque(false);

        JLabel statusTitle = new JLabel("SYSTEM OPERATIONAL");
        statusTitle.setForeground(Color.WHITE);
        statusTitle.setFont(new Font("SansSerif", Font.BOLD, 10));

        JLabel statusSub = new JLabel("Supabase database connected");
        statusSub.setForeground(new Color(153, 179, 210));
        statusSub.setFont(new Font("SansSerif", Font.PLAIN, 10));

        statusText.add(statusTitle);
        statusText.add(statusSub);

        status.add(dot, BorderLayout.WEST);
        status.add(statusText, BorderLayout.CENTER);

        JButton logout = new JButton("↪    Sign out of system");
        logout.setHorizontalAlignment(SwingConstants.LEFT);
        logout.setForeground(new Color(192, 208, 229));
        logout.setFont(new Font("SansSerif", Font.PLAIN, 12));
        logout.setOpaque(false);
        logout.setContentAreaFilled(false);
        logout.setBorder(new EmptyBorder(16, 5, 5, 5));
        logout.setFocusPainted(false);
        logout.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 45));

        JButton refresh = new JButton("↻    Refresh database");
        refresh.setHorizontalAlignment(SwingConstants.LEFT);
        refresh.setForeground(new Color(192, 208, 229));
        refresh.setFont(new Font("SansSerif", Font.PLAIN, 12));
        refresh.setOpaque(false);
        refresh.setContentAreaFilled(false);
        refresh.setBorder(new EmptyBorder(14, 5, 2, 5));
        refresh.setFocusPainted(false);
        refresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refresh.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        refresh.addActionListener(e -> showPage(name));

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
        bottom.add(Box.createVerticalStrut(4));
        bottom.add(refresh);
        bottom.add(Box.createVerticalStrut(3));
        bottom.add(logout);

        side.add(bottom, BorderLayout.SOUTH);

        // MAIN AREA
        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(CANVAS);

        // Top bar
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(Color.WHITE);
        topbar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, LINE),
                new EmptyBorder(16, 30, 16, 30)
        ));

        JPanel breadcrumb = new JPanel();
        breadcrumb.setOpaque(false);
        breadcrumb.setLayout(new BoxLayout(
                breadcrumb, BoxLayout.Y_AXIS));

        JLabel crumb = new JLabel(
                "OPERATIONS     /     " + name.toUpperCase());
        crumb.setForeground(MUTED);
        crumb.setFont(new Font("SansSerif", Font.BOLD, 10));

        JLabel pageTitle = new JLabel(name);
        pageTitle.setForeground(TEXT);
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 21));

        breadcrumb.add(crumb);
        breadcrumb.add(Box.createVerticalStrut(5));
        breadcrumb.add(pageTitle);

        JPanel userPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 12, 0));
        userPanel.setOpaque(false);

        JLabel date = new JLabel(
                LocalDate.now().format(
                        DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy")));

        date.setForeground(MUTED);
        date.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JPanel avatar = new GradientPanel(BLUE, CYAN);
        avatar.setLayout(new GridBagLayout());
        avatar.setPreferredSize(new Dimension(39, 39));
        avatar.setBorder(new LineBorder(
                new Color(214, 229, 255), 1, true));

        JLabel avatarText = new JLabel(userInitial());
        avatarText.setForeground(Color.WHITE);
        avatarText.setFont(new Font("SansSerif", Font.BOLD, 16));
        avatar.add(avatarText);

        JPanel userText = new JPanel(new GridLayout(2, 1, 0, 4));
        userText.setOpaque(false);

        JLabel username = new JLabel(data != null && data.user() != null ? data.user().fullName : "User");
        username.setForeground(TEXT);
        username.setFont(new Font("SansSerif", Font.BOLD, 12));

        JLabel userRole = new JLabel(role);
        userRole.setForeground(MUTED);
        userRole.setFont(new Font("SansSerif", Font.PLAIN, 10));

        userText.add(username);
        userText.add(userRole);

        userPanel.add(date);
        userPanel.add(Box.createHorizontalStrut(8));
        userPanel.add(avatar);
        userPanel.add(userText);

        topbar.add(breadcrumb, BorderLayout.WEST);
        topbar.add(userPanel, BorderLayout.EAST);

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

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(CANVAS);
        footer.setBorder(new EmptyBorder(0, 30, 12, 30));

        JLabel footerText = new JLabel(
                "BARANGAY EMS    •    SUPABASE CONNECTED ENVIRONMENT"
        );
        footerText.setForeground(new Color(143, 156, 176));
        footerText.setFont(new Font("SansSerif", Font.BOLD, 9));

        footer.add(footerText, BorderLayout.WEST);
        area.add(footer, BorderLayout.SOUTH);

        shell.add(side, BorderLayout.WEST);
        shell.add(area, BorderLayout.CENTER);

        setContentPane(shell);
        revalidate();
        repaint();
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
                    getWidth(), getHeight(), second));

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
            return 21;
        }

        public int getIconHeight() {
            return 21;
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
                    ? new Color(125, 204, 255)
                    : new Color(145, 174, 210));

            g.setStroke(new BasicStroke(
                    1.7f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));

            switch (type) {

                case "Dashboard":
                    g.drawPolyline(
                            new int[]{1, 10, 19},
                            new int[]{9, 1, 9}, 3);
                    g.drawRect(4, 8, 12, 11);
                    g.drawRect(8, 12, 4, 7);
                    break;

                case "Evacuation Centers":
                    g.drawRect(3, 2, 14, 17);
                    for (int a = 6; a < 16; a += 5) {
                        for (int b = 5; b < 15; b += 5) {
                            g.fillRect(a, b, 2, 2);
                        }
                    }
                    break;

                case "Evacuees":
                    g.drawOval(7, 1, 6, 6);
                    g.drawArc(3, 9, 14, 14, 0, 180);
                    g.drawLine(3, 16, 17, 16);
                    break;

                case "Resources":
                    g.drawRect(2, 6, 16, 13);
                    g.drawLine(2, 11, 18, 11);
                    g.drawRect(7, 2, 6, 4);
                    break;

                case "Reports":
                    for (int i = 0; i < 3; i++) {
                        g.fillRoundRect(
                                3 + i * 6,
                                12 - i * 4,
                                4,
                                8 + i * 4,
                                2, 2);
                    }
                    break;

                default:
                    g.drawLine(2, 6, 18, 6);
                    g.drawPolyline(
                            new int[]{13, 18, 13},
                            new int[]{2, 6, 10}, 3);
                    g.drawLine(18, 15, 2, 15);
                    g.drawPolyline(
                            new int[]{7, 2, 7},
                            new int[]{11, 15, 19}, 3);
                    break;
            }

            g.dispose();
        }
    }
}
