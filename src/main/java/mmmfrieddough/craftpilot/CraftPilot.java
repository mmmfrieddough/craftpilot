package mmmfrieddough.craftpilot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import mmmfrieddough.craftpilot.config.ModConfig;
import mmmfrieddough.craftpilot.model.HttpModelConnector;
import mmmfrieddough.craftpilot.model.IModelConnector;
import mmmfrieddough.craftpilot.network.NetworkManager;
import mmmfrieddough.craftpilot.service.CraftPilotService;
import mmmfrieddough.craftpilot.service.GhostBlockService;
import mmmfrieddough.craftpilot.service.GhostBlockService.GhostBlockTarget;
import mmmfrieddough.craftpilot.world.IWorldManager;
import mmmfrieddough.craftpilot.world.WorldManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class CraftPilot implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(Reference.MOD_ID);

	private static CraftPilot instance;

	private ModConfig config;
	private IWorldManager worldManager;
	private IModelConnector modelConnector;
	private CraftPilotService craftPilotService;

	public CraftPilot() {
		if (instance != null) {
			throw new RuntimeException("CraftPilot instance already exists");
		}
		instance = this;
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Craftpilot");
		initializeConfig();
		worldManager = new WorldManager();
		modelConnector = new HttpModelConnector();
		craftPilotService = new CraftPilotService(modelConnector, worldManager, config);
		KeyBindings.register();
		NetworkManager.init();
		registerCallbacks();
		LOGGER.info("Craftpilot initialized");
	}

	private void initializeConfig() {
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
	}

	private void registerCallbacks() {
		ClientTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
		ClientTickEvents.END_CLIENT_TICK.register(this::handleClientTick);
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register(this::handleWorldChange);
	}

	private void handleWorldTick(ClientWorld world) {
		craftPilotService.processPendingBlockPlacements(world);
	}

	private void handleClientTick(MinecraftClient client) {
		craftPilotService.processResponses();

		if (KeyBindings.getClearKeyBinding().wasPressed()) {
			LOGGER.info("Clearing suggestions");
			craftPilotService.clearAll();
		}

		if (KeyBindings.getTriggerKeyBinding().wasPressed()) {
			BlockPos pos = getTargetBlockPosition(client);
			if (pos != null) {
				LOGGER.info("Triggering suggestions");
				craftPilotService.cancelSuggestions();
				craftPilotService.requestSuggestions(client.world, pos);
			}
		}
	}

	private BlockPos getTargetBlockPosition(MinecraftClient client) {
		// First check for ghost block target
		GhostBlockTarget target = GhostBlockService.getCurrentTarget();
		if (target != null) {
			return target.pos();
		}

		// Fall back to regular crosshair target
		BlockHitResult hitResult = (BlockHitResult) client.crosshairTarget;
		if (hitResult.getType() == BlockHitResult.Type.BLOCK) {
			return hitResult.getBlockPos();
		}

		return null;
	}

	private void handleWorldChange(MinecraftClient client, ClientWorld world) {
		craftPilotService.clearAll();
	}

	public static CraftPilot getInstance() {
		return instance;
	}

	public ModConfig getConfig() {
		return config;
	}

	public IWorldManager getWorldManager() {
		return worldManager;
	}

	public CraftPilotService getCraftPilotService() {
		return craftPilotService;
	}
}
