package mmmfrieddough.craftpilot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mmmfrieddough.craftpilot.network.NetworkManager;
import net.fabricmc.api.ModInitializer;

public class CraftPilot implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(Reference.MOD_ID);

	private static CraftPilot instance;

	public CraftPilot() {
		if (instance != null) {
			throw new RuntimeException("CraftPilot instance already exists");
		}
		instance = this;
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Craftpilot");

		NetworkManager.init();

		LOGGER.info("Craftpilot initialized");
	}
}
