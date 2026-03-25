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

}