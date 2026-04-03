package com.recipe.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Paginated response wrapper for followers or following user lists.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated response containing a list of follower or following users")
public class PagedFollowResponse {

  @Schema(description = "List of users for the requested page")
  private List<FollowUserResponse> users;

  @Schema(description = "Cursor token to pass as 'pageToken' to retrieve the next page, "
      + "null when there are no more pages")
  private String nextPageToken;
}
