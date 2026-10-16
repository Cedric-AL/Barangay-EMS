package barangayems.frontend;

import barangayems.backend.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
public class Resources extends JPanel {
    final AppFrame app;
    static DefaultTableModel inventory(DataSnapshot s){
        DefaultTableModel m=UI.model("Center","Item","Unit","Available","Used","Remaining","Minimum","Shortage");
        for(Resource r:s.supplies)m.addRow(new Object[]{r.center,r.name,r.unit,r.available,r.used,r.remaining(),r.required,r.shortage()});return m;
    }
    public Resources(AppFrame app){
        super(new BorderLayout());this.app=app;JPanel page=UI.page("5 — RESOURCES AND ALLOCATION");add(page);
        DefaultTableModel m=inventory(app.store);JTable table=UI.table(m);
        JPanel body=UI.panel(new BorderLayout(0,20));JPanel toolbar=UI.panel(new BorderLayout(12,0));
        JPanel search=UI.panel(new FlowLayout(FlowLayout.LEFT,8,0));search.add(UI.label("Search resources:",12,false));search.add(UI.search(table,"Search resources"));toolbar.add(search);
        JPanel actions=UI.panel(new FlowLayout(FlowLayout.RIGHT,8,0));actions.add(UI.button("Add Resource",()->edit(null)));actions.add(UI.button("Update Selected",()->{
            if(table.getSelectedRow()<0){UI.error(this,"Select a resource first.");return;}edit(app.store.supplies.get(table.convertRowIndexToModel(table.getSelectedRow())));
        }));toolbar.add(actions,BorderLayout.EAST);body.add(toolbar,BorderLayout.NORTH);
        body.add(UI.card("Resource Inventory",UI.scroll(table)));
        JPanel foot=UI.panel(new BorderLayout());foot.add(UI.label("Shortage = minimum stock − remaining (minimum 0).",12,false));foot.add(UI.button("Distribute Resources",()->app.showPage("Allocation")),BorderLayout.EAST);body.add(foot,BorderLayout.SOUTH);page.add(body);
    }
    private void edit(Resource r){
        JTextField name=new JTextField(r==null?"":r.name),unit=new JTextField(r==null?"":r.unit),available=new JTextField(r==null?"0":""+r.available),used=new JTextField(r==null?"0":""+r.used),required=new JTextField(r==null?"0":""+r.required);
        JComboBox<EvacuationCenter> center=new JComboBox<>();for(EvacuationCenter c:app.store.centers){center.addItem(c);if(r!=null&&c.id==r.centerId)center.setSelectedItem(c);}
        center.setEnabled(r==null);
        JPanel form=UI.form();UI.field(form,0,"Center",center);UI.field(form,1,"Item",name);UI.field(form,2,"Unit",unit);UI.field(form,3,"Quantity total",available);UI.field(form,4,"Used / allocated",used);UI.field(form,5,"Minimum stock",required);
        while(JOptionPane.showConfirmDialog(this,form,r==null?"Add resource":"Update resource",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION){
            try{
                String n=UI.required(name.getText(),"Item"),u=UI.required(unit.getText(),"Unit");int a=UI.number(available.getText(),"Available",0),v=UI.number(used.getText(),"Used",0),q=UI.number(required.getText(),"Required",0);
                if(v>a)throw new IllegalArgumentException("Used quantity cannot exceed available stock.");
                if(center.getSelectedItem()==null)throw new IllegalArgumentException("Add a center first.");
                long centerId=((EvacuationCenter)center.getSelectedItem()).id;Long id=r==null?null:r.id;
                app.mutate(()->app.data.saveResource(id,centerId,n,u,a,v,q),"Resources");return;
            }catch(IllegalArgumentException e){UI.error(this,e.getMessage());}
        }
    }
}
