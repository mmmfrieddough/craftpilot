package mmmfrieddough.craftpilot.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.Reference;
import mmmfrieddough.craftpilot.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class HttpModelConnector implements IModelConnector {
    private final HttpClient httpClient;
    private final Gson gson;
    private final ConcurrentLinkedQueue<ResponseItem> responseQueue;
    private final String userAgent = "Craftpilot/" + Reference.MOD_VERSION + " (MC " + Reference.MC_VERSION + ")";
    private final String clientId = java.util.UUID.randomUUID().toString();
    private final String playerId = MinecraftClient.getInstance().getSession().getUuidOrNull().toString();
    private CompletableFuture<HttpResponse<InputStream>> currentRequestFuture;
    private long currentRequestId = 0;

    private volatile boolean requestInProgress = false;

    public HttpModelConnector() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.responseQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void sendRequest(ModConfig.Model config, int[][][] matrix, Map<Integer, String> palette, BlockPos origin) {
        final long requestId = currentRequestId;

        CraftPilot.LOGGER.info("Sending request {} to {}", requestId, config.serverUrl);

        Request request = buildRequest(matrix, palette, config);
        String jsonPayload = gson.toJson(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(config.serverUrl))
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent)
                .header("X-Minecraft-Version", Reference.MC_VERSION)
                .header("X-Craftpilot-Version", Reference.MOD_VERSION)
                .header("X-Client-ID", clientId)
                .header("X-Player-ID", playerId)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        currentRequestFuture = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

        requestInProgress = true;

        currentRequestFuture
                .thenAccept(response -> handleResponse(response, requestId, origin))
                .exceptionally(throwable -> {
                    if (requestId == currentRequestId) {
                        requestInProgress = false;
                    }
                    Throwable cause = throwable.getCause();
                    if (cause instanceof CancellationException) {
                        return null;
                    }
                    if (cause instanceof ConnectException) {
                        String setupUrl = "https://github.com/mmmfrieddough/craftpilot#setup";
                        Text message = Text
                                .literal("URL " + config.serverUrl + " appears to be offline or unreachable.\n")
                                .styled(style -> style.withColor(0xFF0000))
                                .append(Text.literal("Make sure you've installed and started the companion program!\n")
                                        .styled(style -> style.withColor(0xFF0000)))
                                .append(Text.literal("Click here for setup instructions")
                                        .styled(style -> style
                                                .withColor(0xFF0000)
                                                .withUnderline(true)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, setupUrl))));

                        CraftPilot.LOGGER.error("Server connection failed: {}", cause.getMessage());
                        MinecraftClient.getInstance().player.sendMessage(message, false);
                    } else if (cause instanceof TimeoutException) {
                        logError("Request timed out - server may be unresponsive");
                    } else {
                        logError("Error sending HTTP request: " + cause.getMessage());
                    }
                    return null;
                });
    }

    @Override
    public ResponseItem getNextResponse() {
        return responseQueue.poll();
    }

    @Override
    public void stop() {
        currentRequestId++;
        cancelCurrentRequest();
        clearResponses();
    }

    @Override
    public boolean isGenerating() {
        return requestInProgress || !responseQueue.isEmpty();
    }

    private static void logError(String message) {
        CraftPilot.LOGGER.error(message);
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(Text.literal(message).formatted(Formatting.RED), false);
        }
    }

    private Request buildRequest(int[][][] matrix, Map<Integer, String> palette, ModConfig.Model modelConfig) {
        Request request = new Request();
        request.setModel_type(modelConfig.modelType.getValue());
        request.setModel_version(modelConfig.modelVersion);
        request.setInference_device(modelConfig.inferenceDevice.getValue());
        request.setPlatform("java");
        request.setVersion_number(Reference.MC_DATA_VERSION);
        request.setTemperature(modelConfig.temperature);
        request.setStart_radius(modelConfig.startRadius);
        request.setMax_iterations(modelConfig.maxIterations);
        request.setMax_blocks(modelConfig.maxBlocks);
        request.setMax_alternatives(modelConfig.maxAlternatives);
        request.setMin_alternative_probability(modelConfig.minAlternativeProbability);
        request.setIgnore_replaceable_blocks(modelConfig.ignoreReplaceableBlocks);
        request.setPalette(palette);
        request.setStructure(matrix);
        return request;
    }

    private BlockPos calculateResponsePosition(ResponseItem item, BlockPos placedBlockPos) {
        return placedBlockPos.add(item.getX(), item.getY(), item.getZ());
    }

    private void handleResponse(HttpResponse<InputStream> response, long requestId, BlockPos origin) {
        CraftPilot.LOGGER.info("Received response for request {} with status code {}", requestId,
                response.statusCode());

        if (response.statusCode() != 200) {
            String errorBody;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                errorBody = reader.lines().collect(Collectors.joining("\n"));
            } catch (Exception e) {
                errorBody = "Failed to read error response: " + e.getMessage();
            }
            logError("Request failed with status code " + response.statusCode() + ": " + errorBody);

            if (requestId == currentRequestId) {
                requestInProgress = false;
            }
            return;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && requestId == currentRequestId) {
                JsonObject jsonObject = JsonParser.parseString(line).getAsJsonObject();

                // Check message type
                String type = jsonObject.get("type").getAsString();

                if ("block".equals(type)) {
                    // Process block placement
                    ResponseItem responseItem = gson.fromJson(jsonObject, ResponseItem.class);
                    BlockPos position = calculateResponsePosition(responseItem, origin);
                    responseItem = new ResponseItem(responseItem.getType(), responseItem.getAlternativeNum(),
                            responseItem.getPreviousAlternativeNum(), responseItem.getBlockState(), position.getX(),
                            position.getY(), position.getZ());
                    responseQueue.offer(responseItem);
                } else if ("complete".equals(type)) {
                    CraftPilot.LOGGER.info("Request {} completed", requestId);
                    break;
                } else if ("error".equals(type)) {
                    // Process error message
                    logError("Request failed: " + jsonObject.get("detail").getAsString());
                    break;
                } else {
                    logError("Received unknown message type: " + type);
                }
            }
        } catch (IOException e) {
            logError("Server connection closed unexpectedly");
        } catch (Exception e) {
            logError("Error handling the streaming response: " + e.getMessage());
        } finally {
            if (requestId == currentRequestId) {
                requestInProgress = false;
            }
        }
    }

    private void cancelCurrentRequest() {
        CraftPilot.LOGGER.info("Cancelling current request");
        if (currentRequestFuture != null) {
            currentRequestFuture.cancel(true);
        }
        requestInProgress = false;
    }

    private void clearResponses() {
        responseQueue.clear();
    }
}