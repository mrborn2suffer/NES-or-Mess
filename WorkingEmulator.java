import javax.swing.JFrame;
import javax.swing.JPanel;

public class WorkingEmulator extends JFrame 
{

    private static final int[] NTSC_PALETTE = 
    {
        0x626262, 0x001FB2, 0x2404C8, 0x5200B2, 0x730076, 0x800024, 0x730B00, 0x522800,
        0x244400, 0x005700, 0x005C00, 0x005324, 0x003C76, 0x000000, 0x000000, 0x000000,
        0xABABAB, 0x0D57FF, 0x4B30FF, 0x8A13FF, 0xBC08D6, 0xD21269, 0xC72E00, 0x9D5400,
        0x607B00, 0x209800, 0x00A300, 0x009942, 0x007DB4, 0x000000, 0x000000, 0x000000,
        0xFFFFFF, 0x53AEFF, 0x9085FF, 0xD365FF, 0xFF57FF, 0xFF5DCF, 0xFF7757, 0xFA9E00,
        0xBDC700, 0x7AE700, 0x43F611, 0x26EF7E, 0x2CD5F6, 0x4E4E4E, 0x000000, 0x000000,
        0xFFFFFF, 0xB6DEFB, 0xC9CAFF, 0xDFC0FF, 0xF2B8FF, 0xFEBCF0, 0xFEC6C3, 0xF8D5A3,
        0xE4E594, 0xCEF192, 0xBEF8A9, 0xB2F9CA, 0xB5F2F2, 0xB8B8B8, 0x000000, 0x000000
    };

    private static final int NES_W  = 256;
    private static final int NES_H  = 240;
    private static final int SCALE  = 3;

    private final NesEmulator emulator;
    private final BufferedImage screen;
    private final JPanel canvas;
    private Timer gameLoop;
    private boolean running = false;

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
            }
        };
        canvas.setPreferredSize(new Dimension(NES_W * SCALE, NES_H * SCALE));
        canvas.setBackground(Color.BLACK);

        addKeyListener(new KeyAdapter() 
        {
            @Override public void keyPressed(KeyEvent e)  
            { 
                handleKey(e.getKeyCode(), true);  
            }
            @Override public void keyReleased(KeyEvent e) 
            { 
                handleKey(e.getKeyCode(), false); 
            }
        });

        setContentPane(canvas);
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setVisible(true);
        requestFocus();
        startGameLoop();
    }

    private void startGameLoop() 
    {
        running = true;
        gameLoop = new Timer(1000 / 60, e -> 
            {
            if (!running) 
                return;
            int cyclesThisFrame = 0;
            while (cyclesThisFrame < 29780) 
            {
                int cyclesBefore = emulator.getCPU().getCycles();
                emulator.step();
                cyclesThisFrame += (emulator.getCPU().getCycles() - cyclesBefore);
            }
            
            renderToBufferedImage();
            canvas.repaint();
        });
        gameLoop.setCoalesce(true);
        gameLoop.start();
    }
    
}