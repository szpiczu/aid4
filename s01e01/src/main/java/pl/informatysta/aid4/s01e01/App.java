package pl.informatysta.aid4.s01e01;

import java.time.LocalDate;
import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import io.github.cdimascio.dotenv.Dotenv;

public class App {
    public static void main(String[] args) throws IOException, InterruptedException {
        String dotenvDir = new File(".env").exists() ? "./" : "../";
        var dotenv = Dotenv.configure()
            .directory(dotenvDir)
            .ignoreIfMissing()
            .load();

        String apiKey = dotenv.get("AIDEVS_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Set AIDEVS_API_KEY environment variable");
            System.exit(1);
        }

        Path cachePath = Path.of(System.getProperty("user.home"), ".aid4", "cache", "people.cache");
        PeopleRepository repository = new PeopleRepository(
                HttpClient.newHttpClient(),
                cachePath,
                apiKey
        );

        List<Person> people = repository.getPeople();
        System.out.printf("People total: %d", people.size());
        people = PersonFilter.apply(people, LocalDate.of(2026, 1, 1));
        System.out.printf("People filtered to %d people%n", people.size());
        people.forEach(System.out::println);

        String llmApiKey = dotenv.get("OPENROUTER_API_KEY");
        if (llmApiKey == null || llmApiKey.isBlank()) {
            System.err.println("Set OPENROUTER_API_KEY environment variable");
            System.exit(1);
        }

        var taggedCachePath = Optional.of(Path.of(System.getProperty("user.home"), ".aid4", "cache", "tagged.cache"));
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

        var taggedWithTransport = tagged
            .stream().filter(tp -> {
                return tp.tags().contains("transport");
            }).toList();


        System.out.println("Tagged with transport:");
        taggedWithTransport.forEach(System.out::println);

        var verificationService = new VerificationService(HttpClient.newHttpClient(), apiKey);
        String result = verificationService.verify(taggedWithTransport);
        System.out.printf("%nVerification result: %s%n", result);
    }
}