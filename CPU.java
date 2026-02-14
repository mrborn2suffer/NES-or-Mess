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

            // CMP operation
            case 0xC9: CMP_IMM(); 
            break;
            case 0xC5: CMP_ZP(); 
            break;
            case 0xD5: CMP_ZPX(); 
            break;
            case 0xCD: CMP_ABS(); 
            break;
            case 0xDD: CMP_ABSX(); 
            break;
            case 0xD9: CMP_ABSY(); 
            break;
            case 0xC1: CMP_INDX(); 
            break;
            case 0xD1: CMP_INDY(); 
            break;
            
            // CPX operation
            case 0xE0: CPX_IMM(); 
            break;
            case 0xE4: CPX_ZP(); 
            break;
            case 0xEC: CPX_ABS(); 
            break;
            
            // CPY operation
            case 0xC0: CPY_IMM(); 
            break;
            case 0xC4: CPY_ZP(); 
            break;
            case 0xCC: CPY_ABS(); 
            break;
            
            // DEC operation
            case 0xC6: DEC_ZP(); 
            break;
            case 0xD6: DEC_ZPX(); 
            break;
            case 0xCE: DEC_ABS(); 
            break;
            case 0xDE: DEC_ABSX(); 
            break;
            
            // DEX operation
            case 0xCA: DEX(); 
            break;
            
            // DEY operation
            case 0x88: DEY(); 
            break;
            
            // EOR operation
            case 0x49: EOR_IMM(); 
            break;
            case 0x45: EOR_ZP(); 
            break;
            case 0x55: EOR_ZPX(); 
            break;
            case 0x4D: EOR_ABS(); 
            break;
            case 0x5D: EOR_ABSX(); 
            break;
            case 0x59: EOR_ABSY(); 
            break;
            case 0x41: EOR_INDX(); 
            break;
            case 0x51: EOR_INDY(); 
            break;
            
            // INC operation
            case 0xE6: INC_ZP(); 
            break;
            case 0xF6: INC_ZPX(); 
            break;
            case 0xEE: INC_ABS(); 
            break;
            case 0xFE: INC_ABSX(); 
            break;
            
            // INX operation
            case 0xE8: INX(); 
            break;
            
            // INY operation
            case 0xC8: INY(); 
            break;
            
            // JMP operation
            case 0x4C: JMP_ABS(); 
            break;
            case 0x6C: JMP_IND(); 
            break;
            
            // JSR operation
            case 0x20: JSR(); 
            break;
            
            // LDA operation
            case 0xA9: LDA_IMM(); 
            break;
            case 0xA5: LDA_ZP(); 
            break;
            case 0xB5: LDA_ZPX(); 
            break;
            case 0xAD: LDA_ABS(); 
            break;
            case 0xBD: LDA_ABSX(); 
            break;
            case 0xB9: LDA_ABSY(); 
            break;
            case 0xA1: LDA_INDX(); 
            break;
            case 0xB1: LDA_INDY(); 
            break;
            
            // LDX operation
            case 0xA2: LDX_IMM(); 
            break;
            case 0xA6: LDX_ZP(); 
            break;
            case 0xB6: LDX_ZPY(); 
            break;
            case 0xAE: LDX_ABS(); 
            break;
            case 0xBE: LDX_ABSY(); 
            break;
            
            // LDY operation
            case 0xA0: LDY_IMM(); 
            break;
            case 0xA4: LDY_ZP(); 
            break;
            case 0xB4: LDY_ZPX(); 
            break;
            case 0xAC: LDY_ABS(); 
            break;
            case 0xBC: LDY_ABSX(); 
            break;
            
            // LSR operation
            case 0x4A: LSR_ACC(); 
            break;
            case 0x46: LSR_ZP(); 
            break;
            case 0x56: LSR_ZPX(); 
            break;
            case 0x4E: LSR_ABS(); 
            break;
            case 0x5E: LSR_ABSX(); 
            break;
            
            // NOP operation
            case 0xEA: NOP(); 
            break;
            
            // ORA operation
            case 0x09: ORA_IMM(); 
            break;
            case 0x05: ORA_ZP(); 
            break;
            case 0x15: ORA_ZPX(); 
            break;
            case 0x0D: ORA_ABS(); 
            break;
            case 0x1D: ORA_ABSX(); 
            break;
            case 0x19: ORA_ABSY(); 
            break;
            case 0x01: ORA_INDX(); 
            break;
            case 0x11: ORA_INDY(); 
            break;

            // PHA operation
            case 0x48: PHA(); 
            break;
            
            // PHP operation
            case 0x08: PHP(); 
            break;
            
            // PLA operation
            case 0x68: PLA(); 
            break;
            
            // PLP operation
            case 0x28: PLP(); 
            break;
            
            // ROL operation
            case 0x2A: ROL_ACC(); 
            break;
            case 0x26: ROL_ZP(); 
            break;
            case 0x36: ROL_ZPX(); 
            break;
            case 0x2E: ROL_ABS(); 
            break;
            case 0x3E: ROL_ABSX(); 
            break;
            
            // ROR operation
            case 0x6A: ROR_ACC(); 
            break;
            case 0x66: ROR_ZP(); 
            break;
            case 0x76: ROR_ZPX(); 
            break;
            case 0x6E: ROR_ABS(); 
            break;
            case 0x7E: ROR_ABSX(); 
            break;
            
            // RTI operation
            case 0x40: RTI(); 
            break;
            
            // RTS operation
            case 0x60: RTS(); 
            break;
            
            // SBC operation
            case 0xE9: SBC_IMM(); 
            break;
            case 0xE5: SBC_ZP(); 
            break;
            case 0xF5: SBC_ZPX(); 
            break;
            case 0xED: SBC_ABS(); 
            break;
            case 0xFD: SBC_ABSX(); 
            break;
            case 0xF9: SBC_ABSY(); 
            break;
            case 0xE1: SBC_INDX(); 
            break;
            case 0xF1: SBC_INDY(); 
            break;
            
            // SEC operation
            case 0x38: SEC(); 
            break;
            
            // SED
            case 0xF8: SED(); 
            break;
            
            // SEI operation
            case 0x78: SEI(); 
            break;
            
            // STA operation
            case 0x85: STA_ZP(); 
            break;
            case 0x95: STA_ZPX(); 
            break;
            case 0x8D: STA_ABS(); 
            break;
            case 0x9D: STA_ABSX(); 
            break;
            case 0x99: STA_ABSY(); 
            break;
            case 0x81: STA_INDX(); 
            break;
            case 0x91: STA_INDY(); 
            break;
            
            // STX operation
            case 0x86: STX_ZP(); 
            break;
            case 0x96: STX_ZPY(); 
            break;
            case 0x8E: STX_ABS(); 
            break;
            
            // STY operation
            case 0x84: STY_ZP(); 
            break;
            case 0x94: STY_ZPX(); 
            break;
            case 0x8C: STY_ABS(); 
            break;
            
            // TAX operation
            case 0xAA: TAX(); 
            break;
            
            // TAY operation
            case 0xA8: TAY(); 
            break;
            
            // TSX operation
            case 0xBA: TSX(); 
            break;
            
            // TXA operation
            case 0x8A: TXA(); 
            break;
            
            // TXS operation
            case 0x9A: TXS(); 
            break;
            
            // TYA operation
            case 0x98: TYA(); 
            break;

            // Unofficial opcodes
            case 0x04: NOP_ZP(); 
            break;
            case 0x0C: NOP_ABS(); 
            break;
            case 0x14: NOP_ZPX(); 
            break;
            case 0x1A: NOP_IMplied(); 
            break;
            case 0x1C: NOP_ABSX(); 
            break;
            case 0x34: NOP_ZPX(); 
            break;
            case 0x3A: NOP_IMplied(); 
            break;
            case 0x3C: NOP_ABSX(); 
            break;
            case 0x44: NOP_ZP(); 
            break;
            case 0x54: NOP_ZPX(); 
            break;
            case 0x5A: NOP_IMplied(); 
            break;
            case 0x5C: NOP_ABSX(); 
            break;
            case 0x64: NOP_ZP(); 
            break;
            case 0x74: NOP_ZPX(); 
            break;
            case 0x7A: NOP_IMplied(); 
            break;
            case 0x7C: NOP_ABSX(); 
            break;
            case 0x80: NOP_IMM(); 
            break;
            case 0x82: NOP_IMM(); 
            break;
            case 0x89: NOP_IMM(); 
            break;
            case 0xC2: NOP_IMM(); 
            break;
            case 0xD4: NOP_ZPX(); 
            break;
            case 0xDA: NOP_IMplied(); 
            break;
            case 0xDC: NOP_ABSX(); 
            break;
            case 0xE2: NOP_IMM(); 
            break;
            case 0xF4: NOP_ZPX(); 
            break;
            case 0xFA: NOP_IMplied(); 
            break;
            case 0xFC: NOP_ABSX(); 
            break;
            
            default:
                cycles += 1;
                break;
        }
    }
    
    //Zero-page (first 256 bytes : $0000 to $00FF)
    private int readZeroPageWord(int zpAddress) 
    {
        zpAddress &= 0xFF; 
        int low = memory.read(zpAddress) & 0xFF;
        int high = memory.read((zpAddress + 1) & 0xFF) & 0xFF; 
        return (high << 8) | low;
    }

    private int immediate() 
    {
        int value = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        return value;
    }
    
    private int zeroPage() 
    {
        int address = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        return address;
    }
    
    private int zeroPageX() 
    {
        int address = (memory.read(PC) + X) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        return address;
    }
    
    private int zeroPageY() 
    {
        int address = (memory.read(PC) + Y) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        return address;
    }

    private int absolute() 
    {
        int address = memory.readWord(PC) & 0xFFFF;
        PC = (PC + 2) & 0xFFFF;
        return address;
    }
    
    private int absoluteX() 
    {
        int address = (memory.readWord(PC) + X) & 0xFFFF;
        PC = (PC + 2) & 0xFFFF;
        return address;
    }
    
    private int absoluteY() 
    {
        int address = (memory.readWord(PC) + Y) & 0xFFFF;
        PC = (PC + 2) & 0xFFFF;
        return address;
    }
    
    private int indexedIndirect() 
    {
        int pointer = (memory.read(PC) + X) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        return readZeroPageWord(pointer);
    }
    
    private int indirectIndexed() 
    {
        int pointer = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        return (readZeroPageWord(pointer) + Y) & 0xFFFF;
    }
    
    private void setZeroFlag(int value) 
    {
        if ((value & 0xFF) == 0) 
        P |= FLAG_ZERO;
        else 
        P &= ~FLAG_ZERO;
    }
    
    private void setZeroFlag(boolean condition) 
    {
        if (condition) 
        P |= FLAG_ZERO;
        else 
        P &= ~FLAG_ZERO;
    }
    
    private void setNegativeFlag(int value) 
    {
        if ((value & 0x80) != 0) 
        P |= FLAG_NEGATIVE;
        else
        P &= ~FLAG_NEGATIVE;
    }
    
    private void setCarryFlag(boolean set)
     {
        if (set) 
        P |= FLAG_CARRY;
        else 
        P &= ~FLAG_CARRY;
    }
    
    private void setOverflowFlag(boolean set) 
    {
        if (set) 
        P |= FLAG_OVERFLOW;
        else 
        P &= ~FLAG_OVERFLOW;
    }

    private boolean getCarryFlag() 
    {
    return (P & FLAG_CARRY) != 0; 
    }
    private boolean getZeroFlag() 
    { 
    return (P & FLAG_ZERO) != 0; 
    }
    private boolean getInterruptFlag() 
    { 
    return (P & FLAG_INTERRUPT) != 0; 
    }
    private boolean getDecimalFlag() 
    { 
    return (P & FLAG_DECIMAL) != 0; 
    }
    private boolean getOverflowFlag() 
    { 
    return (P & FLAG_OVERFLOW) != 0; 
    }
    private boolean getNegativeFlag() 
    { 
    return (P & FLAG_NEGATIVE) != 0; 
    }

    private void push(byte value) 
    {
        memory.write(0x100 + SP, value);
        SP = (SP - 1) & 0xFF;
    }
    
    private int pop() 
    {
        SP = (SP + 1) & 0xFF;
        return memory.read(0x100 + SP) & 0xFF;
    }

    private void ADC_IMM() 
    {
        int value = immediate();
        ADC(value);
        cycles += 2;
    }
    
    private void ADC_ZP() 
    {
        int address = zeroPage();
        int value = memory.read(address) & 0xFF;
        ADC(value);
        cycles += 3;
    }
    
    private void ADC_ZPX() 
    {
        int address = zeroPageX();
        int value = memory.read(address) & 0xFF;
        ADC(value);
        cycles += 4;
    }
    
    private void ADC_ABS() 
    {
        int address = absolute();
        int value = memory.read(address) & 0xFF;
        ADC(value);
        cycles += 4;
    }
    
    private void ADC_ABSX() 
    {
        int baseAddress = absolute();
        int address = baseAddress + X;
        int value = memory.read(address) & 0xFF;
        ADC(value);
        cycles += 4;
        if ((baseAddress & 0xFF00) != ((baseAddress + X) & 0xFF00)) 
        cycles += 1;
    }
    
    private void ADC_ABSY() 
    {
        int baseAddress = absolute();
        int address = baseAddress + Y;
        int value = memory.read(address) & 0xFF;
        ADC(value);
        cycles += 4;
        if ((baseAddress & 0xFF00) != ((baseAddress + Y) & 0xFF00))
        cycles += 1;
    }
    
    private void ADC_INDX() 
    {
        int address = indexedIndirect();
        int value = memory.read(address) & 0xFF;
        ADC(value);
        cycles += 6;
    }
    
    private void ADC_INDY() 
    {
        int pointer = memory.read(PC) & 0xFF;
        int baseAddress = readZeroPageWord(pointer);
        PC = (PC + 1) & 0xFFFF;
        int address = (baseAddress + Y) & 0xFFFF;
        int value = memory.read(address) & 0xFF;
        ADC(value);
        cycles += 5;
        if ((baseAddress & 0xFF00) != (address & 0xFF00)) 
        cycles += 1;
    }
    
    private void ADC(int value) 
    {
        int carry = getCarryFlag() ? 1 : 0;
        int result = A + value + carry;
        setCarryFlag(result > 0xFF);
        setOverflowFlag(((A ^ result) & (value ^ result) & 0x80) != 0);
        A = result & 0xFF;
        setZeroFlag(A);
        setNegativeFlag(A);
    }
    
    private void AND_IMM() 
    {
        int value = immediate();
        A &= value;
        setZeroFlag(A);
        setNegativeFlag(A);
        cycles += 2;
    }
    
    private void AND_ZP() 
    {
        int address = zeroPage();
        A &= memory.read(address) & 0xFF;
        setZeroFlag(A);
        setNegativeFlag(A);
        cycles += 3;
    }
    
    private void AND_ZPX() 
    {
        int address = zeroPageX();
        A &= memory.read(address) & 0xFF;
        setZeroFlag(A);
        setNegativeFlag(A);
        cycles += 4;
    }
    
    private void AND_ABS() 
    {
        int address = absolute();
        A &= memory.read(address) & 0xFF;
        setZeroFlag(A);
        setNegativeFlag(A);
        cycles += 4;
    }
    
    private void AND_ABSX() 
    {
        int baseAddress = absolute();
        int address = baseAddress + X;
        A &= memory.read(address) & 0xFF;
        setZeroFlag(A);
        setNegativeFlag(A);
        cycles += 4;
        if ((baseAddress & 0xFF00) != ((baseAddress + X) & 0xFF00)) 
        cycles += 1;
    }
    
    private void AND_ABSY() 
    {
        int baseAddress = absolute();
        int address = baseAddress + Y;
        A &= memory.read(address) & 0xFF;
        setZeroFlag(A);
        setNegativeFlag(A);
        cycles += 4;
        if ((baseAddress & 0xFF00) != ((baseAddress + Y) & 0xFF00))
        cycles += 1;
    }
    
    private void AND_INDX() 
    {
        int address = indexedIndirect();
        A &= memory.read(address) & 0xFF;
        setZeroFlag(A);
        setNegativeFlag(A);
        cycles += 6;
    }

    private void AND_INDY() 
    {
        int pointer = memory.read(PC) & 0xFF;
        int baseAddress = readZeroPageWord(pointer);
        PC = (PC + 1) & 0xFFFF;
        int address = (baseAddress + Y) & 0xFFFF;
        A &= memory.read(address) & 0xFF;
        setZeroFlag(A);
        setNegativeFlag(A);
        cycles += 5;
        if ((baseAddress & 0xFF00) != (address & 0xFF00)) 
        cycles += 1;
    }

    private void ASL_ACC() 
    {
        setCarryFlag((A & 0x80) != 0);
        A = (A << 1) & 0xFF;
        setZeroFlag(A);
        setNegativeFlag(A);
        cycles += 2;
    }
    
    private void ASL_ZP() 
    {
        int address = zeroPage();
        int value = memory.read(address) & 0xFF;
        setCarryFlag((value & 0x80) != 0);
        value = (value << 1) & 0xFF;
        memory.write(address, (byte)value);
        setZeroFlag(value);
        setNegativeFlag(value);
        cycles += 5;
    }
    
    private void ASL_ZPX() 
    {
        int address = zeroPageX();
        int value = memory.read(address) & 0xFF;
        setCarryFlag((value & 0x80) != 0);
        value = (value << 1) & 0xFF;
        memory.write(address, (byte)value);
        setZeroFlag(value);
        setNegativeFlag(value);
        cycles += 6;
    }
    
    private void ASL_ABS() 
    {
        int address = absolute();
        int value = memory.read(address) & 0xFF;
        setCarryFlag((value & 0x80) != 0);
        value = (value << 1) & 0xFF;
        memory.write(address, (byte)value);
        setZeroFlag(value);
        setNegativeFlag(value);
        cycles += 6;
    }
    
    private void ASL_ABSX() 
    {
        int address = absoluteX();
        int value = memory.read(address) & 0xFF;
        setCarryFlag((value & 0x80) != 0);
        value = (value << 1) & 0xFF;
        memory.write(address, (byte)value);
        setZeroFlag(value);
        setNegativeFlag(value);
        cycles += 7;
    }

    private void BCC() 
    {
        int offset = immediate();
        if (!getCarryFlag()) {
            branch(offset);
            cycles += 1;
        }
        cycles += 2;
    }
    
    private void BCS() 
    {
        int offset = immediate();
        if (getCarryFlag()) 
        {
            branch(offset);
            cycles += 1;
        }
        cycles += 2;
    }
    
    private void BEQ() 
    {
        int offset = immediate();
        if (getZeroFlag()) 
        {
            branch(offset);
            cycles += 1;
        }
        cycles += 2;
    }
    
    private void BMI() 
    {
        int offset = immediate();
        if (getNegativeFlag()) 
        {
            branch(offset);
            cycles += 1;
        }
        cycles += 2;
    }
    
    private void BNE() 
    {
        int offset = immediate();
        if (!getZeroFlag()) 
        {
            branch(offset);
            cycles += 1;
        }
        cycles += 2;
    }

    private void BPL() 
    {
        int offset = immediate();
        if (!getNegativeFlag()) 
        {
            branch(offset);
            cycles += 1;
        }
        cycles += 2;
    }
    
    private void BVC() 
    {
        int offset = immediate();
        if (!getOverflowFlag())
        {
            branch(offset);
            cycles += 1;
        }
        cycles += 2;
    }
    
    private void BVS() 
    {
        int offset = immediate();
        if (getOverflowFlag()) 
        {
            branch(offset);
            cycles += 1;
        }
        cycles += 2;
    }
    
    private void branch(int offset) 
    {
        if ((offset & 0x80) != 0) 
        offset -= 256;
        PC = (PC + offset) & 0xFFFF;
    }
    
    private void BIT_ZP() 
    {
        int address = zeroPage();
        int value = memory.read(address) & 0xFF;
        BIT(value);
        cycles += 3;
    }
    
    private void BIT_ABS() 
    {
        int address = absolute();
        int value = memory.read(address) & 0xFF;
        BIT(value);
        cycles += 4;
    }
    
    private void BIT(int value) 
    {
        setZeroFlag((A & value) == 0);
        P = (P & ~0xC0) | (value & 0xC0);
    }
    
    private void CLC() 
    {
        setCarryFlag(false);
        cycles += 2;
    }
    
    private void CLD() 
    {
        P &= ~FLAG_DECIMAL;
        cycles += 2;
    }
    
    private void CLI() 
    {
        P &= ~FLAG_INTERRUPT;
        cycles += 2;
    }
    
    private void CLV() 
    {
        P &= ~FLAG_OVERFLOW;
        cycles += 2;
    }
    
    private void SEC() 
    {
        setCarryFlag(true);
        cycles += 2;
    }
    
    private void SED() 
    {
        P |= FLAG_DECIMAL;
        cycles += 2;
    }
    
    private void SEI() 
    {
        P |= FLAG_INTERRUPT;
        cycles += 2;
    }
    
    private void BRK() 
    {
        push((byte)((PC >> 8) & 0xFF));
        push((byte)(PC & 0xFF));
        push((byte)(P | FLAG_BREAK));
        P |= FLAG_INTERRUPT;
        PC = memory.readWord(0xFFFE) & 0xFFFF;
        cycles += 7;
    }
    
    private void CMP_IMM() 
    {
        int value = immediate();
        CMP(value);
        cycles += 2;
    }
    
    private void CMP_ZP() 
    {
        int address = zeroPage();
        int value = memory.read(address) & 0xFF;
        CMP(value);
        cycles += 3;
    }
    
    private void CMP_ZPX() 
    {
        int address = zeroPageX();
        int value = memory.read(address) & 0xFF;
        CMP(value);
        cycles += 4;
    }
    
    private void CMP_ABS() 
    {
        int address = absolute();
        int value = memory.read(address) & 0xFF;
        CMP(value);
        cycles += 4;
    }
    
    private void CMP_ABSX() 
    {
        int baseAddress = absolute();
        int address = baseAddress + X;
        int value = memory.read(address) & 0xFF;
        CMP(value);
        cycles += 4;
        if ((baseAddress & 0xFF00) != ((baseAddress + X) & 0xFF00))
        cycles += 1;
    }
    
    private void CMP_ABSY() 
    {
        int baseAddress = absolute();
        int address = baseAddress + Y;
        int value = memory.read(address) & 0xFF;
        CMP(value);
        cycles += 4;
        if ((baseAddress & 0xFF00) != ((baseAddress + Y) & 0xFF00))
        cycles += 1;
    }

    private void CMP_INDX() 
    {
        int address = indexedIndirect();
        int value = memory.read(address) & 0xFF;
        CMP(value);
        cycles += 6;
    }
    
    private void CMP_INDY() 
    {
        int pointer = memory.read(PC) & 0xFF;
        int baseAddress = readZeroPageWord(pointer);
        PC = (PC + 1) & 0xFFFF;
        int address = (baseAddress + Y) & 0xFFFF;
        int value = memory.read(address) & 0xFF;
        CMP(value);
        cycles += 5;
        if ((baseAddress & 0xFF00) != (address & 0xFF00))
        cycles += 1;
    }
    
    private void CMP(int value)
    {
        int result = A - value;
        setCarryFlag(A >= value);
        setZeroFlag(result == 0);
        setNegativeFlag(result);
    }
    
    private void CPX_IMM() 
    {
        int value = immediate();
        CPX(value);
        cycles += 2;
    }
    
    private void CPX_ZP() 
    {
        int address = zeroPage();
        int value = memory.read(address) & 0xFF;
        CPX(value);
        cycles += 3;
    }
    
    private void CPX_ABS() 
    {
        int address = absolute();
        int value = memory.read(address) & 0xFF;
        CPX(value);
        cycles += 4;
    }
    
    private void CPX(int value) 
    {
        int result = X - value;
        setCarryFlag(X >= value);
        setZeroFlag(result == 0);
        setNegativeFlag(result);
    }
    
    private void CPY_IMM() 
    {
        int value = immediate();
        CPY(value);
        cycles += 2;
    }
    
    private void CPY_ZP() 
    {
        int address = zeroPage();
        int value = memory.read(address) & 0xFF;
        CPY(value);
        cycles += 3;
    }
    
    private void CPY_ABS() 
    {
        int address = absolute();
        int value = memory.read(address) & 0xFF;
        CPY(value);
        cycles += 4;
    }
    
    private void CPY(int value) 
    {
        int result = Y - value;
        setCarryFlag(Y >= value);
        setZeroFlag(result == 0);
        setNegativeFlag(result);
    }

}