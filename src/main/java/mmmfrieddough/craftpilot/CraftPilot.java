package mmmfrieddough.craftpilot;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.google.common.base.Splitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import net.minecraft.registry.Registries;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic.SchematicSaveInfo;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import mmmfrieddough.craftpilot.config.ModConfig;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import java.lang.reflect.Field;

public class CraftPilot implements ClientModInitializer {
	private static ModConfig config;
	public static final Logger LOGGER = LoggerFactory.getLogger(Reference.MOD_ID);

	private static final Function<Map.Entry<Property<?>, Comparable<?>>, String> PROPERTY_MAP_PRINTER = new Function<Map.Entry<Property<?>, Comparable<?>>, String>() {
		@Override
		public String apply(Map.Entry<Property<?>, Comparable<?>> entry) {
			Property<?> property = entry.getKey();
			Comparable<?> value = entry.getValue();
			return property.getName() + "=" + value.toString();
		}
	};

	private String[][][] getBlocksMatrix(World world, BlockPos origin) {
		int radius = 5; // Since we want an 11x11x11 cube, the radius is 5
		String[][][] blocksMatrix = new String[11][11][11];

		for (int x = -radius; x < radius + 1; x++) {
			for (int y = -radius; y < radius + 1; y++) {
				for (int z = -radius; z < radius + 1; z++) {
					BlockPos blockPos = origin.add(x, y, z);
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
					blocksMatrix[z + radius][y + radius][x + radius] = blockId;
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

	public void modifyPrivateField(Object instance, String fieldName, Object newValue) {
		try {
			// Get the Field object for the specified field
			Field field = instance.getClass().getDeclaredField(fieldName);

			// Make the field accessible
			field.setAccessible(true);

			// Set the field's value
			field.set(instance, newValue);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private CompletableFuture<HttpResponse<InputStream>> currentRequestFuture = null;

	private void sendHttpRequest(String[][][] matrix, double temperature, int startRadius, int maxIterations,
			int maxBlocks, float airProbabilityIterationScaling) {
		// Cancel the ongoing request if it exists
		if (currentRequestFuture != null && !currentRequestFuture.isDone()) {
			currentRequestFuture.cancel(true);
		}

		Gson gson = new Gson();
		Request request = new Request();
		request.setPlatform("java");
		request.setVersion_number(3700);
		request.setTemperature(temperature);
		request.setStart_radius(startRadius);
		request.setMax_iterations(maxIterations);
		request.setMax_blocks(maxBlocks);
		request.setAir_probability_iteration_scaling(airProbabilityIterationScaling);
		request.setStructure(matrix);
		String jsonPayload = gson.toJson(request);
		HttpClient httpClient = HttpClient.newHttpClient();
		HttpRequest httpRequest = HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:8000/complete-structure/"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
				.build();

		// Store the CompletableFuture of the new request
		currentRequestFuture = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

		currentRequestFuture.thenAccept(inputStream -> {
			LOGGER.info("Response status code: " + inputStream.statusCode());
			if (inputStream.statusCode() != 200) {
				LOGGER.error("Error sending HTTP request");
				return;
			}
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(inputStream.body(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					LOGGER.info(line);
					ResponseItem responseItem = gson.fromJson(line, ResponseItem.class);
					responseQueue.offer(responseItem);
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

	private ConcurrentLinkedQueue<ResponseItem> responseQueue = new ConcurrentLinkedQueue<>();
	private LitematicaSchematic schematic;
	private BlockPos pos1;
	private LitematicaBlockStateContainer container;
	private BlockPos lastInteractedBlockPos;
	private boolean blockPlacementPending = false;
	private SchematicPlacement placement;

	@Override
	public void onInitializeClient() {
		AutoConfig.register(ModConfig.class, Toml4jConfigSerializer::new);
		config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

		KeyBindings.register(); // Register the key bindings

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (config.client.enable == false) {
				return ActionResult.PASS;
			}
			if (world.isClient) {
				lastInteractedBlockPos = hitResult.getBlockPos();
				blockPlacementPending = true;
			}
			return ActionResult.PASS;
		});

		ClientTickEvents.END_WORLD_TICK.register(world -> {
			if (!blockPlacementPending || lastInteractedBlockPos == null) {
				return;
			}
			BlockState state = world.getBlockState(lastInteractedBlockPos);
			// Check if the block at the position is not air (indicating a block has been
			// placed)
			if (state.isAir()) {
				return;
			}
			// Check if the block at the position matches what is in the schematic
			if (container != null && placement != null) {
				BlockPos placementOrigin = placement.getOrigin();
				if (!(lastInteractedBlockPos.getX() < placementOrigin.getX()
						|| lastInteractedBlockPos.getX() >= placementOrigin.getX() + 11
						|| lastInteractedBlockPos.getY() < placementOrigin.getY()
						|| lastInteractedBlockPos.getY() >= placementOrigin.getY() + 11
						|| lastInteractedBlockPos.getZ() < placementOrigin.getZ()
						|| lastInteractedBlockPos.getZ() >= placementOrigin.getZ() + 11)) {
					BlockPos relativePos = lastInteractedBlockPos.subtract(placementOrigin);
					try {
						BlockState schematicState = container.get(relativePos.getX(), relativePos.getY(),
								relativePos.getZ());
						if (schematicState != null && schematicState == state) {
							blockPlacementPending = false;
							return;
						}
					} catch (ArrayIndexOutOfBoundsException e) {
						LOGGER.info("Block position out of bounds");
					}
				}
			}

			AreaSelection selection = new AreaSelection();
			pos1 = lastInteractedBlockPos.add(-5, -5, -5);
			BlockPos pos2 = lastInteractedBlockPos.add(5, 5, 5);
			Box box = new Box();
			box.setPos1(pos1);
			box.setPos2(pos2);
			selection.addSubRegionBox(box, true);
			SchematicSaveInfo info = new LitematicaSchematic.SchematicSaveInfo(false, false);
			schematic = LitematicaSchematic.createFromWorld(world, selection, info, "CraftPilot", str -> {
			});
			Field field;
			try {
				field = schematic.getClass().getDeclaredField("blockContainers");
			} catch (NoSuchFieldException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			field.setAccessible(true);
			Map<String, LitematicaBlockStateContainer> blockContainers;
			try {
				blockContainers = ((Map<String, LitematicaBlockStateContainer>) field.get(schematic));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			container = blockContainers.values().iterator().next();
			SchematicHolder.getInstance().addSchematic(schematic, true);

			String[][][] matrix = getBlocksMatrix(world, lastInteractedBlockPos);
			sendHttpRequest(matrix, config.model.temperature, config.model.startRadius, config.model.maxIterations,
					config.model.maxBlocks, config.model.airProbabilityIterationScaling);
			blockPlacementPending = false;
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ResponseItem item;
			while ((item = responseQueue.poll()) != null) {
				String value = item.getValue();
				List<Integer> position = item.getPosition();
				LOGGER.info("Value: " + value + ", Position: " + position);
				Optional<BlockState> stateOptional = getBlockStateFromString(value);
				stateOptional.ifPresent(blockState -> {
					container.set(position.get(2), position.get(1), position.get(0), blockState);
				});

				SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
				if (manager.getSelectedSchematicPlacement() != null) {
					manager.removeSchematicPlacement(manager.getSelectedSchematicPlacement());
				}
				placement = SchematicPlacement.createFor(schematic, pos1, "test", true, true);
				manager.addSchematicPlacement(placement, true);
				manager.setSelectedSchematicPlacement(placement);
			}
		});
	}
}
