package pl.informatysta.aid4.s01e02;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PowerPlant(
        String name,
        @JsonProperty("is_active") boolean isActive,
        String power,
        String code
) {
}
