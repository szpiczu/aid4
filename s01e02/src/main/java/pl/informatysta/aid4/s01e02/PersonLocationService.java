package pl.informatysta.aid4.s01e02;

import java.util.List;

public interface PersonLocationService {
    List<Coordinates> getRecentLocations(SimplePerson person);
}
