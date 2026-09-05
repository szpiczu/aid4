package org.example.app;

import java.util.List;

public interface TagPeopleLLMService {
    public List<TaggedPerson> tagPeople(List<Person> people);
}
