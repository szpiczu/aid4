package pl.informatysta.aid4.s01e01;

import java.util.List;

public record TaggedPerson(Person person, List<String> tags) {
}