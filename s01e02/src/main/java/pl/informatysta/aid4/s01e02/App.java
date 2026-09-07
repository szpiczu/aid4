package pl.informatysta.aid4.s01e02;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        var powerPlantsPath = Path.of(System.getProperty("user.home"), ".aid4", "cache", "power_plants.cache");
        var powerPlants = new PowerPlantFetcher(HttpClient.newHttpClient(), apiKey, powerPlantsPath, false).fetch();
        powerPlants.forEach(System.out::println);
        
        var personLocationPath = Path.of(System.getProperty("user.home"), ".aid4", "cache", "person_coordinates");
        var locationService = new PersonLocationServiceImpl(HttpClient.newHttpClient(), apiKey, personLocationPath, false);
        
        var cityCoordinatePath = Path.of(System.getProperty("user.home"), ".aid4", "cache", "city_coordinates");
        var cityCoordinateChecker = new CityCoordinateChecker(HttpClient.newHttpClient(), cityCoordinatePath, false);
        var powerPlantsWithCoordinates = powerPlants.stream().map(p -> {
            var coordinates = cityCoordinateChecker.fromCity(p.name());
            return new PowerPlantWithCoordinates(p, coordinates);
        }).toList();
        
        var peoplePath = Path.of(System.getProperty("user.home"), ".aid4", "cache", "tagged.cache");
        var peopleRepository = new PeopleRepository(peoplePath);
        var people = peopleRepository.fetchPeople();

        var proximityService = new ProximityCheckService(locationService, 20);
        var suspectMatches = proximityService.findPowerPlantVisitors(people, powerPlantsWithCoordinates);
        System.out.printf("Potential %s people seen nearby power plant: %n\n", suspectMatches.size());

        var accessLevelService = new AccessLevelService(HttpClient.newHttpClient(), apiKey);
        for (var suspectEntry : suspectMatches) {
            var accessLevel = accessLevelService.get(suspectEntry.getFirst());
            var suspect = suspectEntry.getFirst();
            var answer = new Answer(
                suspect.name(),
                suspect.surname(),
                String.valueOf(accessLevel.accessLevel()),
                suspectEntry.getSecond().code()
            );

            var verificationService = new VerificationService(HttpClient.newHttpClient(), apiKey);
            var result = verificationService.verify(answer);
            System.out.printf("Verification result for answer %s: %s%n", answer, result);
        }
    }
}