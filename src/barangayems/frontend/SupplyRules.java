package barangayems.frontend;

import barangayems.backend.*;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

final class SupplyRules {
    static void show(AppFrame app) {
        var model=UI.model("Item","Unit","People covered","Daily rate","Days","Enabled");
        for(SupplyRule r:app.store.supplyRules)model.addRow(new Object[]{r.name,r.unit,r.group,r.dailyRate,r.days,r.enabled});
        JTable table=UI.table(model);
        JTextField name=new JTextField(),unit=new JTextField(),rate=new JTextField();
        JComboBox<String> group=new JComboBox<>(new String[]{AllocationPlanner.ALL,"Child","Senior Citizen","Person with Disability","Pregnant","Other"});
        JSpinner days=new JSpinner(new SpinnerNumberModel(1,1,365,1));
        JCheckBox enabled=new JCheckBox("Enabled",true);
        JPanel form=UI.form();
        UI.field(form,0,"Item",name);UI.field(form,1,"Unit",unit);UI.field(form,2,"People covered",group);
        UI.field(form,3,"Units per person/day",rate);UI.field(form,4,"Days to cover",days);UI.field(form,5,"Rule status",enabled);
        table.getSelectionModel().addListSelectionListener(e->{
            int row=table.getSelectedRow(); if(row<0)return;
            SupplyRule r=app.store.supplyRules.get(table.convertRowIndexToModel(row));
            name.setText(r.name);unit.setText(r.unit);group.setSelectedItem(r.group);rate.setText(r.dailyRate.toPlainString());days.setValue(r.days);enabled.setSelected(r.enabled);
        });
        JPanel panel=UI.panel(new BorderLayout(0,10));panel.add(UI.scroll(table));panel.add(form,BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(760,500));
        if(!app.isOfficial()){JOptionPane.showMessageDialog(app,UI.scroll(table),"Saved supply rules (officials can edit)",JOptionPane.INFORMATION_MESSAGE);return;}
        while(JOptionPane.showConfirmDialog(app,panel,"Supply Rules - one rule per item and unit",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION){
            try {
                String n=UI.required(name.getText(),"Item"),u=UI.required(unit.getText(),"Unit");
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
