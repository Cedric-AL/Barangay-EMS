package barangayems.frontend;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Flat replacements for the default Swing (Metal) look of drop-downs,
 * scroll bars and check boxes. Registered globally in {@link UI#setup()}.
 */
public final class FlatUI {

    private FlatUI() {
    }

    /** Thin, rounded scroll bar without arrow buttons. */
    public static final class ScrollBar extends BasicScrollBarUI {

        public static ComponentUI createUI(JComponent c) {
            return new ScrollBar();
        }

        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(196, 204, 216);
            trackColor = new Color(0, 0, 0, 0);
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            return scrollbar.getOrientation() == JScrollBar.VERTICAL
                    ? new Dimension(10, 48) : new Dimension(48, 10);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zero();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zero();
        }

        private static JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            // transparent track
        }

        @Override
        protected void paintThumb(Graphics graphics, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(isThumbRollover() ? new Color(160, 170, 186) : thumbColor);
            boolean vertical = scrollbar.getOrientation() == JScrollBar.VERTICAL;
            int pad = 2;
            if (vertical) {
                g.fill(new RoundRectangle2D.Float(r.x + pad, r.y + pad, r.width - pad * 2, r.height - pad * 2, 6, 6));
            } else {
                g.fill(new RoundRectangle2D.Float(r.x + pad, r.y + pad, r.width - pad * 2, r.height - pad * 2, 6, 6));
            }
            g.dispose();
        }
    }

    /** Flat drop-down with a simple chevron and padded list items. */
    public static final class Combo extends BasicComboBoxUI {

        public static ComponentUI createUI(JComponent c) {
            return new Combo();
        }

        @Override
        protected JButton createArrowButton() {
            JButton b = new JButton() {
                @Override
                protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(comboBox != null && comboBox.isEnabled() ? UI.MUTED : UI.LINE_STRONG);
                    g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = getWidth() / 2 - 2, cy = getHeight() / 2;
                    g.drawPolyline(new int[]{cx - 4, cx, cx + 4}, new int[]{cy - 2, cy + 2, cy - 2}, 3);
                    g.dispose();
                }
            };
            b.setBorder(BorderFactory.createEmptyBorder());
            b.setContentAreaFilled(false);
            b.setFocusPainted(false);
            b.setOpaque(false);
            b.setPreferredSize(new Dimension(30, 30));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            comboBox.setBackground(Color.WHITE);
            comboBox.setForeground(UI.INK);
            comboBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            comboBox.setBorder(new javax.swing.border.LineBorder(UI.LINE_STRONG, 1, true));
        }

        @Override
        protected ListCellRenderer<Object> createRenderer() {
            return new Renderer();
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            g.setColor(comboBox.isEnabled() ? Color.WHITE : UI.SUBTLE);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            g.setColor(comboBox.isEnabled() ? Color.WHITE : UI.SUBTLE);
            g.fillRect(0, 0, c.getWidth(), c.getHeight());
            super.paint(g, c);
        }

        @Override
        public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
            super.paintCurrentValue(g, bounds, false);
        }

        static final class Renderer extends BasicComboBoxRenderer implements UIResource {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean selected, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                l.setBorder(new EmptyBorder(7, 10, 7, 10));
                l.setForeground(UI.INK);
                l.setBackground(selected && index >= 0 ? UI.SELECTED : Color.WHITE);
                if (index < 0) l.setOpaque(false);
                return l;
            }
        }
    }

    /** Rounded check box with a blue fill when selected. */
    public static final class CheckIcon implements Icon, UIResource, java.io.Serializable {
        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }

        @Override
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean selected = c instanceof AbstractButton && ((AbstractButton) c).isSelected();
            boolean enabled = c.isEnabled();
            if (selected) {
                g.setColor(enabled ? UI.BLUE : new Color(171, 193, 232));
                g.fill(new RoundRectangle2D.Float(x + 1, y + 1, 16, 16, 5, 5));
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawPolyline(new int[]{x + 5, x + 8, x + 13}, new int[]{y + 9, y + 12, y + 6}, 3);
            } else {
                g.setColor(enabled ? Color.WHITE : UI.SUBTLE);
                g.fill(new RoundRectangle2D.Float(x + 1, y + 1, 16, 16, 5, 5));
                g.setColor(UI.LINE_STRONG);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(new RoundRectangle2D.Float(x + 1.5f, y + 1.5f, 15, 15, 5, 5));
            }
            g.dispose();
        }
    }

    /** Spinner with small flat chevron buttons. */
    public static final class Spinner extends BasicSpinnerUI {

        public static ComponentUI createUI(JComponent c) {
            return new Spinner();
        }

        @Override
        protected Component createNextButton() {
            Component b = chevron(true);
            installNextButtonListeners(b);
            return b;
        }

        @Override
        protected Component createPreviousButton() {
            Component b = chevron(false);
            installPreviousButtonListeners(b);
            return b;
        }

        private static JButton chevron(boolean up) {
            JButton b = new JButton() {
                @Override
                protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (getModel().isRollover()) {
                        g.setColor(UI.SUBTLE);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                    g.setColor(UI.MUTED);
                    g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = getWidth() / 2, cy = getHeight() / 2;
                    if (up) g.drawPolyline(new int[]{cx - 4, cx, cx + 4}, new int[]{cy + 2, cy - 2, cy + 2}, 3);
                    else g.drawPolyline(new int[]{cx - 4, cx, cx + 4}, new int[]{cy - 2, cy + 2, cy - 2}, 3);
                    g.dispose();
                }
            };
            b.setPreferredSize(new Dimension(26, 16));
            b.setBorder(BorderFactory.createEmptyBorder());
            b.setContentAreaFilled(false);
            b.setFocusPainted(false);
            b.setRolloverEnabled(true);
            b.setOpaque(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }
    }
}
