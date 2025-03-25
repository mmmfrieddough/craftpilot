package mmmfrieddough.craftpilot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mmmfrieddough.craftpilot.network.NetworkManager;
import net.fabricmc.api.ModInitializer;

public class CraftPilot implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(Reference.MOD_NAME);

	private static CraftPilot instance;

	public CraftPilot() {
		if (instance != null) {
			throw new RuntimeException("Instance already exists");
		}
		instance = this;
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing");

		NetworkManager.init();

		LOGGER.info("Initialized");
	}
}
