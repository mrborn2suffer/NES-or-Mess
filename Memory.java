public class Memory 
{
    private byte[] ram;
    private Cartridge cartridge;
    private PPU ppu;
    private Controller controller; 
    private CPU cpu; 
    
    public Memory() 
    {
        this.ram = new byte[0x800]; 
        this.cartridge = null;
        this.controller = new Controller();
    }
    
    public void loadCartridge(Cartridge cartridge) 
    { 
        this.cartridge = cartridge; 
    }
    public Cartridge getCartridge() 
    { 
        return cartridge; 
    }
    public void setPPU(PPU ppu) 
    { 
        this.ppu = ppu; 
    }
    public Controller getController() 
    { 
        return controller; 
    }
    public void setCPU(CPU cpu) 
    { 
        this.cpu = cpu; 
    }
    
    public byte read(int address) 
    {
        address &= 0xFFFF; 
        
        if (address < 0x2000) 
        {
            return ram[address & 0x7FF];
        } 
        else if (address >= 0x8000) 
        {
            return (byte)(cartridge != null ? cartridge.readPRG(address - 0x8000) : 0);
        } 
        else if (address >= 0x6000 && address < 0x8000) 
        {
            return (byte)(cartridge != null ? cartridge.readPRGRAM(address - 0x6000) : 0);
        } 
        else if (address >= 0x2000 && address < 0x4000) 
        {
            return readPPURegister(0x2000 + (address & 0x7));
        } 
        else if (address >= 0x4000 && address < 0x4020) 
        {
            return readIORegister(address);
        }
        return 0;
    }

}