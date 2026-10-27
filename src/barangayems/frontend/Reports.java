package barangayems.frontend;

import barangayems.backend.*;
import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
public class Reports extends JPanel {
    final AppFrame app;final JTextArea preview=new JTextArea();
    final JComboBox<String> type=new JComboBox<>(new String[]{"Occupancy","Evacuee Records","Resource Inventory","Low Stock","Demand Shortages","Allocation History","Transfer History"});
    public Reports(AppFrame app){
        super(new BorderLayout());this.app=app;JPanel page=UI.page("6 — REPORTS");add(page);
        JPanel body=UI.panel(new BorderLayout(0,20));JPanel controls=UI.panel(new FlowLayout(FlowLayout.LEFT,16,0));controls.add(UI.label("Report Type:",14,true));type.setPreferredSize(new Dimension(270,35));controls.add(type);controls.add(UI.button("Generate",()->generate()));body.add(UI.card(null,controls),BorderLayout.NORTH);
        preview.setEditable(false);preview.setFont(new Font("Monospaced",Font.PLAIN,14));preview.setMargin(new Insets(24,24,24,24));body.add(UI.scroll(preview));preview.setLineWrap(false);
        JPanel bottom=UI.panel(new FlowLayout(FlowLayout.RIGHT,0,0));bottom.add(UI.button("Export",()->export()));body.add(bottom,BorderLayout.SOUTH);page.add(body);generate();
    }
    void generate(){
        String selected=(String)type.getSelectedItem();StringBuilder text=new StringBuilder("BARANGAY-EMS REPORT\n===================\n\nReport Type : "+selected+"\nReport Date : "+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))+"\nData        : Supabase database snapshot\n\n");
        switch(selected){
            case "Occupancy":
                text.append("Total Capacity   : ").append(app.store.capacity()).append("\nCurrent Occupants: ").append(app.store.occupants()).append("\nAvailable Spaces : ").append(app.store.capacity()-app.store.occupants()).append("\n\n");
                for(EvacuationCenter c:app.store.centers)text.append(c.name).append("\n  Capacity: ").append(c.capacity).append(" | Occupants: ").append(c.occupants).append(" | Available: ").append(c.available()).append(" | ").append(c.status()).append("\n\n");break;
            case "Evacuee Records":for(Evacuee p:app.store.people)text.append(p.name).append(" | Age: ").append(p.age).append("\n  Household: ").append(p.household).append(" | Center: ").append(p.center).append("\n  Needs: ").append(p.needs).append(" | Category: ").append(p.category).append(" | Required facilities: ").append(java.util.Arrays.toString(p.requirements)).append("\n\n");break;
            case "Demand Shortages": text.append(SupplyDemand.report(app.store)); break;
            case "Allocation History":for(DataSnapshot.AllocationRecord row:app.store.allocations){Resource item=app.store.resource(row.resourceId);text.append(row.date).append(" | ").append(app.store.centerName(row.centerId)).append("\n  ").append(item==null?"Unavailable":item.name).append(": ").append(row.quantity).append(' ').append(item==null?"":item.unit).append("\n\n");}if(app.store.allocations.isEmpty())text.append("No distributions recorded.\n");break;
            case "Transfer History":for(DataSnapshot.TransferRecord row:app.store.transfers)text.append(row.date).append(" | Resident #").append(row.personId).append("\n  ").append(app.store.centerName(row.fromId)).append(" -> ").append(app.store.centerName(row.toId)).append("\n  ").append(row.reason).append("\n\n");if(app.store.transfers.isEmpty())text.append("No transfers recorded.\n");break;
            default:for(Resource r:app.store.supplies)if(!selected.equals("Low Stock")||r.shortage()>0)text.append(r.center).append(" / ").append(r.name).append("\n  Remaining: ").append(r.remaining()).append(' ').append(r.unit).append(" | Minimum stock: ").append(r.required).append(" | Low stock deficit: ").append(r.shortage()).append("\n\n");
        }
        preview.setText(text.toString());preview.setCaretPosition(0);
    }
    private void export(){
        JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new java.io.File("BarangayEMS-report.txt"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Path target=chooser.getSelectedFile().toPath();
        if(Files.exists(target)&&JOptionPane.showConfirmDialog(this,"Replace the existing file?","Export report",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{Files.write(target,preview.getText().getBytes(StandardCharsets.UTF_8));JOptionPane.showMessageDialog(this,"Report exported successfully.");}
        catch(IOException e){UI.error(this,"Unable to export report: "+e.getMessage());}
    }
}
