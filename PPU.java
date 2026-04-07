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
    
     public void reset() 
    {
        control = mask = status = oamAddr = oamData = scroll = addr = data = 0;
        vramAddr = tempVramAddr = fineX = scanline = cycle = frame = 0;
        writeToggle = false;
        for (int i = 0; i < oam.length; i++) 
            oam[i] = 0;
        for (int i = 0; i < vram.length; i++) 
            vram[i] = 0;
        for (int i = 0; i < palette.length; i++) 
            palette[i] = 0;
    }
    
    private void incrementY() 
    {
        if ((vramAddr & 0x7000) != 0x7000) 
        {
            vramAddr += 0x1000;
        } 
        else 
        {
            vramAddr &= ~0x7000;
            int y = (vramAddr & 0x03E0) >> 5;
            if (y == 29) 
            {
                y = 0;
                vramAddr ^= 0x0800;
            } 
            else if (y == 31) 
            {
                y = 0;
            } 
            else 
            {
                y++;
            }
            vramAddr = (vramAddr & ~0x03E0) | (y << 5);
        }
    }
}