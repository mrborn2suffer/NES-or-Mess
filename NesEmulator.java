public class NesEmulator 
{
    private Memory memory;
    private CPU cpu;
    private PPU ppu;

    public NesEmulator() 
    {
        memory = new Memory();
        cpu = new CPU(memory);
        ppu = new PPU(memory);

        memory.setPPU(ppu);
        ppu.setCPU(cpu);
        memory.setCPU(cpu);
    }

    public void loadCartridge(String filepath) 
    {
        Cartridge cart = new Cartridge(filepath);
        memory.loadCartridge(cart);
        reset();
    }

    public void reset()
    {
        cpu.reset();
        ppu.reset();
        if (memory.getController() != null) 
        {
            memory.getController().clear();
        }
    }

    public void step() 
    {
        int cyclesBefore = cpu.getCycles();
        cpu.step();
        int cyclesTaken = cpu.getCycles() - cyclesBefore;
        for (int i = 0; i < cyclesTaken * 3; i++) 
        {
            ppu.step();
        }
    }

    public CPU getCPU() 
    { 
        return cpu; 
    }
    public PPU getPPU() 
    { 
        return ppu; 
    }
    public Memory getMemory() 
    { 
        return memory; 
    }
}