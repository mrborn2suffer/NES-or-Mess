import javax.swing.*;
import java.awt.*;
import java.io.File;

public class RomLoader extends JFrame 
{
    private java.util.List<File> romFiles = new java.util.ArrayList<>();
    private JPanel contentPanel;
    private JList<String> listComponent;
    private JPanel gridComponent;

    public RomLoader() 
    {
        super("NES-or-MESS");
        scanRomDirectory();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 720);
        setLocationRelativeTo(null);
        
        contentPanel = new JPanel(new CardLayout());
        
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (File f : romFiles) listModel.addElement(f.getName().toUpperCase().replace(".NES", ""));
        listComponent = new JList<>(listModel);
        contentPanel.add(new JScrollPane(listComponent), "LIST");

        gridComponent = new JPanel(new GridLayout(5, 5, 10, 10));
        contentPanel.add(gridComponent, "GRID");

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
