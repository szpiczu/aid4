package org.example.app;

import java.util.List;

public record TaggedPerson(Person person, List<String> tags) {
}