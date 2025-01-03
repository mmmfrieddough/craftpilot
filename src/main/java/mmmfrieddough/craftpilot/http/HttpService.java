package mmmfrieddough.craftpilot.http;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;

import com.google.gson.Gson;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.config.ModConfig;

public class HttpService {
    private static final Logger LOGGER = CraftPilot.LOGGER;
    private static final String API_ENDPOINT = "http://127.0.0.1:8000/complete-structure/";

    private final HttpClient httpClient;
    private final Gson gson;
    private final ConcurrentLinkedQueue<ResponseItem> responseQueue;
    private CompletableFuture<HttpResponse<InputStream>> currentRequestFuture;

    public HttpService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.responseQueue = new ConcurrentLinkedQueue<>();
    }

    public void sendRequest(String[][][] matrix, ModConfig.Model modelConfig) {
        // Cancel any ongoing request
        cancelCurrentRequest();

        Request request = buildRequest(matrix, modelConfig);
        String jsonPayload = gson.toJson(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(API_ENDPOINT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        currentRequestFuture = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

        currentRequestFuture.thenAccept(this::handleResponse)
                .exceptionally(this::handleError);
    }

    public ResponseItem getNextResponse() {
        return responseQueue.poll();
    }

    private Request buildRequest(String[][][] matrix, ModConfig.Model modelConfig) {
        Request request = new Request();
        request.setPlatform("java");
        request.setVersion_number(3700);
        request.setTemperature(modelConfig.temperature);
        request.setStart_radius(modelConfig.startRadius);
        request.setMax_iterations(modelConfig.maxIterations);
        request.setMax_blocks(modelConfig.maxBlocks);
        request.setAir_probability_iteration_scaling(modelConfig.airProbabilityIterationScaling);
        request.setStructure(matrix);
        return request;
    }

    private void handleResponse(HttpResponse<InputStream> response) {
        LOGGER.info("Response status code: " + response.statusCode());

        if (response.statusCode() != 200) {
            LOGGER.error("Error sending HTTP request");
            return;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.info(line);
                ResponseItem responseItem = gson.fromJson(line, ResponseItem.class);
                responseQueue.offer(responseItem);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling the streaming response", e);
        }
    }

    private Void handleError(Throwable e) {
        LOGGER.error("Error sending HTTP request", e);
        return null;
    }

    private void cancelCurrentRequest() {
        if (currentRequestFuture != null && !currentRequestFuture.isDone()) {
            currentRequestFuture.cancel(true);
        }
    }
}