package barangayems.frontend;

import barangayems.backend.CenterSuitability;
import javax.swing.*;
import java.awt.*;
import java.util.*;

final class FacilityChecklist extends JPanel {
    private final java.util.List<JCheckBox> boxes = new ArrayList<>();
    FacilityChecklist() {
        super(new GridLayout(0,1,0,2)); setOpaque(false);
        for (String name : CenterSuitability.FEATURES) {
            JCheckBox box = new JCheckBox(name); box.setOpaque(false); boxes.add(box); add(box);
        }
    }
    String[] selected() { return boxes.stream().filter(JCheckBox::isSelected).map(JCheckBox::getText).toArray(String[]::new); }
    void select(String[] values) {
        Set<String> set = new HashSet<>(Arrays.asList(values == null ? new String[0] : values));
        for(JCheckBox box:boxes) box.setSelected(set.contains(box.getText()));
    }
    void onChange(Runnable action) { for(JCheckBox box:boxes)box.addActionListener(e -> action.run()); }
    void editable(boolean value) { for(JCheckBox box:boxes)box.setEnabled(value); }
}
