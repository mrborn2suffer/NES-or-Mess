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
    
}