package org.example.app;

import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TagPeopleOpenRouterServiceTest {

    @Test
    void tagPeople_returnsTaggedPeopleFromApiResponse() throws Exception {
        var people = List.of(
                new Person("Jan", "Kowalski", "M", LocalDate.of(1990, 5, 15), "Grudziądz", "Poland", "programista Java"),
                new Person("Anna", "Nowak", "F", LocalDate.of(1985, 3, 10), "Gdańsk", "Poland", "lekarz")
        );
        var fp1 = people.get(0).fingerprint();
        var fp2 = people.get(1).fingerprint();

        var mapper = JsonMapper.builder().build();
        var peopleTagsJson = mapper.writeValueAsString(Map.of(
                "people_tags", List.of(
                        Map.of("fingerprint", fp1, "tags", List.of("IT")),
                        Map.of("fingerprint", fp2, "tags", List.of("medycyna", "praca z ludźmi"))
                )
        ));
        var apiResponse = mapper.writeValueAsString(Map.of(
                "choices", List.of(
                        Map.of("message", Map.of("content", peopleTagsJson))
                )
        ));

        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(apiResponse);

        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        var service = new TagPeopleOpenRouterService("test-key", "test-model", httpClient);
        var result = service.tagPeople(people);

        assertEquals(2, result.size());
        assertEquals("Jan", result.get(0).person().name());
        assertEquals(List.of("IT"), result.get(0).tags());
        assertEquals("Anna", result.get(1).person().name());
        assertEquals(List.of("medycyna", "praca z ludźmi"), result.get(1).tags());
    }

    @Test
    void tagPeople_handlesEmptyList() {
        var httpClient = mock(HttpClient.class);
        var service = new TagPeopleOpenRouterService("test-key", "test-model", httpClient);
        var result = service.tagPeople(List.of());
        assertTrue(result.isEmpty());
        verifyNoInteractions(httpClient);
    }

    @Test
    void tagPeople_skipsUnknownFingerprints() throws Exception {
        var people = List.of(
                new Person("Jan", "Kowalski", "M", LocalDate.of(1990, 5, 15), "Grudziądz", "Poland", "programista Java")
        );
        var fp = people.get(0).fingerprint();

        var mapper = JsonMapper.builder().build();
        var peopleTagsJson = mapper.writeValueAsString(Map.of(
                "people_tags", List.of(
                        Map.of("fingerprint", fp, "tags", List.of("IT")),
                        Map.of("fingerprint", "unknown-fp", "tags", List.of("medycyna"))
                )
        ));
        var apiResponse = mapper.writeValueAsString(Map.of(
                "choices", List.of(
                        Map.of("message", Map.of("content", peopleTagsJson))
                )
        ));

        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(apiResponse);

        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        var service = new TagPeopleOpenRouterService("test-key", "test-model", httpClient);
        var result = service.tagPeople(people);

        assertEquals(1, result.size());
        assertEquals("Jan", result.get(0).person().name());
        assertEquals(List.of("IT"), result.get(0).tags());
    }
}