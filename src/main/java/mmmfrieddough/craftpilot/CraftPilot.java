package mmmfrieddough.craftpilot;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.google.common.base.Splitter;

import java.io.BufferedReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import net.minecraft.registry.Registries;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CraftPilot implements ModInitializer {
	public static final String MOD_ID = "craftpilot";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final Function<Map.Entry<Property<?>, Comparable<?>>, String> PROPERTY_MAP_PRINTER = new Function<Map.Entry<Property<?>, Comparable<?>>, String>() {
		@Override
		public String apply(Map.Entry<Property<?>, Comparable<?>> entry) {
			Property<?> property = entry.getKey();
			Comparable<?> value = entry.getValue();
			return property.getName() + "=" + value.toString();
		}
	};

	private String[][][] getBlocksMatrix(PlayerEntity player) {
		World world = player.getEntityWorld();
		BlockPos playerPos = player.getBlockPos();
		int radius = 4; // Since we want an 8x8x8 cube, the radius is half of 8
		String[][][] blocksMatrix = new String[8][8][8];

		for (int x = -radius; x < radius; x++) {
			for (int y = -radius; y < radius; y++) {
				for (int z = -radius; z < radius; z++) {
					BlockPos blockPos = playerPos.add(x, y, z);
					BlockState blockState = world.getBlockState(blockPos);
					StringBuilder stringBuilder = new StringBuilder();
					stringBuilder.append(Registries.BLOCK.getId(blockState.getBlock()));
					if (!blockState.getEntries().isEmpty()) {
						stringBuilder.append('[');
						stringBuilder.append((String) blockState.getEntries().entrySet().stream()
								.map(PROPERTY_MAP_PRINTER).collect(Collectors.joining(",")));
						stringBuilder.append(']');
					}
					String blockId = stringBuilder.toString();
					blocksMatrix[x + radius][y + radius][z + radius] = blockId;
				}
			}
		}

		return blocksMatrix;
	}

	private static final Splitter COMMA_SPLITTER = Splitter.on(',');
	private static final Splitter EQUAL_SPLITTER = Splitter.on('=').limit(2);

	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> BlockState getBlockStateWithProperty(BlockState state, Property<T> prop,
			Comparable<?> value) {
		return state.with(prop, (T) value);
	}

	@Nullable
	public static <T extends Comparable<T>> T getPropertyValueByName(Property<T> prop, String valStr) {
		return prop.parse(valStr).orElse(null);
	}

	public static Optional<BlockState> getBlockStateFromString(String str) {
		int index = str.indexOf("["); // [f=b]
		String blockName = index != -1 ? str.substring(0, index) : str;

		try {
			Identifier id = new Identifier(blockName);

			if (Registries.BLOCK.containsId(id)) {
				Block block = Registries.BLOCK.get(id);
				BlockState state = block.getDefaultState();

				if (index != -1 && str.length() > (index + 4) && str.charAt(str.length() - 1) == ']') {
					StateManager<Block, BlockState> stateManager = block.getStateManager();
					String propStr = str.substring(index + 1, str.length() - 1);

					for (String propAndVal : COMMA_SPLITTER.split(propStr)) {
						Iterator<String> valIter = EQUAL_SPLITTER.split(propAndVal).iterator();

						if (valIter.hasNext() == false) {
							continue;
						}

						Property<?> prop = stateManager.getProperty(valIter.next());

						if (prop == null || valIter.hasNext() == false) {
							continue;
						}

						Comparable<?> val = getPropertyValueByName(prop, valIter.next());

						if (val != null) {
							state = getBlockStateWithProperty(state, prop, val);
						}
					}
				}

				return Optional.of(state);
			}
		} catch (Exception e) {
			return Optional.empty();
		}

		return Optional.empty();
	}

	private void setBlockAtRelativePosition(MinecraftClient client, BlockPos origin, List<Integer> relativePosition,
			String blockId) {

		int radius = 4; // The radius used in getBlocksMatrix
		BlockPos targetPos = origin.add(relativePosition.get(0) - radius, relativePosition.get(1) - radius,
				relativePosition.get(2) - radius);
		Optional<BlockState> stateOptional = getBlockStateFromString(blockId);

		stateOptional.ifPresent(blockState -> {
			client.world.setBlockState(targetPos, blockState);
		});
	}

	private void sendHttpRequest(MinecraftClient client, World world, BlockPos playerPos, String[][][] matrix,
			double temperature) {
		Gson gson = new Gson();
		Request request = new Request();
		request.setStructure(matrix);
		request.setTemperature(temperature);
		String jsonPayload = gson.toJson(request);
		HttpClient httpClient = HttpClient.newHttpClient();
		HttpRequest httpRequest = HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:8000/complete-structure/"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
				.build();

		httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream())
				.thenAccept(inputStream -> {
					LOGGER.info("Response status code: " + inputStream.statusCode());
					try (BufferedReader reader = new BufferedReader(
							new InputStreamReader(inputStream.body(), StandardCharsets.UTF_8))) {
						String line;
						while ((line = reader.readLine()) != null) {
							LOGGER.info(line);
							ResponseItem responseItem = gson.fromJson(line, ResponseItem.class);
							String value = responseItem.getValue();
							List<Integer> position = responseItem.getPosition();
							setBlockAtRelativePosition(client, playerPos, position, value);
							LOGGER.info("Value: " + value + ", Position: " + position);

						}
					} catch (Exception e) {
						LOGGER.error("Error handling the streaming response", e);
					}
				})
				.exceptionally(e -> {
					LOGGER.error("Error sending HTTP request", e);
					return null;
				});
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");

		KeyBindings.register(); // Register the key bindings

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (KeyBindings.getExampleKeyBinding().wasPressed()) {
				LOGGER.info("Key was pressed!");

				PlayerEntity player = client.player;
				World world = player.getEntityWorld();
				BlockPos playerPos = player.getBlockPos();
				String[][][] matrix = getBlocksMatrix(player);
				sendHttpRequest(client, world, playerPos, matrix, 0.7);
			}
		});
	}
}
