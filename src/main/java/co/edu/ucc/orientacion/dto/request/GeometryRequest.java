package co.edu.ucc.orientacion.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GeometryRequest(
        @NotNull UUID planoId,
        @NotNull JsonNode geometria) {
}
