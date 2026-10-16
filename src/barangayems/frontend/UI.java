package barangayems.frontend;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.regex.Pattern;

/** Shared visual system for Barangay EMS. */
final class UI {
    static final Color BG = new Color(244,247,251);
    static final Color SURFACE = Color.WHITE;
    static final Color LINE = new Color(223,230,238);
    static final Color INK = new Color(29,45,67);
    static final Color MUTED = new Color(108,122,146);
    static final Color NAVY = new Color(15,39,74);
    static final Color NAVY_LIGHT = new Color(24,57,101);
    static final Color BLUE = new Color(37,111,245);
    static final Color BLUE_PALE = new Color(236,245,255);
    static final Color SELECTED = new Color(225,238,255);
    static final Color GREEN = new Color(22,133,96);
    static final Color AMBER = new Color(185,117,20);
    static final Color SHADOW = new Color(13, 27, 46, 26);
    static final Color TEXT_SOFT = new Color(59, 72, 91);

    static void setup() {
        Font base = new Font("SansSerif", Font.PLAIN, 13);
        for (String key : new String[]{"Label.font","Button.font","TextField.font","PasswordField.font",
                "ComboBox.font","Table.font","TableHeader.font","TextArea.font"}) UIManager.put(key, base);
        UIManager.put("Panel.background", BG);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Button.background", BLUE);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.foreground", INK);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("OptionPane.messageForeground", INK);
    }
    static JPanel panel(LayoutManager layout) { JPanel p = new JPanel(layout); p.setBackground(BG); return p; }
    static JPanel page(String title) {
        JPanel p = panel(new BorderLayout(0,18));
        p.setBorder(new EmptyBorder(16,20,18,20));

        RoundedPanel header = new RoundedPanel(22, new Color(10, 22, 43));
        header.setBorder(new EmptyBorder(18, 24, 18, 24));
        header.setGradient(new Color(10, 22, 43), new Color(24, 57, 105));
        header.setLayout(new BorderLayout(20,0));

        JLabel h = label(title, 25, true);
        h.setForeground(Color.WHITE);

        JLabel subtitle = label("Evacuation monitoring  /  Resource readiness  /  Situational awareness", 11, false);
        subtitle.setForeground(new Color(190, 207, 230));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(h);
        text.add(Box.createVerticalStrut(6));
        text.add(subtitle);

        JPanel status = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        status.setOpaque(false);

        JLabel dot = new JLabel("●");
        dot.setForeground(new Color(74, 225, 171));
        dot.setFont(new Font("SansSerif", Font.BOLD, 12));

        JLabel tag = new JLabel("SYSTEM OVERVIEW");
        tag.setForeground(Color.WHITE);
        tag.setFont(new Font("SansSerif", Font.BOLD, 10));

        status.add(dot);
        status.add(tag);

        header.add(text, BorderLayout.CENTER);
        header.add(status, BorderLayout.EAST);

        p.add(header, BorderLayout.NORTH);
        return p;
    }
    static JLabel label(String value, int size, boolean bold) {
        JLabel l = new JLabel(value); l.setFont(new Font("SansSerif",bold?Font.BOLD:Font.PLAIN,size)); l.setForeground(INK); return l;
    }
    static JButton button(String text, Runnable action) {
        JButton b = new JButton(text); b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(new Font("SansSerif", Font.BOLD, 12)); b.setForeground(Color.WHITE);
        b.setBackground(BLUE); b.setOpaque(true);
        b.setBorder(new CompoundBorder(
                new LineBorder(new Color(52, 120, 255), 1, true),
                new EmptyBorder(10,16,10,16)
        ));
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setBackground(new Color(36, 104, 214));
        b.setContentAreaFilled(true);
        b.setMargin(new Insets(8,14,8,14));
        b.addActionListener(e -> action.run()); return b;
    }
    static JPanel card(String title, JComponent body) {
        RoundedPanel p = new RoundedPanel(18, SURFACE);
        p.setLayout(new BorderLayout(0,12));
        p.setBorder(new EmptyBorder(16, 18, 16, 18));

        if (title != null) {
            JLabel t = label(title, 15, true);
            t.setForeground(INK);
            p.add(t, BorderLayout.NORTH);
        }

        p.add(body, BorderLayout.CENTER);
        return p;
    }
    static DefaultTableModel model(String... headers) {
        return new DefaultTableModel(headers,0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    }
    static JTable table(DefaultTableModel model) {
        JTable t = new JTable(model); t.setRowHeight(42); t.setShowGrid(false); t.setIntercellSpacing(new Dimension(0,1));
        t.setBackground(Color.WHITE); t.setForeground(INK); t.setSelectionBackground(SELECTED); t.setSelectionForeground(INK);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); t.setAutoCreateRowSorter(true); t.setFillsViewportHeight(true);
        t.setRowMargin(1); t.setBorder(null);
        t.getTableHeader().setPreferredSize(new Dimension(1,42)); t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setBackground(new Color(239,244,250)); t.getTableHeader().setForeground(new Color(75,95,122));
        t.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,11)); t.getTableHeader().setBorder(new MatteBorder(0,0,1,0,LINE));
        for(int i=0;i<model.getColumnCount();i++){
            String header=model.getColumnName(i);
            t.getColumnModel().getColumn(i).setPreferredWidth(i==0||header.equals("Assigned Center")||header.equals("Household")?180:header.equals("Age")?55:95);
        }
        t.setDefaultRenderer(Object.class,new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focus,int row,int col) {
                JLabel l=(JLabel)super.getTableCellRendererComponent(table,value,selected,focus,row,col);
                l.setBorder(new EmptyBorder(0,12,0,12)); l.setForeground(INK);
                if(!selected) l.setBackground(row%2==0?Color.WHITE:new Color(249,251,254));
                String text=value==null?"":value.toString(); l.setToolTipText(text);
                int room=Math.max(30,table.getColumnModel().getColumn(col).getWidth()-26);
                if(value instanceof String && l.getFontMetrics(l.getFont()).stringWidth(text)>room){
                    int split=text.lastIndexOf(' ');
                    while(split>0&&l.getFontMetrics(l.getFont()).stringWidth(text.substring(0,split))>room)split=text.lastIndexOf(' ',split-1);
                    if(split>0){String first=text.substring(0,split).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");String second=text.substring(split+1).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");l.setText("<html>"+first+"<br>"+second+"</html>");}
                }
                return l;
            }
        }); return t;
    }
    static JScrollPane scroll(JComponent content) {
        if(content instanceof JTextArea){((JTextArea)content).setLineWrap(true);((JTextArea)content).setWrapStyleWord(true);}
        JScrollPane s=new JScrollPane(content);
        if(content instanceof JTextArea)s.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        s.setBorder(new LineBorder(LINE,1,true)); s.getViewport().setBackground(Color.WHITE);
        s.getVerticalScrollBar().setUnitIncrement(16); return s;
    }
    static JTextField search(JTable table, String hint) {
        JTextField field = new JTextField(22); field.setToolTipText(hint); field.putClientProperty("JTextField.placeholderText", hint);
        field.setBorder(new CompoundBorder(
                new LineBorder(new Color(221, 228, 238), 1, true),
                new EmptyBorder(8,12,8,12)
        ));
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT_SOFT);
        field.getAccessibleContext().setAccessibleName(hint);
        field.getDocument().addDocumentListener(new DocumentListener() {
            void change() { @SuppressWarnings("unchecked") TableRowSorter<DefaultTableModel> sorter=(TableRowSorter<DefaultTableModel>)table.getRowSorter(); sorter.setRowFilter(field.getText().trim().isEmpty()?null:RowFilter.regexFilter("(?i)"+Pattern.quote(field.getText().trim()))); }
            public void insertUpdate(DocumentEvent e){change();} public void removeUpdate(DocumentEvent e){change();} public void changedUpdate(DocumentEvent e){change();}
        }); return field;
    }
    static JPanel form() { return panel(new GridBagLayout()); }
    static void field(JPanel p, int row, String label, JComponent input) {
        GridBagConstraints c = new GridBagConstraints(); c.gridy=row; c.gridx=0; c.anchor=GridBagConstraints.NORTHWEST; c.insets=new Insets(0,0,14,12);
        JLabel l=UI.label(label,12,true); l.setForeground(MUTED); l.setLabelFor(input); p.add(l,c);
        c.gridx=1; c.weightx=1; c.fill=GridBagConstraints.HORIZONTAL; c.insets=new Insets(0,0,14,0);
        input.setPreferredSize(new Dimension(180,input instanceof JScrollPane?70:36));
        if(input instanceof JTextField || input instanceof JComboBox) input.setFont(new Font("SansSerif",Font.PLAIN,13)); p.add(input,c);
    }
    static void error(Component p, String message) { JOptionPane.showMessageDialog(p,message,"Check information",JOptionPane.WARNING_MESSAGE); }

    static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;
        private Color gradientStart;
        private Color gradientEnd;

        RoundedPanel(int radius, Color fill) {
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }

        void setGradient(Color start, Color end) {
            gradientStart = start;
            gradientEnd = end;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int shadowX = 6;
            int shadowY = 6;
            int shadowW = getWidth() - shadowX;
            int shadowH = getHeight() - shadowY;

            g.setColor(new Color(15, 30, 60, 26));
            g.fillRoundRect(shadowX, shadowY, shadowW, shadowH, radius, radius);

            if (gradientStart != null && gradientEnd != null) {
                g.setPaint(new GradientPaint(0, 0, gradientStart, getWidth(), getHeight(), gradientEnd));
            } else {
                g.setColor(fill);
            }

            g.fillRoundRect(0, 0, getWidth() - shadowX, getHeight() - shadowY, radius, radius);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    static int number(String value, String label, int minimum) {
        try { int n=Integer.parseInt(value.trim()); if(n<minimum)throw new NumberFormatException(); return n; }
        catch(NumberFormatException e){throw new IllegalArgumentException(label+" must be a whole number of at least "+minimum+".");}
    }
    static String required(String value,String label) { if(value.trim().isEmpty())throw new IllegalArgumentException(label+" is required."); return value.trim(); }
}
