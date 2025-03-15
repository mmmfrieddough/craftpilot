package mmmfrieddough.craftpilot.world;

import java.util.Map;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IWorldManager {
    public void setBlockState(int alternativeNum, int previousAlternativeNum, BlockPos pos, BlockState blockState);

    public BlockState getGhostBlockState(BlockPos pos);

    public BlockState getBlockState(World world, BlockPos pos);

    public void clearBlockState(BlockPos pos);

    public void clearBlockStates();

    public Map<BlockPos, BlockState> getGhostBlocks();

    public int getTotalAlternativeNum();

    public void setSelectedAlternativeNum(int num);

    public int getSelectedAlternativeNum();

    public void pruneAlternatives();

    public boolean hasGhostBlocks();
}
