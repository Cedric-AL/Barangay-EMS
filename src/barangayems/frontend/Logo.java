package barangayems.frontend;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

/** Loads the Barangay-EMS logo from assets/logo.png (project root) or from the classpath. */
final class Logo {
    private static final BufferedImage IMAGE = load();

    private Logo() {}

    private static BufferedImage load() {
        try {
            // Prefer the bundled classpath resource so the logo also works from a packaged JAR.
            try (InputStream in = Logo.class.getResourceAsStream("/assets/logo.png")) {
                if (in != null) return ImageIO.read(in);
            }
            // Keep compatibility with running directly from the project folder.
            File file = new File("assets/logo.png");
            if (file.exists()) return ImageIO.read(file);
        } catch (Exception ignored) {
            // The app still works without the logo.
        }
        return null;
    }

    /** Full-size image for window/taskbar icons, or null if the file is missing. */
    static Image image() { return IMAGE; }

    /** Smoothly scaled square icon, or null if the file is missing. */
    static ImageIcon icon(int size) {
        if (IMAGE == null) return null;
        return new ImageIcon(IMAGE.getScaledInstance(size, size, Image.SCALE_SMOOTH));
    }
}