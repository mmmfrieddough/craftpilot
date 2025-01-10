// package mmmfrieddough.craftpilot.service;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNull;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyInt;
// import static org.mockito.Mockito.doReturn;
// import static org.mockito.Mockito.never;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;

// import java.util.HashMap;
// import java.util.Map;

// import org.junit.jupiter.api.BeforeAll;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.mockito.junit.jupiter.MockitoSettings;
// import org.mockito.quality.Strictness;

// import net.minecraft.Bootstrap;
// import net.minecraft.SharedConstants;
// import net.minecraft.block.Block;
// import net.minecraft.block.BlockState;
// import net.minecraft.block.Blocks;
// import net.minecraft.client.MinecraftClient;
// import net.minecraft.client.network.ClientPlayerEntity;
// import net.minecraft.entity.player.PlayerInventory;
// import net.minecraft.item.Item;
// import net.minecraft.item.ItemStack;
// import net.minecraft.util.math.BlockPos;
// import net.minecraft.util.math.Vec3d;
// import net.minecraft.world.World;

// @ExtendWith(MockitoExtension.class)
// @MockitoSettings(strictness = Strictness.LENIENT)
// class GhostBlockServiceTest {

// @Mock
// private MinecraftClient client;
// @Mock
// private ClientPlayerEntity player;
// @Mock
// private PlayerInventory inventory;
// @Mock
// private World world;
// @Mock
// private BlockState state;
// @Mock
// private Block block;
// @Mock
// private Item item;
// @Mock
// private ItemStack stack;

// @BeforeAll
// static void init() {
// SharedConstants.createGameVersion();
// Bootstrap.initialize();
// }

// @BeforeEach
// void setUp() {
// client.player = player;
// doReturn(inventory).when(player).getInventory();
// when(player.getWorld()).thenReturn(world);
// when(world.getEnabledFeatures()).thenReturn(null);
// when(state.getBlock()).thenReturn(block);
// when(block.asItem()).thenReturn(item);
// when(item.getDefaultStack()).thenReturn(stack);
// when(stack.isEmpty()).thenReturn(false);
// when(stack.isItemEnabled(any())).thenReturn(true);
// }

// @Test
// void pickBlock_EmptyMap_NoAction() {
// Map<BlockPos, BlockState> ghostBlocks = new HashMap<>();
// Vec3d cameraPos = new Vec3d(0, 0, 0);
// Vec3d lookVec = new Vec3d(1, 0, 0);

// GhostBlockService.handleGhostBlockPick(client);

// assertNull(result);
// }

// @Test
// void findTargetedGhostBlock_LookingDirectlyAtBlock_ReturnsBlock() {
// Map<BlockPos, BlockState> ghostBlocks = new HashMap<>();
// BlockPos pos = new BlockPos(2, 1, 0);
// BlockState stoneState = Blocks.STONE.getDefaultState();
// ghostBlocks.put(pos, stoneState);

// Vec3d cameraPos = new Vec3d(0, 1, 0);
// Vec3d lookVec = new Vec3d(1, 0, 0);

// BlockPos result = GhostBlockService.findTargetedGhostBlock(ghostBlocks,
// cameraPos, lookVec, 5.0);

// assertEquals(pos, result);
// }

// @Test
// void findTargetedGhostBlock_LookingAwayFromBlock_ReturnsNull() {
// Map<BlockPos, BlockState> ghostBlocks = new HashMap<>();
// BlockPos pos = new BlockPos(2, 1, 0);
// BlockState stoneState = Blocks.STONE.getDefaultState();
// ghostBlocks.put(pos, stoneState);

// Vec3d cameraPos = new Vec3d(0, 1, 0);
// Vec3d lookVec = new Vec3d(-1, 0, 0);

// BlockPos result = GhostBlockService.findTargetedGhostBlock(ghostBlocks,
// cameraPos, lookVec, 5.0);

// assertNull(result);
// }

// @Test
// void findTargetedGhostBlock_BlockTooFar_ReturnsNull() {
// Map<BlockPos, BlockState> ghostBlocks = new HashMap<>();
// BlockPos pos = new BlockPos(10, 1, 0);
// BlockState stoneState = Blocks.STONE.getDefaultState();
// ghostBlocks.put(pos, stoneState);

// Vec3d cameraPos = new Vec3d(0, 1, 0);
// Vec3d lookVec = new Vec3d(1, 0, 0);

// BlockPos result = GhostBlockService.findTargetedGhostBlock(ghostBlocks,
// cameraPos, lookVec, 5.0);

// assertNull(result);
// }

// @Test
// void findTargetedGhostBlock_MultipleBlocks_ReturnsClosest() {
// Map<BlockPos, BlockState> ghostBlocks = new HashMap<>();
// BlockPos farPos = new BlockPos(4, 1, 0);
// BlockPos closePos = new BlockPos(2, 1, 0);
// BlockState stoneState = Blocks.STONE.getDefaultState();
// ghostBlocks.put(farPos, stoneState);
// ghostBlocks.put(closePos, stoneState);

// Vec3d cameraPos = new Vec3d(0, 1, 0);
// Vec3d lookVec = new Vec3d(1, 0, 0);

// BlockPos result = GhostBlockService.findTargetedGhostBlock(ghostBlocks,
// cameraPos, lookVec, 5.0);

// assertEquals(closePos, result);
// }

// @Test
// void pickGhostBlock_CreativeMode_AddsItemToInventory() {
// when(player.isInCreativeMode()).thenReturn(true);
// when(inventory.getSlotWithStack(any())).thenReturn(-1);

// GhostBlockService.pickGhostBlock(client, state);

// verify(inventory).swapStackWithHotbar(any());
// }

// @Test
// void pickGhostBlock_SurvivalModeItemInHotbar_SelectsHotbarSlot() {
// when(player.isInCreativeMode()).thenReturn(false);
// when(inventory.getSlotWithStack(any())).thenReturn(2);

// GhostBlockService.pickGhostBlock(client, state);

// assertEquals(2, inventory.selectedSlot);
// }

// @Test
// void pickGhostBlock_SurvivalModeItemInInventory_SwapsWithHotbar() {
// when(player.isInCreativeMode()).thenReturn(false);
// when(inventory.getSlotWithStack(any())).thenReturn(15);

// GhostBlockService.pickGhostBlock(client, state);

// verify(inventory).swapSlotWithHotbar(15);
// }

// @Test
// void pickGhostBlock_EmptyStack_NoAction() {
// BlockState airState = Blocks.AIR.getDefaultState();

// GhostBlockService.pickGhostBlock(client, airState);

// verify(inventory, never()).swapStackWithHotbar(any());
// verify(inventory, never()).swapSlotWithHotbar(anyInt());
// }
// }