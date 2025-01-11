package mmmfrieddough.craftpilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GhostBlockServiceTest {
    @Mock
    private MinecraftClient client;
    @Mock
    private ClientPlayerEntity player;
    @Mock
    private PlayerInventory inventory;
    @Mock
    private World world;
    @Mock
    private BlockState state;
    @Mock
    private Block block;
    @Mock
    private Item item;
    @Mock
    private ItemStack stack = Blocks.DIRT.asItem().getDefaultStack();
    @Mock
    private Camera camera;
    @Mock
    private IWorldManager worldManager;
    @Mock
    private HitResult vanillaTarget;
    @Mock
    private ClientPlayerEntity focusedEntity;
    @Mock
    private ClientPlayNetworkHandler networkHandler;

    private Map<BlockPos, BlockState> ghostBlocks;

    @BeforeAll
    static void init() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @BeforeEach
    void setUp() {
        // Basic minecraft setup
        client.player = player;
        when(player.getInventory()).thenReturn(inventory);
        when(player.getWorld()).thenReturn(world);
        when(world.getEnabledFeatures()).thenReturn(null);
        when(vanillaTarget.getType()).thenReturn(HitResult.Type.MISS);

        // Block/item chain setup
        when(state.getBlock()).thenReturn(block);
        when(block.asItem()).thenReturn(item);
        when(item.getDefaultStack()).thenReturn(stack);
        when(stack.isEmpty()).thenReturn(false);
        when(stack.isItemEnabled(any())).thenReturn(true);
        when(state.getOutlineShape(any(), any())).thenReturn(VoxelShapes.fullCube());

        // Camera setup - looking straight ahead along positive X axis by default
        when(camera.getPos()).thenReturn(new Vec3d(0, 0, 0));
        when(camera.getFocusedEntity()).thenReturn(focusedEntity);
        when(focusedEntity.getRotationVec(1.0f)).thenReturn(new Vec3d(1, 0, 0));

        // World manager setup
        ghostBlocks = new HashMap<>();
        when(worldManager.getGhostBlocks()).thenReturn(ghostBlocks);
    }

    @Nested
    class BasicPickingTests {
        @Test
        void givenNoGhostBlocks_whenPicking_thenNoAction() {
            boolean result = GhostBlockService.handleGhostBlockPick(worldManager, camera, 5.0, null, true, inventory,
                    vanillaTarget, networkHandler);

            assertFalse(result);
            verify(inventory, never()).swapStackWithHotbar(any());
            verify(inventory, never()).swapSlotWithHotbar(anyInt());
        }

        @Test
        void givenGhostBlockInRange_whenLookingAtIt_thenAddsToInventory() {
            ghostBlocks.put(new BlockPos(1, 0, 0), state);
            when(inventory.getSlotWithStack(stack)).thenReturn(-1);

            boolean result = GhostBlockService.handleGhostBlockPick(worldManager, camera, 5.0, null, true, inventory,
                    vanillaTarget, networkHandler);

            assertTrue(result);
            verify(inventory).swapStackWithHotbar(stack);
            verify(networkHandler).sendPacket(any(CreativeInventoryActionC2SPacket.class));
        }

        @Test
        void givenGhostBlock_whenLookingAway_thenNoAction() {
            ghostBlocks.put(new BlockPos(1, 0, 0), state);

            // Looking straight up instead of ahead
            when(focusedEntity.getRotationVec(1.0f)).thenReturn(new Vec3d(0, 1, 0));

            boolean result = GhostBlockService.handleGhostBlockPick(worldManager, camera, 5.0, null, true, inventory,
                    vanillaTarget, networkHandler);

            assertFalse(result);
            verify(inventory, never()).swapStackWithHotbar(any());
        }

        @Test
        void givenDistantGhostBlock_whenPicking_thenNoAction() {
            ghostBlocks.put(new BlockPos(10, 0, 0), state);

            boolean result = GhostBlockService.handleGhostBlockPick(worldManager, camera, 5.0, null, true, inventory,
                    vanillaTarget, networkHandler);

            assertFalse(result);
            verify(inventory, never()).swapStackWithHotbar(any());
            verify(inventory, never()).swapSlotWithHotbar(anyInt());
        }

        @Test
        void givenMultipleBlocks_whenPicking_thenSelectsNearest() {
            // Blocks along the positive X axis (where we're looking)
            ghostBlocks.put(new BlockPos(1, 0, 0), state);
            ghostBlocks.put(new BlockPos(2, 0, 0), Blocks.DIRT.getDefaultState());

            when(inventory.getSlotWithStack(stack)).thenReturn(-1);

            boolean result = GhostBlockService.handleGhostBlockPick(worldManager, camera, 5.0, null, true, inventory,
                    vanillaTarget, networkHandler);

            assertTrue(result);
            verify(inventory).swapStackWithHotbar(stack);
            verify(networkHandler).sendPacket(any(CreativeInventoryActionC2SPacket.class));
        }
    }

    @Nested
    class InventoryManagementTests {
        @Test
        void givenItemInHotbar_whenInSurvivalMode_thenSelectsHotbarSlot() {
            ghostBlocks.put(new BlockPos(1, 0, 0), state);
            when(inventory.getSlotWithStack(stack)).thenReturn(0);

            boolean result = GhostBlockService.handleGhostBlockPick(worldManager, camera, 5.0, null, true, inventory,
                    vanillaTarget, networkHandler);

            assertTrue(result);
            assertEquals(0, inventory.selectedSlot);
            verify(inventory, never()).swapStackWithHotbar(any());
            verify(inventory, never()).swapSlotWithHotbar(anyInt());
        }

        @Test
        void givenItemInInventory_whenInSurvivalMode_thenSwapsWithHotbar() {
            ghostBlocks.put(new BlockPos(1, 0, 0), state);
            when(inventory.getSlotWithStack(stack)).thenReturn(9);

            boolean result = GhostBlockService.handleGhostBlockPick(worldManager, camera, 5.0, null, true, inventory,
                    vanillaTarget, networkHandler);

            assertTrue(result);
            verify(inventory).swapSlotWithHotbar(9);
            verify(inventory, never()).swapStackWithHotbar(any());
        }
    }

    @Nested
    class EdgeCaseTests {
        @Test
        void givenNegativeReach_whenPicking_thenNoAction() {
            ghostBlocks.put(new BlockPos(1, 0, 0), state);

            boolean result = GhostBlockService.handleGhostBlockPick(worldManager, camera, -1.0, null, true, inventory,
                    vanillaTarget, networkHandler);

            assertFalse(result);
            verify(inventory, never()).swapStackWithHotbar(any());
        }

        @Test
        void givenDisabledItem_whenPicking_thenNoAction() {
            ghostBlocks.put(new BlockPos(1, 0, 0), state);
            when(stack.isItemEnabled(any())).thenReturn(false);

            boolean result = GhostBlockService.handleGhostBlockPick(worldManager, camera, 5.0, null, true, inventory,
                    vanillaTarget, networkHandler);

            assertTrue(result); // Method still returns true as block was found
            verify(inventory, never()).swapStackWithHotbar(any());
        }

        @Test
        void givenEmptyItemStack_whenPicking_thenNoAction() {
            ghostBlocks.put(new BlockPos(1, 0, 0), state);
            when(stack.isEmpty()).thenReturn(true);

            boolean result = GhostBlockService.handleGhostBlockPick(worldManager, camera, 5.0, null, true, inventory,
                    vanillaTarget, networkHandler);

            assertTrue(result); // Method still returns true as block was found
            verify(inventory, never()).swapStackWithHotbar(any());
        }
    }
}