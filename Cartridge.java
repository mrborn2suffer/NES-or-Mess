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

    public byte readPRG(int address) 
    {

        if (mapperID == 0) //Mapper 0
            return prgROM[address % prgROM.length];

        else if (mapperID == 2) //Mapper 2
        {
            if (address < 0x4000) 
                return prgROM[(prgBankSelect * 0x4000) + address]; 
            else 
                return prgROM[prgROM.length - 0x4000 + (address - 0x4000)]; 
        } 

        else if (mapperID == 3) //Mapper 3
            return prgROM[address % prgROM.length];

        else if (mapperID == 4) //Mapper 4
        {
            if (address < 0x2000) 
                return prgROM[prgBank0 + address];
            else if (address < 0x4000) 
                return prgROM[prgBank1 + (address - 0x2000)];
            else if (address < 0x6000) 
                return prgROM[prgBank2 + (address - 0x4000)];
            else 
                return prgROM[prgBank3 + (address - 0x6000)];
        } 

        else if (mapperID == 7) //Mapper 7 
        {
            return prgROM[(prgBankSelect * 32768) + (address % 32768)];
        }
        return prgROM[address % prgROM.length];
    }

    public void writePRG(int address, byte value) 
    {

        if (mapperID == 2) 
        {
            prgBankSelect = value & 0x0F; 
        } 

        else if (mapperID == 3) 
        {
            chrBankSelect = value & 0x03; 
        } 
        
        else if (mapperID == 7) 
        {
            int maxBanks = prgROM.length / 32768;
            prgBankSelect = (value & 0x07) % maxBanks; 
            mirrorMode = ((value & 0x10) != 0) ? 3 : 2; 
        } 
        
        else if (mapperID == 4) 
        {
            int realAddress = address + 0x8000;
            if (realAddress >= 0x8000 && realAddress <= 0x9FFF) 
                {
                if ((realAddress % 2) == 0) 
                {
                    bankSelect = value & 0x07;
                    prgBankMode = (value >> 6) & 0x01;
                    chrBankMode = (value >> 7) & 0x01;
                } 
                else 
                {
                    registers[bankSelect] = value & 0xFF;
                    updateMMC3Pointers();
                }
            } 
            else if (realAddress >= 0xA000 && realAddress <= 0xBFFF) 
            {
                if ((realAddress % 2) == 0) 
                    mirrorMode = ((value & 0x01) == 0) ? 1 : 0; 
            }
        }
    }

    private void updateMMC3Pointers() 
    {
        int prgLength = prgROM.length;
        int fixedBank1 = prgLength - 0x4000; 
        int fixedBank2 = prgLength - 0x2000; 

        if (prgBankMode == 0) 
        {
            prgBank0 = (registers[6] * 0x2000) % prgLength;
            prgBank1 = (registers[7] * 0x2000) % prgLength;
            prgBank2 = fixedBank1;
            prgBank3 = fixedBank2;
        } 
        else 
        {
            prgBank0 = fixedBank1;
            prgBank1 = (registers[7] * 0x2000) % prgLength;
            prgBank2 = (registers[6] * 0x2000) % prgLength;
            prgBank3 = fixedBank2;
        }

        int chrLen = chrROM.length;
        if (chrLen > 0) 
        {
            if (chrBankMode == 0) 
            {
                chrBankOffsets[0] = ((registers[0] & 0xFE) * 0x400) % chrLen;
                chrBankOffsets[1] = ((registers[0] | 0x01) * 0x400) % chrLen;
                chrBankOffsets[2] = ((registers[1] & 0xFE) * 0x400) % chrLen;
                chrBankOffsets[3] = ((registers[1] | 0x01) * 0x400) % chrLen;
                chrBankOffsets[4] = (registers[2] * 0x400) % chrLen;
                chrBankOffsets[5] = (registers[3] * 0x400) % chrLen;
                chrBankOffsets[6] = (registers[4] * 0x400) % chrLen;
                chrBankOffsets[7] = (registers[5] * 0x400) % chrLen;
            } 
            
            else 
            {
                chrBankOffsets[0] = (registers[2] * 0x400) % chrLen;
                chrBankOffsets[1] = (registers[3] * 0x400) % chrLen;
                chrBankOffsets[2] = (registers[4] * 0x400) % chrLen;
                chrBankOffsets[3] = (registers[5] * 0x400) % chrLen;
                chrBankOffsets[4] = ((registers[0] & 0xFE) * 0x400) % chrLen;
                chrBankOffsets[5] = ((registers[0] | 0x01) * 0x400) % chrLen;
                chrBankOffsets[6] = ((registers[1] & 0xFE) * 0x400) % chrLen;
                chrBankOffsets[7] = ((registers[1] | 0x01) * 0x400) % chrLen;
            }
        }
    }

    public byte readCHR(int address) 
    {
        if (mapperID == 3) 
        {
            return chrROM[(chrBankSelect * 0x2000) + address];
        } 
        else if (mapperID == 4) 
        {
            int window = (address / 0x400) & 7;
            int offset = address % 0x400;
            return chrROM[chrBankOffsets[window] + offset];
        }
        return chrROM[address % chrROM.length];
    }

    public void writeCHR(int address, byte value) 
    {
        chrROM[address % chrROM.length] = value; 
    }

    public byte readPRGRAM(int address) 
    { 
    return prgRAM[address & 0x1FFF]; 
    }
    public void writePRGRAM(int address, byte value) 
    { 
    prgRAM[address & 0x1FFF] = value; 
    }
    public int getMirrorMode() 
    { 
    return mirrorMode; 
    }

}