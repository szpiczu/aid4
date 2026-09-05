package org.example.app;

import java.time.LocalDate;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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

        String llmApiKey = System.getenv("OPENROUTER_API_KEY");
        if (llmApiKey == null || llmApiKey.isBlank()) {
            System.err.println("Set OPENROUTER_API_KEY environment variable");
            System.exit(1);
        }

        var taggedCachePath = Optional.of(Path.of(System.getProperty("user.home"), ".aid4", "tagged.cache"));
        var llmService = new TagPeopleOpenRouterService(llmApiKey);
        var tagger = new TagPeopleService(
                taggedCachePath,
                llmService
        );

        List<TaggedPerson> tagged = tagger.tagPeople(people);
        System.out.printf("%nTagged %d people%n", tagged.size());
        tagged.forEach(tp -> System.out.println(
                tp.person().name() + " " + tp.person().surname() + ": " + tp.tags()
        ));
    }
}