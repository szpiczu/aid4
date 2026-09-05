package org.example.app;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

public class TagPeopleOpenRouterService implements TagPeopleLLMService {
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;

    public TagPeopleOpenRouterService(String apiKey) {
        this(apiKey, DEFAULT_MODEL, HttpClient.newHttpClient());
    }

    public TagPeopleOpenRouterService(String apiKey, String model) {
        this(apiKey, model, HttpClient.newHttpClient());
    }

    TagPeopleOpenRouterService(String apiKey, String model, HttpClient httpClient) {
        Objects.requireNonNull(apiKey);
        Objects.requireNonNull(model);
        Objects.requireNonNull(httpClient);
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = httpClient;
    }

    @Override
    public List<TaggedPerson> tagPeople(List<Person> people) {
        if (people.isEmpty()) {
            return List.of();
        }

        Map<String, Person> fingerprintIndex = new LinkedHashMap<>();
        List<Map<String, String>> inputPairs = new ArrayList<>();
        for (Person p : people) {
            String fp = p.fingerprint();
            fingerprintIndex.put(fp, p);
            Map<String, String> pair = new LinkedHashMap<>();
            pair.put("fingerprint", fp);
            pair.put("job", p.job());
            inputPairs.add(pair);
        }

        String schemaJson = loadSchema();
        var mapper = JsonMapper.builder().build();
        String inputJson = toJson(mapper, inputPairs);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("response_format", Map.of("type", "json_object"));

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content",
                        "Jesteś klasyfikatorem zawodów. Na podstawie opisu pracy (pole \"job\") " +
                        "przypisz odpowiednie tagi z dostępnej listy. " +
                        "Odpowiedź musi być wyłącznie poprawnym JSON zgodnym z podanym schematem.\n" +
                        "Dostępne tagi: IT, transport, edukacja, medycyna, praca z ludźmi, praca z pojazdami, praca fizyczna.\n\n" +
                        "Schema:\n" + schemaJson),
                Map.of("role", "user", "content",
                        "Sklasyfikuj następujące osoby:\n" + inputJson)
        );
        requestBody.put("messages", messages);

        String requestJson = toJson(mapper, requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "http://localhost")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        String responseBody;
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("OpenRouter API returned status " + response.statusCode() + ": " + response.body());
            }
            responseBody = response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("OpenRouter API call interrupted", e);
        } catch (IOException e) {
            throw new RuntimeException("OpenRouter API call failed", e);
        }

        Map<String, Object> responseData = fromJson(mapper, responseBody,
                new TypeReference<Map<String, Object>>() {});
        List<Map<String, Object>> choices = castList(responseData.get("choices"));
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("No choices in OpenRouter response");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        Map<String, Object> resultBody = fromJson(mapper, content,
                new TypeReference<Map<String, Object>>() {});
        List<Map<String, Object>> peopleTags = castList(resultBody.get("people_tags"));

        List<TaggedPerson> tagged = new ArrayList<>();
        for (Map<String, Object> entry : peopleTags) {
            String fp = (String) entry.get("fingerprint");
            List<String> tags = castListOfStrings(entry.get("tags"));
            Person person = fingerprintIndex.get(fp);
            if (person != null) {
                tagged.add(new TaggedPerson(person, tags));
            }
        }

        return tagged;
    }

    private String loadSchema() {
        try (InputStream is = getClass().getResourceAsStream("/tags-schema.json")) {
            if (is == null) {
                throw new RuntimeException("Resource /tags-schema.json not found");
            }
            return new String(is.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load tags schema", e);
        }
    }

    private static String toJson(JsonMapper mapper, Object value) {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    }

    private static <T> T fromJson(JsonMapper mapper, String json, TypeReference<T> typeRef) {
        return mapper.readValue(json, typeRef);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<String> castListOfStrings(Object value) {
        return (List<String>) value;
    }
}