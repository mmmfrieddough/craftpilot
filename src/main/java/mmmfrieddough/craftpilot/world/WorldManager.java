package mmmfrieddough.craftpilot.world;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WorldManager implements IWorldManager {
    private final Map<BlockPos, BlockState> ghostBlocks = new HashMap<>();

    @Override
    public void setBlockState(BlockPos pos, BlockState blockState) {
        ghostBlocks.put(pos, blockState);
    }

    @Override
    public BlockState getGhostBlockState(BlockPos pos) {
        return ghostBlocks.get(pos);
    }

    @Override
    public BlockState getBlockState(World world, BlockPos pos) {
        BlockState ghostState = ghostBlocks.get(pos);
        if (ghostState != null) {
            return ghostState;
        }
        return world.getBlockState(pos);
    }

    @Override
    public void clearBlockState(BlockPos pos) {
        ghostBlocks.remove(pos);
    }

    @Override
    public void clearBlockStates() {
        ghostBlocks.clear();
    }

    @Override
    public Map<BlockPos, BlockState> getGhostBlocks() {
        return Collections.unmodifiableMap(ghostBlocks);
    }
}
