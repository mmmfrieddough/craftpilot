package mmmfrieddough.craftpilot.util;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockMatrixBuilder {
    private static final int MATRIX_SIZE = 11;

    private static final Function<Map.Entry<Property<?>, Comparable<?>>, String> PROPERTY_MAP_PRINTER = new Function<Map.Entry<Property<?>, Comparable<?>>, String>() {
        @Override
        public String apply(Map.Entry<Property<?>, Comparable<?>> entry) {
            Property<?> property = entry.getKey();
            Comparable<?> value = entry.getValue();
            return property.getName() + "=" + value.toString();
        }
    };

    public String[][][] getBlocksMatrix(World world, BlockPos centerPos) {
        int offset = MATRIX_SIZE / 2;
        String[][][] matrix = new String[MATRIX_SIZE][MATRIX_SIZE][MATRIX_SIZE];

        for (int x = 0; x < MATRIX_SIZE; x++) {
            for (int y = 0; y < MATRIX_SIZE; y++) {
                for (int z = 0; z < MATRIX_SIZE; z++) {
                    BlockPos pos = centerPos.add(x - offset, y - offset, z - offset);
                    BlockState state = world.getBlockState(pos);
                    matrix[z][y][x] = getBlockStateString(state);
                }
            }
        }

        return matrix;
    }

    private String getBlockStateString(BlockState state) {
        StringBuilder sb = new StringBuilder();
        sb.append(Registries.BLOCK.getId(state.getBlock()));

        if (!state.getProperties().isEmpty()) {
            sb.append("[");
            sb.append((String) state.getEntries().entrySet().stream().map(PROPERTY_MAP_PRINTER)
                    .collect(Collectors.joining(",")));
            sb.append("]");
        }

        return sb.toString();
    }
}