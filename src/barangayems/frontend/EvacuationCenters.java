package barangayems.frontend;

import barangayems.backend.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class EvacuationCenters extends JPanel {
    private final AppFrame app;
    private final DefaultTableModel model = UI.model("Center Name","Location","Max Capacity","Occupants","Available","Status");
    private final JTable table = UI.table(model);
    private final JTextField name = new JTextField(), location = new JTextField(), capacity = new JTextField();
    private final JTextArea facilities = new JTextArea(3,20);
    private EvacuationCenter selected;
    private final FacilityChecklist features = new FacilityChecklist();
    private final JCheckBox assessed = new JCheckBox("Facility checklist has been assessed");

    public EvacuationCenters(AppFrame app) {
        super(new BorderLayout());
        this.app = app;
        JPanel page = UI.page("3 — EVACUATION CENTERS");
        add(page);

        JPanel body = UI.panel(new BorderLayout(16,0));
        boolean canManage = app.isOfficial();

        // ---- Left: list of centers ----
        JPanel list = UI.panel(new BorderLayout(0,14));
        JPanel toolbar = UI.panel(new BorderLayout(10,0));
        toolbar.add(UI.search(table,"Search centers or locations"), BorderLayout.WEST);
        JButton add = UI.button("+  New Center", this::clear);
        add.setEnabled(canManage);
        toolbar.add(UI.actions(FlowLayout.RIGHT, add), BorderLayout.EAST);
        list.add(toolbar, BorderLayout.NORTH);
        list.add(UI.scroll(table), BorderLayout.CENTER);
        list.add(UI.hint("Select a center to view or edit its details."), BorderLayout.SOUTH);
        body.add(UI.card("All centers", app.store.centers.size() + " registered, including archived", null, list), BorderLayout.CENTER);

        // ---- Right: details form ----
        JPanel fields = UI.form();
        UI.field(fields,0,"Center name",name);
        UI.field(fields,1,"Location",location);
        UI.field(fields,2,"Max capacity",capacity);
        UI.field(fields,3,"Facility notes",UI.scroll(facilities));
        UI.field(fields,4,"Available facilities",features);
        UI.field(fields,5,"Assessment",assessed);
        features.editable(canManage); assessed.setEnabled(canManage);

        JPanel form = UI.panel(new BorderLayout(0,14));
        form.add(UI.scroll(fields),BorderLayout.CENTER);
        JButton save = UI.button("Save as New",()->save(false));
        JButton update = UI.secondary("Update",()->save(true));
        save.setEnabled(canManage); update.setEnabled(canManage);
        JButton archive = UI.secondary("Archive", this::archive);
        archive.setEnabled(canManage);
        JPanel buttons = UI.panel(new BorderLayout(0,8));
        JPanel mainButtons = UI.panel(new GridLayout(1,2,8,0));
        mainButtons.add(update); mainButtons.add(save);
        JPanel otherButtons = UI.panel(new GridLayout(1,1,8,0));
        otherButtons.add(archive);
        buttons.add(mainButtons, BorderLayout.CENTER);
        buttons.add(otherButtons, BorderLayout.SOUTH);
        if (!canManage) {
            JLabel note = UI.hint("<html>Center Staff accounts can view centers. Only Barangay Officials can add, update or archive them.</html>");
            buttons.add(note, BorderLayout.NORTH);
        }
        form.add(buttons,BorderLayout.SOUTH);

        JPanel card = UI.card("Center details","Select a row to edit, or start a new center",null,form);
        card.setPreferredSize(new Dimension(380,0));
        body.add(card,BorderLayout.EAST);
        page.add(body,BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                selected = app.store.centers.get(table.convertRowIndexToModel(table.getSelectedRow()));
                name.setText(selected.name);
                location.setText(selected.location);
                capacity.setText(String.valueOf(selected.capacity));
                facilities.setText(selected.facilities == null ? "" : selected.facilities);
                features.select(selected.facilityFeatures); assessed.setSelected(selected.facilityProfileComplete);
            }
        });
        refresh();
    }

    private void refresh() {
        model.setRowCount(0);
        for (EvacuationCenter c : app.store.centers) {
            model.addRow(new Object[]{c.name,c.location,c.capacity,c.occupants,c.available(),c.status()});
        }
    }

    private void clear() {
        selected = null;
        table.clearSelection();
        name.setText(""); location.setText(""); capacity.setText(""); facilities.setText("");
        features.select(new String[0]); assessed.setSelected(false);
        name.requestFocusInWindow();
    }

    private void save(boolean update) {
        try {
            if (!app.isOfficial()) throw new IllegalArgumentException("Only Barangay Officials can manage evacuation centers.");
            String n = UI.required(name.getText(),"Center name");
            String l = UI.required(location.getText(),"Location");
            int cap = UI.number(capacity.getText(),"Capacity",1);
            if (update && selected == null) throw new IllegalArgumentException("Select a center to update.");
            if (update && cap < selected.occupants) throw new IllegalArgumentException("Capacity cannot be lower than current occupants.");
            for (EvacuationCenter c : app.store.centers) {
                if (c.name.equalsIgnoreCase(n) && (!update || c.id != selected.id)) {
                    throw new IllegalArgumentException("A center with this name already exists.");
                }
            }
            Long id = update ? selected.id : null;
            String f = facilities.getText().trim();
            String[] availableFeatures=features.selected(); boolean complete=assessed.isSelected();
            app.mutate(() -> app.data.saveCenter(id,n,l,cap,f,availableFeatures,complete), "Evacuation Centers");
        } catch (IllegalArgumentException e) {
            UI.error(this,e.getMessage());
        }
    }

    private void archive() {
        try {
            if (!app.isOfficial()) throw new IllegalArgumentException("Only Barangay Officials can archive centers.");
            if (selected == null) throw new IllegalArgumentException("Select a center first.");
            if (selected.archived) throw new IllegalArgumentException("This center is already archived.");
            if (selected.occupants > 0) throw new IllegalArgumentException("Transfer all occupants before archiving this center.");
            long id = selected.id;
            if (JOptionPane.showConfirmDialog(this,"Archive " + selected.name + "?\nHistory and inventory will be retained. New registrations and incoming supplies will be blocked.","Archive center",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
                app.mutate(() -> app.data.archiveCenter(id), "Evacuation Centers");
        } catch (IllegalArgumentException e) { UI.error(this,e.getMessage()); }
    }

}
