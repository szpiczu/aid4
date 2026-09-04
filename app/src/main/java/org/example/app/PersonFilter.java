package org.example.app;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

public class PersonFilter {

    public static List<Person> apply(List<Person> people, LocalDate referenceDate) {
        return people.stream()
                .filter(p -> "M".equals(p.gender()))
                .filter(p -> "Grudziądz".equals(p.birthPlace()))
                .filter(p -> {
                    int age = Period.between(p.birthDate(), referenceDate).getYears();
                    return age >= 20 && age <= 40;
                })
                .filter(p -> p.job().toLowerCase().contains("transport"))
                .toList();
    }
}