package mmmfrieddough.craftpilot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import mmmfrieddough.craftpilot.config.ModConfig;
import mmmfrieddough.craftpilot.http.HttpService;
import mmmfrieddough.craftpilot.http.ResponseItem;
import mmmfrieddough.craftpilot.util.BlockMatrixUtils;
import mmmfrieddough.craftpilot.world.BlockStateHelper;
import mmmfrieddough.craftpilot.world.IWorldManager;
import mmmfrieddough.craftpilot.world.WorldManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CraftPilot implements ClientModInitializer {
	private static final int MATRIX_SIZE = 11;

	public static final Logger LOGGER = LoggerFactory.getLogger(Reference.MOD_ID);

	private static CraftPilot instance;
	private static ModConfig config;
	private final HttpService httpService;
	private final IWorldManager worldManager;

	private BlockPos placedBlockPos;
	private boolean blockPlacementPending = false;
	private int nonMatchingBlockCount = 0;
	private int placedBlockCount = 0;

	public CraftPilot() {
		this.httpService = new HttpService();
		this.worldManager = new WorldManager();
	}

	@Override
	public void onInitializeClient() {
		instance = this;

		// Set up configuration
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

		// Register key bindings
		KeyBindings.register();

		registerCallbacks();
	}

	private void registerCallbacks() {
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!world.isClient)
				return ActionResult.PASS;

			if (!config.general.enable)
				return ActionResult.PASS;

			placedBlockPos = hitResult.getBlockPos().offset(hitResult.getSide());
			blockPlacementPending = true;
			return ActionResult.PASS;
		});

		ClientTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
		ClientTickEvents.END_CLIENT_TICK.register(this::handleClientTick);
	}

	private String[][][] getBlocksMatrix(World world, BlockPos centerPos) {
		int offset = MATRIX_SIZE / 2;
		String[][][] matrix = new String[MATRIX_SIZE][MATRIX_SIZE][MATRIX_SIZE];
		for (int x = 0; x < MATRIX_SIZE; x++) {
			for (int y = 0; y < MATRIX_SIZE; y++) {
				for (int z = 0; z < MATRIX_SIZE; z++) {
					BlockPos pos = centerPos.add(x - offset, y - offset, z - offset);
					BlockState state = worldManager.getBlockState(world, pos);
					matrix[z][y][x] = BlockMatrixUtils.getBlockStateString(state);
				}
			}
		}
		return matrix;
	}

	private void handleWorldTick(World world) {
		// Check if block placement is pending
		if (!blockPlacementPending || placedBlockPos == null)
			return;
		blockPlacementPending = false;

		// Get the suggested and actual block states
		BlockState ghostBlockState = worldManager.getGhostBlockState(placedBlockPos);
		BlockState blockState = world.getBlockState(placedBlockPos);

		// Handle block placement based on state
		if (ghostBlockState == null) {
			// New area - always request suggestions
			requestNewSuggestions(world);
			return;
		}

		// Increment counters
		placedBlockCount++;

		// Clear the old suggestion at the current position
		worldManager.clearBlockState(placedBlockPos);

		if (!ghostBlockState.equals(blockState)) {
			nonMatchingBlockCount++;
			httpService.stop(); // Stop current request when user deviates

			// Clear all suggestions if too many non-matching blocks
			if (nonMatchingBlockCount >= config.general.nonMatchingBlocksThreshold) {
				worldManager.clearBlockStates();
				resetCounters();
				requestNewSuggestions(world); // Request new suggestions after clearing all
				return;
			}
		}

		// Request new suggestions if enough blocks have been placed
		if (placedBlockCount >= config.general.placedBlocksThreshold) {
			requestNewSuggestions(world);
			resetCounters();
		}
	}

	private void requestNewSuggestions(World world) {
		httpService.stop();
		String[][][] matrix = getBlocksMatrix(world, placedBlockPos);
		httpService.sendRequest(matrix, config.model);
	}

	private void resetCounters() {
		placedBlockCount = 0;
		nonMatchingBlockCount = 0;
	}

	private void handleClientTick(MinecraftClient client) {
		ResponseItem item;
		while ((item = httpService.getNextResponse()) != null) {
			BlockPos pos = placedBlockPos.add(item.getX() - 5, item.getY() - 5, item.getZ() - 5);
			BlockState blockState = BlockStateHelper.parseBlockState(item.getBlockState());
			worldManager.setBlockState(pos, blockState);
		}

		while (KeyBindings.getClearKeyBinding().wasPressed()) {
			LOGGER.info("Clearing suggestions");
			httpService.stop();
			worldManager.clearBlockStates();
		}
	}

	public static CraftPilot getInstance() {
		return instance;
	}

	public IWorldManager getWorldManager() {
		return worldManager;
	}

	public static ModConfig getConfig() {
		return config;
	}
}
