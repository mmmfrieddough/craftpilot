package mmmfrieddough.craftpilot.world;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WorldManager implements IWorldManager {
    private final Map<Integer, Map<BlockPos, BlockState>> ghostBlocks = new HashMap<>();
    private volatile int selectedAlternativeNum = 0;

    @Override
    public void setBlockState(int alternativeNum, int previousAlternativeNum, BlockPos pos, BlockState blockState) {
        // Create a new alternative if it doesn't exist
        if (!ghostBlocks.containsKey(alternativeNum)) {
            // Copy from the previous alternative
            Map<BlockPos, BlockState> previousAlternative = ghostBlocks.getOrDefault(previousAlternativeNum,
                    new HashMap<>());
            ghostBlocks.put(alternativeNum, new HashMap<>(previousAlternative));
        }
        // We don't want to store air blocks
        if (blockState.isAir()) {
            return;
        }
        ghostBlocks.get(alternativeNum).put(pos, blockState);
    }

    @Override
    public BlockState getGhostBlockState(BlockPos pos) {
        if (!ghostBlocks.containsKey(selectedAlternativeNum)) {
            return null;
        }
        return ghostBlocks.get(selectedAlternativeNum).get(pos);
    }

    @Override
    public BlockState getBlockState(World world, BlockPos pos) {
        BlockState ghostState = getGhostBlockState(pos);
        if (ghostState != null) {
            return ghostState;
        }
        return world.getBlockState(pos);
    }

    @Override
    public void clearBlockState(BlockPos pos) {
        if (!ghostBlocks.containsKey(selectedAlternativeNum)) {
            return;
        }
        ghostBlocks.get(selectedAlternativeNum).remove(pos);
    }

    @Override
    public void clearBlockStates() {
        ghostBlocks.clear();
        selectedAlternativeNum = 0;
    }

    @Override
    public Map<BlockPos, BlockState> getGhostBlocks() {
        if (!ghostBlocks.containsKey(selectedAlternativeNum)) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(ghostBlocks.get(selectedAlternativeNum));
    }

    @Override
    public void advanceSelectedAlternativeNum(int delta) {
        System.out.println("Selected alternative num: " + selectedAlternativeNum);
        System.out.println("Delta: " + delta);
        System.out.println("Ghost blocks size: " + ghostBlocks.size());
        selectedAlternativeNum = ((selectedAlternativeNum + delta) % ghostBlocks.size() + ghostBlocks.size())
                % ghostBlocks.size();
        System.out.println("New selected alternative num: " + selectedAlternativeNum);
    }

    @Override
    public int getTotalAlternativeNum() {
        return ghostBlocks.size();
    }

    @Override
    public int getSelectedAlternativeNum() {
        return selectedAlternativeNum;
    }

    @Override
    public void pruneAlternatives() {
        // Don't prune if there's 1 or fewer alternatives
        if (ghostBlocks.size() <= 1) {
            return;
        }
        // Move the selected alternative to the first alternative
        if (selectedAlternativeNum != 0) {
            ghostBlocks.put(0, ghostBlocks.get(selectedAlternativeNum));
            selectedAlternativeNum = 0;
        }
        // Clear all alternatives except the first one
        ghostBlocks.keySet().removeIf(key -> key != 0);
    }

    @Override
    public boolean hasGhostBlocks() {
        for (Map<BlockPos, BlockState> alternative : ghostBlocks.values()) {
            if (!alternative.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
