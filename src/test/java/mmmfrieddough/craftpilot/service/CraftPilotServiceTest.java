package mmmfrieddough.craftpilot.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mmmfrieddough.craftpilot.config.ModConfig;
import mmmfrieddough.craftpilot.model.IModelConnector;
import mmmfrieddough.craftpilot.model.ResponseItem;
import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@ExtendWith(MockitoExtension.class)
class CraftPilotServiceTest {
    @Mock
    private IModelConnector modelConnector;

    @Mock
    private IWorldManager worldManager;

    @Mock
    private ModConfig config;

    @Mock
    private World world;

    private CraftPilotService service;
    private BlockPos testPos;
    private BlockState stoneState;

    @BeforeAll
    static void init() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @BeforeEach
    void setUp() {
        ModConfig.General general = new ModConfig.General();
        general.placedBlocksThreshold = 5;
        general.nonMatchingBlocksThreshold = 3;
        config.general = general;

        service = new CraftPilotService(modelConnector, worldManager, config);
        testPos = new BlockPos(0, 0, 0);
        stoneState = Blocks.STONE.getDefaultState();
    }

    @Test
    void processPendingBlockPlacements_NoBlockPlaced_DoesNothing() {
        service.processPendingBlockPlacements(world);

        verify(worldManager, never()).getGhostBlockState(any());
        verify(modelConnector, never()).sendRequest(any(), any(), any(), any());
    }

    @Test
    void processPendingBlockPlacements_BlockPlaced_StopsRequest() {
        service.onPlayerBlockPlaced(testPos);
        when(worldManager.getGhostBlockState(testPos)).thenReturn(null);
        when(worldManager.getBlockState(eq(world), any(BlockPos.class))).thenReturn(stoneState);

        service.processPendingBlockPlacements(world);

        verify(modelConnector).stop();
    }

    @Test
    void processPendingBlockPlacements_BlockPlacedNoGhost_RequestsNewSuggestions() {
        service.onPlayerBlockPlaced(testPos);
        when(worldManager.getGhostBlockState(testPos)).thenReturn(null);
        when(worldManager.getBlockState(eq(world), any(BlockPos.class))).thenReturn(stoneState);

        service.processPendingBlockPlacements(world);

        verify(modelConnector).sendRequest(eq(config.model), any(), any(), any());
    }

    @Test
    void processPendingBlockPlacements_BlockMatchesGhost_ClearsGhostBlock() {
        service.onBlockPlaced(testPos);

        service.processPendingBlockPlacements(world);

        verify(worldManager).clearBlockState(testPos);
    }

    @Test
    void processResponses_ValidResponse_SetsBlockState() {
        ResponseItem response = new ResponseItem(0, 0, "minecraft:stone", 5, 5, 5);

        when(modelConnector.getNextResponse()).thenReturn(response).thenReturn(null);

        service.processResponses();

        verify(worldManager).setBlockState(eq(0), eq(0), eq(new BlockPos(5, 5, 5)), any(BlockState.class));
    }

    @Test
    void processResponses_MultipleAlternatives_SetsCorrectAlternativeNumbers() {
        ResponseItem response1 = new ResponseItem(1, 0, "minecraft:stone", 1, 1, 1);
        ResponseItem response2 = new ResponseItem(2, 1, "minecraft:dirt", 1, 1, 1);

        when(modelConnector.getNextResponse())
                .thenReturn(response1)
                .thenReturn(response2)
                .thenReturn(null);

        service.processResponses();

        verify(worldManager).setBlockState(eq(1), eq(0), eq(new BlockPos(1, 1, 1)), any(BlockState.class));
        verify(worldManager).setBlockState(eq(2), eq(1), eq(new BlockPos(1, 1, 1)), any(BlockState.class));
    }

    @Test
    void clearAll_ClearsAllStatesAndStopsRequest() {
        service.clearAll();

        verify(modelConnector).stop();
        verify(worldManager).clearBlockStates();
    }

    @Test
    void processPendingBlockPlacements_ExceedsThreshold_RequestsNewSuggestions() {
        service.onPlayerBlockPlaced(testPos);
        when(worldManager.getGhostBlockState(testPos)).thenReturn(stoneState);
        when(world.getBlockState(testPos)).thenReturn(stoneState);
        when(worldManager.getBlockState(eq(world), any(BlockPos.class))).thenReturn(stoneState);

        // Simulate placing blocks up to threshold
        for (int i = 0; i < config.general.placedBlocksThreshold; i++) {
            service.processPendingBlockPlacements(world);
            service.onPlayerBlockPlaced(testPos);
        }

        verify(modelConnector).sendRequest(eq(config.model), any(), any(), any());
    }

    @Test
    void processPendingBlockPlacements_ExceedsNonMatchingThreshold_ClearsAllAndRequests() {
        service.onPlayerBlockPlaced(testPos);
        when(worldManager.getGhostBlockState(testPos)).thenReturn(stoneState);
        when(world.getBlockState(testPos)).thenReturn(Blocks.DIRT.getDefaultState());
        when(worldManager.getBlockState(eq(world), any(BlockPos.class))).thenReturn(stoneState);

        // Simulate non-matching blocks up to threshold
        for (int i = 0; i < config.general.nonMatchingBlocksThreshold; i++) {
            service.processPendingBlockPlacements(world);
            service.onPlayerBlockPlaced(testPos);
        }

        verify(worldManager).clearBlockStates();
        verify(modelConnector).sendRequest(eq(config.model), any(), any(), any());
    }

    @Test
    void processResponses_ExceptionThrown_ContinuesProcessing() {
        ResponseItem badResponse = new ResponseItem(0, 0, "invalid:format", 1, 1, 1);
        ResponseItem goodResponse = new ResponseItem(0, 0, "minecraft:stone", 2, 2, 2);

        when(modelConnector.getNextResponse())
                .thenReturn(badResponse)
                .thenReturn(goodResponse)
                .thenReturn(null);

        service.processResponses();

        // Verify that despite the exception, processing continued and the good response
        // was handled
        verify(worldManager).setBlockState(eq(0), eq(0), eq(new BlockPos(2, 2, 2)), any(BlockState.class));
    }
}