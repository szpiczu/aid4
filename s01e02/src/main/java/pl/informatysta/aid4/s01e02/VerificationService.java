package pl.informatysta.aid4.s01e02;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import tools.jackson.databind.json.JsonMapper;

public class VerificationService {
    private static final String API_URL = "https://hub.ag3nts.org/verify";

    private final HttpClient httpClient;
    private final String apiKey;

    public VerificationService(HttpClient httpClient, String apiKey) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.apiKey = Objects.requireNonNull(apiKey);
    }

    public String verify(Answer answer) {
        var mapper = JsonMapper.builder().build();

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("apikey", apiKey);
        requestBody.put("task", "findhim");
        requestBody.put("answer", answer);

        String requestJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Verification call interrupted", e);
        } catch (IOException e) {
            throw new RuntimeException("Verification call failed", e);
        }

        if (response.statusCode() == 200 || response.statusCode() == 400) {
            return response.body();
        } else {
            throw new RuntimeException("Verification API returned status " + response.statusCode() + ": " + response.body());
        }
    }
}