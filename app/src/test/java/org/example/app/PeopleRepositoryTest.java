package org.example.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PeopleRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void parseCsv() {
        String csv = """
                name,surname,gender,birthDate,birthPlace,birthCountry,job
                Alice,Smith,female,1990-05-15,London,UK,Engineer
                Bob,Jones,male,1985-11-02,Paris,France,Doctor
                """;
        List<Person> people = PeopleRepository.parseCsv(csv);
        assertEquals(2, people.size());

        Person alice = people.get(0);
        assertEquals("Alice", alice.name());
        assertEquals("Smith", alice.surname());
        assertEquals("female", alice.gender());
        assertEquals(LocalDate.of(1990, 5, 15), alice.birthDate());
        assertEquals("London", alice.birthPlace());
        assertEquals("UK", alice.birthCountry());
        assertEquals("Engineer", alice.job());

        Person bob = people.get(1);
        assertEquals("Bob", bob.name());
        assertEquals("Jones", bob.surname());
        assertEquals(LocalDate.of(1985, 11, 2), bob.birthDate());
    }

    @Test
    void parseCsv_skipsEmptyLines() {
        String csv = """
                name,surname,gender,birthDate,birthPlace,birthCountry,job
                Alice,Smith,female,1990-05-15,London,UK,Engineer

                Bob,Jones,male,1985-11-02,Paris,France,Doctor
                """;
        List<Person> people = PeopleRepository.parseCsv(csv);
        assertEquals(2, people.size());
    }

    @Test
    void getPeople_usesCacheWhenFileExists() throws Exception {
        String csv = """
                name,surname,gender,birthDate,birthPlace,birthCountry,job
                Cached,User,female,2000-01-01,Town,Country,Role
                """;
        Path cacheFile = tempDir.resolve("people.cache");
        Files.writeString(cacheFile, csv);

        HttpClient httpClient = mock(HttpClient.class);
        PeopleRepository repo = new PeopleRepository(httpClient, cacheFile, "dummy");
        List<Person> people = repo.getPeople();

        assertEquals(1, people.size());
        assertEquals("Cached", people.get(0).name());
        verify(httpClient, never()).send(any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getPeople_writesCacheOnMiss() throws Exception {
        Path cacheFile = tempDir.resolve("people.cache");
        String csv = """
                name,surname,gender,birthDate,birthPlace,birthCountry,job
                Remote,User,male,1995-06-15,City,Country,Job
                """;

        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.body()).thenReturn(csv);
        
        HttpClient httpClient = mock(HttpClient.class);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        PeopleRepository repo = new PeopleRepository(httpClient, cacheFile, "dummy");
        List<Person> people = repo.getPeople();

        assertEquals(1, people.size());
        assertEquals("Remote", people.get(0).name());
        assertTrue(Files.exists(cacheFile));
        String cached = Files.readString(cacheFile);
        assertTrue(cached.contains("Remote"));
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
}