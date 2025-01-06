package mmmfrieddough.craftpilot.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;

class BlockMatrixUtilsTest {

    @BeforeAll
    static void init() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @Test
    void getBlockStateString_SimpleBlock_ReturnsIdOnly() {
        BlockState state = Blocks.STONE.getDefaultState();

        String result = BlockMatrixUtils.getBlockStateString(state);

        assertEquals("minecraft:stone", result);
    }

    @Test
    void getBlockStateString_BlockWithProperties_ReturnsIdAndProperties() {
        BlockState state = Blocks.CHEST.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, net.minecraft.util.math.Direction.NORTH);

        String result = BlockMatrixUtils.getBlockStateString(state);

        assertEquals("minecraft:chest[facing=north,type=SINGLE,waterlogged=false]", result);
    }

    @Test
    void getBlockStateString_BlockWithMultipleProperties_ReturnsAllProperties() {
        BlockState state = Blocks.REDSTONE_WIRE.getDefaultState()
                .with(Properties.POWER, 15)
                .with(net.minecraft.block.RedstoneWireBlock.WIRE_CONNECTION_NORTH,
                        net.minecraft.block.enums.WireConnection.UP)
                .with(net.minecraft.block.RedstoneWireBlock.WIRE_CONNECTION_SOUTH,
                        net.minecraft.block.enums.WireConnection.SIDE);

        String result = BlockMatrixUtils.getBlockStateString(state);

        assertEquals("minecraft:redstone_wire[east=none,north=up,power=15,south=side,west=none]", result);
    }
}