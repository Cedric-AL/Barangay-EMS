package barangayems.frontend;

import barangayems.backend.*;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

final class SupplyRules {
    record InventoryItem(String name,String unit) {
        @Override public String toString(){return name+" ("+unit+")";}
    }
    static InventoryItem[] inventoryItems(DataSnapshot snapshot) {
        java.util.Map<String,InventoryItem> unique=new java.util.LinkedHashMap<>();
        for(Resource r:snapshot.supplies)if(r.name!=null&&r.unit!=null&&!r.name.isBlank()&&!r.unit.isBlank()) {
            String n=r.name.trim(),u=r.unit.trim();
            unique.putIfAbsent(n.toLowerCase(java.util.Locale.ROOT)+"\0"+u.toLowerCase(java.util.Locale.ROOT),new InventoryItem(n,u));
        }
        return unique.values().toArray(InventoryItem[]::new);
    }
    static void show(AppFrame app) {
        var model=UI.model("Item","Unit","People covered","Daily rate","Days","Enabled");
        for(SupplyRule r:app.store.supplyRules)model.addRow(new Object[]{r.name,r.unit,r.group,r.rateText(),r.days,r.enabled});
        JTable table=UI.table(model);
        JComboBox<InventoryItem> item=new JComboBox<>(inventoryItems(app.store));
        JTextField unit=new JTextField(),rate=new JTextField();unit.setEditable(false);
        Runnable updateUnit=()->{InventoryItem selected=(InventoryItem)item.getSelectedItem();unit.setText(selected==null?"":selected.unit());};
        item.addActionListener(e->updateUnit.run());updateUnit.run();
        JComboBox<String> group=new JComboBox<>(new String[]{AllocationPlanner.ALL,"Child","Senior Citizen","Person with Disability","Pregnant","Other"});
        JSpinner days=new JSpinner(new SpinnerNumberModel(1,1,365,1));
        JCheckBox enabled=new JCheckBox("Enabled",true);
        table.getSelectionModel().addListSelectionListener(e->{
            int row=table.getSelectedRow(); if(row<0)return;
            SupplyRule r=app.store.supplyRules.get(table.convertRowIndexToModel(row));
            item.setSelectedIndex(-1);
            for(int i=0;i<item.getItemCount();i++)if(r.matches(item.getItemAt(i).name(),item.getItemAt(i).unit())){item.setSelectedIndex(i);break;}
            group.setSelectedItem(r.group);rate.setText(r.rateText());days.setValue(r.days);enabled.setSelected(r.enabled);
            if(item.getSelectedIndex()<0)UI.error(app,"This rule's item and unit are no longer in inventory. Add the resource first before updating its rule.");
        });
        JPanel grid=UI.panel(new GridLayout(1,2,16,0));JPanel left=UI.form(),right=UI.form();
        UI.field(left,0,"Existing item",item);UI.field(left,1,"Unit (automatic)",unit);UI.field(left,2,"People covered",group);
        UI.field(right,0,"Units per person/day",rate);UI.field(right,1,"Days to cover",days);UI.field(right,2,"Rule status",enabled);
        grid.add(left);grid.add(right);
        JPanel panel=UI.panel(new BorderLayout(0,14));panel.add(UI.scroll(table));JPanel bottom=UI.panel(new BorderLayout(0,8));bottom.add(UI.label("Add or update a rule (select a row to load it)",13,true),BorderLayout.NORTH);bottom.add(grid);panel.add(bottom,BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(760,500));
        if(!app.isOfficial()){JOptionPane.showMessageDialog(app,UI.scroll(table),"Saved supply rules (officials can edit)",JOptionPane.INFORMATION_MESSAGE);return;}
        if(item.getItemCount()==0){UI.error(app,"Add a resource to inventory before creating a supply rule.");return;}
        while(JOptionPane.showConfirmDialog(app,panel,"Supply Rules - one rule per item and unit",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION){
            try {
                InventoryItem selected=(InventoryItem)item.getSelectedItem();
                if(selected==null)throw new IllegalArgumentException("Select an existing inventory item and unit.");
                String n=selected.name(),u=selected.unit();
                BigDecimal daily=new BigDecimal(rate.getText().trim());days.commitEdit();int d=((Number)days.getValue()).intValue();
                if(daily.signum()<=0||daily.compareTo(new BigDecimal("100000"))>0||daily.scale()>4)
                    throw new IllegalArgumentException("Daily rate must be greater than zero, at most 100000, with up to four decimal places.");
                String target=(String)group.getSelectedItem();boolean active=enabled.isSelected();
                app.mutate(()->app.data.saveSupplyRule(n,u,target,daily,d,active),"Resources");return;
            }catch(NumberFormatException e){UI.error(app,"Enter a numeric daily rate.");}
            catch(java.text.ParseException|IllegalArgumentException e){UI.error(app,e.getMessage());}
        }
    }
}
