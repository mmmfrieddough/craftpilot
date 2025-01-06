package mmmfrieddough.craftpilot.http;

public class ResponseItem {
    private String block_state;
    private int x;
    private int y;
    private int z;

    public ResponseItem(String blockState, int x, int y, int z) {
        this.block_state = blockState;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getBlockState() {
        return block_state;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }
}
