import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

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
    private final int[] screenPixels; // Direct reference to BufferedImage backbuffer array
    private final JPanel canvas;
    
    // Performance optimized dedicated game loop thread
    private Thread loopThread;
    private volatile boolean running = false;

    // Paused options menu & CRT toggles
    private boolean paused = false;
    private boolean crtEffectEnabled = false;
    private int menuSelectedIndex = 0;

    private static Font pricedownFont = null;
    static 
    {
        try 
        {
            pricedownFont = Font.createFont(Font.TRUETYPE_FONT, new java.io.File("pricedown.otf"));
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(pricedownFont);
        } 
        catch (Exception e) 
        {
            pricedownFont = new Font("SansSerif", Font.BOLD, 22);
        }
    }

    public WorkingEmulator(String romPath) 
    {
        super("NES Emulator - " + romPath);

        emulator = new NesEmulator();
        emulator.loadCartridge(romPath);
        
        // Fast direct pixel memory writing setup
        screen = new BufferedImage(NES_W, NES_H, BufferedImage.TYPE_INT_RGB);
        screenPixels = ((DataBufferInt) screen.getRaster().getDataBuffer()).getData();
        
        canvas = new JPanel() 
        {
            @Override
            protected void paintComponent(Graphics g) 
            {
                super.paintComponent(g);
                g.drawImage(screen, 0, 0, NES_W * SCALE, NES_H * SCALE, null);

                // Render CRT effect
                if (crtEffectEnabled) 
                {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    
                    // Generate blurred image for phosphor bloom
                    java.awt.image.BufferedImage blurred = null;
                    try 
                    {
                        float[] matrix = {
                            1f/16f, 2f/16f, 1f/16f,
                            2f/16f, 4f/16f, 2f/16f,
                            1f/16f, 2f/16f, 1f/16f
                        };
                        java.awt.image.ConvolveOp op = new java.awt.image.ConvolveOp(
                            new java.awt.image.Kernel(3, 3, matrix), 
                            java.awt.image.ConvolveOp.EDGE_NO_OP, null
                        );
                        blurred = op.filter(screen, null);
                    } 
                    catch (Exception ex) {}

                    int w = getWidth();
                    int h = getHeight();

                    // 1. Draw base game screen
                    g2d.drawImage(screen, 0, 0, w, h, null);

                    // 2. Draw blurred overlay to create authentic phosphor bleed/glow
                    if (blurred != null) 
                    {
                        g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.4f));
                        g2d.drawImage(blurred, -2, -2, w + 4, h + 4, null);
                        g2d.setComposite(java.awt.AlphaComposite.SrcOver);
                    }
                    
                    // 3. Draw soft scanlines
                    g2d.setColor(new Color(0, 0, 0, 75));
                    g2d.setStroke(new BasicStroke(1));
                    for (int y = 0; y < h; y += 3) 
                    {
                        g2d.drawLine(0, y, w, y);
                    }
                    
                    // 4. Draw aperture grille lines
                    g2d.setColor(new Color(0, 0, 0, 25));
                    for (int x = 0; x < w; x += 3) 
                    {
                        g2d.drawLine(x, 0, x, h);
                    }
                    
                    // 5. Radial vignette overlay
                    float[] fractions = {0.0f, 0.70f, 1.0f};
                    Color[] colors = {new Color(0, 0, 0, 0), new Color(0, 0, 0, 30), new Color(0, 0, 0, 170)};
                    RadialGradientPaint rgp = new RadialGradientPaint(
                        w / 2.0f, h / 2.0f, 
                        (float)(Math.sqrt(w*w + h*h) / 2.0), 
                        fractions, colors
                    );
                    g2d.setPaint(rgp);
                    g2d.fillRect(0, 0, w, h);
                }

                // Render Pause button
                if (!paused) 
                {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(new Color(0, 0, 0, 160));
                    g2d.fillOval(10, 10, 32, 32);
                    g2d.setColor(new Color(255, 255, 255, 200));
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawOval(10, 10, 32, 32);
                    g2d.fillRect(20, 20, 4, 12);
                    g2d.fillRect(28, 20, 4, 12);
                }

                // Render Pause Menu
                if (paused) 
                {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    g2d.setColor(new Color(0, 0, 0, 185));
                    g2d.fillRect(0, 0, getWidth(), getHeight());

                    g2d.setFont(pricedownFont.deriveFont(48.0f).deriveFont(
                        java.awt.geom.AffineTransform.getScaleInstance(1.4, 1.0)
                    ));
                    String title = "PAUSED";
                    FontMetrics fm = g2d.getFontMetrics();
                    int titleX = (getWidth() - fm.stringWidth(title)) / 2;
                    int titleY = 120;

                    g2d.setColor(new Color(0, 0, 0, 200));
                    g2d.drawString(title, titleX + 3, titleY + 3);
                    g2d.setColor(Color.WHITE);
                    g2d.drawString(title, titleX, titleY);

                    g2d.setColor(new Color(255, 255, 255, 100));
                    g2d.fillRect(titleX - 20, titleY + 15, fm.stringWidth(title) + 40, 2);

                    int startY = 220;
                    int spacing = 65;

                    int skew = 12;
                    int blockW = 380;
                    int blockH = 45;
                    int blockX = (getWidth() - blockW) / 2;
                    int blockY = startY + menuSelectedIndex * spacing - 32;

                    g2d.setColor(Color.WHITE);
                    int[] xPoints = { blockX + skew, blockX + blockW + skew, blockX + blockW, blockX };
                    int[] yPoints = { blockY, blockY, blockY + blockH, blockY + blockH };
                    g2d.fillPolygon(xPoints, yPoints, 4);

                    for (int i = 0; i < 5; i++) 
                    {
                        String optText = getOptionText(i);
                        boolean isSelected = (i == menuSelectedIndex);

                        if (isSelected) 
                        {
                            g2d.setFont(pricedownFont.deriveFont(26.0f).deriveFont(
                                java.awt.geom.AffineTransform.getScaleInstance(1.5, 1.0)
                            ));
                            g2d.setColor(Color.BLACK);
                        } 
                        else 
                        {
                            g2d.setFont(pricedownFont.deriveFont(22.0f).deriveFont(
                                java.awt.geom.AffineTransform.getScaleInstance(1.4, 1.0)
                            ));
                            g2d.setColor(new Color(0, 0, 0, 160));
                            int textX = (getWidth() - g2d.getFontMetrics().stringWidth(optText)) / 2;
                            g2d.drawString(optText, textX + 2, startY + i * spacing + 2);
                            g2d.setColor(Color.LIGHT_GRAY);
                        }

                        int textX = (getWidth() - g2d.getFontMetrics().stringWidth(optText)) / 2;
                        g2d.drawString(optText, textX, startY + i * spacing);
                    }
                }
            }
        };
        canvas.setPreferredSize(new Dimension(NES_W * SCALE, NES_H * SCALE));
        canvas.setBackground(Color.BLACK);

        canvas.addMouseListener(new MouseAdapter() 
        {
            @Override
            public void mousePressed(MouseEvent e) 
            {
                if (!paused) 
                {
                    int mx = e.getX();
                    int my = e.getY();
                    if (mx >= 10 && mx <= 42 && my >= 10 && my <= 42) 
                    {
                        pauseGame();
                    }
                }
            }
        });

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
        loopThread = new Thread(() -> 
        {
            final long targetFrameTimeNs = 1_000_000_000 / 60; // 60 FPS Target
            
            while (running) 
            {
                long frameStart = System.nanoTime();
                
                int cyclesThisFrame = 0;
                while (cyclesThisFrame < 29780) 
                {
                    int cyclesBefore = emulator.getCPU().getCycles();
                    emulator.step();
                    cyclesThisFrame += (emulator.getCPU().getCycles() - cyclesBefore);
                }
                
                renderToBufferedImage();
                SwingUtilities.invokeLater(canvas::repaint);
                
                long frameDuration = System.nanoTime() - frameStart;
                long sleepTimeNs = targetFrameTimeNs - frameDuration;
                if (sleepTimeNs > 0) 
                {
                    try 
                    {
                        Thread.sleep(sleepTimeNs / 1_000_000, (int)(sleepTimeNs % 1_000_000));
                    } 
                    catch (InterruptedException ex) 
                    {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
        loopThread.setPriority(Thread.MAX_PRIORITY);
        loopThread.start();
    }
    
    private void renderToBufferedImage() 
    {
        int[] ppuBuffer = emulator.getPPU().getScreenBuffer();
        for (int i = 0; i < NES_W * NES_H; i++) 
        {
            screenPixels[i] = NTSC_PALETTE[ppuBuffer[i] & 0x3F];
        }
    }

    private void pauseGame() 
    {
        paused = true;
        running = false;
        if (loopThread != null) 
        {
            try 
            {
                loopThread.join(500);
            } 
            catch (InterruptedException e) 
            {
                Thread.currentThread().interrupt();
            }
            loopThread = null;
        }
        canvas.repaint();
    }

    private void resumeGame() 
    {
        paused = false;
        startGameLoop();
        canvas.repaint();
    }

    private String getOptionText(int index) 
    {
        switch (index) 
        {
            case 0: return "RESUME GAME";
            case 1: return "RESTART";
            case 2: return "CRT SHADER: " + (crtEffectEnabled ? "ON" : "OFF");
            case 3: return "LOAD NEW ROM";
            case 4: return "EXIT EMULATOR";
            default: return "";
        }
    }

    private void triggerMenuAction() 
    {
        switch (menuSelectedIndex) 
        {
            case 0: // RESUME GAME
                resumeGame();
                break;
            case 1: // RESTART
                emulator.reset();
                resumeGame();
                break;
            case 2: // CRT SHADER
                crtEffectEnabled = !crtEffectEnabled;
                canvas.repaint();
                break;
            case 3: // LOAD NEW ROM
                running = false;
                if (loopThread != null) 
                {
                    try { loopThread.join(100); } catch(InterruptedException e){}
                    loopThread = null;
                }
                dispose();
                SwingUtilities.invokeLater(() -> new RomLoader().setVisible(true));
                break;
            case 4: // EXIT EMULATOR
                running = false;
                if (loopThread != null) 
                {
                    try { loopThread.join(100); } catch(InterruptedException e){}
                    loopThread = null;
                }
                dispose();
                System.exit(0);
                break;
        }
    }

    private void handleKey(int code, boolean pressed) 
    {
        if (paused) 
        {
            if (pressed) 
            {
                if (code == KeyEvent.VK_UP) 
                {
                    menuSelectedIndex = (menuSelectedIndex - 1 + 5) % 5;
                    canvas.repaint();
                } 
                else if (code == KeyEvent.VK_DOWN) 
                {
                    menuSelectedIndex = (menuSelectedIndex + 1) % 5;
                    canvas.repaint();
                } 
                else if (code == KeyEvent.VK_ENTER) 
                {
                    triggerMenuAction();
                }
                else if (code == KeyEvent.VK_ESCAPE) 
                {
                    resumeGame();
                }
            }
            return;
        }

        if (code == KeyEvent.VK_ESCAPE && pressed) 
        {
            pauseGame();
            return;
        }

        Controller controller = emulator.getMemory().getController();
        if (controller == null) 
            return;

        switch (code) 
        {
            case KeyEvent.VK_SPACE: //Action
            case KeyEvent.VK_Z:     
                controller.setButtonState(Controller.BUTTON_A, pressed); 
                break;
            case KeyEvent.VK_J:
            case KeyEvent.VK_X:     
                controller.setButtonState(Controller.BUTTON_B, pressed); 
                break;
                
            case KeyEvent.VK_SHIFT: //Menu
                controller.setButtonState(Controller.BUTTON_SELECT, pressed); 
                break;
            case KeyEvent.VK_ENTER: 
                controller.setButtonState(Controller.BUTTON_START, pressed); 
                break;
                
            case KeyEvent.VK_W: //WASD & Arrow keys
            case KeyEvent.VK_UP:    
                controller.setButtonState(Controller.BUTTON_UP, pressed); 
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:  
                controller.setButtonState(Controller.BUTTON_DOWN, pressed); 
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:  
                controller.setButtonState(Controller.BUTTON_LEFT, pressed); 
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT: 
                controller.setButtonState(Controller.BUTTON_RIGHT, pressed); 
                break;
        }
    }

    public static void main(String[] args) 
    {
        String romPath = args.length > 0 ? args[0] : "Donkey Kong (World) (Rev A).nes";
        SwingUtilities.invokeLater(() -> new WorkingEmulator(romPath));
    }
}