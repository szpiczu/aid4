package org.example.app;

import java.time.LocalDate;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.List;

public class App {
    public static void main(String[] args) throws IOException, InterruptedException {
        String apiKey = System.getenv("PEOPLE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Set PEOPLE_API_KEY environment variable");
            System.exit(1);
        }

        Path cachePath = Path.of(System.getProperty("user.home"), ".aid4", "people.cache");
        PeopleRepository repository = new PeopleRepository(
                HttpClient.newHttpClient(),
                cachePath,
                apiKey
        );

        List<Person> people = repository.getPeople();
        people = PersonFilter.apply(people, LocalDate.of(2026, 1, 1));
        System.out.printf("Filtered to %d people%n", people.size());
        people.forEach(System.out::println);
    }
}