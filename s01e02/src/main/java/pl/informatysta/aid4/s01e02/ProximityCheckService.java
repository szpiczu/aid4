package pl.informatysta.aid4.s01e02;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProximityCheckService {
    private PersonLocationService personLocationService;
    private final double threshold;

    public ProximityCheckService(PersonLocationService personLocationService, double threshold) {
        this.personLocationService = Objects.requireNonNull(personLocationService);
        this.threshold = threshold;
    }

    public List<Pair<SimplePerson, PowerPlant>> findPowerPlantVisitors(List<SimplePerson> people,
            List<PowerPlantWithCoordinates> powerPlants) {
        var peopleWithCoordinates = people.stream().collect(
                Collectors.toMap(Function.identity(), personLocationService::getRecentLocations));

        var list = new ArrayList<Pair<SimplePerson, PowerPlant>>();

        for (Entry<SimplePerson, List<Coordinates>> personWithCoordinates : peopleWithCoordinates.entrySet()) {
            var recentLocations = personWithCoordinates.getValue();

            for (Coordinates personLocation : recentLocations) {
                for (PowerPlantWithCoordinates powerPlant : powerPlants) {
                    var isPersonNearby = isProximityMatch(personLocation, powerPlant.coordinates());

                    if (isPersonNearby) {
                        var pair = Pair.of(personWithCoordinates.getKey(), powerPlant.powerPlant());
                        list.add(pair);                        
                    }
                }
            }
        }

        return list;
    }

    private boolean isProximityMatch(Coordinates personLocation, Coordinates powerPlantCoordinates) {
        var distance = GeoUtils.distance(personLocation.latitude(), personLocation.longitude(),
                powerPlantCoordinates.latitude(),
                powerPlantCoordinates.longitude());
                
        return distance <= threshold;
    }
}
