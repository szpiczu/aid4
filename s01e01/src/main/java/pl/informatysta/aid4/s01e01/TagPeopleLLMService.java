package pl.informatysta.aid4.s01e01;

import java.util.List;

public interface TagPeopleLLMService {
    public List<TaggedPerson> tagPeople(List<Person> people);
}
