package barangayems.frontend;

import barangayems.backend.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

final class StaffApplications extends JPanel {
    private StaffApplication[] applications = new StaffApplication[0];
    StaffApplications(AppFrame app) {
        super(new BorderLayout(0, 15));
        setBackground(UI.BG);
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        add(UI.label("Staff Applications and Access", 25, true), BorderLayout.NORTH);
        DefaultTableModel model = UI.model("Name", "Username", "Email", "Status", "Assigned Center", "Review note");
        JTable table = UI.table(model);
        add(UI.scroll(table), BorderLayout.CENTER);
        JComboBox<EvacuationCenter> centers = new JComboBox<>(app.store.centers.toArray(new EvacuationCenter[0]));
        centers.setSelectedIndex(-1);
        JTextField note = new JTextField(20);
        JPanel controls = UI.panel(new GridLayout(0, 1, 0, 10));
        JPanel fields = UI.form();
        UI.field(fields, 0, "Assign center:", centers);
        UI.field(fields, 1, "Review reason:", note);
        controls.add(fields);
        JLabel state = UI.label("Loading applications...", 12, false);
        controls.add(state);
        JPanel actions = UI.panel(new FlowLayout(FlowLayout.LEFT));
        for (String action : new String[]{"Approve", "Reject", "Deactivate"}) {
            JButton button = UI.button(action, () -> {
                int selected = table.getSelectedRow();
                if (selected < 0) { UI.error(this, "Select a staff application first."); return; }
                StaffApplication application = applications[table.convertRowIndexToModel(selected)];
                EvacuationCenter center = (EvacuationCenter) centers.getSelectedItem();
                if (action.equals("Approve") && center == null) { UI.error(this, "Select an evacuation center."); return; }
                if (action.equals("Reject") && note.getText().isBlank()) { UI.error(this, "Enter a rejection reason."); return; }
                if (JOptionPane.showConfirmDialog(app, action + " staff account for " + application.fullName + "?", "Confirm staff review", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
                String reason = note.getText().trim();
                app.mutate(() -> app.data.reviewStaff(application.authId, action, center == null ? null : center.id, reason), "Staff Applications");
            });
            actions.add(button);
        }
        actions.add(UI.button("Refresh", () -> app.showPage("Staff Applications")));
        controls.add(actions);
        add(controls, BorderLayout.SOUTH);
        // Defer loading until the page has been attached to the frame.
        SwingUtilities.invokeLater(() -> app.task(() -> app.data.staffApplications(), rows -> {
            applications = rows;
            for (StaffApplication row : rows) model.addRow(new Object[]{row.fullName, row.username, row.email, row.status, row.centerName, row.reviewNote});
            state.setText(rows.length == 0 ? "No staff applications yet." : rows.length + " staff application(s). Select a row to review.");
        }));
    }
}
