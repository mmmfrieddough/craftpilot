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
import mmmfrieddough.craftpilot.http.HttpService;
import mmmfrieddough.craftpilot.http.ResponseItem;
import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@ExtendWith(MockitoExtension.class)
class BlockPlacementServiceTest {

    @Mock
    private HttpService httpService;

    @Mock
    private IWorldManager worldManager;

    @Mock
    private ModConfig config;

    @Mock
    private World world;

    private BlockPlacementService service;
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

        service = new BlockPlacementService(httpService, worldManager, config);
        testPos = new BlockPos(0, 0, 0);
        stoneState = Blocks.STONE.getDefaultState();
    }

    @Test
    void handleWorldTick_NoBlockPlaced_DoesNothing() {
        service.handleWorldTick(world);

        verify(worldManager, never()).getGhostBlockState(any());
        verify(httpService, never()).sendRequest(any(), any());
    }

    @Test
    void handleWorldTick_BlockPlacedNoGhost_RequestsNewSuggestions() {
        service.onBlockPlaced(testPos);
        when(worldManager.getGhostBlockState(testPos)).thenReturn(null);
        when(worldManager.getBlockState(eq(world), any(BlockPos.class))).thenReturn(stoneState);

        service.handleWorldTick(world);

        verify(httpService).sendRequest(any(), eq(config.model));
    }

    @Test
    void handleWorldTick_BlockMatchesGhost_ClearsGhostBlock() {
        service.onBlockPlaced(testPos);
        when(worldManager.getGhostBlockState(testPos)).thenReturn(stoneState);
        when(world.getBlockState(testPos)).thenReturn(stoneState);

        service.handleWorldTick(world);

        verify(worldManager).clearBlockState(testPos);
    }

    @Test
    void handleWorldTick_BlockMismatchesGhost_StopsHttpService() {
        service.onBlockPlaced(testPos);
        when(worldManager.getGhostBlockState(testPos)).thenReturn(stoneState);
        when(world.getBlockState(testPos)).thenReturn(Blocks.DIRT.getDefaultState());

        service.handleWorldTick(world);

        verify(httpService).stop();
    }

    @Test
    void processResponses_ValidResponse_SetsBlockState() {
        ResponseItem response = new ResponseItem("minecraft:stone", 5, 5, 5);

        when(httpService.getNextResponse()).thenReturn(response).thenReturn(null);
        service.onBlockPlaced(testPos);

        service.processResponses();

        verify(worldManager).setBlockState(any(BlockPos.class),
                any(BlockState.class));
    }

    @Test
    void clearAll_ClearsAllStatesAndStopsHttp() {
        service.clearAll();

        verify(httpService).stop();
        verify(worldManager).clearBlockStates();
    }

    @Test
    void handleWorldTick_ExceedsThreshold_RequestsNewSuggestions() {
        service.onBlockPlaced(testPos);
        when(worldManager.getGhostBlockState(testPos)).thenReturn(stoneState);
        when(world.getBlockState(testPos)).thenReturn(stoneState);
        when(worldManager.getBlockState(eq(world), any(BlockPos.class))).thenReturn(stoneState);

        // Simulate placing blocks up to threshold
        for (int i = 0; i < config.general.placedBlocksThreshold; i++) {
            service.handleWorldTick(world);
            service.onBlockPlaced(testPos);
        }

        verify(httpService).sendRequest(any(), eq(config.model));
    }

    @Test
    void handleWorldTick_ExceedsNonMatchingThreshold_ClearsAllAndRequests() {
        service.onBlockPlaced(testPos);
        when(worldManager.getGhostBlockState(testPos)).thenReturn(stoneState);
        when(world.getBlockState(testPos)).thenReturn(Blocks.DIRT.getDefaultState());
        when(worldManager.getBlockState(eq(world), any(BlockPos.class))).thenReturn(stoneState);

        // Simulate non-matching blocks up to threshold
        for (int i = 0; i < config.general.nonMatchingBlocksThreshold; i++) {
            service.handleWorldTick(world);
            service.onBlockPlaced(testPos);
        }

        verify(worldManager).clearBlockStates();
        verify(httpService).sendRequest(any(), eq(config.model));
    }
}