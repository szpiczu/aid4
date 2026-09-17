package pl.informatysta.aid4.s01e02;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import pl.informatysta.aid4.s01e01.TaggedPerson;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

public class PeopleRepository {
    private Path peoplePath;

    public PeopleRepository(Path peoplePath) {
        this.peoplePath = peoplePath;
        Objects.requireNonNull(peoplePath);

        if (!this.peoplePath.toFile().exists()) {
            System.err.printf("No people available. File %s does not exist", this.peoplePath);
            System.exit(1);
        }
    }

    List<SimplePerson> fetchPeople() {
        JsonMapper mapper = JsonMapper.builder().build();
        List<TaggedPerson> taggedPeople = mapper.readValue(this.peoplePath.toFile(),
                new TypeReference<List<TaggedPerson>>() {
                });

        return taggedPeople.stream().map(SimplePerson::from).toList();
    }
}
