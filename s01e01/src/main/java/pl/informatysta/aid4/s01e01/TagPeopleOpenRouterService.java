package pl.informatysta.aid4.s01e01;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class TagPeopleOpenRouterService implements TagPeopleLLMService {
    private static final String SYSTEM_PROMPT = """
Jesteś precyzyjnym asystentem do analizy i kategoryzacji profili zawodowych.

### Definicje tagów:
- **IT** — Praca związana z komputerami, programowaniem, systemami, technologiami informatycznymi. Użyj, gdy głównym obszarem jest technologia/programowanie/systemy.
- **transport** — Organizacja, planowanie, spedycja i logistyka transportowa oraz fizyczny przewóz towarów i osób (wybór środków i metod transportu, negocjacje z przewoźnikami, zarządzanie dokumentacją przewozową). Użyj, gdy praca dotyczy przewozu lub organizacji przewozu towarów/osób. Negocjacje z przewoźnikami NIE czynią pracy z 'praca z ludźmi'.
- **edukacja** — Praca związana z nauczaniem, uczeniem, kształceniem, edukacją.
- **medycyna** — Praca związana z opieką zdrowotną, zdrowiem, leczeniem, diagnostyką medyczną.
- **praca z ludźmi** — Użyj TYLKO, gdy głównym aspektem pracy jest bezpośrednia interakcja z ludźmi (sprzedaż, obsługa klienta, zarządzanie personelem, nauczanie, terapia). NIE używaj, gdy kontakt z ludźmi jest tylko pobocznym elementem pracy należącej do innej dziedziny (np. negocjacje z przewoźnikami w logistyce).
- **praca z pojazdami** — Użyj przy fizycznym kierowaniu pojazdami lub ich naprawie/obsłudze technicznej (kierowca, mechanik). NIE używaj dla ról organizacyjno-logistycznych, które obejmuje tag 'transport'.
- **praca fizyczna** — Użyj, gdy praca wymaga znacznego wysiłku fizycznego i pracy mięśni.

### Priorytet tagów:
- Tagi dziedzinowe (transport, IT, medycyna, edukacja) mają pierwszeństwo przed tagami ogólnymi (praca z ludźmi, praca z pojazdami, praca fizyczna).
- Tag 'praca z ludźmi' przypisuj TYLKO wtedy, gdy głównym aspektem pracy jest bezpośrednia interakcja z ludźmi (sprzedaż, obsługa klienta, zarządzanie personelem, nauczanie, terapia). Nie przypisuj go, gdy kontakt z ludźmi jest tylko pobocznym elementem pracy z innej dziedziny.
- Tag 'transport' obejmuje organizację, planowanie, spedycję i logistykę transportową, również gdy wiążą się z negocjacjami z przewoźnikami. Nie zastępuj go tagiem 'praca z ludźmi' ani 'praca z pojazdami'.
- Tag 'praca z pojazdami' przypisuj tylko przy fizycznym kierowaniu pojazdami lub ich naprawie, nie przy roli organizacyjno-logistycznej.

### Przykład:
Opis: "Kluczowy gracz w świecie, gdzie towary muszą docierać do celu szybko i sprawnie. Odpowiada za wybór najlepszych metod transportu, negocjacje z przewoźnikami i zarządzanie dokumentacją."
Oczekiwane tagi: ["transport"]
            """;

    private static final String USER_PROMPT_TEMPLATE = """
Classify each job description with ALL applicable tags from the JSON Schema.

Job descriptions:
%s
            """;

    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String DEFAULT_MODEL = "openai/gpt-4o-mini";

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;

    private final JsonNode responseSchema;

    public TagPeopleOpenRouterService(String apiKey) {
        this(apiKey, DEFAULT_MODEL, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    TagPeopleOpenRouterService(String apiKey, String model, HttpClient httpClient) {
        Objects.requireNonNull(apiKey);
        Objects.requireNonNull(model);
        Objects.requireNonNull(httpClient);
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = httpClient;

        this.responseSchema = loadSchema();
    }

    @Override
    public List<TaggedPerson> tagPeople(List<Person> people) {
        if (people.isEmpty()) {
            return List.of();
        }

        Map<String, Person> fingerprintIndex = new LinkedHashMap<>();
        var jobLinesBuilder = new StringBuilder();
        for (Person p : people) {
            String fp = p.fingerprint();
            fingerprintIndex.put(fp, p);
            jobLinesBuilder.append(fp + ": " + p.job() + "\n");
        }

        var mapper = JsonMapper.builder().build();

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("response_format", Map.of("type", "json_schema", "json_schema", this.responseSchema));

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content",
                        SYSTEM_PROMPT),
                Map.of("role", "user", "content",
                        USER_PROMPT_TEMPLATE.formatted(jobLinesBuilder.toString())));
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0);
        requestBody.put("reasoning", Map.of("effort", "medium"));

        String requestJson = toJson(mapper, requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(10))
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        String responseBody;
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException(
                        "OpenRouter API returned status " + response.statusCode() + ": " + response.body());
            }
            responseBody = response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("OpenRouter API call interrupted", e);
        } catch (IOException e) {
            throw new RuntimeException("OpenRouter API call failed", e);
        }

        Map<String, Object> responseData = fromJson(mapper, responseBody,
                new TypeReference<Map<String, Object>>() {
                });
        List<Map<String, Object>> choices = castList(responseData.get("choices"));
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("No choices in OpenRouter response");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        Map<String, Object> resultBody = fromJson(mapper, content,
                new TypeReference<Map<String, Object>>() {
                });
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

    private JsonNode loadSchema() {
        try (InputStream is = getClass().getResourceAsStream("/tags-schema.json")) {
            if (is == null) {
                throw new RuntimeException("Resource /tags-schema.json not found");
            }
            var mapper = JsonMapper.builder().build();
            return mapper.readTree(is);
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