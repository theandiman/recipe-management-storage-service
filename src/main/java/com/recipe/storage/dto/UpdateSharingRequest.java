package com.recipe.storage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating recipe sharing status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update recipe sharing status")
public class UpdateSharingRequest {

    @NotNull(message = "isPublic field is required")
    @Schema(description = "Whether the recipe should be public", required = true, example = "true")
    @JsonProperty("isPublic")
    private Boolean isPublic;
}
