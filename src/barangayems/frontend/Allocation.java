package barangayems.frontend;

import barangayems.backend.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;

public class Allocation extends JPanel {
    public Allocation(AppFrame app) {
        super(new BorderLayout());
        JPanel page = UI.page("5 — RESOURCES AND ALLOCATION");
        add(page);

        JPanel body = UI.panel(new BorderLayout(0,16));
        JPanel tables = UI.panel(new GridLayout(2,1,0,16));
        tables.add(UI.card("Resource Inventory", UI.scroll(UI.table(Resources.inventory(app.store)))));

        DefaultTableModel history = UI.model(   "Destination Center","Resource","Quantity","Unit","Date");
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
        for (EvacuationCenter c : app.store.activeCenters()) destination.addItem(c);
        JComboBox<Resource> resource = new JComboBox<>();
        for (Resource r : app.store.supplies) resource.addItem(r);
        JTextField quantity = new JTextField(6);

        JComboBox<String> group = new JComboBox<>(new String[]{AllocationPlanner.ALL,
                "Child", "Senior Citizen", "Person with Disability", "Pregnant", "Other"});
        JTextField rate = new JTextField(6);
        JSpinner days = new JSpinner(new SpinnerNumberModel(1, 1, 365, 1));
        JTextArea estimate = new JTextArea(3, 60);
        estimate.setEditable(false);
        estimate.setLineWrap(true);
        estimate.setWrapStyleWord(true);
        estimate.setText("Enter your approved supply rate in the selected resource's units. Use a saved rule below, or enter a temporary scenario rate. "
                + "Group counts use recorded vulnerability categories. Destination stock is shared across groups.");
        JPanel plannerFields = UI.panel(new GridLayout(2,3,12,8));
        plannerFields.add(UI.label("People covered",12,false));
        plannerFields.add(UI.label("Units per person per day",12,false));
        plannerFields.add(UI.label("Days to cover",12,false));
        plannerFields.add(group); plannerFields.add(rate); plannerFields.add(days);
        JPanel planner = UI.panel(new BorderLayout(0,8));
        JPanel plannerTop=UI.panel(new BorderLayout(0,6)); plannerTop.add(plannerFields);
        plannerTop.add(UI.button("Load Saved Rule for Selected Resource",()->{
            Resource item=(Resource)resource.getSelectedItem();
            if(item==null){UI.error(this,"Select a source resource.");return;}
            SupplyRule saved=app.store.supplyRules.stream().filter(r->r.enabled&&r.matches(item.name,item.unit)).findFirst().orElse(null);
            if(saved==null){UI.error(this,"No enabled rule for this item and unit. Officials can configure Supply Rules in Resources.");return;}
            group.setSelectedItem(saved.group);rate.setText(saved.dailyRate.toPlainString());days.setValue(saved.days);
            estimate.setText("Saved rule loaded. Calculate to review the suggested quantity.");
        }),BorderLayout.SOUTH);
        planner.add(plannerTop,BorderLayout.NORTH);
        planner.add(UI.scroll(estimate),BorderLayout.CENTER);
        planner.add(UI.button("Calculate and Use Suggested Quantity",()->{
            try {
                days.commitEdit();
                AllocationPlanner.Plan plan = AllocationPlanner.calculate(app.store,
                        (EvacuationCenter)destination.getSelectedItem(), (Resource)resource.getSelectedItem(),
                        (String)group.getSelectedItem(), new BigDecimal(rate.getText().trim()),
                        ((Number)days.getValue()).intValue());
                Resource item = (Resource)resource.getSelectedItem();
                estimate.setText("People covered: " + plan.people() + " | Required: " + plan.required() + " " + item.unit
                        + " | Destination remaining stock: " + plan.onHand() + "\nAdditional need: " + plan.deficit()
                        + " | Suggested transfer: " + plan.suggested() + " | Still unmet after transfer: " + plan.unmet()
                        + "\nEstimate uses the loaded snapshot. Review the quantity before distributing; shared stock must not be counted twice across plans.");
                quantity.setText(String.valueOf(plan.suggested()));
            } catch (NumberFormatException e) {
                UI.error(this,"Enter a positive numeric supply rate, for example 2 or 0.5.");
            } catch (java.text.ParseException | IllegalArgumentException e) {
                UI.error(this,e.getMessage());
            }
        }),BorderLayout.SOUTH);
        Runnable invalidateEstimate = () -> {
            quantity.setText("");
            estimate.setText("Planning inputs changed. Calculate again to use a suggested quantity.");
        };
        destination.addActionListener(e -> invalidateEstimate.run());
        resource.addActionListener(e -> invalidateEstimate.run());
        group.addActionListener(e -> invalidateEstimate.run());
        days.addChangeListener(e -> invalidateEstimate.run());
        rate.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { invalidateEstimate.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { invalidateEstimate.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { invalidateEstimate.run(); }
        });

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
        footer.add(UI.card("Plan Supplies from Evacuee Requirements",planner),BorderLayout.NORTH);
        footer.add(distribution,BorderLayout.CENTER);
        footer.add(UI.label("Distribution debits the source, credits the destination, and records the acting user in Supabase.",11,false),BorderLayout.SOUTH);
        body.add(footer,BorderLayout.SOUTH);
        page.add(body,BorderLayout.CENTER);
    }
}
