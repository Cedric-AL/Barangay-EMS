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
    private final JCheckBox assessed = new JCheckBox("Facility checklist assessed");

    public EvacuationCenters(AppFrame app) {
        super(new BorderLayout());
        this.app = app;
        JPanel page = UI.page("3 — EVACUATION CENTERS");
        add(page);

        JPanel body = UI.panel(new BorderLayout(0,18));
        JPanel toolbar = UI.panel(new BorderLayout(10,0));
        JPanel search = UI.panel(new FlowLayout(FlowLayout.LEFT,10,0));
        search.add(UI.label("Search centers:",12,false));
        search.add(UI.search(table,"Search centers"));
        toolbar.add(search, BorderLayout.WEST);

        JButton add = UI.button("Add Center", this::clear);
        add.setEnabled(app.isOfficial());
        JPanel toolbarRight = UI.panel(new FlowLayout(FlowLayout.RIGHT,8,0));
        toolbarRight.add(add);
        toolbar.add(toolbarRight, BorderLayout.EAST);
        body.add(toolbar, BorderLayout.NORTH);

        JPanel split = UI.panel(new BorderLayout(16,0));
        split.add(UI.scroll(table), BorderLayout.CENTER);

        JPanel fields = UI.form();
        UI.field(fields,0,"Center Name",name);
        UI.field(fields,1,"Location",location);
        UI.field(fields,2,"Max Capacity",capacity);
        UI.field(fields,3,"Facility notes",UI.scroll(facilities));
        UI.field(fields,4,"Available facilities",features);
        UI.field(fields,5,"Assessment",assessed);
        features.editable(app.isOfficial()); assessed.setEnabled(app.isOfficial());

        JPanel form = UI.panel(new BorderLayout(0,12));
        form.add(UI.scroll(fields),BorderLayout.CENTER);
        JPanel buttons = UI.panel(new FlowLayout(FlowLayout.LEFT,6,0));
        JButton save = UI.button("Save",()->save(false));
        JButton update = UI.button("Update",()->save(true));
        JButton delete = UI.button("Delete",this::delete);
        boolean canManage = app.isOfficial();
        save.setEnabled(canManage); update.setEnabled(canManage); delete.setEnabled(canManage);
        JButton archive = UI.button("Archive", this::archive);
        archive.setEnabled(canManage);
        buttons.add(save); buttons.add(update); buttons.add(delete); buttons.add(archive);
        form.add(buttons,BorderLayout.SOUTH);

        JPanel right = UI.panel(new BorderLayout(0,10));
        right.add(form, BorderLayout.CENTER);
        if (!canManage) {
            JLabel note = UI.label("Center Staff accounts can view centers, but only Barangay Officials can add, update, or delete them.",11,false);
            note.setForeground(UI.MUTED);
            right.add(note, BorderLayout.SOUTH);
        }
        JPanel card = UI.card("Center Information",right);
        card.setPreferredSize(new Dimension(360,0));
        split.add(card,BorderLayout.EAST);
        body.add(split,BorderLayout.CENTER);
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

    private void delete() {
        try {
            if (!app.isOfficial()) throw new IllegalArgumentException("Only Barangay Officials can delete evacuation centers.");
            if (selected == null) throw new IllegalArgumentException("Select a center first.");
            if (selected.occupants > 0) throw new IllegalArgumentException("Transfer all occupants before deleting this center.");
            long id = selected.id;
            if (JOptionPane.showConfirmDialog(this,"Permanently delete " + selected.name + "?\nIf linked records prevent deletion, use Archive to preserve history.","Delete center",JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                app.mutate(() -> app.data.deleteCenter(id), "Evacuation Centers");
            }
        } catch (IllegalArgumentException e) {
            UI.error(this,e.getMessage());
        }
    }
}
