package pl.informatysta.aid4.s01e02;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PowerPlantFetcher {

    private static final String ENDPOINT_TEMPLATE = "https://hub.ag3nts.org/data/%s/findhim_locations.json";

    private final HttpClient httpClient;
    private final String apiKey;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Path cachePath;
    private final boolean forceRefresh;

    public PowerPlantFetcher(HttpClient httpClient, String apiKey, Path cachePath, boolean forceRefresh) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.apiKey = Objects.requireNonNull(apiKey);
        this.cachePath = Objects.requireNonNull(cachePath);
        this.forceRefresh = forceRefresh;
    }

    public List<PowerPlant> fetch() throws IOException, InterruptedException {
        if (!forceRefresh && Files.exists(cachePath)) {
            String json = Files.readString(cachePath);
            System.out.println("Getting power plants from cache...");
            return parsePlants(json);
        }

        System.out.println("Getting power plants from endpoint...");
        String json = fetchJson();
        Files.createDirectories(cachePath.getParent());
        Files.writeString(cachePath, json);
        return parsePlants(json);
    }

    String fetchJson() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT_TEMPLATE.formatted(apiKey)))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private List<PowerPlant> parsePlants(String json) throws IOException {
        PowerPlantResponse wrapper = mapper.readValue(json, PowerPlantResponse.class);
        return wrapper.power_plants.entrySet().stream()
                .map(e -> new PowerPlant(e.getKey(), e.getValue().isActive(), e.getValue().power(), e.getValue().code()))
                .toList();
    }

    private record PowerPlantDto(
            @JsonProperty("is_active") boolean isActive,
            String power,
            String code
    ) {
    }

    private record PowerPlantResponse(Map<String, PowerPlantDto> power_plants) {
    }
}
