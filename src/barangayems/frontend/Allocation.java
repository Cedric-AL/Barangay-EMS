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

        JPanel body = UI.panel(new BorderLayout(16,0));
        JPanel tables = UI.panel(new GridLayout(2,1,0,16));
        tables.add(UI.card("Resource inventory", "Current stock at every center", null, UI.scroll(UI.table(Resources.inventory(app.store)))));

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
        tables.add(UI.card("Allocation history", app.store.allocations.size() + " distributions recorded", null, UI.scroll(UI.table(history))));
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
        JTextArea estimate = new JTextArea(3, 20);
        estimate.setEditable(false);
        estimate.setLineWrap(true);
        estimate.setWrapStyleWord(true);
        estimate.setText("Enter your approved supply rate in the selected resource's units. Load a saved rule, or enter a temporary scenario rate. "
                + "Group counts use recorded vulnerability categories. Destination stock is shared across groups.");
        estimate.setBackground(UI.SUBTLE);
        JPanel plannerFields = UI.form();
        UI.field(plannerFields,0,"People covered",group);
        JPanel rateDays = UI.panel(new GridLayout(1,2,10,0));
        JPanel rateForm = UI.form(), daysForm = UI.form();
        UI.field(rateForm,0,"Units / person / day",rate);
        UI.field(daysForm,0,"Days to cover",days);
        rateDays.add(rateForm); rateDays.add(daysForm);
        JPanel planner = UI.panel(new BorderLayout(0,10));
        JPanel plannerTop=UI.panel(new BorderLayout(0,0)); plannerTop.add(plannerFields,BorderLayout.NORTH); plannerTop.add(rateDays,BorderLayout.CENTER);
        JPanel plannerButtons = UI.panel(new GridLayout(1,2,8,0));
        plannerButtons.add(UI.secondary("Load Saved Rule",()->{
            Resource item=(Resource)resource.getSelectedItem();
            if(item==null){UI.error(this,"Select a source resource.");return;}
            SupplyRule saved=app.store.supplyRules.stream().filter(r->r.enabled&&r.matches(item.name,item.unit)).findFirst().orElse(null);
            if(saved==null){UI.error(this,"No enabled rule for this item and unit. Officials can configure Supply Rules in Resources.");return;}
            group.setSelectedItem(saved.group);rate.setText(saved.dailyRate.toPlainString());days.setValue(saved.days);
            estimate.setText("Saved rule loaded. Calculate to review the suggested quantity.");
        }));
        plannerTop.add(plannerButtons,BorderLayout.SOUTH);
        planner.add(plannerTop,BorderLayout.NORTH);
        JScrollPane estimateScroll = UI.scroll(estimate);
        estimateScroll.setPreferredSize(new Dimension(200, 96));
        planner.add(estimateScroll,BorderLayout.CENTER);
        plannerButtons.add(UI.secondary("Calculate Quantity",()->{
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
        }));
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

        JPanel controls = UI.form();
        UI.field(controls,0,"Destination center",destination);
        UI.field(controls,1,"Source resource",resource);
        JPanel quantityForm = UI.form();
        UI.field(quantityForm,0,"Quantity to move",quantity);

        JPanel distribution = UI.panel(new BorderLayout(0,4));
        distribution.add(quantityForm,BorderLayout.CENTER);
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
        }),BorderLayout.SOUTH);

        JPanel steps = UI.panel(null);
        steps.setLayout(new BoxLayout(steps, BoxLayout.Y_AXIS));
        steps.add(step("1", "Choose where and what"));
        steps.add(Box.createVerticalStrut(10));
        steps.add(controls);
        steps.add(Box.createVerticalStrut(10));
        steps.add(step("2", "Estimate the quantity (optional)"));
        steps.add(Box.createVerticalStrut(4));
        JLabel plannerHint = UI.hint("Uses evacuee counts and a daily rate per person.");
        plannerHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        steps.add(plannerHint);
        steps.add(Box.createVerticalStrut(12));
        steps.add(planner);
        for (Component c : steps.getComponents()) ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel stepsHolder = UI.panel(new BorderLayout());
        stepsHolder.add(steps, BorderLayout.NORTH);
        JScrollPane stepsScroll = UI.scroll(stepsHolder);
        stepsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel left = UI.card("Distribute supplies", "Move stock from one center to another", null, stepsScroll);
        JPanel confirm = UI.panel(new BorderLayout(0,10));
        confirm.setBorder(new javax.swing.border.CompoundBorder(
                new javax.swing.border.MatteBorder(1,0,0,0,UI.LINE), new javax.swing.border.EmptyBorder(14,0,0,0)));
        confirm.add(step("3", "Confirm and distribute"), BorderLayout.NORTH);
        confirm.add(distribution, BorderLayout.CENTER);
        confirm.add(UI.hint("Debits the source and credits the destination."), BorderLayout.SOUTH);
        left.add(confirm, BorderLayout.SOUTH);
        left.setPreferredSize(new Dimension(400, 0));
        body.add(left, BorderLayout.WEST);
        page.add(body,BorderLayout.CENTER);
    }

    private static JComponent step(String number, String title) {
        JPanel row = UI.panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel badge = UI.pill(number).colors(UI.BLUE, UI.BLUE_PALE);
        row.add(badge);
        row.add(Box.createHorizontalStrut(8));
        row.add(UI.label(title, 13, true));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        return row;
    }
}
