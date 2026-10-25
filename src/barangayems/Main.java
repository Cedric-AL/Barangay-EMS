package barangayems;
import javax.swing.SwingUtilities;

import barangayems.frontend.AppFrame;
public class Main {
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> new AppFrame().setVisible(true));
    }
}
