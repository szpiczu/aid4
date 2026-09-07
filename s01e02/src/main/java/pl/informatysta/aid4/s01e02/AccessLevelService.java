package pl.informatysta.aid4.s01e02;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

import tools.jackson.databind.json.JsonMapper;

public class AccessLevelService {

    private static final String ENDPOINT = "https://hub.ag3nts.org/api/accesslevel";

    private final HttpClient httpClient;
    private final String apiKey;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public AccessLevelService(HttpClient httpClient, String apiKey) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.apiKey = Objects.requireNonNull(apiKey);
    }

    public AccessLevel get(SimplePerson person) {
        try {
            AccessLevelRequest body = new AccessLevelRequest(apiKey, person.name(), person.surname(), person.birthDate().getYear());
            String requestJson = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return mapper.readValue(response.body(), AccessLevel.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to get access level", e);
        }
    }

    private record AccessLevelRequest(String apikey, String name, String surname, int birthYear) {
    }
}