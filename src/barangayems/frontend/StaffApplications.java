package barangayems.frontend;

import barangayems.backend.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

final class StaffApplications extends JPanel {
    private StaffApplication[] applications = new StaffApplication[0];
    StaffApplications(AppFrame app) {
        super(new BorderLayout());
        setBackground(UI.BG);
        JPanel page = UI.page("Staff Applications");
        add(page);
        DefaultTableModel model = UI.model("Name", "Username", "Email", "Status", "Assigned Center", "Review note");
        JTable table = UI.table(model);
        JLabel state = UI.hint("Loading applications...");
        JPanel list = UI.panel(new BorderLayout(0, 12));
        list.add(UI.scroll(table), BorderLayout.CENTER);
        list.add(state, BorderLayout.SOUTH);
        JPanel body = UI.panel(new BorderLayout(16, 0));
        body.add(UI.card("Applications", "Select an application, then review it on the right", null, list), BorderLayout.CENTER);
        JComboBox<EvacuationCenter> centers = new JComboBox<>(app.store.activeCenters().toArray(new EvacuationCenter[0]));
        centers.setSelectedIndex(-1);
        JTextField note = new JTextField(20);
        JPanel controls = UI.panel(new BorderLayout(0, 14));
        JPanel fields = UI.form();
        UI.field(fields, 0, "Assign center", centers);
        UI.field(fields, 1, "Review reason", note);
        controls.add(fields, BorderLayout.CENTER);
        controls.add(UI.hint("<html>Approving requires a center.<br>Rejecting requires a reason.</html>"), BorderLayout.NORTH);
        JPanel actions = UI.panel(new GridLayout(0, 1, 0, 8));
        for (String action : new String[]{"Approve", "Reject", "Deactivate"}) {
            Runnable review = () -> {
                int selected = table.getSelectedRow();
                if (selected < 0) { UI.error(this, "Select a staff application first."); return; }
                StaffApplication application = applications[table.convertRowIndexToModel(selected)];
                EvacuationCenter center = (EvacuationCenter) centers.getSelectedItem();
                if (action.equals("Approve") && center == null) { UI.error(this, "Select an evacuation center."); return; }
                if (action.equals("Reject") && note.getText().isBlank()) { UI.error(this, "Enter a rejection reason."); return; }
                if (JOptionPane.showConfirmDialog(app, action + " staff account for " + application.fullName + "?", "Confirm staff review", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
                String reason = note.getText().trim();
                app.mutate(() -> app.data.reviewStaff(application.authId, action, center == null ? null : center.id, reason), "Staff Applications");
            };
            JButton button = action.equals("Approve") ? UI.button(action, review)
                    : action.equals("Reject") ? UI.danger(action, review)
                    : UI.secondary(action, review);
            actions.add(button);
        }
        actions.add(UI.ghost("Refresh list", () -> app.showPage("Staff Applications")));
        controls.add(actions, BorderLayout.SOUTH);
        JPanel review = UI.card("Review", "Decide on the selected application", null, controls);
        review.setPreferredSize(new Dimension(340, 0));
        body.add(review, BorderLayout.EAST);
        page.add(body, BorderLayout.CENTER);
        // Defer loading until the page has been attached to the frame.
        SwingUtilities.invokeLater(() -> app.task(() -> app.data.staffApplications(), rows -> {
            applications = rows;
            for (StaffApplication row : rows) model.addRow(new Object[]{row.fullName, row.username, row.email, row.status, row.centerName, row.reviewNote});
            state.setText(rows.length == 0 ? "No staff applications yet." : rows.length + " staff application(s). Select a row to review.");
        }));
    }
}
