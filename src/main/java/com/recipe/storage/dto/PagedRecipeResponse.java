package com.recipe.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Paginated response wrapper for public recipe listings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated response containing public recipes")
public class PagedRecipeResponse {

    @Schema(description = "List of recipes for the requested page")
    private List<RecipeResponse> recipes;

    @Schema(description = "Number of recipes per page", example = "20")
    private int size;

    @Schema(description = "Total number of public recipes", example = "47")
    private long totalCount;

    @Schema(description = "Cursor token to pass as 'pageToken' to retrieve the next page, "
            + "null when there are no more pages")
    private String nextPageToken;
}
