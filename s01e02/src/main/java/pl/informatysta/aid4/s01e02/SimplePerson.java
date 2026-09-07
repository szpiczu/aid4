package pl.informatysta.aid4.s01e02;

import java.time.LocalDate;

import pl.informatysta.aid4.s01e01.TaggedPerson;

public record SimplePerson(String name, String surname, LocalDate birthDate) {
    static SimplePerson from(TaggedPerson taggedPerson) {
        return new SimplePerson(taggedPerson.person().name(), taggedPerson.person().surname(),
                taggedPerson.person().birthDate());
    }
}
