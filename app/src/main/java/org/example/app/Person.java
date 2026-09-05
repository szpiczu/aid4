package org.example.app;

import java.time.LocalDate;
import java.util.Objects;

public record Person(
    String name,
    String surname,
    String gender,
    LocalDate birthDate,
    String birthPlace,
    String birthCountry,
    String job
) {
    public String fingerprint() {
        return Integer.toHexString(Objects.hash(name, surname, birthDate));
    }
}