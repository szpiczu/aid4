package org.example.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TagPeopleServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsFromCacheWhenFileExists() throws Exception {
        var cacheFile = tempDir.resolve("tagged.cache");
        var json = """
                [ {
                  "person" : {
                    "name" : "Alice",
                    "surname" : "Smith",
                    "gender" : "female",
                    "birthDate" : "1990-05-15",
                    "birthPlace" : "London",
                    "birthCountry" : "UK",
                    "job" : "Engineer"
                  },
                  "tags" : ["friendly", "smart"]
                } ]
                """;
        Files.writeString(cacheFile, json);

        var llm = mock(TagPeopleLLMService.class);
        var service = new TagPeopleService(Optional.of(cacheFile), llm);

        var people = List.of(new Person("Alice", "Smith", "female", LocalDate.of(1990, 5, 15), "London", "UK", "Engineer"));
        var result = service.tagPeople(people);

        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).person().name());
        assertEquals(List.of("friendly", "smart"), result.get(0).tags());
        verifyNoInteractions(llm);
    }

    @Test
    void callsLlmOnCacheMissAndWritesFile() throws Exception {
        var cacheFile = tempDir.resolve("tagged.cache");
        var people = List.of(
                new Person("Alice", "Smith", "female", LocalDate.of(1990, 5, 15), "London", "UK", "Engineer")
        );
        var llmResult = List.of(
                new TaggedPerson(people.get(0), List.of("tag1", "tag2"))
        );

        var llm = mock(TagPeopleLLMService.class);
        when(llm.tagPeople(people)).thenReturn(llmResult);

        var service = new TagPeopleService(Optional.of(cacheFile), llm);
        var result = service.tagPeople(people);

        assertSame(llmResult, result);
        assertTrue(Files.exists(cacheFile));
        var cached = Files.readString(cacheFile);
        assertTrue(cached.contains("tag1"));
        assertTrue(cached.contains("Alice"));
        verify(llm, times(1)).tagPeople(people);
    }

    @Test
    void callsLlmWhenCacheIsCorrupt() throws Exception {
        var cacheFile = tempDir.resolve("tagged.cache");
        Files.writeString(cacheFile, "not valid json");

        var people = List.of(
                new Person("Bob", "Jones", "male", LocalDate.of(1985, 11, 2), "Paris", "France", "Doctor")
        );
        var llmResult = List.of(
                new TaggedPerson(people.get(0), List.of("doctor"))
        );

        var llm = mock(TagPeopleLLMService.class);
        when(llm.tagPeople(people)).thenReturn(llmResult);

        var service = new TagPeopleService(Optional.of(cacheFile), llm);
        var result = service.tagPeople(people);

        assertEquals(1, result.size());
        assertEquals("doctor", result.get(0).tags().get(0));
        assertTrue(Files.exists(cacheFile));
        var cached = Files.readString(cacheFile);
        assertTrue(cached.contains("doctor"));
        assertTrue(cached.contains("Bob"));
        verify(llm, times(1)).tagPeople(people);
    }

    @Test
    void doesNotAttemptCacheWhenPathEmpty() {
        var people = List.of(
                new Person("Alice", "Smith", "female", LocalDate.of(1990, 5, 15), "London", "UK", "Engineer")
        );
        var llmResult = List.of(
                new TaggedPerson(people.get(0), List.of("tag"))
        );

        var llm = mock(TagPeopleLLMService.class);
        when(llm.tagPeople(people)).thenReturn(llmResult);

        var service = new TagPeopleService(Optional.empty(), llm);
        var result = service.tagPeople(people);

        assertSame(llmResult, result);
        verify(llm, times(1)).tagPeople(people);
    }
}