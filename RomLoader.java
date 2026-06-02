import javax.swing.*;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class RomLoader extends JFrame 
{
    private List<File> romFiles = new ArrayList<>();
    private JList<String> listComponent;
    private DefaultListModel<String> listModel;

    public RomLoader() 
    {
        super("NES-or-MESS");
        scanRomDirectory();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 720);
        setLocationRelativeTo(null);
        
        listModel = new DefaultListModel<>();
        for (File f : romFiles) 
        {
            listModel.addElement(f.getName().toUpperCase().replace(".NES", ""));
        }
        listComponent = new JList<>(listModel);
        add(new JScrollPane(listComponent));
    }

    private void scanRomDirectory() 
    {
        File dir = new File("Game files");
        if (!dir.exists() || !dir.isDirectory()) dir = new File("game files");
        if (!dir.exists() || !dir.isDirectory()) dir = new File(".");
        
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".nes"));
        if (files != null) 
        {
            for (File f : files) romFiles.add(f);
        }
    }

    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(() -> new RomLoader().setVisible(true));
    }
}
