package mmmfrieddough.craftpilot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import mmmfrieddough.craftpilot.config.ModConfig;
import mmmfrieddough.craftpilot.http.HttpService;
import mmmfrieddough.craftpilot.http.ResponseItem;
import mmmfrieddough.craftpilot.schematic.SchematicManager;
import mmmfrieddough.craftpilot.util.BlockMatrixBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CraftPilot implements ClientModInitializer {
	private static ModConfig config;
	public static final Logger LOGGER = LoggerFactory.getLogger(Reference.MOD_ID);

	private final HttpService httpService;
	private final SchematicManager schematicManager;
	private final BlockMatrixBuilder blockMatrixBuilder;

	private BlockPos lastInteractedBlockPos;
	private boolean blockPlacementPending = false;

	public CraftPilot() {
		this.httpService = new HttpService();
		this.schematicManager = new SchematicManager();
		this.blockMatrixBuilder = new BlockMatrixBuilder();
	}

	@Override
	public void onInitializeClient() {
		// Set up configuration
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

		// Register key bindings
		KeyBindings.register();

		registerCallbacks();
	}

	private void registerCallbacks() {
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!config.client.enable)
				return ActionResult.PASS;

			lastInteractedBlockPos = hitResult.getBlockPos();
			blockPlacementPending = true;
			return ActionResult.PASS;
		});

		ClientTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
		ClientTickEvents.END_CLIENT_TICK.register(this::handleClientTick);
	}

	private void handleWorldTick(World world) {
		if (!blockPlacementPending || lastInteractedBlockPos == null)
			return;

		if (!schematicManager.shouldProcessBlock(world, lastInteractedBlockPos))
			return;

		String[][][] matrix = blockMatrixBuilder.getBlocksMatrix(world, lastInteractedBlockPos);
		schematicManager.createSchematic(world, lastInteractedBlockPos);

		httpService.sendRequest(matrix, config.model);
		blockPlacementPending = false;
	}

	private void handleClientTick(MinecraftClient client) {
		ResponseItem item;
		while ((item = httpService.getNextResponse()) != null) {
			schematicManager.processResponse(item);
		}
	}
}
