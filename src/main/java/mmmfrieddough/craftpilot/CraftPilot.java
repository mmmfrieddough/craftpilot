package mmmfrieddough.craftpilot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import mmmfrieddough.craftpilot.config.ModConfig;
import mmmfrieddough.craftpilot.http.HttpService;
import mmmfrieddough.craftpilot.http.ResponseItem;
import mmmfrieddough.craftpilot.schematic.SchematicManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CraftPilot implements ClientModInitializer {
	private static ModConfig config;
	public static final Logger LOGGER = LoggerFactory.getLogger(Reference.MOD_ID);

	private static CraftPilot instance;
	private final HttpService httpService;
	private final SchematicManager schematicManager;

	private BlockPos lastInteractedBlockPos;
	private boolean blockPlacementPending = false;

	public CraftPilot() {
		this.httpService = new HttpService();
		this.schematicManager = new SchematicManager();
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

		// Register shaders
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
				new SimpleSynchronousResourceReloadListener() {
					@Override
					public Identifier getFabricId() {
						return new Identifier("craftpilot", "shaders");
					}

					@Override
					public void reload(ResourceManager manager) {
						// Load shaders here if needed
					}
				});
	}

	private void registerCallbacks() {
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!config.client.enable)
				return ActionResult.PASS;

			lastInteractedBlockPos = hitResult.getBlockPos().offset(hitResult.getSide());
			blockPlacementPending = true;
			return ActionResult.PASS;
		});

		ClientTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
		ClientTickEvents.END_CLIENT_TICK.register(this::handleClientTick);
	}

	private void handleWorldTick(World world) {
		if (!blockPlacementPending || lastInteractedBlockPos == null)
			return;

		blockPlacementPending = false;

		String[][][] matrix = schematicManager.getBlocksMatrix(world, lastInteractedBlockPos);
		httpService.sendRequest(matrix, config.model);
		schematicManager.createSchematic(world, lastInteractedBlockPos);
	}

	private void handleClientTick(MinecraftClient client) {
		ResponseItem item;
		while ((item = httpService.getNextResponse()) != null) {
			schematicManager.processResponse(item);
		}
	}

	public static CraftPilot getInstance() {
		return instance;
	}

	public SchematicManager getSchematicManager() {
		return schematicManager;
	}

	public static ModConfig getConfig() {
		return config;
	}
}
