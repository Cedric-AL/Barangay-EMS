package barangayems.frontend;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class Login extends JPanel {

    private static final Color BG = new Color(244, 247, 251);
    private static final Color WHITE = Color.WHITE;
    private static final Color NAVY = new Color(29, 45, 67);
    private static final Color BLUE = new Color(37, 103, 216);
    private static final Color LINE = new Color(205, 215, 228);
    private static final Color MUTED = new Color(100, 116, 139);

    private final AppFrame app;

    private JTextField emailField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;

    public Login(AppFrame app) {
        super(new GridBagLayout());

        this.app = app;
        setBackground(BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Main login card
        JPanel body = new JPanel(new BorderLayout(0, 22));
        body.setBackground(WHITE);
        body.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(LINE, 1),
                new EmptyBorder(24, 40, 24, 40)
        ));
        body.setPreferredSize(new Dimension(620, 650));
        body.setMinimumSize(new Dimension(500, 620));

        // =========================
        // HEADER AND LOGO
        // =========================
        JPanel header = new JPanel();
        header.setBackground(WHITE);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        try {
            javax.swing.ImageIcon icon = Logo.icon(85);

            if (icon != null) {
                JLabel logo = new JLabel(icon);
                logo.setAlignmentX(CENTER_ALIGNMENT);
                header.add(logo);
                header.add(Box.createVerticalStrut(8));
            }
        } catch (Exception ex) {
            System.err.println("Could not load Barangay-EMS logo: "
                    + ex.getMessage());
        }

        JLabel brand = new JLabel("BARANGAY-EMS");
        brand.setFont(new java.awt.Font(
                "SansSerif", java.awt.Font.BOLD, 32));
        brand.setForeground(NAVY);
        brand.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel(
                "Barangay Evacuation Management System");
        subtitle.setFont(new java.awt.Font(
                "SansSerif", java.awt.Font.BOLD, 15));
        subtitle.setForeground(NAVY);
        subtitle.setAlignmentX(CENTER_ALIGNMENT);

        JLabel description = new JLabel(
                "Sign in to access your account");
        description.setFont(new java.awt.Font(
                "SansSerif", java.awt.Font.PLAIN, 12));
        description.setForeground(MUTED);
        description.setAlignmentX(CENTER_ALIGNMENT);

        header.add(brand);
        header.add(Box.createVerticalStrut(8));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(8));
        header.add(description);
        header.add(Box.createVerticalStrut(16));

        JSeparator headerLine = new JSeparator();
        headerLine.setForeground(LINE);
        headerLine.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 1));
        header.add(headerLine);

        body.add(header, BorderLayout.NORTH);

        // =========================
        // LOGIN FIELDS
        // =========================
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setBackground(WHITE);
        fields.setOpaque(true);

        emailField = new JTextField();
        passwordField = new JPasswordField();

        roleCombo = new JComboBox<>(new String[]{
            "Barangay Official",
            "Center Staff"
        });

        styleInput(emailField);
        styleInput(passwordField);
        styleComboBox(roleCombo);

        addLoginField(fields, 0, "Email Address", emailField);
        JPanel passwordRow = new JPanel(new BorderLayout(8, 0));
        passwordRow.setBackground(WHITE);
        passwordRow.add(passwordField, BorderLayout.CENTER);
        javax.swing.JToggleButton showPassword = new javax.swing.JToggleButton("Show password");
        char passwordMask = passwordField.getEchoChar();
        showPassword.addActionListener(e -> {
            passwordField.setEchoChar(showPassword.isSelected() ? (char)0 : passwordMask);
            showPassword.setText(showPassword.isSelected() ? "Hide password" : "Show password");
        });
        passwordRow.add(showPassword, BorderLayout.EAST);
        addLoginField(fields, 1, "Password", passwordRow);
        addLoginField(fields, 2, "Sign in as", roleCombo);

        fields.setBorder(new EmptyBorder(10, 0, 10, 0));
        fields.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(fields, BorderLayout.CENTER);

        // =========================
        // BUTTONS AND FOOTER
        // =========================
        JPanel footer = new JPanel();
        footer.setBackground(WHITE);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));

        JSeparator footerLine = new JSeparator();
        footerLine.setForeground(LINE);
        footerLine.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 1));

        footer.add(footerLine);
        footer.add(Box.createVerticalStrut(14));

        JButton loginButton = createButton("Login", BLUE);
        loginButton.addActionListener(e -> performLogin());

        JButton registerButton = createButton(
                "Register as Center Staff", BLUE);
        registerButton.addActionListener(
                e -> this.app.showRegistration());

        footer.add(loginButton);
        footer.add(Box.createVerticalStrut(10));
        footer.add(registerButton);
        footer.add(Box.createVerticalStrut(14));

        JLabel note = new JLabel(
                "<html><div style='text-align:center;'>"
                + "Use your linked Supabase Auth account. "
                + "The account determines your role."
                + "</div></html>");

        note.setFont(new java.awt.Font(
                "SansSerif", java.awt.Font.PLAIN, 11));
        note.setForeground(MUTED);
        note.setAlignmentX(CENTER_ALIGNMENT);

        footer.add(note);
        body.add(footer, BorderLayout.SOUTH);

        // Center the login card
        GridBagConstraints cardConstraints
                = new GridBagConstraints();

        cardConstraints.gridx = 0;
        cardConstraints.gridy = 0;
        cardConstraints.weightx = 1;
        cardConstraints.weighty = 1;
        cardConstraints.anchor = GridBagConstraints.CENTER;
        cardConstraints.fill = GridBagConstraints.NONE;

        add(body, cardConstraints);

        // Press Enter in the password field to log in
        passwordField.addActionListener(
                e -> loginButton.doClick());
    }

    // =========================
    // FIELD LAYOUT
    // =========================
    
private void addLoginField(
        JPanel panel,
        int row,
        String labelText,
        JComponent input) {

    GridBagConstraints constraints = new GridBagConstraints();

    constraints.gridx = 0;
    constraints.weightx = 1.0;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.gridwidth = 1;

    JLabel label = new JLabel(labelText);
    label.setFont(new java.awt.Font(
            "SansSerif", java.awt.Font.BOLD, 13));
    label.setForeground(NAVY);
    label.setLabelFor(input);

    // Label row
    constraints.gridy = row * 2;
    constraints.insets = new Insets(5, 0, 5, 0);
    panel.add(label, constraints);

    // Input row
    constraints.gridy = row * 2 + 1;
    constraints.insets = new Insets(0, 0, 10, 0);

    input.setPreferredSize(new Dimension(400, 40));
    input.setMinimumSize(new Dimension(150, 40));

    panel.add(input, constraints);
}


    // =========================
    // INPUT STYLING
    // =========================
    private void styleInput(JTextField field) {
        field.setFont(new java.awt.Font(
                "SansSerif", java.awt.Font.PLAIN, 14));
        field.setForeground(NAVY);
        field.setBackground(WHITE);
        field.setCaretColor(BLUE);
        field.setOpaque(true);
        field.setEditable(true);
        field.setEnabled(true);

        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(LINE, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
    }

    private void styleComboBox(JComboBox<String> combo) {
        combo.setFont(new java.awt.Font(
                "SansSerif", java.awt.Font.PLAIN, 14));
        combo.setForeground(NAVY);
        combo.setBackground(WHITE);
        combo.setOpaque(true);
        combo.setEnabled(true);
        combo.setBorder(new LineBorder(LINE, 1, true));
    }

    // =========================
    // BUTTON STYLING
    // =========================
    private JButton createButton(
            String text,
            Color background) {

        JButton button = new JButton(text);

        button.setFont(new java.awt.Font(
                "SansSerif", java.awt.Font.BOLD, 13));
        button.setForeground(WHITE);
        button.setBackground(background);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new java.awt.Cursor(
                java.awt.Cursor.HAND_CURSOR));

        button.setAlignmentX(CENTER_ALIGNMENT);
        button.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 42));
        button.setPreferredSize(new Dimension(400, 42));
        button.setMinimumSize(new Dimension(150, 42));

        return button;
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
