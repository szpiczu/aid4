package org.example.app;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PeopleRepository {

    private static final String ENDPOINT_TEMPLATE = "https://hub.ag3nts.org/data/%s/people.csv";

    private final HttpClient httpClient;
    private final Path cachePath;
    private final String apiKey;

    public PeopleRepository(HttpClient httpClient, Path cachePath, String apiKey) {
        this.httpClient = httpClient;
        this.cachePath = cachePath;
        this.apiKey = apiKey;
    }

    public List<Person> getPeople() throws IOException, InterruptedException {
        if (Files.exists(cachePath)) {
            return parseCsv(Files.readString(cachePath));
        }
        String csv = fetchCsv();
        Files.createDirectories(cachePath.getParent());
        Files.writeString(cachePath, csv);
        return parseCsv(csv);
    }

    String fetchCsv() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT_TEMPLATE.formatted(apiKey)))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    static List<Person> parseCsv(String csv) {
        List<Person> people = new ArrayList<>();
        String[] lines = csv.split("\n", -1);
        // skip header (line 0)
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.isEmpty()) continue;
            people.add(parseLine(line));
        }
        return people;
    }

    private static Person parseLine(String line) {
        String[] fields = line.split(",", -1);
        return new Person(
                fields[0].strip(),
                fields[1].strip(),
                fields[2].strip(),
                LocalDate.parse(fields[3].strip()),
                fields[4].strip(),
                fields[5].strip(),
                fields[6].strip()
        );
    }
}