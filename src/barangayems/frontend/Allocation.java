package barangayems.frontend;

import barangayems.backend.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class Allocation extends JPanel {
    public Allocation(AppFrame app) {
        super(new BorderLayout());
        JPanel page = UI.page("5 — RESOURCES AND ALLOCATION");
        add(page);

        JPanel body = UI.panel(new BorderLayout(0,16));
        JPanel tables = UI.panel(new GridLayout(2,1,0,16));
        tables.add(UI.card("Resource Inventory", UI.scroll(UI.table(Resources.inventory(app.store)))));

        DefaultTableModel history = UI.model("Destination Center","Resource","Quantity","Unit","Date");
        for (DataSnapshot.AllocationRecord row : app.store.allocations) {
            Resource item = app.store.resource(row.resourceId);
            history.addRow(new Object[]{
                    app.store.centerName(row.centerId),
                    item == null ? "Unavailable resource #" + row.resourceId : item.name,
                    row.quantity,
                    item == null ? "" : item.unit,
                    row.date
            });
        }
        tables.add(UI.card("Allocation Records", UI.scroll(UI.table(history))));
        body.add(tables,BorderLayout.CENTER);

        JComboBox<EvacuationCenter> destination = new JComboBox<>();
        for (EvacuationCenter c : app.store.centers) destination.addItem(c);
        JComboBox<Resource> resource = new JComboBox<>();
        for (Resource r : app.store.supplies) resource.addItem(r);
        JTextField quantity = new JTextField(6);

        JPanel controls = UI.panel(new GridLayout(2,3,12,8));
        controls.add(UI.label("Destination Center",12,false));
        controls.add(UI.label("Source Resource",12,false));
        controls.add(UI.label("Quantity",12,false));
        controls.add(destination); controls.add(resource); controls.add(quantity);

        JPanel distribution = UI.panel(new BorderLayout(12,0));
        distribution.add(controls,BorderLayout.CENTER);
        distribution.add(UI.button("Distribute Resources",()->{
            try {
                EvacuationCenter dest = (EvacuationCenter) destination.getSelectedItem();
                Resource source = (Resource) resource.getSelectedItem();
                if (dest == null || source == null) throw new IllegalArgumentException("Add at least two centers and one resource first.");
                if (dest.id == source.centerId) throw new IllegalArgumentException("Choose a destination different from the resource's current center.");
                int q = UI.number(quantity.getText(),"Quantity",1);
                if (q > source.remaining()) throw new IllegalArgumentException("Only " + source.remaining() + " " + source.unit + " remain in the source inventory.");
                String message = "Move " + q + " " + source.unit + " of " + source.name + " from " + source.center + " to " + dest.name + "?";
                if (JOptionPane.showConfirmDialog(this,message,"Confirm distribution",JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
                app.mutate(() -> app.data.allocate(source.id,dest.id,q), "Allocation");
            } catch (IllegalArgumentException e) {
                UI.error(this,e.getMessage());
            }
        }),BorderLayout.EAST);

        JPanel footer = UI.panel(new BorderLayout(0,8));
        footer.add(distribution,BorderLayout.NORTH);
        footer.add(UI.label("Distribution debits the source, credits the destination, and records the acting user in Supabase.",11,false),BorderLayout.SOUTH);
        body.add(footer,BorderLayout.SOUTH);
        page.add(body,BorderLayout.CENTER);
    }
}
