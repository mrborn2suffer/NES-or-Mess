import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
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
    private boolean paused = false;

    public WorkingEmulator(String romPath) 
    {
        super("NES Emulator - " + romPath);
        emulator = new NesEmulator();
        emulator.loadCartridge(romPath);
        screen = new BufferedImage(NES_W, NES_H, BufferedImage.TYPE_INT_RGB);
        
        canvas = new JPanel() 
        {
            @Override
            protected void paintComponent(Graphics g) 
            {
                super.paintComponent(g);
                g.drawImage(screen, 0, 0, NES_W * SCALE, NES_H * SCALE, null);
                if (!paused) 
                {
                    g.setColor(new Color(0, 0, 0, 160));
                    g.fillOval(10, 10, 32, 32);
                    g.setColor(Color.WHITE);
                    g.drawOval(10, 10, 32, 32);
                }
            }
        };
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int mx = e.getX();
                int my = e.getY();
                if (mx >= 10 && mx <= 42 && my >= 10 && my <= 42) {
                    paused = !paused;
                    canvas.repaint();
                }
            }
        });
        
        setContentPane(canvas);
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
