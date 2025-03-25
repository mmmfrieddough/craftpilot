package mmmfrieddough.craftpilot.model;

public class ResponseItem {
    private final String type;
    private final int alternative_num;
    private final int previous_alternative_num;
    private final String block_state;
    private final int x;
    private final int y;
    private final int z;

    public ResponseItem(String type, int alternativeNum, int previousAlternativeNum, String blockState, int x, int y,
            int z) {
        this.type = type;
        this.alternative_num = alternativeNum;
        this.previous_alternative_num = previousAlternativeNum;
        this.block_state = blockState;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getType() {
        return type;
    }

    public int getAlternativeNum() {
        return alternative_num;
    }

    public int getPreviousAlternativeNum() {
        return previous_alternative_num;
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
