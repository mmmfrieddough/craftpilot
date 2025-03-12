package mmmfrieddough.craftpilot.model;

public class ResponseItem {
    private int alternative_num;
    private String block_state;
    private int x;
    private int y;
    private int z;

    public ResponseItem(int alternativeNum, String blockState, int x, int y, int z) {
        this.alternative_num = alternativeNum;
        this.block_state = blockState;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getAlternativeNum() {
        return alternative_num;
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
