package barangayems.frontend;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.FlowLayout;
import java.util.Arrays;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class Login extends JPanel {

    private final AppFrame app;

    private JTextField emailField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;

    public Login(AppFrame app) {
        super(new GridBagLayout());

        this.app = app;
        setBackground(UI.BG);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        UI.RoundedPanel card = new UI.RoundedPanel(20, Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(920, 580));

        card.add(brandPanel(
                "Barangay Evacuation Management System",
                "Coordinate evacuation centers, evacuees and relief supplies in one place."),
                BorderLayout.WEST);

        // =========================
        // FORM
        // =========================
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(48, 52, 40, 52));

        JLabel title = UI.label("Welcome back", 26, true);
        title.setAlignmentX(LEFT_ALIGNMENT);
        JLabel description = UI.hint("Sign in with your Barangay-EMS account to continue.");
        description.setFont(UI.font(java.awt.Font.PLAIN, 13));
        description.setAlignmentX(LEFT_ALIGNMENT);

        form.add(title);
        form.add(Box.createVerticalStrut(6));
        form.add(description);
        form.add(Box.createVerticalStrut(28));

        JPanel fields = UI.form();
        fields.setAlignmentX(LEFT_ALIGNMENT);

        emailField = new UI.HintField("name@example.com", false);
        passwordField = new JPasswordField();

        roleCombo = new JComboBox<>(new String[]{
            "Barangay Official",
            "Center Staff"
        });

        UI.field(fields, 0, "Email address", emailField);
        UI.field(fields, 1, "Password", UI.password(passwordField));
        UI.field(fields, 2, "Sign in as", roleCombo);
        fields.setMaximumSize(new Dimension(Integer.MAX_VALUE, fields.getPreferredSize().height));

        form.add(fields);
        form.add(Box.createVerticalStrut(8));

        JButton loginButton = UI.button("Sign in", this::performLogin);
        loginButton.setAlignmentX(LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        form.add(loginButton);
        form.add(Box.createVerticalStrut(18));

        JPanel register = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        register.setOpaque(false);
        register.setAlignmentX(LEFT_ALIGNMENT);
        JLabel newHere = UI.hint("Center staff without an account? ");
        newHere.setFont(UI.font(java.awt.Font.PLAIN, 13));
        JButton registerButton = UI.ghost("Register as Center Staff", () -> this.app.showRegistration());
        registerButton.setBorder(new EmptyBorder(4, 6, 4, 6));
        register.add(newHere);
        register.add(registerButton);
        register.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        form.add(register);

        form.add(Box.createVerticalGlue());

        JLabel note = UI.hint("Use your linked Supabase Auth account. The account determines your role.");
        note.setFont(UI.font(java.awt.Font.PLAIN, 11));
        note.setAlignmentX(LEFT_ALIGNMENT);
        form.add(note);

        card.add(form, BorderLayout.CENTER);

        add(card, new GridBagConstraints());

        // Press Enter in the email or password field to log in
        passwordField.addActionListener(e -> loginButton.doClick());
        emailField.addActionListener(e -> passwordField.requestFocusInWindow());
    }

    /** Navy brand panel shared by the login and registration screens. */
    static JPanel brandPanel(String headline, String tagline) {
        JPanel brand = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(java.awt.Graphics graphics) {
                java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
                g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new java.awt.GradientPaint(0, 0, new Color(18, 44, 82), 0, getHeight(), new Color(9, 21, 41)));
                g.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth() + 30, getHeight() - 1.5f, 20, 20));
                g.setColor(new Color(255, 255, 255, 10));
                g.fillOval(getWidth() - 190, getHeight() - 220, 300, 300);
                g.fillOval(-120, -100, 260, 260);
                g.dispose();
            }
        };
        brand.setOpaque(false);
        brand.setPreferredSize(new Dimension(380, 0));
        brand.setBorder(new EmptyBorder(44, 40, 40, 40));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        javax.swing.ImageIcon icon = Logo.icon(72);
        if (icon != null) {
            JLabel logo = new JLabel(icon);
            logo.setAlignmentX(LEFT_ALIGNMENT);
            top.add(logo);
            top.add(Box.createVerticalStrut(26));
        }

        JLabel name = new JLabel("BARANGAY-EMS");
        name.setFont(UI.font(java.awt.Font.BOLD, 13));
        name.setForeground(new Color(130, 196, 255));
        name.setAlignmentX(LEFT_ALIGNMENT);

        JLabel title = new JLabel("<html><div style='width:230px'>" + headline + "</div></html>");
        title.setFont(UI.font(java.awt.Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel sub = new JLabel("<html><div style='width:230px'>" + tagline + "</div></html>");
        sub.setFont(UI.font(java.awt.Font.PLAIN, 14));
        sub.setForeground(new Color(176, 196, 224));
        sub.setAlignmentX(LEFT_ALIGNMENT);

        top.add(name);
        top.add(Box.createVerticalStrut(10));
        top.add(title);
        top.add(Box.createVerticalStrut(14));
        top.add(sub);

        JPanel points = new JPanel();
        points.setOpaque(false);
        points.setLayout(new BoxLayout(points, BoxLayout.Y_AXIS));
        for (String point : new String[]{"Monitor center capacity in real time",
                "Register and transfer evacuees", "Plan and distribute relief supplies"}) {
            JLabel item = new JLabel("✓   " + point);
            item.setFont(UI.font(java.awt.Font.PLAIN, 13));
            item.setForeground(new Color(206, 220, 240));
            item.setBorder(new EmptyBorder(5, 0, 5, 0));
            points.add(item);
        }

        brand.add(top, BorderLayout.NORTH);
        brand.add(points, BorderLayout.SOUTH);
        return brand;
    }

    // =========================
    // LOGIN ACTION
    // =========================
    private void performLogin() {
        String email = emailField.getText().trim();
        char[] password = passwordField.getPassword();

        if (email.isEmpty() || password.length == 0) {
            Arrays.fill(password, '\0');

            UI.error(
                    this,
                    "Enter your Supabase Auth email and password."
            );
            return;
        }

        try {
            // Keep the existing AppFrame/Supabase login flow.
            this.app.login(
                    email,
                    password,
                    (String) roleCombo.getSelectedItem()
            );
        } catch (RuntimeException ex) {
            Arrays.fill(password, '\0');
            throw ex;
        } finally {
            passwordField.setText("");
        }
    }
}
