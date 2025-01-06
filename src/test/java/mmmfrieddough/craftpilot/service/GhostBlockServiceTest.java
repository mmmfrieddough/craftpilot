package mmmfrieddough.craftpilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GhostBlockServiceTest {

    @Mock
    private MinecraftClient client;
    @Mock
    private ClientPlayerEntity player;
    @Mock
    private ClientPlayerInteractionManager interactionManager;
    @Mock
    private PlayerInventory inventory;
    @Mock
    private ItemStack mockStack;
    @Mock
    private BlockState mockBlockState;

    @BeforeAll
    static void init() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @BeforeEach
    void setUp() {
        client.player = player;
        client.interactionManager = interactionManager;
        doReturn(inventory).when(player).getInventory();
    }

    @Test
    void findTargetedGhostBlock_EmptyMap_ReturnsNull() {
        Map<BlockPos, BlockState> ghostBlocks = new HashMap<>();
        Vec3d cameraPos = new Vec3d(0, 0, 0);
        Vec3d lookVec = new Vec3d(1, 0, 0);

        BlockPos result = GhostBlockService.findTargetedGhostBlock(ghostBlocks, cameraPos, lookVec, 5.0);

        assertNull(result);
    }

    @Test
    void findTargetedGhostBlock_LookingDirectlyAtBlock_ReturnsBlock() {
        Map<BlockPos, BlockState> ghostBlocks = new HashMap<>();
        BlockPos pos = new BlockPos(2, 1, 0); // Block at (2,1,0)
        BlockState concreteBlockState = Blocks.STONE.getDefaultState();
        ghostBlocks.put(pos, concreteBlockState);

        Vec3d cameraPos = new Vec3d(0, 1, 0); // Camera at (0,1,0)
        Vec3d lookVec = new Vec3d(1, 0, 0); // Looking straight along X axis

        BlockPos result = GhostBlockService.findTargetedGhostBlock(ghostBlocks,
                cameraPos, lookVec, 5.0);

        assertEquals(pos, result);
    }

    @Test
    void findTargetedGhostBlock_LookingAwayFromBlock_ReturnsNull() {
        Map<BlockPos, BlockState> ghostBlocks = new HashMap<>();
        BlockPos pos = new BlockPos(2, 1, 0); // Block at (2,1,0)
        ghostBlocks.put(pos, mockBlockState);

        Vec3d cameraPos = new Vec3d(0, 1, 0); // Camera at (0,1,0)
        Vec3d lookVec = new Vec3d(-1, 0, 0); // Looking in opposite direction (negative X)

        BlockPos result = GhostBlockService.findTargetedGhostBlock(ghostBlocks,
                cameraPos, lookVec, 5.0);

        assertNull(result);
    }

    @Test
    void findTargetedGhostBlock_BlockTooFar_ReturnsNull() {
        Map<BlockPos, BlockState> ghostBlocks = new HashMap<>();
        BlockPos pos = new BlockPos(10, 1, 0); // Block at (10,1,0)
        ghostBlocks.put(pos, mockBlockState);

        Vec3d cameraPos = new Vec3d(0, 1, 0); // Camera at (0,1,0)
        Vec3d lookVec = new Vec3d(1, 0, 0); // Looking towards block

        BlockPos result = GhostBlockService.findTargetedGhostBlock(ghostBlocks,
                cameraPos, lookVec, 5.0); // Max
        // distance 5

        assertNull(result);
    }

    @Test
    void findTargetedGhostBlock_MultipleBlocks_ReturnsClosest() {
        Map<BlockPos, BlockState> ghostBlocks = new HashMap<>();
        BlockPos farPos = new BlockPos(4, 1, 0); // Block at (4,1,0)
        BlockPos closePos = new BlockPos(2, 1, 0); // Block at (2,1,0)
        ghostBlocks.put(farPos, mockBlockState);
        ghostBlocks.put(closePos, mockBlockState);

        Vec3d cameraPos = new Vec3d(0, 1, 0); // Camera at (0,1,0)
        Vec3d lookVec = new Vec3d(1, 0, 0); // Looking along X axis

        BlockPos result = GhostBlockService.findTargetedGhostBlock(ghostBlocks,
                cameraPos, lookVec, 5.0);

        assertEquals(closePos, result);
    }

    @Test
    void handleInventoryPick_CreativeMode_ReturnsHotbarSlot() {
        inventory.selectedSlot = 0;

        int result = GhostBlockService.handleInventoryPick(inventory, mockStack,
                true, mockStack);

        assertEquals(36, result);
        verify(inventory).addPickBlock(mockStack);
    }

    @Test
    void handleInventoryPick_SurvivalModeWithItemInHotbar_ReturnsHotbarSlot() {
        when(inventory.getSlotWithStack(mockStack)).thenReturn(3);

        int result = GhostBlockService.handleInventoryPick(inventory, mockStack,
                false, mockStack);

        assertEquals(3, result);
    }

    @Test
    void handleInventoryPick_SurvivalModeNoItem_ReturnsMinusOne() {
        when(inventory.getSlotWithStack(mockStack)).thenReturn(-1);

        int result = GhostBlockService.handleInventoryPick(inventory, mockStack,
                false, mockStack);

        assertEquals(-1, result);
    }

    @Test
    void executeInventoryPick_CreativeMode_ClicksCreativeStack() {
        when(player.getStackInHand(Hand.MAIN_HAND)).thenReturn(mockStack);

        GhostBlockService.executeInventoryPick(client, 1, true);

        verify(interactionManager).clickCreativeStack(mockStack, 1);
    }

    @Test
    void executeInventoryPick_SurvivalModeHotbarSlot_UpdatesSelectedSlot() {
        GhostBlockService.executeInventoryPick(client, 38, false);

        assertEquals(2, inventory.selectedSlot);
    }

    @Test
    void executeInventoryPick_SurvivalModeInventorySlot_PicksFromInventory() {
        GhostBlockService.executeInventoryPick(client, 15, false);

        verify(interactionManager).pickFromInventory(15);
    }

    @Test
    void executeInventoryPick_InvalidSlot_NoAction() {
        GhostBlockService.executeInventoryPick(client, -1, false);

        // Verify inventory.selectedSlot wasn't changed
        verify(interactionManager, never()).pickFromInventory(anyInt());
        assertEquals(0, inventory.selectedSlot);
    }
}