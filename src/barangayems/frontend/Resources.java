package barangayems.frontend;

import barangayems.backend.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
public class Resources extends JPanel {
    final AppFrame app;
    static DefaultTableModel inventory(DataSnapshot s){
        DefaultTableModel m=UI.model("Center","Item","Unit","Available","Used","Remaining","Minimum","Low stock deficit");
        for(Resource r:s.supplies)m.addRow(new Object[]{r.center,r.name,r.unit,r.available,r.used,r.remaining(),r.required,r.shortage()});return m;
    }
    public Resources(AppFrame app){
        super(new BorderLayout());this.app=app;JPanel page=UI.page("5 — RESOURCES AND ALLOCATION");add(page);
        DefaultTableModel m=inventory(app.store);JTable table=UI.table(m);
        JPanel body=UI.panel(new BorderLayout(0,16));JPanel toolbar=UI.panel(new BorderLayout(12,0));
        toolbar.add(UI.search(table,"Search by center or item"),BorderLayout.WEST);
        JPanel actions=UI.panel(new FlowLayout(FlowLayout.RIGHT,8,0));
        actions.add(UI.secondary("Supply Rules",()->SupplyRules.show(app)));
        actions.add(UI.secondary("Demand Shortages",()->JOptionPane.showMessageDialog(this,UI.scroll(new JTextArea(SupplyDemand.report(app.store),22,75)),"Demand shortages",JOptionPane.INFORMATION_MESSAGE)));
        actions.add(UI.secondary("Edit Selected",()->{
            if(table.getSelectedRow()<0){UI.error(this,"Select a resource first.");return;}edit(app.store.supplies.get(table.convertRowIndexToModel(table.getSelectedRow())));
        }));
        actions.add(UI.button("+  Add Resource",()->edit(null)));
        toolbar.add(actions,BorderLayout.EAST);
        JPanel inner=UI.panel(new BorderLayout(0,14));inner.add(toolbar,BorderLayout.NORTH);inner.add(UI.scroll(table));
        long low=app.store.supplies.stream().filter(x->x.shortage()>0).count();
        UI.Pill lowPill=UI.pill(low==0?"All stocked":low+" low on stock");
        if(low==0)lowPill.colors(UI.GREEN,UI.GREEN_PALE);else lowPill.colors(UI.AMBER,UI.AMBER_PALE);
        body.add(UI.card("Resource inventory",app.store.supplies.size()+" items across all centers",lowPill,inner));
        JPanel foot=UI.panel(new BorderLayout(12,0));
        foot.add(UI.hint("Low stock = remaining quantity is below the minimum reserve. Demand Shortages uses the saved supply rules."));
        foot.add(UI.secondary("Distribute Resources  \u2192",()->app.showPage("Allocation")),BorderLayout.EAST);
        body.add(foot,BorderLayout.SOUTH);page.add(body);
    }
    private void edit(Resource r){
        JTextField name=new JTextField(r==null?"":r.name),unit=new JTextField(r==null?"":r.unit),available=new JTextField(r==null?"0":""+r.available),used=new JTextField(r==null?"0":""+r.used),required=new JTextField(r==null?"0":""+r.required);
        JComboBox<EvacuationCenter> center=new JComboBox<>();for(EvacuationCenter c:app.store.activeCenters()){center.addItem(c);if(r!=null&&c.id==r.centerId)center.setSelectedItem(c);}
        center.setEnabled(r==null);
        JPanel form=UI.form();UI.field(form,0,"Center",center);UI.field(form,1,"Item",name);UI.field(form,2,"Unit",unit);UI.field(form,3,"Quantity total",available);UI.field(form,4,"Used / allocated",used);UI.field(form,5,"Minimum stock",required);
        form.setPreferredSize(new Dimension(380,form.getPreferredSize().height));
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
