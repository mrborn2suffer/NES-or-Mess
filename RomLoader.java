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
    private JPanel contentPanel;
    private JList<String> listComponent;

    public RomLoader() 
    {
        super("NES-or-MESS");
        scanRomDirectory();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 720);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.BLACK);
        
        contentPanel = new JPanel(new CardLayout());
        contentPanel.setBackground(Color.BLACK);
        
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (File f : romFiles) listModel.addElement(f.getName().toUpperCase().replace(".NES", ""));
        listComponent = new JList<>(listModel);
        listComponent.setBackground(Color.BLACK);
        listComponent.setForeground(Color.WHITE);
        listComponent.setFont(pricedownFont.deriveFont(22.0f));
        
        contentPanel.add(new JScrollPane(listComponent), "LIST");
        add(contentPanel, BorderLayout.CENTER);
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
