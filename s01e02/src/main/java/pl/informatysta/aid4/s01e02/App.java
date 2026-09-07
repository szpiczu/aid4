package pl.informatysta.aid4.s01e02;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.List;

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

        boolean forceRefresh = args.length > 0 && "--force-fetch".equals(args[0]);

        Path cachePath = Path.of(System.getProperty("user.home"), ".aid4", "cache", "power_plants.cache");
        PowerPlantFetcher fetcher = new PowerPlantFetcher(HttpClient.newHttpClient(), apiKey, cachePath, forceRefresh);
        List<PowerPlant> plants = fetcher.fetch();
        System.out.printf("Power plants: %d%n", plants.size());
        plants.forEach(System.out::println);
    }
}
