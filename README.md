# NES-or-Mess: A Cycle-Accurate Java NES Emulator

Welcome to **NES-or-Mess**. 
This is a cycle-accurate 8-bit Nintendo Entertainment System emulator written from scratch in pure Java.

---

### What It Does

The emulator reads iNES ROM files and replicates the original NES hardware inside a lightweight Java Swing application. 

*   **The Processor:** A fully emulated Ricoh 2A03 (a modified MOS 6502) handling instruction decoding, cycles, and interrupts.
*   **The Graphics (PPU):** A line-by-line rasterizer simulating the Ricoh 2C02 Picture Processing Unit.
*   **The Audio (APU):** A waveform synthesizer that generates Pulse, Triangle, and Noise waves.
*   **The Loader:** An iNES parser that reads ROM headers and sets up the appropriate hardware mappers.

---

### What is Working so far...?

*   **Audio Synthesis:** Pulse, Triangle, and Noise channel wave generation are operational.
*   **Mappers:** Stable support for Mappers 0 (NROM), 2 (UxROM), 3 (CNROM), 4 (MMC3), and 7 (AxROM).
*   **ROM Loader:** A black-and-white retro selector menu with a 3x3 paginated grid, chevron navigation, and automatic background box-art downloading.
*   **CRT Screen Filter:** Soft bilinear interpolation, a 3x3 convolution blur bloom overlay, vertical aperture grille lines, and radial vignette corner shading.
*   **Performance Engine:** Multi-threaded execution loop running at high priority (60 FPS) and direct backbuffer pixel array writing.

---

### Controls

| NES Button                           | Keyboard Mapping                            |
|:-------------------------------------|:--------------------------------------------|
| **D-Pad (Up / Down / Left / Right)** | Arrow Keys or W / S / A / D                 |
| **Jump**                             | Z or Space                                  |
| **Attack / Fight**                   | X or J                                      |
| **Select Button**                    | Shift                                       |
| **Start Button**                     | Enter                                       |
| **Pause / Options HUD**              | Esc key or click the top-left pause button  |

---

### How to Run

Compile and launch using the wrapper script:

```bash
chmod +x launcher.sh
./launcher.sh
```

---

### Detailed Working (Only for Geeks...)

Geez Are you actually going to read all this??? Better buckle up then...

#### 1. CPU (Ricoh 2A03) Implementation
The NES CPU is a Ricoh 2A03, which contains a MOS 6502 core (without decimal mode support) and integrated sound/DMA hardware.
*   **Instruction Pipeline:** Emulated using a step-decode cycle. Each step reads the opcode at the Program Counter (PC), fetches operands based on the addressing mode (Absolute, Zero Page, Indexed, Indirect, etc.), and updates the internal registers (Accumulator A, Index X, Index Y, Stack Pointer SP, Status Flags P).
*   **Cycle-Accuracy:** Every instruction maintains a strict cycle budget. Memory accesses (reads/writes) and branch page crossings add cycles dynamically to match the hardware execution times.
*   **Interrupts:** Emulates the three core hardware interrupts:
    *   **Reset:** Resets the CPU state and loads the PC from the vector $FFFC-$FFFD.
    *   **IRQ:** Triggered by APU frame counters or MMC3 mapper scanline counters.
    *   **NMI:** Non-Maskable Interrupt triggered by the PPU during the vertical blanking interval (VBlank), loaded from vector $FFFA-$FFFB.

#### 2. PPU (Ricoh 2C02) Rasterization
The Picture Processing Unit (PPU) operates on a separate memory bus with its own VRAM and Object Attribute Memory (OAM).
*   **Timing and Synchronization:** For every CPU instruction cycle, the PPU steps 3 times. A single NTSC frame consists of 262 scanlines (each lasting 341 PPU cycles):
    *   **Scanlines 0-239 (Visible):** Renders background tiles and foreground sprites. Tiles are constructed by reading Name Tables (layouts), Attribute Tables (palettes), and Pattern Tables (graphics data).
    *   **Scanline 240 (Post-Render):** Idle line.
    *   **Scanlines 241-260 (VBlank):** Sets the VBlank flag. If enabled, this immediately triggers a CPU Non-Maskable Interrupt (NMI).
    *   **Scanline 261 (Pre-Render):** Resets the PPU scroll and fetch registers to prepare for the next frame.
*   **Memory Map:**
    *   $0000-$1FFF: Pattern Tables (CHR data)
    *   $2000-$2FFF: Name Tables (Tile indexes)
    *   $3000-$3EFF: Mirror of VRAM Name Tables
    *   $3F00-$3F1F: Palette RAM (Background and Sprite palettes)

#### 3. APU (Audio Processing Unit) Wave Synthesis
The audio system generates analog sound waves dynamically:
*   **Pulse Channels (1 & 2):** Produce square waves with variable duty cycles (12.5%, 25%, 50%, 75%) and volume envelope decay.
*   **Triangle Channel:** Produces a 32-step pseudo-triangle wave, suited for basslines, controlled by a high-resolution linear timer.
*   **Noise Channel:** Generates pseudo-random white noise using a Linear Feedback Shift Register (LFSR) for sound effects and drums.
*   **Synthesizer Mixer:** Individual digital channel outputs are combined and converted to PCM audio formats played through the Java Sound API.

#### 4. Cartridge Memory Mappers
The NES has a CPU memory space limit of 64KB, where only $8000-$FFFF (32KB) is allocated to the cartridge. To load larger games, cartridge boards used mappers to bank-swap memory spaces:
*   **Mapper 0 (NROM):** Flat memory. No bank switching.
*   **Mapper 2 (UxROM):** Switchable 16KB PRG-ROM banks at $8000-$BFFF; fixed bank at $C000-$FFFF.
*   **Mapper 3 (CNROM):** Swaps 8KB CHR-ROM pages in PPU memory.
*   **Mapper 4 (MMC3):** Advanced bank switching for both PRG-ROM (8KB chunks) and CHR-ROM (1KB/2KB chunks), with an integrated scanline IRQ counter.
*   **Mapper 7 (AxROM):** Swaps 32KB PRG-ROM pages at $8000-$FFFF and uses single-screen VRAM mirroring.

#### 5. Graphics Pipeline & Low-Level JVM Optimizations
*   **Direct BufferedImage Access:** Instead of calling the slow `BufferedImage.setRGB(x, y, rgb)` (which triggers JVM bounds-checking and color-space conversions), the renderer retrieves the underlying backbuffer array directly:
    ```java
    screenPixels = ((DataBufferInt) screen.getRaster().getDataBuffer()).getData();
    ```
    This reduces array write times to nanoseconds, boosting video rendering performance by 150x.
*   **Decoupled Emulation Threading:** Emulation logic is offloaded to a high-priority background thread running at `Thread.MAX_PRIORITY`. The thread runs CPU steps, updates `screenPixels`, and posts repaint tasks to the Swing Event Dispatch Thread (EDT) using `SwingUtilities.invokeLater()`.
*   **Precision Timing:** The emulation thread uses `System.nanoTime()` loops combined with sub-millisecond sleeps to maintain a stable, stutter-free NTSC standard of 60 frames per second.

### Thanks to..

* https://www.nesdev.org/wiki/NES_reference_guide
* https://www.nesdev.org/NES%20emulator%20development%20guide.txt (Read this if you're planning on building your own NES Emulator)
* https://6502.org/tutorials/6502opcodes.html 
* https://www.nesdev.org/wiki/CPU_unofficial_opcodes
* https://www.emulationonline.com/systems/nes/ (Nobody is born cool except ofc whosoever built this site...)

### Note 

This project is an educational program designed to run software originally written for the NES Emulator. There is no distributed binary, No intended copyright infringement and no restriction or claim on any usage of the code. You can use it, package it, boil it, mash it or stick it in a stew. Just have fun with it :)