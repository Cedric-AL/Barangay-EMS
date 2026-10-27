package barangayems.frontend;

import barangayems.backend.*;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

final class StaffRegistration extends JPanel {
    StaffRegistration(AppFrame app) {
        super(new GridBagLayout());
        setBackground(UI.BG);
        JPanel body = UI.panel(new BorderLayout(0, 20));
        body.setPreferredSize(new Dimension(650, 590));
        body.setBorder(BorderFactory.createEmptyBorder(25, 35, 25, 35));
        body.add(UI.label("Register as Center Staff", 27, true), BorderLayout.NORTH);
        JTextField name = new JTextField(), username = new JTextField(), email = new JTextField();
        JPasswordField password = new JPasswordField(), confirm = new JPasswordField();
        JPanel fields = UI.form();
        UI.field(fields, 0, "Full name:", name);
        UI.field(fields, 1, "Username:", username);
        UI.field(fields, 2, "Email:", email);
        UI.field(fields, 3, "Password:", passwordWithToggle(password));
        UI.field(fields, 4, "Confirm password:", passwordWithToggle(confirm));
        body.add(fields, BorderLayout.CENTER);
        JPanel footer = UI.panel(new GridLayout(0, 1, 0, 12));
        footer.add(UI.label("A Barangay Official must approve your application before you can use the system.", 12, false));
        footer.add(UI.button("Submit Staff Application", () -> {
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
        footer.add(UI.button("Back to Login", app::showLogin));
        body.add(footer, BorderLayout.SOUTH);
        add(body);
    }

    private static JPanel passwordWithToggle(JPasswordField field) {
        JPanel row = UI.panel(new BorderLayout(8, 0));
        row.add(field, BorderLayout.CENTER);
        JToggleButton toggle = new JToggleButton("Show password");
        char mask = field.getEchoChar();
        toggle.addActionListener(e -> {
            field.setEchoChar(toggle.isSelected() ? (char)0 : mask);
            toggle.setText(toggle.isSelected() ? "Hide password" : "Show password");
        });
        row.add(toggle, BorderLayout.EAST);
        return row;
    }
}
