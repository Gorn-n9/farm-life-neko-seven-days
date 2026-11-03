package farmlife;

import farmlife.ui.GameWindow;
import javax.swing.*;
import java.nio.file.Path;

public final class Main {
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        if (args.length == 2 && args[0].equals("--preview")) {
            SwingUtilities.invokeAndWait(
                    () -> {
                        try {
                            new GameWindow().renderPreview(Path.of(args[1]));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
            return;
        }
        SwingUtilities.invokeLater(
                () -> {
                    JFrame frame = new JFrame("Farm Life · Neko's Seven Days");
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setContentPane(new GameWindow());
                    frame.setSize(1200, 900);
                    frame.setMinimumSize(new java.awt.Dimension(1100, 830));
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                });
    }
}
