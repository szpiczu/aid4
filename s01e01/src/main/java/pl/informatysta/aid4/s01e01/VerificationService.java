package pl.informatysta.aid4.s01e01;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import tools.jackson.databind.json.JsonMapper;

public class VerificationService {
    private static final String API_URL = "https://hub.ag3nts.org/verify";

    private final HttpClient httpClient;
    private final String apiKey;

    public VerificationService(HttpClient httpClient, String apiKey) {
        Objects.requireNonNull(httpClient);
        Objects.requireNonNull(apiKey);
        this.httpClient = httpClient;
        this.apiKey = apiKey;
    }

    public String verify(List<TaggedPerson> taggedPeople) {
        var mapper = JsonMapper.builder().build();

        List<Map<String, Object>> answer = new ArrayList<>();
        for (TaggedPerson tp : taggedPeople) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", tp.person().name());
            entry.put("surname", tp.person().surname());
            entry.put("gender", tp.person().gender());
            entry.put("born", tp.person().birthDate().getYear());
            entry.put("city", tp.person().birthPlace());
            entry.put("tags", tp.tags());
            answer.add(entry);
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("apikey", apiKey);
        requestBody.put("task", "people");
        requestBody.put("answer", answer);

        String requestJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody);
        System.out.println("Verification service request: \n" + requestJson);

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

        if (response.statusCode() != 200) {
            throw new RuntimeException("Verification API returned status " + response.statusCode() + ": " + response.body());
        }

        return response.body();
    }
}