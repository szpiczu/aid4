package org.example.app;

import java.time.LocalDate;
// 
public record Person(
    String name,
    String surname,
    String gender,
    LocalDate birthDate,
    String birthPlace,
    String birthCountry,
    String job
) {}