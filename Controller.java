public class Controller 
{
    private boolean[] buttons = new boolean[8];
    private int shiftRegister = 0;
    private boolean strobe = false;

    public static final int BUTTON_A = 0;
    public static final int BUTTON_B = 1;
    public static final int BUTTON_SELECT = 2;
    public static final int BUTTON_START = 3;
    public static final int BUTTON_UP = 4;
    public static final int BUTTON_DOWN = 5;
    public static final int BUTTON_LEFT = 6;
    public static final int BUTTON_RIGHT = 7;

    public void setButtonState(int button, boolean pressed) 
    {
        if (button >= 0 && button < 8) 
        {
            buttons[button] = pressed;
        }
    }
    
    public void clear() 
    {
        for (int i = 0; i < 8; i++) 
        {
            buttons[i] = false;
        }
    }

    public void write(int value) 
    {
        strobe = (value & 1) == 1;
        if (strobe) 
        {
            shiftRegister = 0; 
        }
    }

    public byte read() 
    {
        if (shiftRegister >= 8) 
        {
            return 1; 
        }
        
        byte state = (byte) (buttons[shiftRegister] ? 1 : 0);

        if (!strobe) 
        {
            shiftRegister++;
        }
        return state;
    }
}