import javax.swing.*;
import java.awt.*;
import java.io.File;

public class RomLoader extends JFrame 
{
    private static Font pricedownFont = null;
    static {
        try {
            pricedownFont = Font.createFont(Font.TRUETYPE_FONT, new File("pricedown.otf"));
        } catch (Exception e) {
            pricedownFont = new Font("SansSerif", Font.BOLD, 22);
        }
    }

    private java.util.List<File> romFiles = new java.util.ArrayList<>();
    private int selectedRomIndex = 0;
    private JPanel contentPanel;
    private JPanel gridComponent;

    public RomLoader() 
    {
        super("NES-or-MESS");
        scanRomDirectory();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 720);
        setLocationRelativeTo(null);
        
        contentPanel = new JPanel(new CardLayout());
        buildGridView();
        add(contentPanel, BorderLayout.CENTER);
    }

    private void buildGridView() 
    {
        if (gridComponent == null) {
            gridComponent = new JPanel(new BorderLayout());
            contentPanel.add(gridComponent, "GRID");
        }
        gridComponent.removeAll();
        JPanel gridGrid = new JPanel(new GridLayout(3, 3, 15, 15));
        gridGrid.setBackground(Color.BLACK);
        
        int currentPage = selectedRomIndex / 9;
        int pageStart = currentPage * 9;
        for (int i = 0; i < 9; i++) {
            int idx = pageStart + i;
            if (idx < romFiles.size()) {
                JPanel cell = new JPanel();
                cell.setBackground(Color.DARK_GRAY);
                gridGrid.add(cell);
            } else {
                JPanel empty = new JPanel();
                empty.setOpaque(false);
                gridGrid.add(empty);
            }
        }
        gridComponent.add(gridGrid, BorderLayout.CENTER);
        
        JButton prev = new JButton("<");
        JButton next = new JButton(">");
        gridComponent.add(prev, BorderLayout.WEST);
        gridComponent.add(next, BorderLayout.EAST);
    }

    private void scanRomDirectory() 
    {
        File dir = new File("Game files");
        if (!dir.exists() || !dir.isDirectory()) dir = new File("game files");
        if (!dir.exists() || !dir.isDirectory()) dir = new File(".");
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".nes"));
        if (files != null) { for (File f : files) romFiles.add(f); }
    }

    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(() -> new RomLoader().setVisible(true));
    }
}
