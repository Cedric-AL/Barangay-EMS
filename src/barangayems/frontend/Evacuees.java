package barangayems.frontend;

import barangayems.backend.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class Evacuees extends JPanel {
    private final AppFrame app;
    private final DefaultTableModel model=UI.model("Name","Age","Household","Assigned Center");
    private final JTable table=UI.table(model);
    private final JTextField name=new JTextField(),age=new JTextField(),household=new JTextField(),contact=new JTextField();
    private final JTextArea needs=new JTextArea(3,18);
    private final JComboBox<EvacuationCenter> center=new JComboBox<>();
    private final JComboBox<String> category=new JComboBox<>(new String[]{"None","Child","Senior Citizen","Person with Disability","Pregnant","Other"});
    private Long editingId;
    private final JButton submit=UI.button("Register",()->register());
    private final JLabel mode=UI.label("Register a new evacuee",12,true);
    public Evacuees(AppFrame app){
        super(new BorderLayout());this.app=app;JPanel page=UI.page("4 — EVACUEE REGISTRATION");add(page);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JPanel body=UI.panel(new BorderLayout(16,0));
        for(EvacuationCenter c:app.store.centers)center.addItem(c);
        JPanel fields=UI.form();UI.field(fields,0,"Full Name",name);UI.field(fields,1,"Age",age);UI.field(fields,2,"Household",household);UI.field(fields,3,"Contact Number",contact);UI.field(fields,4,"Assigned Center",center);UI.field(fields,5,"Special Needs",UI.scroll(needs));UI.field(fields,6,"Vulnerability",category);
        JPanel form=UI.panel(new BorderLayout(0,12));form.add(mode,BorderLayout.NORTH);form.add(fields,BorderLayout.CENTER);
        JPanel formActions=UI.panel(new GridLayout(0,1,0,8));formActions.add(submit);formActions.add(UI.button("Cancel Edit / Clear",()->clearForm()));form.add(formActions,BorderLayout.SOUTH);
        JPanel left=UI.card("Registration Form",form);left.setPreferredSize(new Dimension(365,0));body.add(left,BorderLayout.WEST);
        JPanel list=UI.panel(new BorderLayout(0,14));JPanel search=UI.panel(new BorderLayout(10,0));search.add(UI.label("Search:",12,false),BorderLayout.WEST);search.add(UI.search(table,"Search registered evacuees"));list.add(search,BorderLayout.NORTH);list.add(UI.scroll(table));
        JPanel actions=UI.panel(new GridLayout(0,1,0,8));
        actions.add(UI.button("Edit Selected",()->editSelected()));
        actions.add(UI.button("Select All Visible",()->table.selectAll()));
        if(app.isOfficial())actions.add(UI.button("Delete Selected",()->deleteSelected()));
        actions.add(UI.label("<html>Ctrl+click selects multiple rows; Shift+click selects a range.<br>Edit one record at a time. Officials can delete multiple selected records.</html>",11,false));
        list.add(actions,BorderLayout.SOUTH);body.add(UI.card("Registered Evacuees",list));page.add(body);refresh();
    }
    private void refresh(){model.setRowCount(0);for(Evacuee p:app.store.people)model.addRow(new Object[]{p.name,p.age,p.household,p.center});}
    private Evacuee selected(){
        int row=table.getSelectedRow();
        if(row<0){UI.error(this,"Select an evacuee from the table first.");return null;}
        return app.store.people.get(table.convertRowIndexToModel(row));
    }
    private void editSelected(){
        if(table.getSelectedRowCount()!=1){UI.error(this,"Select exactly one evacuee to edit.");return;}
        Evacuee person=selected();if(person==null)return;
        editingId=person.id;name.setText(person.name);age.setText(String.valueOf(person.age));
        household.setText(person.household);contact.setText(person.contact);needs.setText(person.needs);
        category.setSelectedItem(person.category==null?"None":person.category);
        center.setSelectedIndex(-1);
        for(int i=0;i<center.getItemCount();i++)if(java.util.Objects.equals(person.centerId,center.getItemAt(i).id))center.setSelectedIndex(i);
        center.setEnabled(false);submit.setText("Save Changes");mode.setText("Editing selected evacuee (#"+person.id+")");name.requestFocusInWindow();
    }
    private void clearForm(){
        editingId=null;name.setText("");age.setText("");household.setText("");contact.setText("");needs.setText("");category.setSelectedIndex(0);
        center.setEnabled(true);if(center.getItemCount()>0)center.setSelectedIndex(0);
        submit.setText("Register");mode.setText("Register a new evacuee");table.clearSelection();
    }
    private void deleteSelected(){
        int[] rows=table.getSelectedRows();
        if(rows.length==0){UI.error(this,"Select at least one evacuee first.");return;}
        long[] ids=new long[rows.length];
        for(int i=0;i<rows.length;i++)ids[i]=app.store.people.get(table.convertRowIndexToModel(rows[i])).id;
        if(JOptionPane.showConfirmDialog(this,"Permanently delete "+ids.length+" selected evacuee record(s)?\nCenter occupancy will be updated. This cannot be undone.\nIf any record has transfer history, none will be deleted.","Delete selected evacuees",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
        app.mutate(()->app.data.deleteEvacuees(ids),"Evacuees");
    }
    private void register(){
        try{
            String n=UI.required(name.getText(),"Full name"),h=UI.required(household.getText(),"Household"),phone=contact.getText().trim();
            int a=UI.number(age.getText(),"Age",0);if(a>120)throw new IllegalArgumentException("Age must be between 0 and 120.");
            if(!phone.isEmpty()&&!phone.matches("[+0-9 ()-]{7,20}"))throw new IllegalArgumentException("Enter a valid contact number, or leave it blank.");
            if(n.length()>150||h.length()>100)throw new IllegalArgumentException("Name must be at most 150 characters and household at most 100.");
            if(editingId!=null){
                long id=editingId;String special=needs.getText().trim(),vulnerability=(String)category.getSelectedItem();
                app.mutate(()->app.data.updateEvacuee(id,n,a,h,phone,special,vulnerability),"Evacuees");return;
            }
            if(center.getSelectedItem()==null)throw new IllegalArgumentException("Add an evacuation center first.");
            EvacuationCenter destination=(EvacuationCenter)center.getSelectedItem();
            Evacuee match=null;
            for(Evacuee p:app.store.people)if(p.name.equalsIgnoreCase(n)&&p.age==a&&((p.household!=null&&p.household.equalsIgnoreCase(h))||(!phone.isEmpty()&&phone.equals(p.contact)))){match=p;break;}
            String special=needs.getText().trim(),vulnerability=(String)category.getSelectedItem();
            boolean distinct=false;
            if(match!=null){
                boolean same=java.util.Objects.equals(match.centerId,destination.id);
                String[] choices=same?new String[]{"Register distinct person","Cancel"}:new String[]{"Transfer this person","Register distinct person","Cancel"};
                int choice=JOptionPane.showOptionDialog(this,"Possible match: "+match.name+" at "+match.center+".\nReview identifying details before continuing.","Review possible duplicate",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,choices,"Cancel");
                if(choice<0||choice==choices.length-1)return;
                if(!same&&choice==0){
                    long id=match.id;app.mutate(()->app.data.transfer(id,destination.id,"Confirmed duplicate match; shelter transfer"),"Evacuees");return;
                }
                distinct=true;
            }
            boolean allowDistinct=distinct;
            app.mutate(()->app.data.register(n,a,h,phone,destination.id,special,vulnerability,allowDistinct),"Evacuees");
        }catch(IllegalArgumentException e){UI.error(this,e.getMessage());}
    }
}
