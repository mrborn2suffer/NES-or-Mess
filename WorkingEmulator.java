import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class WorkingEmulator extends JFrame 
{
    private static final int[] NTSC_PALETTE = { 0x626262, 0x001FB2, 0x2404C8, 0x000000 };
    private static final int NES_W = 256;
    private static final int NES_H = 240;
    private static final int SCALE = 3;

    private final NesEmulator emulator;
    private final BufferedImage screen;
    private final JPanel canvas;
    private boolean crtEffectEnabled = true;

    public WorkingEmulator(String romPath) 
    {
        super("NES Emulator - " + romPath);
        emulator = new NesEmulator();
        screen = new BufferedImage(NES_W, NES_H, BufferedImage.TYPE_INT_RGB);
        
        canvas = new JPanel() 
        {
            @Override
            protected void paintComponent(Graphics g) 
            {
                super.paintComponent(g);
                g.drawImage(screen, 0, 0, NES_W * SCALE, NES_H * SCALE, null);
                if (crtEffectEnabled) 
                {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setColor(new Color(0, 0, 0, 50));
                    for (int y = 0; y < getHeight(); y += 3) g2d.fillRect(0, y, getWidth(), 1);
                }
            }
        };
        
        setContentPane(canvas);
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
