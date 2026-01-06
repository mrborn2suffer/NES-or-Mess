public class CPU {
    private Memory memory;
    
    private int A;  // Accumulator
    private int X;  // X register
    private int Y;  // Y register
    private int SP; // Stack pointer
    private int PC; // Program counter
    private int P;  // Status register
    
    //Staus FLags
    private static final int FLAG_CARRY = 0x01;
    private static final int FLAG_ZERO = 0x02;
    private static final int FLAG_INTERRUPT = 0x04;
    private static final int FLAG_DECIMAL = 0x08;
    private static final int FLAG_BREAK = 0x10;
    private static final int FLAG_UNUSED = 0x20;
    private static final int FLAG_OVERFLOW = 0x40;
    private static final int FLAG_NEGATIVE = 0x80;
    
    private int cycles;
    private boolean nmiPending;
    private boolean irqPending;

    public CPU(Memory memory) 
    {
        this.memory = memory;
        reset();
    }
    
    public void reset() 
    {
        A = 0;
        X = 0;
        Y = 0;
        SP = 0xFD;
        P = 0x24; 
        cycles = 0;
        nmiPending = false;
        irqPending = false;
        
        PC = memory.readWord(0xFFFC) & 0xFFFF;
    }
    
}