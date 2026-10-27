package barangayems.frontend;

import barangayems.backend.*;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

final class StaffRegistration extends JPanel {
    StaffRegistration(AppFrame app) {
        super(new GridBagLayout());
        setBackground(UI.BG);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        UI.RoundedPanel card = new UI.RoundedPanel(20, Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(920, 640));
        card.add(Login.brandPanel("Join your barangay's response team",
                "Create a Center Staff account. A Barangay Official reviews every application before access is granted."),
                BorderLayout.WEST);

        JPanel body = UI.panel(new BorderLayout(0, 18));
        body.setBorder(BorderFactory.createEmptyBorder(40, 52, 36, 52));
        JPanel heading = UI.panel(new GridLayout(2, 1, 0, 4));
        heading.add(UI.label("Register as Center Staff", 24, true));
        JLabel intro = UI.hint("All fields are required. Use at least 8 characters for your password.");
        intro.setFont(UI.font(Font.PLAIN, 13));
        heading.add(intro);
        body.add(heading, BorderLayout.NORTH);
        JTextField name = new JTextField(), username = new JTextField(), email = new JTextField();
        JPasswordField password = new JPasswordField(), confirm = new JPasswordField();
        JPanel fields = UI.form();
        UI.field(fields, 0, "Full name", name);
        UI.field(fields, 1, "Username", username);
        UI.field(fields, 2, "Email", email);
        UI.field(fields, 3, "Password", passwordWithToggle(password));
        UI.field(fields, 4, "Confirm password", passwordWithToggle(confirm));
        body.add(fields, BorderLayout.CENTER);
        JPanel footer = UI.panel(new BorderLayout(0, 14));
        JLabel approval = UI.hint("A Barangay Official must approve your application before you can sign in.");
        footer.add(approval, BorderLayout.NORTH);
        JPanel buttons = UI.panel(new GridLayout(1, 2, 10, 0));
        buttons.add(UI.secondary("Back to Login", app::showLogin));
        buttons.add(UI.button("Submit Application", () -> {
            char[] secret = password.getPassword(), repeated = confirm.getPassword();
            String fullName = name.getText().trim(), handle = username.getText().trim(), address = email.getText().trim();
            String error = null;
            if (fullName.isEmpty() || fullName.length() > 100 || handle.isEmpty() || handle.length() > 50)
                error = "Enter a full name (up to 100 characters) and username (up to 50 characters).";
            else if (!address.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) error = "Enter a valid email address.";
            else if (secret.length < 8) error = "Use at least 8 characters for your password.";
            else if (!Arrays.equals(secret, repeated)) error = "Passwords do not match.";
            Arrays.fill(repeated, '\0');
            if (error != null) { Arrays.fill(secret, '\0'); UI.error(this, error); return; }
            password.setText(""); confirm.setText("");
            app.task(() -> {
                try {
                    new SupabaseClient(SupabaseConfig.load()).registerStaff(address, secret, handle, fullName);
                    return true;
                } finally { Arrays.fill(secret, '\0'); }
            }, result -> {
                app.showLogin();
                JOptionPane.showMessageDialog(app, "If registration was accepted, wait for Barangay Official approval.\nEmail confirmation is not required for this classroom setup.\nIf you already have an account, use Login instead.", "Application submitted", JOptionPane.INFORMATION_MESSAGE);
            });
        }));
        footer.add(buttons, BorderLayout.CENTER);
        body.add(footer, BorderLayout.SOUTH);
        card.add(body, BorderLayout.CENTER);
        add(card);
    }

    private static JPanel passwordWithToggle(JPasswordField field) {
        return UI.password(field);
    }
}
