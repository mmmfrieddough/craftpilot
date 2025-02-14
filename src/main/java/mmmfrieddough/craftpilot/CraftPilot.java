package mmmfrieddough.craftpilot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import mmmfrieddough.craftpilot.config.ModConfig;
import mmmfrieddough.craftpilot.http.HttpService;
import mmmfrieddough.craftpilot.network.ServerNetworking;
import mmmfrieddough.craftpilot.network.payloads.PlayerPlaceBlockPayload;
import mmmfrieddough.craftpilot.service.BlockPlacementService;
import mmmfrieddough.craftpilot.world.IWorldManager;
import mmmfrieddough.craftpilot.world.WorldManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;

public class CraftPilot implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(Reference.MOD_ID);

	private static CraftPilot instance;
	private static ModConfig config;
	private final IWorldManager worldManager;
	private BlockPlacementService blockPlacementService;

	public CraftPilot() {
		this.worldManager = new WorldManager();
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Craftpilot");
		instance = this;
		initializeConfig();
		this.blockPlacementService = new BlockPlacementService(new HttpService(), worldManager, config);
		KeyBindings.register();
		registerCallbacks();
		PayloadTypeRegistry.playC2S().register(PlayerPlaceBlockPayload.ID, PlayerPlaceBlockPayload.CODEC);
		ServerNetworking.registerReceivers();
	}

	private void initializeConfig() {
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
	}

	private void registerCallbacks() {
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!world.isClient || !config.general.enable) {
				return ActionResult.PASS;
			}

			blockPlacementService.onBlockPlaced(hitResult.getBlockPos().offset(hitResult.getSide()));
			return ActionResult.PASS;
		});

		ClientTickEvents.END_WORLD_TICK.register(blockPlacementService::handleWorldTick);
		ClientTickEvents.END_CLIENT_TICK.register(this::handleClientTick);
	}

	private void handleClientTick(MinecraftClient client) {
		blockPlacementService.processResponses();

		while (KeyBindings.getClearKeyBinding().wasPressed()) {
			LOGGER.info("Clearing suggestions");
			blockPlacementService.clearAll();
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
