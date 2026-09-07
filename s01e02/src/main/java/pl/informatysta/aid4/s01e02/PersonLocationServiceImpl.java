package pl.informatysta.aid4.s01e02;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

public class PersonLocationServiceImpl implements PersonLocationService {

    private static final String ENDPOINT = "https://hub.ag3nts.org/api/location";

    private final HttpClient httpClient;
    private final String apiKey;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Path cacheDir;
    private final boolean forceRefresh;

    public PersonLocationServiceImpl(HttpClient httpClient, String apiKey, Path cacheDir, boolean forceRefresh) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.apiKey = Objects.requireNonNull(apiKey);
        this.cacheDir = Objects.requireNonNull(cacheDir);
        this.forceRefresh = forceRefresh;
    }

    public List<Coordinates> getRecentLocations(SimplePerson person) {
        try {
            Path cachePath = cacheDir.resolve(person.name() + "_" + person.surname() + ".json");
    
            if (!forceRefresh && Files.exists(cachePath)) {
                String json = Files.readString(cachePath);
                System.out.printf("Getting locations for %s from cache...\n", person);
                return parseLocations(json);
            }
    
            System.out.printf("Getting locations for %s from endpoint...\n", person);
            String json = fetchJson(person);
            Files.createDirectories(cachePath.getParent());
            Files.writeString(cachePath, json);
            return parseLocations(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String fetchJson(SimplePerson person) throws IOException, InterruptedException {
        LocationRequest body = new LocationRequest(apiKey, person.name(), person.surname());
        String requestJson = mapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private List<Coordinates> parseLocations(String json) throws IOException {
        return mapper.readValue(json, new TypeReference<List<Coordinates>>() {});
    }

    private record LocationRequest(String apikey, String name, String surname) {
    }
}