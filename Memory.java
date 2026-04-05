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

    public void write(int address, byte value) 
    {
        address &= 0xFFFF; 
        
        if (address < 0x2000) 
        {
            ram[address & 0x7FF] = value;
        } 
        else if (address >= 0x8000) 
        {
            if (cartridge != null) cartridge.writePRG(address - 0x8000, value);
        } 
        else if (address >= 0x6000 && address < 0x8000) 
        {
            if (cartridge != null) cartridge.writePRGRAM(address - 0x6000, value);
        } 
        else if (address >= 0x2000 && address < 0x4000) 
        {
            writePPURegister(0x2000 + (address & 0x7), value);
        } 
        else if (address >= 0x4000 && address < 0x4020) 
        {
            writeIORegister(address, value);
        }
    }
    
    private byte readPPURegister(int address) 
    {
        return (byte)(ppu != null ? ppu.readRegister(address) : 0);
    }
    
    private void writePPURegister(int address, byte value) 
    {
        if (ppu != null) 
        ppu.writeRegister(address, value & 0xFF);
    }
    
    private byte readIORegister(int address) 
    {
        if (address == 0x4016) 
        {
            return (byte)(controller != null ? controller.read() : 0);
        }
        return 0;
    }
    
    private void writeIORegister(int address, byte value) 
    {
        if (address == 0x4014) 
        {
            writePPURegister(0x2003, (byte)0x00); 
            int page = (value & 0xFF) << 8;
            for (int i = 0; i < 256; i++) 
            {
                writePPURegister(0x2004, read(page + i));
            }
        } 
        else if (address == 0x4016) 
        {
            if (controller != null) 
            controller.write(value);
        }
    }
    
    public short readWord(int address) 
    {
        return (short)((read(address) & 0xFF) | ((read(address + 1) & 0xFF) << 8));
    }
    
    public void writeWord(int address, short value) 
    {
        write(address, (byte)(value & 0xFF));   
        write(address + 1, (byte)((value >> 8) & 0xFF));
    }
}