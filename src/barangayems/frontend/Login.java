package barangayems.frontend;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class Login extends JPanel {
    public Login(AppFrame app){
        super(new GridBagLayout());setBackground(UI.BG);
        JPanel body=UI.panel(new BorderLayout(0,25));body.setPreferredSize(new Dimension(620,540));
        body.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UI.LINE),BorderFactory.createEmptyBorder(36,40,30,40)));
        JPanel headings=UI.panel(new GridLayout(3,1,0,10));
        JLabel brand=UI.label("BARANGAY-EMS",36,true);brand.setHorizontalAlignment(SwingConstants.CENTER);headings.add(brand);
        JLabel subtitle=UI.label("Barangay Evacuation Management System",16,true);subtitle.setHorizontalAlignment(SwingConstants.CENTER);headings.add(subtitle);headings.add(new JSeparator());
JPanel top=UI.panel(new BorderLayout(0,12));
if(Logo.icon(120)!=null){JLabel logo=new JLabel(Logo.icon(120));logo.setHorizontalAlignment(SwingConstants.CENTER);top.add(logo,BorderLayout.NORTH);}
top.add(headings,BorderLayout.CENTER);body.add(top,BorderLayout.NORTH);
        JPanel fields=UI.form();JTextField user=new JTextField();JPasswordField password=new JPasswordField();
        JComboBox<String> role=new JComboBox<>(new String[]{"Barangay Official","Center Staff"});
        UI.field(fields,0,"Email:",user);UI.field(fields,1,"Password:",password);UI.field(fields,2,"Role:",role);body.add(fields);
        JPanel footer=UI.panel(new BorderLayout(0,12));
        footer.add(new JSeparator(),BorderLayout.NORTH);
        JButton login=UI.button("Login",()->{
            char[] value=password.getPassword();
            if(user.getText().trim().isEmpty()||value.length==0){Arrays.fill(value,'\0');UI.error(this,"Enter your Supabase Auth email and password.");return;}
            password.setText("");app.login(user.getText().trim(),value,(String)role.getSelectedItem());
        });
        JPanel actions=UI.panel(new GridLayout(2,1,0,10));
        actions.add(login);
        actions.add(UI.button("Register as Center Staff",()->app.showRegistration()));
        footer.add(actions);
        JLabel note=UI.label("Use your linked Supabase Auth account. The account determines your role.",11,false);
        note.setHorizontalAlignment(SwingConstants.CENTER);footer.add(note,BorderLayout.SOUTH);body.add(footer,BorderLayout.SOUTH);add(body);
        password.addActionListener(e->login.doClick());
    }
}
