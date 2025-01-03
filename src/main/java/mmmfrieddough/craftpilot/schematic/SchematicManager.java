package mmmfrieddough.craftpilot.schematic;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.http.ResponseItem;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.util.Map;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic.SchematicSaveInfo;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;

public class SchematicManager {
    private LitematicaSchematic currentSchematic;
    private SchematicPlacement currentPlacement;
    private BlockPos originPos;
    private BlockPos pos1;
    private LitematicaBlockStateContainer container;
    private SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();

    public boolean shouldProcessBlock(World world, BlockPos pos) {
        // Add logic to determine if we should process this block
        // For example, check if it's a valid block type, if we're not already
        // processing, etc.
        return true;
    }

    public void createSchematic(World world, BlockPos pos) {
        this.originPos = pos;

        // Create a selection
        AreaSelection selection = new AreaSelection();
        pos1 = originPos.add(-5, -5, -5);
        BlockPos pos2 = originPos.add(5, 5, 5);
        Box box = new Box();
        box.setPos1(pos1);
        box.setPos2(pos2);
        selection.addSubRegionBox(box, true);

        // Create a schematic from the selection
        SchematicSaveInfo info = new LitematicaSchematic.SchematicSaveInfo(false, true);
        LitematicaSchematic schematic = LitematicaSchematic.createFromWorld(world, selection, info, "CraftPilot",
                str -> {
                });
        this.currentSchematic = schematic;

        try {
            Field containerField = LitematicaSchematic.class.getDeclaredField("blockContainers");
            containerField.setAccessible(true);
            Object containerObj = containerField.get(schematic);

            if (containerObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, LitematicaBlockStateContainer> containers = (Map<String, LitematicaBlockStateContainer>) containerObj;
                if (!containers.isEmpty()) {
                    container = containers.values().iterator().next();
                    SchematicHolder.getInstance().addSchematic(schematic, true);
                } else {
                    CraftPilot.LOGGER.error("No block containers found in schematic");
                }
            } else {
                CraftPilot.LOGGER.error("Invalid block container type: {}", containerObj.getClass());
            }
        } catch (ReflectiveOperationException e) {
            CraftPilot.LOGGER.error("Failed to access schematic block containers", e);
        }
    }

    public void processResponse(ResponseItem response) {
        if (currentSchematic == null || originPos == null) {
            CraftPilot.LOGGER.error("No schematic or origin position set");
            return;
        }

        try {
            BlockState blockState = BlockStateHelper.parseBlockState(response.getBlockState());
            BlockPos relativePos = new BlockPos(
                    response.getX(),
                    response.getY(),
                    response.getZ());

            // Update the schematic with the new block
            updateSchematic(relativePos, blockState);
        } catch (Exception e) {
            CraftPilot.LOGGER.error("Error processing response", e);
        }
    }

    private void updateSchematic(BlockPos position, BlockState state) {
        // Update the block state in the schematic container
        container.set(position.getX(), position.getY(), position.getZ(), state);

        // Remove the old placement
        if (manager.getSelectedSchematicPlacement() != null) {
            manager.removeSchematicPlacement(manager.getSelectedSchematicPlacement());
        }

        // Create a new placement
        currentPlacement = SchematicPlacement.createFor(currentSchematic, pos1, "test", true, true);
        manager.addSchematicPlacement(currentPlacement, true);
        manager.setSelectedSchematicPlacement(currentPlacement);
    }

    public void clear() {
        currentSchematic = null;
        currentPlacement = null;
        originPos = null;
    }
}