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

    public void step() 
    {
        if (scanline < 240 && cycle >= 1 && cycle <= 256) 
        {
            renderPixel(cycle - 1, scanline);
        }
        cycle++;
        
        if (cycle > 340) 
        {
            cycle = 0;
            scanline++;
            if (scanline > 261) 
            {
                scanline = 0;
                frame++;
            }
        }

        boolean renderingEnabled = (mask & 0x18) != 0;

        if (scanline == 261 && cycle == 1) 
        {
            status &= ~0xE0; 
        }

        if (scanline == 261 && cycle >= 280 && cycle <= 304) 
        {
            if (renderingEnabled) 
            {
                vramAddr = (vramAddr & 0x041F) | (tempVramAddr & 0x7BE0);
            }
        }

        if (renderingEnabled && (scanline < 240 || scanline == 261)) 
        {
            if (cycle == 256) 
                incrementY();

            if (cycle == 257) 
            {
                vramAddr = (vramAddr & 0xFBE0) | (tempVramAddr & 0x041F);
                oamAddr = 0;
                
                if (scanline < 240) 
                {
                    int spriteCount = 0;
                    boolean is8x16 = (control & 0x20) != 0;
                    int spriteHeight = is8x16 ? 16 : 8;
                    for (int i = 0; i < 64; i++) 
                    {
                        int spriteY = (oam[i * 4] & 0xFF) + 1;
                        if (scanline >= spriteY && scanline < spriteY + spriteHeight) 
                        spriteCount++;
                    }

                    if (spriteCount > 8) 
                    status |= 0x20;
                }
            }
        }

        if (scanline == 241 && cycle == 1) 
        {
            status |= 0x80;
            if ((control & 0x80) != 0 && cpu != null) 
            cpu.triggerNMI();
        }
    }

    private void renderPixel(int x, int y) 
    {
        int bgColor = 0;
        boolean bgOpaque = false;
        
        boolean showBg = (mask & 0x08) != 0 && ((mask & 0x02) != 0 || x >= 8);
        boolean showSprites = (mask & 0x10) != 0 && ((mask & 0x04) != 0 || x >= 8);
        
        if (showBg) 
        {
            int scrollX = ((vramAddr & 0x001F) << 3) | fineX;
            int pixelX = scrollX + x;
            int effectiveTileX = pixelX / 8;
            int effectiveTileY = (vramAddr & 0x03E0) >> 5;
            int fineY = (vramAddr >> 12) & 7;
            
            int nameTableAddr = 0x2000 | (vramAddr & 0x0C00);
            if (effectiveTileX >= 32) 
            {
                effectiveTileX -= 32;
                nameTableAddr ^= 0x0400;
            }
            
            int patternAddr = getPatternAddress(nameTableAddr, effectiveTileX, effectiveTileY);
            int patternLow  = readVRAM(patternAddr + fineY) & 0xFF;
            int patternHigh = readVRAM(patternAddr + 8 + fineY) & 0xFF;
            
            int bit = 7 - (pixelX % 8);
            int bgPaletteIndex = ((patternHigh >> bit) & 1) << 1 | ((patternLow >> bit) & 1);
            
            bgOpaque = (bgPaletteIndex != 0); 
            
            int attrAddr = nameTableAddr + 0x03C0 + ((effectiveTileY / 4) * 8) + (effectiveTileX / 4);
            int attrByte = readVRAM(attrAddr) & 0xFF;
            
            int shift = ((effectiveTileY % 4) / 2) * 4 + ((effectiveTileX % 4) / 2) * 2;
            int paletteNum = (attrByte >> shift) & 0x03;
            
            bgColor = getColorFromPalette(paletteNum, bgPaletteIndex);
        } 
        else 
        {
            bgColor = getColorFromPalette(0, 0); 
        }
}

int spriteColor = 0;
        boolean spriteOpaque = false;
        boolean spritePriority = false;
        boolean isSpriteZero = false;
        
        if (showSprites) 
        {
            boolean is8x16 = (control & 0x20) != 0;
            int spriteHeight = is8x16 ? 16 : 8;
            
            for (int i = 0; i < 64; i++) 
            {
                int spriteY     = oam[i * 4] & 0xFF;
                int spriteIndex = oam[i * 4 + 1] & 0xFF;
                int spriteAttr  = oam[i * 4 + 2] & 0xFF;
                int spriteX     = oam[i * 4 + 3] & 0xFF;
                
                spriteY += 1; 
                
                if (y >= spriteY && y < spriteY + spriteHeight) 
                {
                    int spriteRow = y - spriteY;
                    if ((spriteAttr & 0x80) != 0) 
                    spriteRow = (spriteHeight - 1) - spriteRow;
                    
                    int patternAddr;
                    if (is8x16) 
                    {
                        int table = (spriteIndex & 1) * 0x1000;
                        int tile = spriteIndex & 0xFE;
                        if (spriteRow >= 8) 
                        {
                            tile++; 
                            spriteRow -= 8;
                        }
                        patternAddr = table + tile * 16 + spriteRow;
                    } 
                    else 
                    {
                        int table = (control & 0x08) != 0 ? 0x1000 : 0x0000;
                        patternAddr = table + spriteIndex * 16 + spriteRow;
                    }
                    
                    int patternLow  = readVRAM(patternAddr) & 0xFF;
                    int patternHigh = readVRAM(patternAddr + 8) & 0xFF;
                    
                    int spriteCol = x - spriteX;
                    if (spriteCol >= 0 && spriteCol < 8) 
                    {
                        if ((spriteAttr & 0x40) != 0) 
                        spriteCol = 7 - spriteCol;
                        
                        int bit = 7 - spriteCol;
                        int spritePaletteIndex = ((patternHigh >> bit) & 1) << 1 | ((patternLow >> bit) & 1);
                        
                        if (spritePaletteIndex != 0) 
                        {
                            spriteOpaque = true;
                            spriteColor = getColorFromPalette(4 + (spriteAttr & 0x03), spritePaletteIndex);
                            spritePriority = (spriteAttr & 0x20) == 0;
                            isSpriteZero = (i == 0);
                            break; 
                        }
                    }
                }
            }
        }

        int finalColor = bgColor;
        if (spriteOpaque) 
        {
            if (!bgOpaque || spritePriority) 
            finalColor = spriteColor;
        }
        
        if (isSpriteZero && bgOpaque && x < 255 && showBg && showSprites) 
        {
            status |= 0x40; 
        }
        
        screenBuffer[y * 256 + x] = finalColor;
    }
    
    private int getPatternAddress(int nameTableAddr, int tileX, int tileY) 
    {
        int nameTableIndex = tileY * 32 + tileX;
        int tileIndex = readVRAM(nameTableAddr + nameTableIndex) & 0xFF;
        int patternTableAddr = (control & 0x10) != 0 ? 0x1000 : 0x0000;
        return patternTableAddr + tileIndex * 16;
    }
    
    private int getColorFromPalette(int paletteNum, int index) 
    {
        if (index == 0) 
        return palette[0] & 0xFF; 
        return palette[paletteNum * 4 + index] & 0xFF;
    }
