public class PPU 
{
    private Memory memory;
    private CPU cpu;
    
    private int control, mask, status, oamAddr, oamData, scroll, addr, data;
    private int vramAddr, tempVramAddr, fineX;
    private boolean writeToggle;
    private int scanline, cycle, frame;
    
    private byte[] oam = new byte[256];
    private byte[] vram = new byte[0x4000];
    private byte[] palette = new byte[32];
    private int[] screenBuffer = new int[256 * 240];
    
    public PPU(Memory memory) 
    { 
        this.memory = memory; reset(); 
    }
    public void setCPU(CPU cpu) 
    { 
        this.cpu = cpu; 
    }
    
    
}