package org.example.app;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PersonFilterTest {

    private static final LocalDate REFERENCE = LocalDate.of(2026, 1, 1);

    private final List<Person> all = List.of(
            new Person("Jan", "Kowalski", "M", LocalDate.of(1990, 5, 15), "Grudziądz", "Poland", "transport driver"),
            new Person("Anna", "Nowak", "F", LocalDate.of(1990, 5, 15), "Grudziądz", "Poland", "transport"),
            new Person("Piotr", "Zalewski", "M", LocalDate.of(1980, 1, 1), "Grudziądz", "Poland", "transport"),
            new Person("Marek", "Kos", "M", LocalDate.of(1995, 7, 20), "Gdańsk", "Poland", "transport"),
            new Person("Adam", "Wit", "M", LocalDate.of(1995, 7, 20), "Grudziądz", "Poland", "cook")
    );

    @Test
    void apply_returnsOnlyMatching() {
        List<Person> result = PersonFilter.apply(all, REFERENCE);
        assertEquals(1, result.size());
        assertEquals("Jan", result.get(0).name());
    }

    @Test
    void apply_excludesWrongGender() {
        List<Person> input = List.of(
                new Person("A", "B", "F", LocalDate.of(1995, 1, 1), "Grudziądz", "Poland", "transport")
        );
        assertTrue(PersonFilter.apply(input, REFERENCE).isEmpty());
    }

    @Test
    void apply_excludesWrongBirthPlace() {
        List<Person> input = List.of(
                new Person("A", "B", "M", LocalDate.of(1995, 1, 1), "Gdańsk", "Poland", "transport")
        );
        assertTrue(PersonFilter.apply(input, REFERENCE).isEmpty());
    }

    @Test
    void apply_excludesTooYoung() {
        List<Person> input = List.of(
                new Person("A", "B", "M", LocalDate.of(2007, 1, 1), "Grudziądz", "Poland", "transport")
        );
        assertTrue(PersonFilter.apply(input, REFERENCE).isEmpty());
    }

    @Test
    void apply_excludesTooOld() {
        List<Person> input = List.of(
                new Person("A", "B", "M", LocalDate.of(1985, 1, 1), "Grudziądz", "Poland", "transport")
        );
        assertTrue(PersonFilter.apply(input, REFERENCE).isEmpty());
    }

    @Test
    void apply_includesBoundaryAge20() {
        List<Person> input = List.of(
                new Person("A", "B", "M", LocalDate.of(2006, 1, 1), "Grudziądz", "Poland", "transport")
        );
        assertEquals(1, PersonFilter.apply(input, REFERENCE).size());
    }

    @Test
    void apply_includesBoundaryAge40() {
        List<Person> input = List.of(
                new Person("A", "B", "M", LocalDate.of(1986, 1, 1), "Grudziądz", "Poland", "transport")
        );
        assertEquals(1, PersonFilter.apply(input, REFERENCE).size());
    }

    @Test
    void apply_excludesNonTransportJob() {
        List<Person> input = List.of(
                new Person("A", "B", "M", LocalDate.of(1995, 1, 1), "Grudziądz", "Poland", "cook")
        );
        assertTrue(PersonFilter.apply(input, REFERENCE).isEmpty());
    }
}