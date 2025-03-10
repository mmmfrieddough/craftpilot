package mmmfrieddough.craftpilot.model;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;
import java.util.Map;

import org.slf4j.Logger;

import com.google.gson.Gson;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.Reference;
import mmmfrieddough.craftpilot.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class HttpModelConnector implements IModelConnector {
    private static final Logger LOGGER = CraftPilot.LOGGER;

    private final HttpClient httpClient;
    private final Gson gson;
    private final ConcurrentLinkedQueue<ResponseItem> responseQueue;
    private final String userAgent = "Craftpilot/" + Reference.MOD_VERSION + " (MC " + Reference.MC_VERSION + ")";
    private final String clientId = java.util.UUID.randomUUID().toString();
    private final String playerId = MinecraftClient.getInstance().getSession().getUuidOrNull().toString();
    private CompletableFuture<HttpResponse<InputStream>> currentRequestFuture;
    private long currentRequestId = 0;

    private volatile boolean generating = false;

    public HttpModelConnector() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.responseQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void sendRequest(ModConfig.Model config, int[][][] matrix, Map<Integer, String> palette, BlockPos origin) {
        final long requestId = currentRequestId;

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

        generating = true;

        currentRequestFuture
                .thenAccept(response -> handleResponse(response, requestId, origin))
                .exceptionally(throwable -> {
                    generating = false;
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

                        LOGGER.error("Server connection failed: {}", cause.getMessage());
                        MinecraftClient.getInstance().player.sendMessage(message, false);
                    } else if (cause instanceof TimeoutException) {
                        String errorMsg = "Request timed out - server may be unresponsive";
                        LOGGER.error(errorMsg);
                        MinecraftClient.getInstance().player
                                .sendMessage(Text.literal(errorMsg).styled(style -> style.withColor(0xFF0000)), false);
                    } else {
                        LOGGER.error("Error sending HTTP request", throwable);
                        MinecraftClient.getInstance().player.sendMessage(
                                Text.literal("Error sending HTTP request").styled(style -> style.withColor(0xFF0000)),
                                false);
                    }
                    return null;
                });
    }

    public ResponseItem getNextResponse() {
        return responseQueue.poll();
    }

    public boolean isGenerating() {
        return generating || !responseQueue.isEmpty();
    }

    private Request buildRequest(int[][][] matrix, Map<Integer, String> palette, ModConfig.Model modelConfig) {
        Request request = new Request();
        request.setPlatform("java");
        request.setVersion_number(Reference.MC_DATA_VERSION);
        request.setTemperature(modelConfig.temperature);
        request.setStart_radius(modelConfig.startRadius);
        request.setMax_iterations(modelConfig.maxIterations);
        request.setMax_blocks(modelConfig.maxBlocks);
        request.setAir_probability_iteration_scaling(modelConfig.airProbabilityIterationScaling);
        request.setStructure(matrix);
        request.setPalette(palette);
        return request;
    }

    private BlockPos calculateResponsePosition(ResponseItem item, BlockPos placedBlockPos) {
        return placedBlockPos.add(item.getX(), item.getY(), item.getZ());
    }

    private void handleResponse(HttpResponse<InputStream> response, long requestId, BlockPos origin) {
        LOGGER.info("Response status code: " + response.statusCode());

        if (response.statusCode() != 200) {
            LOGGER.error("Error sending HTTP request");
            return;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && generating) {
                // Ignore responses from old requests
                if (requestId != currentRequestId) {
                    return;
                }
                ResponseItem responseItem = gson.fromJson(line, ResponseItem.class);
                BlockPos position = calculateResponsePosition(responseItem, origin);
                responseItem = new ResponseItem(responseItem.getBlockState(), position.getX(), position.getY(),
                        position.getZ());
                responseQueue.offer(responseItem);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling the streaming response", e);
        } finally {
            generating = false;
        }
    }

    private void cancelCurrentRequest() {
        if (currentRequestFuture != null) {
            currentRequestFuture.cancel(true);
        }
        generating = false;
    }

    private void clearResponses() {
        responseQueue.clear();
    }

    public void stop() {
        currentRequestId++;
        cancelCurrentRequest();
        clearResponses();
    }
}