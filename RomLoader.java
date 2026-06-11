import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class RomLoader extends JFrame 
{
    private static final Color BG_COLOR = new Color(0, 0, 0);          // Pure Black
    private static final Color PANEL_COLOR = new Color(20, 20, 20);     // Very Dark Gray
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color MUTED_TEXT = new Color(150, 150, 150);   // Medium Gray

    // Dynamically loaded Pricedown font
    private static Font pricedownFont = null;
    static 
    {
        try 
        {
            pricedownFont = Font.createFont(Font.TRUETYPE_FONT, new File("pricedown.otf"));
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(pricedownFont);
        } 
        catch (Exception e) 
        {
            System.err.println("Could not load Pricedown font: " + e.getMessage());
            // Fallback font
            pricedownFont = new Font("SansSerif", Font.BOLD, 22);
        }
    }

    private List<File> romFiles = new ArrayList<>();
    private int selectedRomIndex = 0;
    private boolean gridMode = false;

    private JPanel contentPanel;
    private JList<String> listComponent;
    private DefaultListModel<String> listModel;
    private JPanel gridComponent;
    private List<JPanel> gridCells = new ArrayList<>();
    private IconButton toggleViewButton;

    // Pulse animation variables
    private Timer pulseTimer;
    private float pulseAlpha = 0.6f;
    private boolean pulseDirection = true;

    // Track active cover downloads to prevent duplicate threads
    private List<String> downloadingRoms = new ArrayList<>();

    public RomLoader() 
    {
        super("NES-or-MESS");
        scanRomDirectory();
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 720);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_COLOR);
        setLayout(new BorderLayout());

        // Pulsing animation timer (50ms update rate)
        pulseTimer = new Timer(50, e -> {
            if (pulseDirection) {
                pulseAlpha += 0.04f;
                if (pulseAlpha >= 1.0f) {
                    pulseAlpha = 1.0f;
                    pulseDirection = false;
                }
            } else {
                pulseAlpha -= 0.04f;
                if (pulseAlpha <= 0.4f) {
                    pulseAlpha = 0.4f;
                    pulseDirection = true;
                }
            }
            repaint();
        });
        pulseTimer.start();

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.WHITE);
                g.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        headerPanel.setBackground(PANEL_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Bold White Header Title with Transparent Black Shadow
        JLabel titleLabel = new JLabel("NES-or-MESS") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                
                String text = getText();
                Font font = getFont();
                g2d.setFont(font);

                // Transparent Black Shadow Offset
                g2d.setColor(new Color(0, 0, 0, 160));
                g2d.drawString(text, 2, 28);

                // White Front Text
                g2d.setColor(Color.WHITE);
                g2d.drawString(text, 0, 26);
            }
        };
        titleLabel.setFont(pricedownFont.deriveFont(28.0f).deriveFont(
            java.awt.geom.AffineTransform.getScaleInstance(1.4, 1.0)
        ));
        titleLabel.setPreferredSize(new Dimension(400, 36));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Grid/List View Toggle Button
        toggleViewButton = new IconButton();
        headerPanel.add(toggleViewButton, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Content Area with Flat Grid Pattern
        contentPanel = new JPanel(new CardLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                // Pure Black Background
                g2d.setColor(BG_COLOR);
                g2d.fillRect(0, 0, w, h);
                
                // Draw a very subtle, elegant dark gray grid (minimalist arcade style)
                g2d.setColor(new Color(20, 20, 20));
                g2d.setStroke(new BasicStroke(1));
                for (int x = 0; x < w; x += 40) {
                    g2d.drawLine(x, 0, x, h);
                }
                for (int y = 0; y < h; y += 40) {
                    g2d.drawLine(0, y, w, y);
                }
            }
        };
        contentPanel.setBackground(BG_COLOR);
        add(contentPanel, BorderLayout.CENTER);

        buildListView();
        buildGridView();

        // Show default list view
        showLayout();

        // Footer Help Info
        JPanel footerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), 2);
            }
        };
        footerPanel.setBackground(PANEL_COLOR);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        JLabel helpLabel = new JLabel("USE ARROW KEYS TO NAVIGATE • PRESS ENTER TO SELECT GAME • CLICK ICONS TO TOGGLE/PAGE");
        helpLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
        helpLabel.setForeground(MUTED_TEXT);
        footerPanel.add(helpLabel);
        add(footerPanel, BorderLayout.SOUTH);

        // Key Listeners
        addKeyListener(new KeyAdapter() 
        {
            @Override
            public void keyPressed(KeyEvent e) 
            {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_UP) 
                {
                    navigate(gridMode ? -3 : -1);
                } 
                else if (code == KeyEvent.VK_DOWN) 
                {
                    navigate(gridMode ? 3 : 1);
                } 
                else if (code == KeyEvent.VK_LEFT && gridMode) 
                {
                    navigate(-1);
                } 
                else if (code == KeyEvent.VK_RIGHT && gridMode) 
                {
                    navigate(1);
                } 
                else if (code == KeyEvent.VK_ENTER) 
                {
                    launchSelectedGame();
                }
                else if (code == KeyEvent.VK_V) 
                {
                    toggleView();
                }
            }
        });

        setFocusable(true);
        requestFocusInWindow();
    }

    private void scanRomDirectory() 
    {
        // Try scanning "Game files" directory first, then fallback to lowercase "game files" or current directory "."
        File dir = new File("Game files");
        if (!dir.exists() || !dir.isDirectory()) 
        {
            dir = new File("game files");
            if (!dir.exists() || !dir.isDirectory()) 
            {
                dir = new File(".");
            }
        }
        
        File[] files = dir.listFiles(new FilenameFilter() 
        {
            @Override
            public boolean accept(File dir, String name) 
            {
                return name.toLowerCase().endsWith(".nes");
            }
        });
        if (files != null) 
        {
            for (File f : files) 
            {
                romFiles.add(f);
            }
        }
    }

    private void toggleView() 
    {
        gridMode = !gridMode;
        showLayout();
    }

    private void showLayout() 
    {
        CardLayout cl = (CardLayout) contentPanel.getLayout();
        if (gridMode) 
        {
            cl.show(contentPanel, "GRID");
            updateGridHighlight();
        } 
        else 
        {
            cl.show(contentPanel, "LIST");
            listComponent.setSelectedIndex(selectedRomIndex);
            listComponent.ensureIndexIsVisible(selectedRomIndex);
        }
        requestFocusInWindow();
    }

    private void navigate(int delta) 
    {
        if (romFiles.isEmpty()) return;
        int size = romFiles.size();
        int newIndex = (selectedRomIndex + delta + size) % size;
        
        selectedRomIndex = newIndex;
        if (gridMode) 
        {
            updateGridHighlight();
        } 
        else 
        {
            listComponent.setSelectedIndex(selectedRomIndex);
            listComponent.ensureIndexIsVisible(selectedRomIndex);
        }
    }

    private void navigatePage(int direction) 
    {
        if (romFiles.isEmpty()) return;
        int totalPages = (int) Math.ceil((double) romFiles.size() / 9.0);
        if (totalPages <= 1) return;
        
        int currentPage = selectedRomIndex / 9;
        int nextPage = (currentPage + direction + totalPages) % totalPages;
        
        int localIndex = selectedRomIndex % 9;
        int newIndex = nextPage * 9 + localIndex;
        if (newIndex >= romFiles.size()) 
        {
            newIndex = romFiles.size() - 1;
        }
        
        selectedRomIndex = newIndex;
        updateGridHighlight();
        requestFocusInWindow();
    }

    private void updateGridHighlight() 
    {
        buildGridView(); // Automatically rebuilds the current page grid
        
        int cellIndex = selectedRomIndex % 9;
        int borderAlpha = (int)(pulseAlpha * 255);
        Color borderHighlight = new Color(255, 255, 255, borderAlpha);
        
        for (int i = 0; i < gridCells.size(); i++) 
        {
            JPanel cell = gridCells.get(i);
            if (i == cellIndex) 
            {
                cell.setBorder(new LineBorder(borderHighlight, 3, true));
                cell.setBackground(new Color(40, 40, 40));
            } 
            else 
            {
                cell.setBorder(new LineBorder(PANEL_COLOR, 2, true));
                cell.setBackground(PANEL_COLOR);
            }
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void buildListView() 
    {
        listModel = new DefaultListModel<>();
        for (File f : romFiles) 
        {
            listModel.addElement(f.getName().toUpperCase().replace(".NES", ""));
        }
        listComponent = new JList<>(listModel);
        listComponent.setBackground(new Color(0, 0, 0, 0)); // Fully transparent list
        listComponent.setOpaque(false);
        listComponent.setSelectionBackground(new Color(0, 0, 0, 0));
        listComponent.setSelectionForeground(Color.WHITE);
        listComponent.setFont(pricedownFont.deriveFont(22.0f));
        listComponent.setBorder(BorderFactory.createEmptyBorder(30, 15, 30, 15));
        
        listComponent.setCellRenderer(new DefaultListCellRenderer() 
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) 
            {
                JPanel itemPanel = new JPanel(new BorderLayout()) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        if (isSelected) {
                            Graphics2D g2d = (Graphics2D) g;
                            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            
                            // Pure White selection block (parallelogram slanted like / in GTA)
                            g2d.setColor(Color.WHITE);
                            
                            Font selectedFont = pricedownFont.deriveFont(26.0f).deriveFont(
                                java.awt.geom.AffineTransform.getScaleInstance(1.5, 1.0)
                            );
                            FontMetrics fm = g2d.getFontMetrics(selectedFont);
                            int textW = fm.stringWidth(value.toString());
                            int blockW = textW + 60;
                            int blockH = getHeight() - 8;
                            int blockX = (getWidth() - blockW) / 2;
                            int blockY = 4;
                            
                            int skew = 12;
                            int[] xPoints = { blockX + skew, blockX + blockW + skew, blockX + blockW, blockX };
                            int[] yPoints = { blockY, blockY, blockY + blockH, blockY + blockH };
                            g2d.fillPolygon(xPoints, yPoints, 4);
                        }
                    }
                };
                itemPanel.setOpaque(false);
                itemPanel.setBorder(BorderFactory.createEmptyBorder(14, 0, 14, 0));

                // Centered text with transparent black shadow/outline
                JLabel label = new JLabel(value.toString(), JLabel.CENTER) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2d = (Graphics2D) g;
                        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        
                        String text = getText();
                        Font font = getFont();
                        FontMetrics fm = g2d.getFontMetrics(font);
                        int x = (getWidth() - fm.stringWidth(text)) / 2;
                        int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                        
                        g2d.setFont(font);
                        
                        if (!isSelected) {
                            // Transparent black shadow
                            g2d.setColor(new Color(0, 0, 0, 160));
                            g2d.drawString(text, x + 2, y + 2);
                            g2d.setColor(Color.LIGHT_GRAY); // Unselected light gray
                        } else {
                            // Selected black text on white background
                            g2d.setColor(Color.BLACK);
                        }
                        g2d.drawString(text, x, y);
                    }
                };
                
                // Stretch text horizontally (1.4x unselected, 1.5x selected for enlargement effect)
                if (isSelected) 
                {
                    label.setFont(pricedownFont.deriveFont(26.0f).deriveFont(
                        java.awt.geom.AffineTransform.getScaleInstance(1.5, 1.0)
                    ));
                } 
                else 
                {
                    label.setFont(pricedownFont.deriveFont(22.0f).deriveFont(
                        java.awt.geom.AffineTransform.getScaleInstance(1.4, 1.0)
                    ));
                }
                itemPanel.add(label, BorderLayout.CENTER);
                return itemPanel;
            }
        });

        // Double click mouse listener
        listComponent.addMouseListener(new MouseAdapter() 
        {
            @Override
            public void mouseClicked(MouseEvent e) 
            {
                if (e.getClickCount() == 2) 
                {
                    selectedRomIndex = listComponent.getSelectedIndex();
                    launchSelectedGame();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(listComponent);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        contentPanel.add(scrollPane, "LIST");
    }

    private void buildGridView() 
    {
        if (gridComponent == null) 
        {
            gridComponent = new JPanel(new BorderLayout());
            gridComponent.setBackground(new Color(0, 0, 0, 0));
            gridComponent.setOpaque(false);
            contentPanel.add(gridComponent, "GRID");
        }
        
        gridComponent.removeAll();
        
        int totalPages = (int) Math.ceil((double) romFiles.size() / 9.0);
        if (totalPages == 0) totalPages = 1;
        int currentPage = selectedRomIndex / 9;
        
        JPanel gridGrid = new JPanel(new GridLayout(3, 3, 15, 15));
        gridGrid.setBackground(new Color(0, 0, 0, 0));
        gridGrid.setOpaque(false);
        gridGrid.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        gridCells.clear();
        int pageStartIndex = currentPage * 9;
        
        for (int i = 0; i < 9; i++) 
        {
            int romIndex = pageStartIndex + i;
            if (romIndex < romFiles.size()) 
            {
                File rom = romFiles.get(romIndex);
                JPanel cell = createGridCell(rom, romIndex);
                gridGrid.add(cell);
                gridCells.add(cell);
            } 
            else 
            {
                // Empty slot panel - leave completely blank and transparent
                JPanel emptyCell = new JPanel();
                emptyCell.setOpaque(false);
                emptyCell.setBackground(new Color(0, 0, 0, 0));
                gridGrid.add(emptyCell);
            }
        }
        gridComponent.add(gridGrid, BorderLayout.CENTER);
        
        // Page Navigation Side Buttons (< and >)
        PageButton prevButton = new PageButton(true);
        PageButton nextButton = new PageButton(false);
        gridComponent.add(prevButton, BorderLayout.WEST);
        gridComponent.add(nextButton, BorderLayout.EAST);
        
        // Page Indicator Label (GTA style)
        JLabel pageLabel = new JLabel("PAGE " + (currentPage + 1) + " OF " + totalPages, JLabel.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                String text = getText();
                Font font = getFont();
                FontMetrics fm = g2d.getFontMetrics(font);
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2d.setFont(font);
                
                // Shadow
                g2d.setColor(new Color(0, 0, 0, 180));
                g2d.drawString(text, x + 2, y + 2);
                
                g2d.setColor(Color.WHITE);
                g2d.drawString(text, x, y);
            }
        };
        pageLabel.setFont(pricedownFont.deriveFont(16.0f).deriveFont(
            java.awt.geom.AffineTransform.getScaleInstance(1.3, 1.0)
        ));
        pageLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 15, 0));
        gridComponent.add(pageLabel, BorderLayout.SOUTH);

        gridComponent.revalidate();
        gridComponent.repaint();
    }

    private JPanel createGridCell(File romFile, int index) 
    {
        JPanel cell = new JPanel(new BorderLayout());
        cell.setBackground(PANEL_COLOR);
        cell.setBorder(new LineBorder(PANEL_COLOR, 2, true));

        // Get matching screenshot or cartridge render
        Image img = getGameImage(romFile.getName());
        JLabel imgLabel = new JLabel(new ImageIcon(img));
        cell.add(imgLabel, BorderLayout.CENTER);

        // Title text underneath
        String name = romFile.getName().replace(".nes", "").toUpperCase();
        if (name.length() > 20) 
        {
            name = name.substring(0, 17) + "...";
        }
        JLabel title = new JLabel(name, JLabel.CENTER);
        title.setFont(pricedownFont.deriveFont(12.0f));
        title.setForeground(TEXT_COLOR);
        title.setBorder(BorderFactory.createEmptyBorder(8, 5, 8, 5));
        cell.add(title, BorderLayout.SOUTH);

        // Click to select/launch
        cell.addMouseListener(new MouseAdapter() 
        {
            @Override
            public void mousePressed(MouseEvent e) 
            {
                selectedRomIndex = index;
                updateGridHighlight();
                if (e.getClickCount() == 2) 
                {
                    launchSelectedGame();
                }
            }
        });

        return cell;
    }

    private Image getGameImage(String romName) 
    {
        String baseName = romName.replace(".nes", "").replace(".NES", "");
        File exactFile = new File("Output", baseName + ".png");
        
        File imageFile = null;
        if (exactFile.exists()) 
        {
            imageFile = exactFile;
        } 
        else 
        {
            // Try fuzzy matching in Output first
            String lowerName = romName.toLowerCase();
            File outputDir = new File("Output");
            if (outputDir.exists() && outputDir.isDirectory()) 
            {
                File[] files = outputDir.listFiles();
                if (files != null) 
                {
                    for (File f : files) 
                    {
                        String fName = f.getName().toLowerCase();
                        if (lowerName.contains("aladdin") && fName.contains("aladdin") && fName.contains("game")) 
                        {
                            imageFile = f;
                            break;
                        } 
                        else if (lowerName.contains("battletoads") && fName.contains("battletoads")) 
                        {
                            imageFile = f;
                            break;
                        } 
                        else if (lowerName.contains("donkey") && fName.contains("donkey") && fName.contains("game")) 
                        {
                            imageFile = f;
                            break;
                        } 
                        else if (lowerName.contains("mario") && fName.contains("mario") && fName.contains("game")) 
                        {
                            imageFile = f;
                            break;
                        } 
                        else if (lowerName.contains("jungle") && fName.contains("jungle")) 
                        {
                            imageFile = f;
                            break;
                        }
                    }
                }
            }
        }
        
        // Trigger background download if no local visual asset exists
        if (imageFile == null) 
        {
            downloadBoxArt(romName);
        }

        if (imageFile != null && imageFile.exists()) 
        {
            try 
            {
                BufferedImage bimg = ImageIO.read(imageFile);
                return bimg.getScaledInstance(210, 150, Image.SCALE_SMOOTH);
            } 
            catch (Exception e) 
            {
                // Fallback to dynamic rendering
            }
        }

        // Draw default cartridge label graphic dynamically (Black & White Minimalist Theme)
        BufferedImage fallback = new BufferedImage(210, 150, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = fallback.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Dark gray cartridge shell
        g.setColor(new Color(40, 40, 40));
        g.fillRect(0, 0, 210, 150);
        g.setColor(new Color(20, 20, 20));
        g.drawRoundRect(10, 10, 190, 130, 8, 8);
        
        // Light Gray Label
        g.setColor(new Color(200, 200, 200));
        g.fillRect(25, 25, 160, 100);
        
        // Label Text
        g.setColor(Color.BLACK);
        g.setFont(pricedownFont.deriveFont(22.0f));
        g.drawString("NES", 45, 60);
        g.setFont(pricedownFont.deriveFont(12.0f));
        String label = romName.replace(".nes", "").toUpperCase();
        if (label.length() > 20) 
        {
            label = label.substring(0, 17) + "...";
        }
        g.drawString(label, 40, 95);
        
        g.dispose();
        return fallback;
    }

    private void downloadBoxArt(String romName) 
    {
        String baseName = romName.replace(".nes", "").replace(".NES", "");
        if (downloadingRoms.contains(baseName)) return; // Already downloading
        
        downloadingRoms.add(baseName);
        
        new Thread(() -> {
            try 
            {
                // Encode spaces and parentheses for libretro raw assets path
                String encodedName = java.net.URLEncoder.encode(baseName, "UTF-8")
                        .replace("+", "%20")
                        .replace("%28", "(")
                        .replace("%29", ")");
                
                String urlStr = "https://raw.githubusercontent.com/libretro-thumbnails/Nintendo_-_Nintendo_Entertainment_System/master/Named_Boxarts/" + encodedName + ".png";
                java.net.URL url = new java.net.URL(urlStr);
                
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) 
                {
                    // Create Output folder if it doesn't exist
                    File outputDir = new File("Output");
                    if (!outputDir.exists()) outputDir.mkdirs();
                    
                    File outputFile = new File(outputDir, baseName + ".png");
                    try (java.io.InputStream in = conn.getInputStream();
                         java.io.FileOutputStream out = new java.io.FileOutputStream(outputFile)) 
                    {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) 
                        {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    
                    // Repaint grid immediately in Event Dispatch Thread
                    SwingUtilities.invokeLater(() -> {
                        updateGridHighlight();
                    });
                }
            } 
            catch (Exception e) 
            {
                // Fail silently (will display cartridge fallback render)
            } 
            finally 
            {
                downloadingRoms.remove(baseName);
            }
        }).start();
    }

    private void launchSelectedGame() 
    {
        if (romFiles.isEmpty() || selectedRomIndex < 0 || selectedRomIndex >= romFiles.size()) return;
        File selectedRom = romFiles.get(selectedRomIndex);
        
        // Hide loader menu window
        setVisible(false);
        dispose();

        // Launch emulator window
        SwingUtilities.invokeLater(() -> 
        {
            new WorkingEmulator(selectedRom.getPath());
        });
    }

    // Custom 2x2 Grid/List Toggle Icon Button (B&W style)
    private class IconButton extends JPanel 
    {
        private boolean hover = false;

        public IconButton() 
        {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(40, 40));
            
            // Trigger repaint on hover and toggleView on press
            addMouseListener(new MouseAdapter() {
                @Override 
                public void mouseEntered(MouseEvent e) 
                { 
                    hover = true; 
                    repaint(); 
                }
                
                @Override 
                public void mouseExited(MouseEvent e) 
                { 
                    hover = false; 
                    repaint(); 
                }

                @Override
                public void mousePressed(MouseEvent e)
                {
                    toggleView();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) 
        {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background glow (Black & White theme)
            if (hover) 
            {
                g2d.setColor(new Color(255, 255, 255, 40));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2d.setColor(Color.WHITE);
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            } 
            else 
            {
                g2d.setColor(new Color(255, 255, 255, 10));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2d.setColor(new Color(120, 120, 120));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            }

            g2d.setColor(Color.WHITE);
            if (gridMode) 
            {
                // Draw LIST icon (three horizontal lines)
                g2d.fillRect(10, 13, 20, 3);
                g2d.fillRect(10, 19, 20, 3);
                g2d.fillRect(10, 25, 20, 3);
            } 
            else 
            {
                // Draw 2x2 GRID icon (4 small squares)
                g2d.fillRect(10, 10, 8, 8);
                g2d.fillRect(22, 10, 8, 8);
                g2d.fillRect(10, 22, 8, 8);
                g2d.fillRect(22, 22, 8, 8);
            }
            g2d.dispose();
        }
    }

    // Custom Page navigation side button (B&W style)
    private class PageButton extends JPanel 
    {
        private boolean isLeft;
        private boolean hover = false;

        public PageButton(boolean isLeft) 
        {
            this.isLeft = isLeft;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(50, 200)); // Tall buttons on left/right
            
            addMouseListener(new MouseAdapter() {
                @Override 
                public void mouseEntered(MouseEvent e) 
                { 
                    hover = true; 
                    repaint(); 
                }
                
                @Override 
                public void mouseExited(MouseEvent e) 
                { 
                    hover = false; 
                    repaint(); 
                }

                @Override
                public void mousePressed(MouseEvent e)
                {
                    navigatePage(isLeft ? -1 : 1);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) 
        {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Background glow on hover
            if (hover) 
            {
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.fillRoundRect(5, h/2 - 30, w - 10, 60, 8, 8);
                g2d.setColor(Color.WHITE);
                g2d.drawRoundRect(5, h/2 - 30, w - 10, 60, 8, 8);
            }
            else 
            {
                g2d.setColor(new Color(255, 255, 255, 10));
                g2d.fillRoundRect(5, h/2 - 30, w - 10, 60, 8, 8);
                g2d.setColor(new Color(100, 100, 100));
                g2d.drawRoundRect(5, h/2 - 30, w - 10, 60, 8, 8);
            }

            // Draw Chevron (< or >)
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = w / 2;
            int cy = h / 2;
            if (isLeft) 
            {
                g2d.drawLine(cx + 5, cy - 10, cx - 5, cy);
                g2d.drawLine(cx - 5, cy, cx + 5, cy + 10);
            } 
            else 
            {
                g2d.drawLine(cx - 5, cy - 10, cx + 5, cy);
                g2d.drawLine(cx + 5, cy, cx - 5, cy + 10);
            }
            g2d.dispose();
        }
    }

    public static void main(String[] args) 
    {
        // Set look and feel
        try 
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } 
        catch (Exception e) {}
        
        SwingUtilities.invokeLater(() -> 
        {
            new RomLoader().setVisible(true);
        });
    }
}
