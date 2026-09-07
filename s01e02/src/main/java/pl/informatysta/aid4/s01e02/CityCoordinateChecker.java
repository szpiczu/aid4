package pl.informatysta.aid4.s01e02;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class CityCoordinateChecker {

    private static final String ENDPOINT_TEMPLATE = "https://nominatim.openstreetmap.org/search?city=%s&country=Poland&format=json&limit=1";
    private static final String USER_AGENT = "aid4/1.0 (pl.informatysta.aid4)";

    private final HttpClient httpClient;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Path cacheDir;
    private final boolean forceRefresh;

    public CityCoordinateChecker(HttpClient httpClient, Path cacheDir, boolean forceRefresh) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.cacheDir = Objects.requireNonNull(cacheDir);
        this.forceRefresh = forceRefresh;
    }

    public Coordinates fromCity(String cityName) {
        try {
            Path cachePath = cacheDir.resolve(cityName + ".json");
    
            if (!forceRefresh && Files.exists(cachePath)) {
                String json = Files.readString(cachePath);
                System.out.println("Getting city coordinates from cache...");
                return parseCoordinates(json);
            }
    
            System.out.println("Getting city coordinates from endpoint...");
            String json = fetchJson(cityName);
            
            var coordinates = parseCoordinates(json);
            
            Files.createDirectories(cachePath.getParent());
            Files.writeString(cachePath, json);

            return coordinates;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String fetchJson(String cityName) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(cityName, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT_TEMPLATE.formatted(encoded)))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private Coordinates parseCoordinates(String json) throws IOException {
        System.out.println("Nominatim response JSON: " + json);
        List<NominatimResult> results = mapper.readValue(json, new TypeReference<List<NominatimResult>>() {
        });
        if (results.isEmpty()) {
            throw new IllegalArgumentException("No coordinates found for city");
        }
        NominatimResult result = results.getFirst();
        return new Coordinates(Double.parseDouble(result.lat()), Double.parseDouble(result.lon()));
    }

    private record NominatimResult(String lat, String lon) {
    }
}