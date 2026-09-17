package pl.informatysta.aid4.s01e02;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

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

        var peoplePath = Path.of(System.getProperty("user.home"), ".aid4", "cache", "tagged.cache");
        var peopleRepository = new PeopleRepository(peoplePath);
        peopleRepository.fetchPeople().forEach(System.out::println);
    }
}