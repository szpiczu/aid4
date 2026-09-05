package org.example.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

public class TagPeopleService {
    private final Optional<Path> cachePath;
    private final TagPeopleLLMService llmService;

    public TagPeopleService(Optional<Path> cachePath, TagPeopleLLMService llmService) {
        this.cachePath = cachePath;

        Objects.requireNonNull(llmService);
        this.llmService = llmService;
    }

    public List<TaggedPerson> tagPeople(List<Person> people) {
        if (cachePath.isPresent()) {
            var cacheFile = cachePath.get();
            if (Files.exists(cacheFile)) {
                try {
                    var mapper = createMapper();
                    return mapper.readValue(cacheFile.toFile(), new TypeReference<List<TaggedPerson>>() {});
                } catch (Exception e) {
                    // fall through to LLM
                }
            }
        }

        var result = llmService.tagPeople(people);

        if (cachePath.isPresent()) {
            try {
                var mapper = createMapper();
                var cacheFile = cachePath.get();
                Files.createDirectories(cacheFile.getParent());
                mapper.writerWithDefaultPrettyPrinter().writeValue(cacheFile.toFile(), result);
            } catch (Exception e) {
                // cache write failure is non-fatal
            }
        }

        return result;
    }

    private static JsonMapper createMapper() {
        var localDateModule = new SimpleModule();
        localDateModule.addSerializer(LocalDate.class, new ValueSerializer<LocalDate>() {
            @Override
            public void serialize(LocalDate value, JsonGenerator gen, SerializationContext serializers) {
                gen.writeString(value.toString());
            }
        });
        localDateModule.addDeserializer(LocalDate.class, new ValueDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) {
                return LocalDate.parse(p.getValueAsString());
            }
        });
        return JsonMapper.builder()
                .addModule(localDateModule)
                .build();
    }
}