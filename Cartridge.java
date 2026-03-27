import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Cartridge
{
    private byte[] prgROM;
    private byte[] chrROM;
    private byte[] prgRAM;

    private int mapperID;
    private int prgBanks;
    private int chrBanks;
    private int mirrorMode; 

    private int[] registers = new int[8];
    private int bankSelect = 0;
    private int prgBankMode = 0;
    private int chrBankMode = 0;
    private int prgBank0, prgBank1, prgBank2, prgBank3;
    private int[] chrBankOffsets = new int[8];
    
    private int prgBankSelect = 0;
    private int chrBankSelect = 0;

    public Cartridge(String filepath) 
    {
        try (FileInputStream fis = new FileInputStream(new File(filepath))) 
        {
            byte[] header = new byte[16];
            fis.read(header);
            if (header[0] != 'N' || header[1] != 'E' || header[2] != 'S' || header[3] != 0x1A) 
            {
                throw new RuntimeException("Invalid NES ROM file!");
            }

            prgBanks = header[4] & 0xFF;
            chrBanks = header[5] & 0xFF;
            
            boolean verticalMirroring = (header[6] & 0x01) != 0;
            mirrorMode = verticalMirroring ? 1 : 0; 
            
            mapperID = ((header[6] >> 4) & 0x0F) | (header[7] & 0xF0);

            System.out.println("PROGRAM FILE INFO :");
            System.out.println("Loaded ROM: " + filepath);
            System.out.println("Mapper ID:  " + mapperID);
            System.out.println("PRG Size:   " + (prgBanks * 16) + " KB");
            System.out.println("CHR Size:   " + (chrBanks * 8) + " KB");

            if ((header[6] & 0x04) != 0) 
            fis.skip(512);
            prgROM = new byte[prgBanks * 16384];
            fis.read(prgROM);

            if (chrBanks > 0) 
            {
                chrROM = new byte[chrBanks * 8192];
                fis.read(chrROM);
            } 
            else 
            {
                chrROM = new byte[8192]; 
            }

            prgRAM = new byte[8192];
            if (mapperID == 4) 
                updateMMC3Pointers();
            else if (mapperID == 7) 
            {
                int maxBanks = prgBanks / 2; 
                prgBankSelect = maxBanks - 1; 
            }

        } 
        catch (IOException e) 
        {
            System.err.println("Failed to load ROM: " + e.getMessage());
        }
    }

}