package gay.pancake.ambientled.host.util;

public class Color {

    public int value = 0x00000000;

    public int getRed() { return (value >> 16) & 0xFF; }
    public int getGreen() {
        return (value >> 8) & 0xFF;
    }
    public int getBlue() {
        return value & 0xFF;
    }

    public Color setRGB(int r, int g, int b) {
        this.value = (0xFF << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8)  |
                (b & 0xFF);
        return this;
    }

}
