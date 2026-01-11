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

    public void step() 
    {
        if (nmiPending) 
            {
            handleNMI();
            nmiPending = false;
        } 
        else if (irqPending && !getInterruptFlag()) 
            {
            handleIRQ();
            irqPending = false;
        }
        int opcode = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF; 
        executeOpcode(opcode);
    }
    
    //Main operation i.e execution of operation codes
    private void executeOpcode(int opcode) 
    {
        switch (opcode) 
        {
            //ADC operation
            case 0x69: ADC_IMM(); 
            break;
            case 0x65: ADC_ZP(); 
            break;
            case 0x75: ADC_ZPX(); 
            break;
            case 0x6D: ADC_ABS(); 
            break;
            case 0x7D: ADC_ABSX(); 
            break;
            case 0x79: ADC_ABSY(); 
            break;
            case 0x61: ADC_INDX(); 
            break;
            case 0x71: ADC_INDY(); 
            break;
            
            // AND operation
            case 0x29: AND_IMM(); 
            break;
            case 0x25: AND_ZP(); 
            break;
            case 0x35: AND_ZPX(); 
            break;
            case 0x2D: AND_ABS(); 
            break;
            case 0x3D: AND_ABSX(); 
            break;
            case 0x39: AND_ABSY(); 
            break;
            case 0x21: AND_INDX(); 
            break;
            case 0x31: AND_INDY(); 
            break;
            
            // ASL operation
            case 0x0A: ASL_ACC(); 
            break;
            case 0x06: ASL_ZP(); 
            break;
            case 0x16: ASL_ZPX(); 
            break;
            case 0x0E: ASL_ABS(); 
            break;
            case 0x1E: ASL_ABSX(); 
            break;
            
            // BCC operation
            case 0x90: BCC(); 
            break;
            
            // BCS operation
            case 0xB0: BCS(); 
            break;
            
            // BEQ operation
            case 0xF0: BEQ(); 
            break;
            
            // BIT operation
            case 0x24: BIT_ZP(); 
            break;
            case 0x2C: BIT_ABS(); 
            break;
            
            // BMI operation
            case 0x30: BMI(); 
            break;
            
            // BNE operation
            case 0xD0: BNE(); 
            break;
            
            // BPL operation
            case 0x10: BPL(); 
            break;
            
            // BRK operation
            case 0x00: BRK(); 
            break;
            
            // BVC operation
            case 0x50: BVC(); 
            break;
            
            // BVS operation
            case 0x70: BVS(); 
            break;
            
            // CLC operation
            case 0x18: CLC(); 
            break;
            
            // CLD operatin
            case 0xD8: CLD(); 
            break;
            
            // CLI operation
            case 0x58: CLI(); 
            break;
            
            // CLV operation
            case 0xB8: CLV(); 
            break;

    
}