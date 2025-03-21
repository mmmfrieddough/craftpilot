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
import mmmfrieddough.craftpilot.ui.ActivityIndicatorRenderer;
import mmmfrieddough.craftpilot.ui.AlternativesRenderer;
import mmmfrieddough.craftpilot.world.IWorldManager;
import mmmfrieddough.craftpilot.world.WorldManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
public class CraftPilotClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(Reference.MOD_ID);

    private static CraftPilotClient instance;

    private ModConfig config;
    private IWorldManager worldManager;
    private IModelConnector modelConnector;
    private CraftPilotService craftPilotService;

    public CraftPilotClient() {
        if (instance != null) {
            throw new RuntimeException("CraftPilotClient instance already exists");
        }
        instance = this;
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Craftpilot client");

        initializeConfig();
        worldManager = new WorldManager();
        modelConnector = new HttpModelConnector();
        craftPilotService = new CraftPilotService(modelConnector, worldManager, config);

        // Initialize and register activity indicator
        ActivityIndicatorRenderer activityIndicator = new ActivityIndicatorRenderer();
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> activityIndicator.render(drawContext,
                MinecraftClient.getInstance(), modelConnector, worldManager, tickDelta));
        AlternativesRenderer alternativesRenderer = new AlternativesRenderer();
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> alternativesRenderer.render(drawContext,
                config.rendering, MinecraftClient.getInstance(), worldManager));

        KeyBindings.register();
        NetworkManager.initClient();

        registerCallbacks();

        LOGGER.info("Craftpilot client initialized");
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
            LOGGER.info("Triggering suggestions");
            craftPilotService.triggerSuggestions(client);
        }

        if (KeyBindings.getAcceptAllKeyBinding().wasPressed()) {
            craftPilotService.acceptAll(client);
        }
    }

    private void handleWorldChange(MinecraftClient client, ClientWorld world) {
        craftPilotService.clearAll();
    }

    public static CraftPilotClient getInstance() {
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